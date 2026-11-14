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
export async function addCard(page: Page, front: string, back: string, hint?: string) {
  await page.getByRole('button', { name: /Add a Card|\+ Add Card/ }).first().click()
  await expect(page).toHaveURL(/\/cards\/new$/)

  await page.getByPlaceholder('What are the classic symptoms of left-sided heart failure?').fill(front)
  await page.getByPlaceholder(/^Dyspnea, orthopnea/).fill(back)
  if (hint) {
    await page.getByPlaceholder(/^Think about what happens/).fill(hint)
  }
  await page.getByRole('button', { name: 'Save Card' }).click()

  await expect(page).toHaveURL(/\/decks\/[0-9a-f-]{36}$/)
  await expect(page.getByText(front)).toBeVisible()
}

// Makes a deck with the given cards and leaves the browser on the deck's page
export async function createDeckWithCards(
  page: Page,
  title: string,
  cards: { front: string; back: string; hint?: string }[],
  options: { isPublic?: boolean } = {},
) {
  await createDeck(page, title, options)
  await openDeck(page, title)
  for (const card of cards) {
    await addCard(page, card.front, card.back, card.hint)
  }
}

// The frontend keeps the login token in the browser's storage (see src/api/client.ts).
// The helpers below use it to call the API as the user who is logged in on the page.
async function authHeader(page: Page) {
  const token = await page.evaluate(() => localStorage.getItem('pa_access_token'))
  return { Authorization: `Bearer ${token}` }
}

// Makes decks by calling the API directly. Tests that need a lot of decks use
// this, because clicking through the form that many times would take minutes.
// The decks are made one after the other, so the last one in the list is the
// newest. Returns the ids of the new decks in the same order.
export async function createDecksThroughApi(
  page: Page,
  decks: { title: string; category?: string; isPublic?: boolean }[],
): Promise<string[]> {
  const headers = await authHeader(page)
  const ids: string[] = []

  for (const deck of decks) {
    const response = await page.request.post('/api/v1/decks', {
      headers,
      data: {
        title: deck.title,
        description: '',
        category: deck.category ?? 'CARDIOLOGY',
        isPublic: deck.isPublic ?? false,
      },
    })
    expect(response.status()).toBe(201)
    ids.push((await response.json()).id)
  }
  return ids
}

export async function createPublicDecksThroughApi(
  page: Page,
  decks: { title: string; category?: string }[],
) {
  return createDecksThroughApi(page, decks.map(deck => ({ ...deck, isPublic: true })))
}

// Adds cards to a deck by calling the API directly, in the order given
export async function createCardsThroughApi(
  page: Page,
  deckId: string,
  cards: { front: string; back: string }[],
) {
  const headers = await authHeader(page)

  for (const card of cards) {
    const response = await page.request.post(`/api/v1/decks/${deckId}/cards`, {
      headers,
      data: { front: card.front, back: card.back, tags: [] },
    })
    expect(response.status()).toBe(201)
  }
}

export interface ExamChoices {
  // The category's name as shown on the setup page, for example 'Cardiology'
  category: string
  questions: 5 | 10 | 20 | 30
  timeLimit?: '15 min' | '30 min' | '60 min'
}

// Fills in the exam setup page, starts the exam and waits for the first question.
// The starter question bank only has questions for Cardiology, Pharmacology and
// Pulmonology (10 each), so the tests stick to those.
export async function startExam(page: Page, choices: ExamChoices) {
  await page.goto('/exam')
  await page.getByLabel(choices.category, { exact: true }).check()
  await page.getByRole('button', { name: String(choices.questions), exact: true }).click()
  if (choices.timeLimit) {
    await page.getByRole('button', { name: choices.timeLimit }).click()
  }
  await page.getByRole('button', { name: /Start Exam/ }).click()

  await expect(page).toHaveURL(/\/exam\/[0-9a-f-]{36}$/)
  await expect(page.getByText(`Question 1 / ${choices.questions}`)).toBeVisible()
}

// Picks an answer on the exam question that is on screen. optionIndex 0 is option A.
export async function pickOption(page: Page, questionNumber: number, total: number, optionIndex = 0) {
  // Wait for the right question first, so a pick is never made on the previous one
  await expect(page.getByText(`Question ${questionNumber} / ${total}`)).toBeVisible()
  await page.getByRole('radio').nth(optionIndex).click()
}

// Answers every exam question with option A and finishes the exam
export async function answerAllAndFinish(page: Page, total: number) {
  for (let number = 1; number <= total; number++) {
    await pickOption(page, number, total)
    if (number < total) {
      await page.getByRole('button', { name: 'Next Question →' }).click()
    }
  }
  await page.getByRole('button', { name: 'Finish Exam' }).click()
  await expect(page).toHaveURL(/\/exam\/[0-9a-f-]{36}\/results$/)
}
