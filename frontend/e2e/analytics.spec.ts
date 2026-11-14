import { test, expect, type Page } from '@playwright/test'
import {
  newUser,
  register,
  createDeckWithCards,
  startExam,
  answerAllAndFinish,
} from './helpers'

// The big number in one of the three boxes at the top, found by the label under it
function statNumber(page: Page, label: string) {
  return page.getByText(label, { exact: true }).locator('..').locator('p').first()
}

// The squares of the 30-day activity grid. Each one has a label like
// "Nov 12: 3 cards, 5 exam questions, 88% accuracy" or "Nov 12: no activity".
function daySquares(page: Page, label?: RegExp) {
  return page.getByRole('img', { name: label ?? /: / })
}

// One of the white boxes on the page, found by its heading
function section(page: Page, heading: string) {
  return page.locator('section').filter({ has: page.getByRole('heading', { name: heading }) })
}

// The name of a category next to its bar in the "Accuracy by Category" chart.
// The chart draws each name as a <text> with a <tspan> inside it, and both
// match the name, so the inner one is picked.
function categoryLabel(page: Page, name: string) {
  return section(page, 'Accuracy by Category').locator('tspan', { hasText: name })
}

async function openAnalytics(page: Page) {
  await page.goto('/analytics')
  await expect(page.getByRole('heading', { name: 'Analytics', exact: true })).toBeVisible()
}

test.describe('Analytics', () => {
  test('a new account sees an empty page with no errors', async ({ page }) => {
    await register(page, newUser())
    await openAnalytics(page)

    await expect(statNumber(page, 'Overall accuracy')).toHaveText('0%')
    await expect(statNumber(page, 'Cards reviewed')).toHaveText('0')
    await expect(statNumber(page, 'Study days')).toHaveText('0')

    // 30 days, none of them with any studying
    await expect(daySquares(page)).toHaveCount(30)
    await expect(daySquares(page, /no activity$/)).toHaveCount(30)

    await expect(page.getByText('Not enough data yet.', { exact: false })).toBeVisible()
    await expect(page.getByText('No category data yet.', { exact: false })).toBeVisible()
  })

  test('a day with only a practice exam counts as a day of studying', async ({ page }) => {
    await register(page, newUser())
    await startExam(page, { category: 'Cardiology', questions: 5 })
    await answerAllAndFinish(page, 5)

    await openAnalytics(page)

    await expect(statNumber(page, 'Study days')).toHaveText('1')
    // Exam questions are not flashcards
    await expect(statNumber(page, 'Cards reviewed')).toHaveText('0')

    // Today's square used to stay grey when no flashcards were reviewed
    await expect(daySquares(page, /: 5 exam questions, \d+% accuracy$/)).toHaveCount(1)
    await expect(daySquares(page, /no activity$/)).toHaveCount(29)

    // The exam's category has a bar in the chart
    await expect(categoryLabel(page, 'Cardiology')).toBeVisible()
    await expect(page.getByText('No category data yet.', { exact: false })).toBeHidden()
  })

  test('cards and exam questions from the same day are shown together', async ({ page }) => {
    await register(page, newUser())

    // One flashcard, rated Good
    await createDeckWithCards(page, 'Analytics deck', [
      { front: 'First-line drug for anaphylaxis?', back: 'Intramuscular epinephrine' },
    ])
    await page.getByRole('button', { name: 'Study (1)' }).click()
    await page.getByRole('button', { name: 'Reveal Answer', exact: true }).click()
    await page.getByRole('button', { name: 'Good' }).click()
    await expect(page.getByRole('heading', { name: 'Session Complete!' })).toBeVisible()

    // And a five question exam in another category
    await startExam(page, { category: 'Pulmonology', questions: 5 })
    await answerAllAndFinish(page, 5)

    await openAnalytics(page)

    await expect(statNumber(page, 'Cards reviewed')).toHaveText('1')
    await expect(statNumber(page, 'Study days')).toHaveText('1')
    await expect(daySquares(page, /: 1 card, 5 exam questions, \d+% accuracy$/)).toHaveCount(1)

    // One bar for the deck's category and one for the exam's
    await expect(categoryLabel(page, 'Cardiology')).toBeVisible()
    await expect(categoryLabel(page, 'Pulmonology')).toBeVisible()

    // One day is not a trend yet
    await expect(page.getByText('Not enough data yet.', { exact: false })).toBeVisible()
  })

  test('the accuracy trend is drawn once there are two or more days of studying', async ({ page }) => {
    await register(page, newUser())

    // A test cannot study on three different days, so the last three days are
    // filled in on the way from the backend to the page. The rest of the answer
    // is what the backend really sent.
    await page.route('**/api/v1/progress/dashboard', async route => {
      const response = await route.fetch()
      const dashboard = await response.json()
      const days = dashboard.last30DaysActivity
      Object.assign(days[27], { cardsReviewed: 0, examQuestions: 10, accuracy: 70, minutesStudied: 15 })
      Object.assign(days[28], { cardsReviewed: 25, examQuestions: 0, accuracy: 80, minutesStudied: 33, goalMet: true })
      Object.assign(days[29], { cardsReviewed: 4, examQuestions: 0, accuracy: 50, minutesStudied: 3 })
      await route.fulfill({ response, json: dashboard })
    })

    await openAnalytics(page)

    await expect(daySquares(page, /: 10 exam questions, 70% accuracy$/)).toHaveCount(1)
    await expect(daySquares(page, /: 25 cards, 80% accuracy$/)).toHaveCount(1)
    await expect(daySquares(page, /: 4 cards, 50% accuracy$/)).toHaveCount(1)
    await expect(daySquares(page, /no activity$/)).toHaveCount(27)

    // The chart replaces the "not enough data" text, with one dot for each day
    // studied. The day with only an exam is one of them.
    const trend = section(page, 'Accuracy Trend')
    await expect(trend.getByText('Not enough data yet.', { exact: false })).toBeHidden()
    await expect(trend.locator('.recharts-line-dot')).toHaveCount(3)
  })

  test('a failed load shows an error, and Try Again loads the page', async ({ page }) => {
    await register(page, newUser())

    await page.route('**/api/v1/progress/dashboard', route =>
      route.fulfill({ status: 500, json: { title: 'Server Error', detail: 'Something went wrong' } }),
    )
    await page.goto('/analytics')

    // The page used to show 0% and empty charts here, like a brand-new account
    await expect(page.getByText('Failed to load analytics')).toBeVisible()
    await expect(page.getByText('Overall accuracy')).toBeHidden()

    await page.unroute('**/api/v1/progress/dashboard')
    await page.getByRole('button', { name: 'Try Again' }).click()

    await expect(page.getByText('Failed to load analytics')).toBeHidden()
    await expect(statNumber(page, 'Overall accuracy')).toHaveText('0%')
  })
})
