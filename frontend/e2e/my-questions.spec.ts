import { test, expect } from '@playwright/test'
import {
  newUser,
  register,
  startExam,
  answerAllAndFinish,
  createQuestionsThroughApi,
} from './helpers'

test.describe('Writing your own exam questions', () => {
  test('a question can be written, shared, edited and deleted', async ({ page }) => {
    await register(page, newUser())
    await page.goto('/exam')
    await page.getByRole('button', { name: 'My Questions' }).click()

    await expect(page.getByText('No questions yet')).toBeVisible()
    await page.getByRole('button', { name: 'Write Your First Question' }).click()
    await expect(page.getByRole('heading', { name: 'Write a Question' })).toBeVisible()

    await page.getByLabel('Patient scenario').fill('A 24-year-old woman has palpitations and weight loss.')
    await page.getByLabel(/^Question/).fill('Which lab test should be ordered first?')
    await page.getByLabel('Answer option A').fill('Free T4')
    await page.getByLabel('Answer option B').fill('TSH')
    await page.getByLabel('Answer option C').fill('Thyroid ultrasound')
    await page.getByLabel('Answer option D').fill('Remove me')
    await page.getByRole('button', { name: 'Remove option D' }).click()
    await page.getByLabel('Option B is the right answer').check()
    await page.getByLabel(/^Explanation/).fill('TSH is the best first screening test for thyroid disease.')
    await page.getByLabel(/^Category/).selectOption({ label: 'Endocrinology' })
    await page.getByLabel(/^Task area/).selectOption({ label: 'Diagnostic & lab studies' })
    await page.getByLabel('Share with other users').check()
    await page.getByRole('button', { name: 'Save Question' }).click()

    // Back on the list, with the right answer and the shared label
    await expect(page).toHaveURL(/\/exam\/questions$/)
    const item = page.getByRole('listitem').filter({ hasText: 'Which lab test should be ordered first?' })
    await expect(item).toBeVisible()
    await expect(item.getByText('Right answer: TSH')).toBeVisible()
    await expect(item.getByText('Shared')).toBeVisible()
    await expect(item.getByText('Diagnostic & lab studies')).toBeVisible()

    // Edit: the form opens with what was saved
    await item.getByRole('button', { name: 'Edit' }).click()
    await expect(page.getByRole('heading', { name: 'Edit Question' })).toBeVisible()
    await expect(page.getByLabel('Answer option C')).toHaveValue('Thyroid ultrasound')
    await expect(page.getByLabel('Answer option D')).toHaveCount(0)
    await expect(page.getByLabel('Option B is the right answer')).toBeChecked()
    await page.getByLabel(/^Question/).fill('Which test is the best first step?')
    await page.getByLabel('Share with other users').uncheck()
    await page.getByRole('button', { name: 'Save Changes' }).click()

    const edited = page.getByRole('listitem').filter({ hasText: 'Which test is the best first step?' })
    await expect(edited).toBeVisible()
    await expect(edited.getByText('Shared')).toHaveCount(0)
    await expect(page.getByText('Which lab test should be ordered first?')).toHaveCount(0)

    // Delete after confirming
    await edited.getByRole('button', { name: 'Delete' }).click()
    await page.getByRole('dialog').getByRole('button', { name: 'Delete' }).click()
    await expect(page.getByText('No questions yet')).toBeVisible()
  })

  test('an incomplete question shows what is missing and is not saved', async ({ page }) => {
    await register(page, newUser())
    await page.goto('/exam/questions/new')

    await page.getByLabel('Answer option A').fill('Only one option')
    await page.getByRole('button', { name: 'Save Question' }).click()

    await expect(page.getByText('The question is required')).toBeVisible()
    await expect(page.getByText('Write at least 2 answer options')).toBeVisible()
    await expect(page.getByText('The explanation is required')).toBeVisible()
    await expect(page.getByText('Pick a category', { exact: true }).last()).toBeVisible()
    await expect(page).toHaveURL(/\/exam\/questions\/new$/)

    // Two options but no right answer marked
    await page.getByLabel('Answer option B').fill('Second option')
    await page.getByRole('button', { name: 'Save Question' }).click()
    await expect(page.getByText('Mark the right answer')).toBeVisible()

    await page.goto('/exam/questions')
    await expect(page.getByText('No questions yet')).toBeVisible()
  })

  test('your own questions show up in your exams', async ({ page }) => {
    await register(page, newUser())
    // No starter questions exist for hematology, so the exam can only use these
    const stems = [1, 2, 3, 4, 5].map(n => `Hematology question ${n} ${Date.now()}`)
    await createQuestionsThroughApi(page, stems.map(stem => ({ stem, category: 'HEMATOLOGY' })))

    await startExam(page, { category: 'Hematology', questions: 5 })
    await answerAllAndFinish(page, 5)

    for (const stem of stems) {
      await expect(page.getByText(stem, { exact: true })).toBeVisible()
    }
    await expect(page.getByText('· Written by you')).toHaveCount(5)
  })

  test('questions others shared are only used when the exam asks for them', async ({ page, browser }) => {
    // Another user writes and shares five psychiatry questions
    const authorContext = await browser.newContext()
    const authorPage = await authorContext.newPage()
    await register(authorPage, newUser('Jordan'))
    await createQuestionsThroughApi(authorPage, [1, 2, 3, 4, 5].map(n => ({
      stem: `Shared psychiatry question ${n}`,
      category: 'PSYCHIATRY',
      isPublic: true,
    })))
    await authorContext.close()

    await register(page, newUser())

    // Without shared questions there are none for psychiatry
    await page.goto('/exam')
    await page.getByLabel('Psychiatry', { exact: true }).check()
    await page.getByRole('button', { name: '5', exact: true }).click()
    await page.getByRole('button', { name: /Start Exam/ }).click()
    await expect(page.getByText(/Not enough questions available/)).toBeVisible()

    // With them the exam starts, and the results mark the questions as not checked
    await startExam(page, { category: 'Psychiatry', questions: 5, includeShared: true })
    await answerAllAndFinish(page, 5)
    await expect(page.getByText('· Shared by another user, not checked')).toHaveCount(5)
  })
})
