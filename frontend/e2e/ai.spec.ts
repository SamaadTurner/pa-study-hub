import { test, expect, type Page } from '@playwright/test'
import { newUser, register, createDeckWithCards, startExam, answerAllAndFinish } from './helpers'

// The AI features need an API key, and the test backend has none, which is the
// same as a server where they are turned off. To test the pages with AI on,
// these tests answer the /api/v1/ai/* requests themselves. Everything else
// (logging in, the deck, saving the cards, the exam) uses the real backend.

async function turnAiOn(page: Page, remainingToday = 20) {
  await page.route('**/api/v1/ai/status', route =>
    route.fulfill({ json: { enabled: true, model: 'test-model', remainingToday } }))
}

test.describe('AI features', () => {
  test('with AI off on the server, the AI buttons are not shown', async ({ page }) => {
    await register(page, newUser())
    await createDeckWithCards(page, 'No AI here', [{ front: 'Q', back: 'A' }])

    await expect(page.getByRole('button', { name: '+ Add Card' })).toBeVisible()
    await expect(page.getByRole('button', { name: 'Suggest cards' })).toBeHidden()

    await startExam(page, { category: 'Cardiology', questions: 5 })
    await answerAllAndFinish(page, 5)
    await expect(page.getByRole('heading', { name: 'Review your answers' })).toBeVisible()
    await expect(page.getByRole('button', { name: /Explain/ })).toHaveCount(0)
  })

  test('suggested cards can be edited and left out, and only the kept ones are saved', async ({ page }) => {
    await register(page, newUser())
    await turnAiOn(page)

    let sentBody: { notes: string; count: number } | null = null
    await page.route('**/api/v1/ai/flashcards', async route => {
      sentBody = route.request().postDataJSON()
      await route.fulfill({
        json: {
          cards: [
            { front: 'What causes a diastolic rumble at the apex?', back: 'Mitral stenosis' },
            { front: 'A card I do not want', back: 'Leave me out' },
            { front: 'Which murmur radiates to the carotids?', back: 'Aortic stenosis' },
          ],
        },
      })
    })

    await createDeckWithCards(page, 'Murmurs', [{ front: 'Existing card', back: 'Already here' }])
    await page.getByRole('button', { name: 'Suggest cards' }).click()

    const dialog = page.getByRole('dialog')
    await expect(dialog.getByText('20 AI requests left today.')).toBeVisible()
    await dialog.getByLabel('Your notes').fill('Mitral stenosis: diastolic rumble. Aortic stenosis: radiates to carotids.')
    await dialog.getByRole('button', { name: '3', exact: true }).click()
    await dialog.getByRole('button', { name: 'Suggest cards' }).click()

    // The warning comes before anything can be added
    await expect(dialog.getByText(/can contain mistakes/)).toBeVisible()
    expect(sentBody).toEqual({
      notes: 'Mitral stenosis: diastolic rumble. Aortic stenosis: radiates to carotids.',
      count: 3,
    })

    await dialog.getByLabel('Back of card 1').fill('Mitral stenosis (left atrium to left ventricle)')
    await dialog.getByLabel('Keep card 2').uncheck()
    await dialog.getByRole('button', { name: 'Add 2 cards' }).click()

    await expect(dialog).toBeHidden()
    await expect(page.getByText('3 cards')).toBeVisible()
    await expect(page.getByText('What causes a diastolic rumble at the apex?')).toBeVisible()
    await expect(page.getByText('Which murmur radiates to the carotids?')).toBeVisible()
    await expect(page.getByText('A card I do not want')).toBeHidden()

    // The edit was saved, not the original answer
    await page.getByText('What causes a diastolic rumble at the apex?').click()
    await expect(page.getByText('Mitral stenosis (left atrium to left ventricle)')).toBeVisible()

    // After a reload the cards are still there: they were saved by the real backend
    await page.reload()
    await expect(page.getByText('3 cards')).toBeVisible()
  })

  test('the daily limit message from the server is shown', async ({ page }) => {
    await register(page, newUser())
    await turnAiOn(page, 1)
    await page.route('**/api/v1/ai/flashcards', route =>
      route.fulfill({
        status: 429,
        contentType: 'application/problem+json',
        json: { status: 429, detail: 'You have used all 20 AI requests for today. They reset at midnight UTC.' },
      }))

    await createDeckWithCards(page, 'Limit', [{ front: 'Q', back: 'A' }])
    await page.getByRole('button', { name: 'Suggest cards' }).click()

    const dialog = page.getByRole('dialog')
    await expect(dialog.getByText('1 AI request left today.')).toBeVisible()
    await dialog.getByLabel('Your notes').fill('Some notes')
    await dialog.getByRole('button', { name: 'Suggest cards' }).click()

    await expect(dialog.getByRole('alert')).toHaveText(
      'You have used all 20 AI requests for today. They reset at midnight UTC.')
  })

  test('a question on the results page can be explained', async ({ page }) => {
    await register(page, newUser())
    await turnAiOn(page)

    let explainedUrl = ''
    await page.route('**/api/v1/ai/exams/*/questions/*/explanation', async route => {
      explainedUrl = route.request().url()
      await route.fulfill({ json: { explanation: 'B looks right because of the chest pain, but the ECG points to A.' } })
    })

    await startExam(page, { category: 'Cardiology', questions: 5 })
    await answerAllAndFinish(page, 5)
    await expect(page.getByRole('heading', { name: 'Review your answers' })).toBeVisible()

    // One button per question
    const explainButtons = page.getByRole('button', { name: /Explain (my mistake|this question)/ })
    await expect(explainButtons).toHaveCount(5)

    await explainButtons.first().click()

    await expect(page.getByText('AI explanation')).toBeVisible()
    await expect(page.getByText('B looks right because of the chest pain, but the ECG points to A.')).toBeVisible()
    await expect(page.getByText('It can be wrong.')).toBeVisible()
    // The request named this exam (from the page address) and a question id
    const sessionId = page.url().match(/\/exam\/([0-9a-f-]{36})\/results/)![1]
    expect(explainedUrl).toMatch(new RegExp(`/ai/exams/${sessionId}/questions/[0-9a-f-]{36}/explanation$`))
    // The other questions keep their buttons
    await expect(explainButtons).toHaveCount(4)
  })
})
