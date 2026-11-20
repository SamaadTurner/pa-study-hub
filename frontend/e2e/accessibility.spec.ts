import { test, expect, type Page } from '@playwright/test'
import AxeBuilder from '@axe-core/playwright'
import { newUser, register, createDeckWithCards, startExam, answerAllAndFinish } from './helpers'

// Automated accessibility checks with axe (WCAG 2.1 A and AA rules) on every main
// page. axe finds a good share of problems (missing labels, low contrast, wrong
// ARIA), not all of them: keyboard use and screen reader wording still need a person.
async function expectNoViolations(page: Page, pageName: string) {
  // Wait until nothing is loading, so axe sees the finished page
  await page.waitForLoadState('networkidle')
  const results = await new AxeBuilder({ page })
    .withTags(['wcag2a', 'wcag2aa', 'wcag21a', 'wcag21aa'])
    .analyze()
  const summary = results.violations.map(v =>
    `${v.id} (${v.impact}): ${v.help} -> ${v.nodes.slice(0, 3).map(n => n.target.join(' ')).join(' | ')}`)
  expect(summary, `axe on the ${pageName} page`).toEqual([])
}

test.describe('Accessibility (axe)', () => {
  test('pages before signing in', async ({ page }) => {
    for (const [path, name] of [['/login', 'login'], ['/register', 'register'],
      ['/forgot-password', 'forgot password'], ['/privacy', 'privacy']]) {
      await page.goto(path)
      await expectNoViolations(page, name)
    }
  })

  test('decks, a deck, the card editor and studying', async ({ page }) => {
    await register(page, newUser())
    await expectNoViolations(page, 'dashboard')

    await createDeckWithCards(page, `Axe deck ${Date.now()}`, [
      { front: 'What does ACE stand for?', back: 'Angiotensin converting enzyme', hint: 'An enzyme' },
    ])
    await expectNoViolations(page, 'deck')

    await page.goto('/decks')
    await expectNoViolations(page, 'decks')
    await page.goto('/decks/public')
    await expectNoViolations(page, 'public decks')

    await page.goBack()
    await page.goBack()
    await page.getByRole('button', { name: /Add a Card|\+ Add Card/ }).first().click()
    await expectNoViolations(page, 'card editor')
    await page.getByText('Fill in the blank', { exact: true }).click()
    await page.getByLabel(/Text with blanks/).fill('{{c1::Aspirin}} inhibits {{c2::COX}}')
    await expectNoViolations(page, 'card editor (fill in the blank)')
    await page.getByRole('button', { name: 'Cancel' }).click()

    await page.getByRole('button', { name: /^Study/ }).click()
    await expectNoViolations(page, 'study (question)')
    await page.getByRole('button', { name: 'Reveal Answer', exact: true }).click()
    await expectNoViolations(page, 'study (answer)')
  })

  test('exams, results, analytics, profile and questions', async ({ page }) => {
    await register(page, newUser())
    await page.goto('/exam')
    await expectNoViolations(page, 'exam setup')

    await startExam(page, { category: 'Cardiology', questions: 5 })
    await expectNoViolations(page, 'exam')
    await answerAllAndFinish(page, 5)
    await expectNoViolations(page, 'exam results')

    for (const [path, name] of [['/exam/history', 'exam history'], ['/analytics', 'analytics'],
      ['/profile', 'profile'], ['/exam/questions', 'my questions'], ['/exam/questions/new', 'question editor'],
      ['/exam/shared', 'shared questions']]) {
      await page.goto(path)
      await expectNoViolations(page, name)
    }
  })
})
