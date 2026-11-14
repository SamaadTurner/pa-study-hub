import { test, expect, type Page } from '@playwright/test'
import { newUser, register, startExam, pickOption, answerAllAndFinish } from './helpers'

// The small labels on the results page: Correct, Incorrect or Not answered
function resultBadges(page: Page, label: string) {
  return page.locator('span', { hasText: new RegExp(`^${label}$`) })
}

// Makes the page believe a timed exam has this many seconds left, so a test does
// not have to wait out a 15 minute exam. Only the load of the exam is changed.
// Everything the page then does (saving the answer, finishing) hits the real backend.
async function setSecondsLeft(page: Page, seconds: number) {
  await page.route(/\/api\/v1\/exams\/[0-9a-f-]{36}$/, async route => {
    if (route.request().method() !== 'GET') {
      await route.continue()
      return
    }
    const response = await route.fetch()
    const exam = await response.json()
    exam.timeRemainingSeconds = seconds
    await route.fulfill({ response, json: exam })
  })
}

test.describe('Practice exams', () => {
  test('an exam can be taken from start to results', async ({ page }) => {
    await register(page, newUser())
    await startExam(page, { category: 'Cardiology', questions: 5 })

    // No clock on an untimed exam
    await expect(page.getByText('⏱')).toBeHidden()
    await expect(page.getByText('0 of 5 answered')).toBeVisible()

    await answerAllAndFinish(page, 5)

    await expect(page.getByRole('heading', { name: 'Review your answers' })).toBeVisible()

    // Every question is in the review, each with an explanation
    await expect(page.getByText(/^Question \d$/)).toHaveCount(5)
    await expect(page.getByText('Explanation', { exact: true })).toHaveCount(5)

    // The score at the top matches the labels on the questions
    const scoreText = await page.getByText(/^\d\/5$/).textContent()
    const correct = Number(scoreText!.split('/')[0])
    await expect(resultBadges(page, 'Correct')).toHaveCount(correct)
    await expect(resultBadges(page, 'Incorrect')).toHaveCount(5 - correct)
    await expect(resultBadges(page, 'Not answered')).toHaveCount(0)
    await expect(page.getByText(`${correct * 20}%`).first()).toBeVisible()
  })

  test('an answer is kept when going back to a question', async ({ page }) => {
    await register(page, newUser())
    await startExam(page, { category: 'Pharmacology', questions: 5 })

    // Option C on question 1, then on to question 2 and back
    await pickOption(page, 1, 5, 2)
    await page.getByRole('button', { name: 'Next Question →' }).click()
    await expect(page.getByText('Question 2 / 5')).toBeVisible()
    await expect(page.getByRole('radio', { checked: true })).toHaveCount(0)

    await page.getByRole('button', { name: '← Previous' }).click()

    await expect(page.getByText('Question 1 / 5')).toBeVisible()
    await expect(page.getByRole('radio').nth(2)).toBeChecked()
    await expect(page.getByText('1 of 5 answered')).toBeVisible()
  })

  test('a reload continues the exam at the first question without an answer', async ({ page }) => {
    await register(page, newUser())
    await startExam(page, { category: 'Pulmonology', questions: 5 })

    await pickOption(page, 1, 5, 1)
    await page.getByRole('button', { name: 'Next Question →' }).click()
    await pickOption(page, 2, 5, 3)
    await page.getByRole('button', { name: 'Next Question →' }).click()
    await expect(page.getByText('Question 3 / 5')).toBeVisible()

    await page.reload()

    await expect(page.getByText('Question 3 / 5')).toBeVisible()
    await expect(page.getByText('2 of 5 answered')).toBeVisible()

    // The earlier answers are still the ones that were picked
    await page.getByRole('button', { name: '← Previous' }).click()
    await expect(page.getByText('Question 2 / 5')).toBeVisible()
    await expect(page.getByRole('radio').nth(3)).toBeChecked()
  })

  test('finishing with unanswered questions asks first and counts them as wrong', async ({ page }) => {
    await register(page, newUser())
    await startExam(page, { category: 'Cardiology', questions: 5 })

    // Answer the first three, skip the last two
    for (let number = 1; number <= 3; number++) {
      await pickOption(page, number, 5)
      await page.getByRole('button', { name: 'Next Question →' }).click()
    }
    await expect(page.getByText('Question 4 / 5')).toBeVisible()
    await page.getByRole('button', { name: 'Skip →' }).click()
    await expect(page.getByText('Question 5 / 5')).toBeVisible()

    // Saying no to the warning keeps the exam open
    page.once('dialog', async dialog => {
      expect(dialog.message()).toContain('You have 2 unanswered questions')
      await dialog.dismiss()
    })
    await page.getByRole('button', { name: 'Finish Exam' }).click()
    await expect(page.getByText('Question 5 / 5')).toBeVisible()

    // Saying yes finishes it
    page.once('dialog', dialog => dialog.accept())
    await page.getByRole('button', { name: 'Finish Exam' }).click()
    await expect(page).toHaveURL(/\/exam\/[0-9a-f-]{36}\/results$/)

    await expect(resultBadges(page, 'Not answered')).toHaveCount(2)
    // The score is out of all 5 questions, not only the 3 that were answered
    await expect(page.getByText(/^\d\/5$/)).toBeVisible()
  })

  test('a finished exam is in the history and opens its results again', async ({ page }) => {
    await register(page, newUser())

    await page.goto('/exam/history')
    await expect(page.getByText('No exams yet')).toBeVisible()

    await startExam(page, { category: 'Cardiology', questions: 5 })
    await answerAllAndFinish(page, 5)
    const resultsUrl = page.url()

    await page.getByRole('button', { name: 'View History' }).click()
    await expect(page).toHaveURL(/\/exam\/history$/)
    await expect(page.getByText('1 exam completed')).toBeVisible()

    // The row shows the category and a score, and clicking it opens the results
    const row = page.getByRole('button', { name: /Cardiology/ })
    await expect(row).toContainText('%')
    await row.click()

    await expect(page).toHaveURL(resultsUrl)
    await expect(page.getByRole('heading', { name: 'Review your answers' })).toBeVisible()
  })

  test('an exam that was left open can be continued from the history', async ({ page }) => {
    await register(page, newUser())
    await startExam(page, { category: 'Pharmacology', questions: 5 })
    const examUrl = page.url()

    await pickOption(page, 1, 5)
    await page.getByRole('button', { name: 'Next Question →' }).click()
    await expect(page.getByText('Question 2 / 5')).toBeVisible()

    // Walk away through the menu instead of pressing Exit
    await page.getByRole('link', { name: 'Dashboard' }).first().click()
    await expect(page).toHaveURL(/\/dashboard$/)

    await page.goto('/exam/history')
    await expect(page.getByText('0 exams completed')).toBeVisible()
    await page.getByRole('button', { name: /In progress/ }).click()

    await expect(page).toHaveURL(examUrl)
    await expect(page.getByText('Question 2 / 5')).toBeVisible()
    await expect(page.getByText('1 of 5 answered')).toBeVisible()
  })

  test('Exit abandons the exam after asking', async ({ page }) => {
    await register(page, newUser())
    await startExam(page, { category: 'Pulmonology', questions: 5 })
    const examUrl = page.url()

    // Saying no keeps the exam open
    page.once('dialog', dialog => dialog.dismiss())
    await page.getByRole('button', { name: 'Exit' }).click()
    await expect(page.getByText('Question 1 / 5')).toBeVisible()

    page.once('dialog', async dialog => {
      expect(dialog.message()).toContain('It will be abandoned')
      await dialog.accept()
    })
    await page.getByRole('button', { name: 'Exit' }).click()
    await expect(page).toHaveURL(/\/exam$/)

    // It is listed as abandoned, has no score and cannot be opened
    await page.goto('/exam/history')
    await expect(page.getByText('0 exams completed')).toBeVisible()
    await expect(page.getByRole('button', { name: /Abandoned/ })).toBeDisabled()

    // The old link goes back to the setup page instead of showing the questions
    await page.goto(examUrl)
    await expect(page).toHaveURL(/\/exam$/)
    await expect(page.getByRole('heading', { name: 'Practice Exam' })).toBeVisible()
  })

  test('a category without enough questions gives a message and no exam', async ({ page }) => {
    await register(page, newUser())
    await page.goto('/exam')

    // There are no starter questions for dermatology
    await page.getByLabel('Dermatology', { exact: true }).check()
    await page.getByRole('button', { name: /Start Exam/ }).click()

    await expect(page.getByText(/Not enough questions available/)).toBeVisible()
    await expect(page).toHaveURL(/\/exam$/)

    await page.goto('/exam/history')
    await expect(page.getByText('No exams yet')).toBeVisible()
  })

  test('a timed exam hands itself in when the time runs out', async ({ page }) => {
    await register(page, newUser())
    await setSecondsLeft(page, 4)
    await startExam(page, { category: 'Cardiology', questions: 5, timeLimit: '15 min' })
    await expect(page.getByText('⏱')).toBeVisible()

    // An answer that was picked but not saved yet when the time runs out still counts
    await pickOption(page, 1, 5)

    await expect(page.getByText("Time's up! Submitting your exam...")).toBeVisible({ timeout: 10_000 })
    await expect(page).toHaveURL(/\/exam\/[0-9a-f-]{36}\/results$/)
    await expect(resultBadges(page, 'Not answered')).toHaveCount(4)
  })

  test('opening a timed exam after its time is over goes straight to the results', async ({ page }) => {
    await register(page, newUser())
    await startExam(page, { category: 'Cardiology', questions: 5, timeLimit: '15 min' })
    const examUrl = page.url()
    await pickOption(page, 1, 5)
    await page.getByRole('button', { name: 'Next Question →' }).click()
    await expect(page.getByText('Question 2 / 5')).toBeVisible()

    // Come back later, when there is no time left
    await page.goto('/dashboard')
    await setSecondsLeft(page, 0)
    await page.goto(examUrl)

    await expect(page).toHaveURL(/\/exam\/[0-9a-f-]{36}\/results$/)
    await expect(resultBadges(page, 'Not answered')).toHaveCount(4)
  })

  test('a finished exam counts toward the streak but not the card goal', async ({ page }) => {
    await register(page, newUser())
    await expect(page.getByText('0 total study days')).toBeVisible()

    await startExam(page, { category: 'Pulmonology', questions: 5 })
    await answerAllAndFinish(page, 5)
    await page.getByRole('button', { name: '← Dashboard' }).click()

    await expect(page).toHaveURL(/\/dashboard$/)
    await expect(page.getByText('1 total study day', { exact: true })).toBeVisible()
    // Exam questions are not flashcards, so the card goal has not moved
    await expect(page.getByText('0 / 20')).toBeVisible()
  })
})
