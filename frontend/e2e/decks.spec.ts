import { test, expect } from '@playwright/test'
import { newUser, register, createDeck, openDeck, addCard } from './helpers'

test.describe('Decks and cards', () => {
  test('a new user has no decks and can create the first one', async ({ page }) => {
    await register(page, newUser())

    await page.goto('/decks')
    await expect(page.getByText('No decks yet')).toBeVisible()

    await createDeck(page, 'Heart failure basics')

    await expect(page.getByText('No decks yet')).toBeHidden()
    // A new deck has a real date on it, not "Invalid Date"
    await expect(page.locator('body')).not.toContainText('Invalid Date')
  })

  test('a deck without a title is not created', async ({ page }) => {
    await register(page, newUser())
    await page.goto('/decks')

    await page.getByRole('button', { name: '+ Create Deck' }).click()
    const dialog = page.getByRole('dialog')
    await dialog.getByRole('button', { name: 'Create Deck' }).click()

    // The form stays open and the list is still empty
    await expect(dialog).toBeVisible()
    await dialog.getByRole('button', { name: 'Cancel' }).click()
    await expect(page.getByText('No decks yet')).toBeVisible()
  })

  test('a card can be added to a deck and is ready to study', async ({ page }) => {
    await register(page, newUser())
    await createDeck(page, 'Murmurs')
    await openDeck(page, 'Murmurs')
    await expect(page.getByText('No cards yet')).toBeVisible()

    await addCard(page, 'Which murmur is heard in aortic stenosis?', 'Crescendo-decrescendo systolic murmur')

    await expect(page.getByText('1 card', { exact: false })).toBeVisible()
    // A new card can be studied right away
    await expect(page.getByRole('button', { name: 'Study (1)' })).toBeVisible()

    // Opening the card shows the answer that was typed in
    await page.getByText('Which murmur is heard in aortic stenosis?').click()
    await expect(page.getByText('Crescendo-decrescendo systolic murmur')).toBeVisible()
  })

  test('a card can be edited', async ({ page }) => {
    await register(page, newUser())
    await createDeck(page, 'Editing deck')
    await openDeck(page, 'Editing deck')
    await addCard(page, 'Old question', 'Old answer')

    await page.getByText('Old question').click()
    await page.getByRole('button', { name: 'Edit' }).click()
    await expect(page).toHaveURL(/\/cards\/[0-9a-f-]{36}\/edit$/)

    // Wait until the form has loaded the card. Typing before that gets overwritten
    // when the old text arrives.
    const frontField = page.getByPlaceholder('What are the classic symptoms of left-sided heart failure?')
    await expect(frontField).toHaveValue('Old question')
    await frontField.fill('New question')
    await page.getByRole('button', { name: 'Save Changes' }).click()

    await expect(page).toHaveURL(/\/decks\/[0-9a-f-]{36}$/)
    await expect(page.getByText('New question')).toBeVisible()
    await expect(page.getByText('Old question')).toBeHidden()
  })

  test('a card can be deleted', async ({ page }) => {
    await register(page, newUser())
    await createDeck(page, 'Deleting deck')
    await openDeck(page, 'Deleting deck')
    await addCard(page, 'Card to keep', 'Answer one')
    await addCard(page, 'Card to delete', 'Answer two')

    await page.getByText('Card to delete').click()
    // exact, because the card's own row is a button with "delete" in its text
    await page.getByRole('button', { name: 'Delete', exact: true }).click()
    // Confirm in the "Delete Card" dialog
    await page.getByRole('dialog').getByRole('button', { name: 'Delete', exact: true }).click()

    // Wait for the dialog to close first. It repeats the card's text in its question.
    await expect(page.getByRole('dialog')).toBeHidden()
    await expect(page.getByText('Card to delete')).toHaveCount(0)
    await expect(page.getByText('Card to keep')).toBeVisible()
  })

  test('a deck can be renamed and deleted', async ({ page }) => {
    await register(page, newUser())
    await createDeck(page, 'First name')

    await page.getByRole('button', { name: 'Edit' }).click()
    const editDialog = page.getByRole('dialog')
    await editDialog.getByPlaceholder('e.g. Cardiology — Heart Failure').fill('Second name')
    await editDialog.getByRole('button', { name: 'Save Changes' }).click()

    await expect(page.getByRole('heading', { name: 'Second name' })).toBeVisible()
    await expect(page.getByRole('heading', { name: 'First name' })).toBeHidden()

    await page.getByRole('button', { name: 'Delete' }).click()
    await page.getByRole('dialog').getByRole('button', { name: 'Delete' }).click()

    await expect(page.getByText('No decks yet')).toBeVisible()
  })

  test('decks and cards are still there after logging out and back in', async ({ page }) => {
    const user = newUser()
    await register(page, user)
    await createDeck(page, 'Saved deck')
    await openDeck(page, 'Saved deck')
    await addCard(page, 'Saved question', 'Saved answer')

    await page.getByRole('button', { name: 'Sign out' }).click()
    await expect(page).toHaveURL(/\/login$/)
    await page.locator('#email').fill(user.email)
    await page.locator('#password').fill(user.password)
    await page.getByRole('button', { name: 'Sign in' }).click()
    await expect(page).toHaveURL(/\/dashboard$/)

    await openDeck(page, 'Saved deck')
    await expect(page.getByText('Saved question')).toBeVisible()
  })

  test('a public deck can be seen by another user, a private one cannot', async ({ page, browser }) => {
    // The owner makes one public and one private deck. The titles are unique
    // because the public list is shared by every user in the database.
    const stamp = Date.now()
    const publicTitle = `Shared deck ${stamp}`
    const privateTitle = `Private deck ${stamp}`

    await register(page, newUser('Owner'))
    await createDeck(page, publicTitle, { isPublic: true })
    await createDeck(page, privateTitle)
    await openDeck(page, privateTitle)
    await addCard(page, 'Only the owner should see this', 'Private answer')
    const privateDeckUrl = page.url()

    // A second person in their own browser window
    const otherContext = await browser.newContext()
    const otherPage = await otherContext.newPage()
    await register(otherPage, newUser('Visitor'))

    await otherPage.goto('/decks/public')
    await otherPage.getByPlaceholder(/search/i).fill(String(stamp))
    await expect(otherPage.getByText(publicTitle)).toBeVisible()
    await expect(otherPage.getByText(privateTitle)).toBeHidden()

    // Having the link to the private deck is not enough
    await otherPage.goto(privateDeckUrl)
    await expect(otherPage.getByText('You do not have permission to view this deck')).toBeVisible()
    await expect(otherPage.getByText('Only the owner should see this')).toBeHidden()

    await otherContext.close()
  })
})
