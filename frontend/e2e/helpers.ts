import { expect, type Page } from '@playwright/test'

export interface TestUser {
  firstName: string
  lastName: string
  email: string
  password: string
}

// Every test makes its own account. That way the tests never see each other's
// decks, and the whole suite can be run again and again against the same database.
export function newUser(firstName = 'Taylor'): TestUser {
  const unique = `${Date.now()}-${Math.floor(Math.random() * 1_000_000)}`
  return {
    firstName,
    lastName: 'Tester',
    email: `e2e-${unique}@example.com`,
    password: 'Str0ng!Passw0rd',
  }
}

export async function fillRegisterForm(page: Page, user: TestUser, confirmPassword = user.password) {
  await page.goto('/register')
  await page.locator('#firstName').fill(user.firstName)
  await page.locator('#lastName').fill(user.lastName)
  await page.locator('#email').fill(user.email)
  await page.locator('#password').fill(user.password)
  await page.locator('#confirmPassword').fill(confirmPassword)
}

// Registers the user through the real form and waits for the dashboard
export async function register(page: Page, user: TestUser) {
  await fillRegisterForm(page, user)
  await page.getByRole('button', { name: 'Create account' }).click()
  await expect(page).toHaveURL(/\/dashboard$/)
}

export async function logIn(page: Page, email: string, password: string) {
  await page.goto('/login')
  await page.locator('#email').fill(email)
  await page.locator('#password').fill(password)
  await page.getByRole('button', { name: 'Sign in' }).click()
}

// Creates a deck from the decks page and waits until it shows up in the list
export async function createDeck(page: Page, title: string, options: { isPublic?: boolean } = {}) {
  await page.goto('/decks')
  await page.getByRole('button', { name: '+ Create Deck' }).click()

  const dialog = page.getByRole('dialog')
  await dialog.getByPlaceholder('e.g. Cardiology — Heart Failure').fill(title)
  if (options.isPublic) {
    await dialog.locator('#isPublic').check()
  }
  await dialog.getByRole('button', { name: 'Create Deck' }).click()

  await expect(dialog).toBeHidden()
  await expect(page.getByRole('heading', { name: title })).toBeVisible()
}

// Opens a deck from the decks page by its title
export async function openDeck(page: Page, title: string) {
  await page.goto('/decks')
  // The deck's box on the page: the smallest block that has both the title and a View button
  const deckCard = page.locator('div')
    .filter({ has: page.getByRole('heading', { name: title, exact: true }) })
    .filter({ has: page.getByRole('button', { name: 'View' }) })
    .last()
  await deckCard.getByRole('button', { name: 'View' }).click()
  await expect(page).toHaveURL(/\/decks\/[0-9a-f-]{36}$/)
  await expect(page.getByRole('heading', { name: title })).toBeVisible()
}

// Adds a card to the deck that is open right now
export async function addCard(page: Page, front: string, back: string) {
  await page.getByRole('button', { name: /Add a Card|\+ Add Card/ }).first().click()
  await expect(page).toHaveURL(/\/cards\/new$/)

  await page.getByPlaceholder('What are the classic symptoms of left-sided heart failure?').fill(front)
  await page.getByPlaceholder(/^Dyspnea, orthopnea/).fill(back)
  await page.getByRole('button', { name: 'Save Card' }).click()

  await expect(page).toHaveURL(/\/decks\/[0-9a-f-]{36}$/)
  await expect(page.getByText(front)).toBeVisible()
}
