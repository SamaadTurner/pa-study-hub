import { test, expect, type Page } from '@playwright/test'
import { newUser } from './helpers'

// Google itself can't be used in an automated test, so these tests stand in for
// it: the status call says Google sign-in is on, Google's script is replaced by a
// small one that draws a button and hands over a made-up credential, and the
// server's answer to that credential is faked. The tokens in that answer are real
// ones from an account the test registers, so the app works normally afterwards.
// The server side of Google sign-in is tested in GoogleSignInIntegrationTest.

const FAKE_GOOGLE_SCRIPT = `
  window.google = { accounts: { id: {
    initialize: function (options) { window.__googleCallback = options.callback },
    renderButton: function (parent) {
      var button = document.createElement('button')
      button.type = 'button'
      button.textContent = 'Continue with Google (test)'
      button.onclick = function () { window.__googleCallback({ credential: 'fake-credential' }) }
      parent.appendChild(button)
    }
  } } }
`

async function pretendGoogleIsOn(page: Page) {
  await page.route('**/api/v1/auth/google/status', route =>
    route.fulfill({ json: { enabled: true, clientId: 'test-client.apps.googleusercontent.com' } }))
  await page.route('https://accounts.google.com/gsi/client', route =>
    route.fulfill({ contentType: 'application/javascript', body: FAKE_GOOGLE_SCRIPT }))
}

// A real account and its tokens, made through the API
async function realSignInResponse(page: Page) {
  const user = newUser('Riley')
  const response = await page.request.post('/api/v1/auth/register', {
    data: { email: user.email, password: user.password, firstName: user.firstName, lastName: user.lastName },
  })
  expect(response.status()).toBe(201)
  return response.json()
}

test.describe('Continue with Google', () => {
  test('without a Google client ID on the server there is no Google button', async ({ page }) => {
    await page.goto('/login')
    await expect(page.getByRole('button', { name: 'Sign in' })).toBeVisible()
    await expect(page.getByText('or', { exact: true })).toHaveCount(0)

    // The privacy page is linked and open to visitors
    await page.getByRole('link', { name: 'Privacy' }).click()
    await expect(page.getByRole('heading', { name: 'Privacy' })).toBeVisible()
    await expect(page.getByText(/send what you give them to Groq/)).toBeVisible()
  })

  test('a Google sign-in lands on the dashboard', async ({ page }) => {
    await pretendGoogleIsOn(page)
    const signIn = await realSignInResponse(page)
    await page.route('**/api/v1/auth/google', async route => {
      expect(route.request().postDataJSON()).toEqual({ credential: 'fake-credential' })
      await route.fulfill({ json: signIn })
    })

    await page.goto('/register')
    await page.getByRole('button', { name: 'Continue with Google (test)' }).click()

    await expect(page).toHaveURL(/\/dashboard$/)
    await expect(page.getByRole('heading', { level: 1 })).toContainText('Riley')
  })

  test('an existing account asks for its password once, and a wrong one is refused', async ({ page }) => {
    await pretendGoogleIsOn(page)
    const signIn = await realSignInResponse(page)
    await page.route('**/api/v1/auth/google', route => route.fulfill({
      status: 409,
      json: { detail: 'An account with this email already exists. Enter its password once to add Google sign-in.' },
    }))
    await page.route('**/api/v1/auth/google/link', async route => {
      const body = route.request().postDataJSON()
      expect(body.credential).toBe('fake-credential')
      if (body.password === 'Right!Passw0rd') {
        await route.fulfill({ json: signIn })
      } else {
        await route.fulfill({ status: 401, json: { detail: 'Invalid email or password' } })
      }
    })

    await page.goto('/login')
    await page.getByRole('button', { name: 'Continue with Google (test)' }).click()

    await expect(page.getByText(/Enter its password once to add Google sign-in/)).toBeVisible()
    await page.getByLabel('Password of your PA Study Hub account').fill('Wr0ng!Passw0rd')
    await page.getByRole('button', { name: 'Link and sign in' }).click()
    await expect(page.getByRole('alert')).toHaveText('That password is not right.')

    await page.getByLabel('Password of your PA Study Hub account').fill('Right!Passw0rd')
    await page.getByRole('button', { name: 'Link and sign in' }).click()
    await expect(page).toHaveURL(/\/dashboard$/)
  })
})
