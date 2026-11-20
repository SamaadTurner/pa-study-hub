import { test, expect } from '@playwright/test'
import { newUser, register, startExam, answerAllAndFinish, createQuestionsThroughApi } from './helpers'

test.describe('Shared questions', () => {
  test('a shared question can be reported from the results, once', async ({ page, browser }) => {
    // Someone shares five reproductive health questions
    const authorContext = await browser.newContext()
    const authorPage = await authorContext.newPage()
    await register(authorPage, newUser('Jordan'))
    await createQuestionsThroughApi(authorPage, [1, 2, 3, 4, 5].map(n => ({
      stem: `Shared reproductive question ${n}`, category: 'REPRODUCTIVE', isPublic: true,
    })))
    await authorContext.close()

    await register(page, newUser())
    await startExam(page, { category: 'Reproductive', questions: 5, includeShared: true })
    await answerAllAndFinish(page, 5)

    await page.getByRole('button', { name: 'Report this question' }).first().click()
    const form = page.getByRole('form', { name: 'Report this question' })
    await form.getByLabel('The question is unclear').check()
    await form.getByLabel(/Note for the author/).fill('Two options say the same thing')
    await form.getByRole('button', { name: 'Send report' }).click()
    await expect(page.getByText('You reported this question. Thank you.')).toBeVisible()

    // Still marked after a reload, and the other four can still be reported
    await page.reload()
    await expect(page.getByText('You reported this question. Thank you.')).toHaveCount(1)
    await expect(page.getByRole('button', { name: 'Report this question' })).toHaveCount(4)
  })

  test('the author sees reports, and when a question is left out of shared exams', async ({ page }) => {
    await register(page, newUser())
    // Only the list is faked here; the server side is in SharedQuestionsIntegrationTest
    await page.route('**/api/v1/questions/mine*', route => route.fulfill({
      json: {
        content: [{
          id: '00000000-0000-0000-0000-000000000001', stem: 'My reported question', clinicalVignette: null,
          answerOptions: [{ text: 'A', isCorrect: true }, { text: 'B', isCorrect: false }],
          explanation: 'Because', category: 'CARDIOLOGY', taskArea: 'BASIC_SCIENCE', difficulty: 'EASY',
          isPublic: true, hiddenFromShared: true, earlierReportCount: 2, createdAt: '2026-11-19T10:00:00', updatedAt: '2026-11-19T10:00:00',
          reports: [
            { reason: 'WRONG_ANSWER', note: 'B is right', createdAt: '2026-11-19T11:00:00' },
            { reason: 'UNCLEAR', note: null, createdAt: '2026-11-19T12:00:00' },
            { reason: 'OTHER', note: null, createdAt: '2026-11-19T13:00:00' },
          ],
        }],
        totalElements: 1, totalPages: 1, number: 0, size: 100, last: true,
      },
    }))

    await page.goto('/exam/questions')
    await expect(page.getByText('3 reports')).toBeVisible()
    await expect(page.getByText(/Left out of other people's exams because of the reports/)).toBeVisible()
    await expect(page.getByText('"B is right"')).toBeVisible()
    await expect(page.getByText('2 reports on earlier versions, still counted.')).toBeVisible()
  })

  test('the shared questions can be searched', async ({ page, browser }) => {
    const token = `searchme${Date.now()}`
    const authorContext = await browser.newContext()
    const authorPage = await authorContext.newPage()
    await register(authorPage, newUser('Jordan'))
    await createQuestionsThroughApi(authorPage, [{ stem: `Which test? ${token}`, category: 'PEDIATRICS', isPublic: true }])
    await authorContext.close()

    await register(page, newUser())
    await page.goto('/exam/shared')
    await page.getByLabel('Search').fill(token.toUpperCase())
    await expect(page.getByText('Showing 1 of 1')).toBeVisible()
    await expect(page.getByText(`Which test? ${token}`)).toBeVisible()

    await page.getByLabel('Search').fill(`${token}-nothing`)
    await expect(page.getByText('No shared question matches that search.')).toBeVisible()
  })

  test('shared questions can be browsed by topic, with the answer hidden until asked for', async ({ page, browser }) => {
    const stem = `Browse me ${Date.now()}`
    const authorContext = await browser.newContext()
    const authorPage = await authorContext.newPage()
    await register(authorPage, newUser('Jordan'))
    await createQuestionsThroughApi(authorPage, [{ stem, category: 'NEPHROLOGY', isPublic: true }])
    await authorContext.close()

    await register(page, newUser())
    await page.goto('/exam/questions')
    await page.getByRole('button', { name: 'Browse shared' }).click()
    await expect(page.getByRole('heading', { name: 'Shared Questions' })).toBeVisible()
    await page.getByLabel('Topic').selectOption({ label: 'Nephrology' })

    // Newest first, so the question just written is at the top
    const card = page.getByRole('listitem').filter({ hasText: stem })
    await expect(card).toBeVisible()
    await expect(card.getByText('(right answer)')).toHaveCount(0)
    await card.getByRole('button', { name: 'Show answer and explanation' }).click()
    await expect(card.getByText('A. Right answer (right answer)')).toBeVisible()
    await expect(card.getByText(`Explanation for: ${stem}`)).toBeVisible()
  })
})
