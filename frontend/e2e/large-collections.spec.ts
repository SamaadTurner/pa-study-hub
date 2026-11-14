import { test, expect } from '@playwright/test'
import {
  newUser,
  register,
  createDecksThroughApi,
  createCardsThroughApi,
  openDeck,
} from './helpers'

// The backend hands out decks and cards in pages of at most 100. These tests
// use a few more than that, so the pages have to be put together correctly.

test.describe('Large collections', () => {
  test('a user with more than 100 decks sees every one of them and can open the oldest', async ({ page }) => {
    await register(page, newUser())

    // The oldest deck gets a card, so it has something to study
    const decks = [{ title: 'The very first deck' }]
    for (let i = 1; i <= 104; i++) {
      decks.push({ title: `Later deck ${i}` })
    }
    const deckIds = await createDecksThroughApi(page, decks)
    await createCardsThroughApi(page, deckIds[0], [
      { front: 'Is this deck still reachable?', back: 'Yes' },
    ])

    // The page used to load only the 20 newest decks
    await page.goto('/decks')
    await expect(page.getByText('105 decks total')).toBeVisible()
    await expect(page.getByRole('heading', { level: 3 })).toHaveCount(105)

    // The search looks through all of them
    await page.getByPlaceholder('Search decks...').fill('very first')
    await expect(page.getByRole('heading', { level: 3 })).toHaveCount(1)
    await expect(page.getByRole('heading', { name: 'The very first deck' })).toBeVisible()

    // The oldest deck used to open as "Deck" with no Study button, because its
    // page looked for it among the 20 newest decks
    await openDeck(page, 'The very first deck')
    await expect(page.getByText('Is this deck still reachable?')).toBeVisible()
    await expect(page.getByRole('button', { name: 'Study (1)' })).toBeVisible()
  })

  test('a deck with more than 100 cards shows them all in order, and the last one can be edited', async ({ page }) => {
    await register(page, newUser())
    const [deckId] = await createDecksThroughApi(page, [{ title: 'Big deck' }])

    // Three digits in every name, so that "Card 001" is not also part of "Card 0010"
    const fronts: string[] = []
    for (let i = 1; i <= 105; i++) {
      fronts.push(`Card ${String(i).padStart(3, '0')}`)
    }
    await createCardsThroughApi(page, deckId, fronts.map(front => ({ front, back: `Answer to ${front}` })))

    // The page used to load only the first 50 cards
    await openDeck(page, 'Big deck')
    await expect(page.getByText('105 cards', { exact: false })).toBeVisible()

    // All of them, in the order they were added
    const shownFronts = page.locator('p', { hasText: /^Card \d{3}$/ })
    await expect(shownFronts).toHaveCount(105)
    expect(await shownFronts.allTextContents()).toEqual(fronts)

    // The edit form finds its card in the full list. For a card that was not in
    // the first 50 it used to open empty.
    await page.getByText('Card 105', { exact: true }).click()
    await page.getByRole('button', { name: 'Edit' }).click()
    const frontField = page.getByPlaceholder('What are the classic symptoms of left-sided heart failure?')
    await expect(frontField).toHaveValue('Card 105')
    await frontField.fill('Card 105, edited')
    await page.getByRole('button', { name: 'Save Changes' }).click()

    await expect(page).toHaveURL(/\/decks\/[0-9a-f-]{36}$/)
    await expect(page.getByText('Card 105, edited')).toBeVisible()

    // Editing did not move the card: it is still the last one in the list
    const allFronts = page.locator('p', { hasText: /^Card \d{3}/ })
    await expect(allFronts).toHaveCount(105)
    await expect(allFronts.last()).toHaveText('Card 105, edited')
  })
})
