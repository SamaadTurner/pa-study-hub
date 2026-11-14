import { test, expect, type Page } from '@playwright/test'
import {
  newUser,
  register,
  createDeckWithCards,
  createPublicDecksThroughApi,
  openDeck,
  addCard,
} from './helpers'

// The public list is shared by every user in the database, and other tests add
// decks to it while these run. So each test puts its own unique word in its deck
// titles and, where it counts decks, searches for that word first.
function uniqueWord() {
  return `pd${Date.now()}${Math.floor(Math.random() * 10_000)}`
}

function searchBox(page: Page) {
  return page.getByPlaceholder('Search public decks...')
}

// The title of every deck shown in the list
function deckTitles(page: Page) {
  return page.getByRole('heading', { level: 3 })
}

test.describe('Public decks', () => {
  test('the list shows 20 decks and "Load more" brings the rest', async ({ page }) => {
    const word = uniqueWord()
    await register(page, newUser())

    const decks = []
    for (let i = 1; i <= 25; i++) {
      decks.push({ title: `Paging ${word} ${i}` })
    }
    await createPublicDecksThroughApi(page, decks)

    await page.goto('/decks/public')
    await searchBox(page).fill(word)

    await expect(page.getByText('Showing 20 of 25 decks')).toBeVisible()
    await expect(deckTitles(page)).toHaveCount(20)
    // Newest first: deck 25 is on the first page, deck 1 is not
    await expect(page.getByRole('heading', { name: `Paging ${word} 25`, exact: true })).toBeVisible()
    await expect(page.getByRole('heading', { name: `Paging ${word} 1`, exact: true })).toBeHidden()

    await page.getByRole('button', { name: 'Load more' }).click()

    await expect(page.getByText('Showing 25 of 25 decks')).toBeVisible()
    await expect(deckTitles(page)).toHaveCount(25)
    await expect(page.getByRole('heading', { name: `Paging ${word} 1`, exact: true })).toBeVisible()
    // Nothing left to load
    await expect(page.getByRole('button', { name: 'Load more' })).toBeHidden()
  })

  test('search finds a deck that is not among the first 20', async ({ page }) => {
    const word = uniqueWord()
    await register(page, newUser())

    // One deck, then 25 newer ones. The list is newest first, so the old deck
    // cannot be on the first page of 20.
    const decks = [{ title: `Oldest ${word}` }]
    for (let i = 1; i <= 25; i++) {
      decks.push({ title: `Newer ${word} ${i}` })
    }
    await createPublicDecksThroughApi(page, decks)

    await page.goto('/decks/public')
    await expect(deckTitles(page)).toHaveCount(20)
    await expect(page.getByRole('heading', { name: `Oldest ${word}` })).toBeHidden()

    // The search used to look only at the 20 decks on the page and found nothing
    await searchBox(page).fill(`oldest ${word}`)

    await expect(page.getByRole('heading', { name: `Oldest ${word}` })).toBeVisible()
    await expect(page.getByText('Showing 1 of 1 deck')).toBeVisible()
  })

  test('a category button narrows the list, All brings everything back', async ({ page }) => {
    const word = uniqueWord()
    await register(page, newUser())
    await createPublicDecksThroughApi(page, [
      { title: `Heart ${word}`, category: 'CARDIOLOGY' },
      { title: `Brain ${word}`, category: 'NEUROLOGY' },
    ])

    await page.goto('/decks/public')
    await searchBox(page).fill(word)
    await expect(page.getByText('Showing 2 of 2 decks')).toBeVisible()

    await page.getByRole('button', { name: 'Neurology', exact: true }).click()
    await expect(page.getByText('Showing 1 of 1 deck')).toBeVisible()
    await expect(page.getByRole('heading', { name: `Brain ${word}` })).toBeVisible()
    await expect(page.getByRole('heading', { name: `Heart ${word}` })).toBeHidden()

    // A category with no matching deck
    await page.getByRole('button', { name: 'Dermatology', exact: true }).click()
    await expect(page.getByText('No decks match your search')).toBeVisible()

    await page.getByRole('button', { name: 'All', exact: true }).click()
    await expect(page.getByText('Showing 2 of 2 decks')).toBeVisible()
  })

  test('a search with no match says so, and clearing it shows decks again', async ({ page }) => {
    const word = uniqueWord()
    await register(page, newUser())
    await createPublicDecksThroughApi(page, [{ title: `Only deck ${word}` }])

    await page.goto('/decks/public')
    await searchBox(page).fill(`no deck is called ${word}`)
    await expect(page.getByText('No decks match your search')).toBeVisible()

    // The search box is still there and still has what was typed
    await expect(searchBox(page)).toHaveValue(`no deck is called ${word}`)

    await searchBox(page).fill(word)
    await expect(page.getByRole('heading', { name: `Only deck ${word}` })).toBeVisible()
  })

  test('a public deck can be copied into your own decks', async ({ page, browser }) => {
    const word = uniqueWord()
    const title = `Valve disease ${word}`

    // The owner publishes a deck with two cards
    await register(page, newUser('Owner'))
    await createDeckWithCards(
      page,
      title,
      [
        { front: 'Murmur of mitral stenosis?', back: 'Diastolic rumble at the apex' },
        { front: 'Murmur of aortic regurgitation?', back: 'Early diastolic decrescendo' },
      ],
      { isPublic: true },
    )
    const originalUrl = page.url()

    // Someone else finds it and copies it
    const otherContext = await browser.newContext()
    const otherPage = await otherContext.newPage()
    await register(otherPage, newUser('Visitor'))

    await otherPage.goto('/decks/public')
    await searchBox(otherPage).fill(word)
    // Wait for the search, so the only deck left on the page is the owner's
    await expect(otherPage.getByText('Showing 1 of 1 deck')).toBeVisible()
    await expect(otherPage.getByRole('heading', { name: title })).toBeVisible()
    await expect(otherPage.getByText('2 cards')).toBeVisible()
    await otherPage.getByRole('button', { name: 'Clone to My Decks' }).click()

    // The visitor lands on their own copy, with both cards ready to study
    await expect(otherPage).toHaveURL(/\/decks\/[0-9a-f-]{36}$/)
    expect(otherPage.url()).not.toBe(originalUrl)
    await expect(otherPage.getByRole('heading', { name: `${title} (Clone)` })).toBeVisible()
    await expect(otherPage.getByText('Murmur of mitral stenosis?')).toBeVisible()
    await expect(otherPage.getByText('Murmur of aortic regurgitation?')).toBeVisible()
    await expect(otherPage.getByRole('button', { name: 'Study (2)' })).toBeVisible()

    // The copy is theirs to change
    await addCard(otherPage, 'Murmur of mitral regurgitation?', 'Holosystolic at the apex')
    await openDeck(otherPage, `${title} (Clone)`)
    await expect(otherPage.getByRole('button', { name: 'Study (3)' })).toBeVisible()

    // The copy is private: the public list still has only the original
    await otherPage.goto('/decks/public')
    await searchBox(otherPage).fill(word)
    await expect(otherPage.getByText('Showing 1 of 1 deck')).toBeVisible()
    await expect(otherPage.getByRole('heading', { name: title, exact: true })).toBeVisible()

    // And the owner's deck did not get the visitor's new card
    await page.reload()
    await expect(page.getByText('Murmur of mitral stenosis?')).toBeVisible()
    await expect(page.getByText('Murmur of mitral regurgitation?')).toBeHidden()

    await otherContext.close()
  })
})
