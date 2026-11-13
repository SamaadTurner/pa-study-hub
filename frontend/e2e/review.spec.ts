import { test, expect, type Page } from '@playwright/test'
import { newUser, register, createDeckWithCards } from './helpers'

// Shows the answer of the card on screen and rates it
async function flipAndRate(page: Page, rating: 'Again' | 'Hard' | 'Good' | 'Easy') {
  await page.getByRole('button', { name: 'Reveal Answer', exact: true }).click()
  await page.getByRole('button', { name: rating }).click()
}

test.describe('Studying a deck', () => {
  test('a card shows its answer after flipping and rating it ends the session', async ({ page }) => {
    await register(page, newUser())
    await createDeckWithCards(page, 'Valve disease', [
      { front: 'Which murmur is heard in mitral stenosis?', back: 'Low-pitched diastolic rumble at the apex' },
    ])

    await page.getByRole('button', { name: 'Study (1)' }).click()
    await expect(page).toHaveURL(/\/decks\/[0-9a-f-]{36}\/review$/)
    await expect(page.getByText('Card 1 of 1')).toBeVisible()
    await expect(page.getByText('Which murmur is heard in mitral stenosis?')).toBeVisible()

    // The rating buttons are not there until the answer has been revealed
    await expect(page.getByRole('button', { name: 'Good' })).toBeHidden()

    await page.getByRole('button', { name: 'Reveal Answer', exact: true }).click()
    await expect(page.getByRole('button', { name: /Card showing answer/ })).toBeVisible()
    await expect(page.getByText('Low-pitched diastolic rumble at the apex')).toBeVisible()

    await page.getByRole('button', { name: 'Good' }).click()

    await expect(page.getByRole('heading', { name: 'Session Complete!' })).toBeVisible()
    await expect(page.getByText('You reviewed 1 card in')).toBeVisible()
    await expect(page.getByText('Accuracy (1/1 correct)')).toBeVisible()
  })

  test('the summary counts each rating', async ({ page }) => {
    await register(page, newUser())
    await createDeckWithCards(page, 'Three cards', [
      { front: 'Question one', back: 'Answer one' },
      { front: 'Question two', back: 'Answer two' },
      { front: 'Question three', back: 'Answer three' },
    ])

    await page.getByRole('button', { name: 'Study (3)' }).click()

    await expect(page.getByText('Card 1 of 3')).toBeVisible()
    await flipAndRate(page, 'Again')
    await expect(page.getByText('Card 2 of 3')).toBeVisible()
    await flipAndRate(page, 'Good')
    await expect(page.getByText('Card 3 of 3')).toBeVisible()
    await flipAndRate(page, 'Easy')

    await expect(page.getByText('You reviewed 3 cards in')).toBeVisible()
    // Good and Easy count as correct, Again does not: 2 of 3 is 67%
    await expect(page.getByText('67%')).toBeVisible()
    await expect(page.getByText('Accuracy (2/3 correct)')).toBeVisible()
  })

  test('a card that was just studied is not due again the same day', async ({ page }) => {
    await register(page, newUser())
    await createDeckWithCards(page, 'Due dates', [
      { front: 'Studied today', back: 'Comes back tomorrow at the earliest' },
    ])
    const deckUrl = page.url()

    await page.getByRole('button', { name: 'Study (1)' }).click()
    await flipAndRate(page, 'Good')
    await expect(page.getByRole('heading', { name: 'Session Complete!' })).toBeVisible()

    // The deck page no longer offers to study, and the card has a next review date
    await page.goto(deckUrl)
    await expect(page.getByText('Studied today')).toBeVisible()
    await expect(page.getByRole('button', { name: /^Study/ })).toBeHidden()

    // Going to the review page anyway shows there is nothing to do
    await page.goto(`${deckUrl}/review`)
    await expect(page.getByRole('heading', { name: 'All caught up!' })).toBeVisible()
  })

  test('leaving in the middle keeps the cards that were already rated', async ({ page }) => {
    await register(page, newUser())
    await createDeckWithCards(page, 'Half done', [
      { front: 'First card', back: 'First answer' },
      { front: 'Second card', back: 'Second answer' },
    ])

    await page.getByRole('button', { name: 'Study (2)' }).click()
    await expect(page.getByText('Card 1 of 2')).toBeVisible()
    await flipAndRate(page, 'Good')
    await expect(page.getByText('Card 2 of 2')).toBeVisible()

    await page.getByRole('button', { name: 'Exit' }).click()

    // Back on the deck page with one card left to study
    await expect(page).toHaveURL(/\/decks\/[0-9a-f-]{36}$/)
    await expect(page.getByRole('button', { name: 'Study (1)' })).toBeVisible()
  })

  test('a card can be studied with the keyboard', async ({ page }) => {
    await register(page, newUser())
    await createDeckWithCards(page, 'Keyboard deck', [
      { front: 'Keyboard question', back: 'Keyboard answer' },
    ])

    await page.getByRole('button', { name: 'Study (1)' }).click()
    await expect(page.getByText('Keyboard question')).toBeVisible()

    // Space reveals the answer, 3 rates it Good
    await page.keyboard.press('Space')
    await expect(page.getByRole('button', { name: 'Good' })).toBeVisible()
    await page.keyboard.press('3')

    await expect(page.getByRole('heading', { name: 'Session Complete!' })).toBeVisible()
    await expect(page.getByText('Accuracy (1/1 correct)')).toBeVisible()
  })

  test('the hint stays hidden until it is asked for', async ({ page }) => {
    await register(page, newUser())
    await createDeckWithCards(page, 'Hints', [
      {
        front: 'First-line drug for anaphylaxis?',
        back: 'Epinephrine, intramuscular',
        hint: 'It is given in the thigh',
      },
    ])

    await page.getByRole('button', { name: 'Study (1)' }).click()
    await expect(page.getByText('First-line drug for anaphylaxis?')).toBeVisible()
    await expect(page.getByText('It is given in the thigh')).toBeHidden()

    await page.getByRole('button', { name: 'Show Hint' }).click()

    await expect(page.getByText('It is given in the thigh')).toBeVisible()
    // Asking for the hint does not flip the card
    await expect(page.getByRole('button', { name: 'Reveal Answer', exact: true })).toBeVisible()
  })

  test('studying counts toward the streak and the daily card goal', async ({ page }) => {
    await register(page, newUser())

    // Nothing studied yet
    await expect(page.getByText('0 total study days')).toBeVisible()
    await expect(page.getByText('0 / 20')).toBeVisible()

    await createDeckWithCards(page, 'Goal deck', [
      { front: 'Goal question one', back: 'Goal answer one' },
      { front: 'Goal question two', back: 'Goal answer two' },
    ])
    await page.getByRole('button', { name: 'Study (2)' }).click()
    await flipAndRate(page, 'Good')
    await expect(page.getByText('Card 2 of 2')).toBeVisible()
    await flipAndRate(page, 'Easy')
    await page.getByRole('button', { name: 'Back to Dashboard' }).click()

    await expect(page).toHaveURL(/\/dashboard$/)
    await expect(page.getByText('1 total study day', { exact: true })).toBeVisible()
    await expect(page.getByText('2 / 20')).toBeVisible()
    // Both cards are scheduled for a later day now
    await expect(page.getByText('No cards due for review right now.')).toBeVisible()
  })
})
