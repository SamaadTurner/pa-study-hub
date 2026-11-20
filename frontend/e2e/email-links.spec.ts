import { test, expect, type Page } from '@playwright/test'
import { newUser, register, logIn } from './helpers'

// The dev profile logs emails instead of sending them and keeps them in an outbox
// (DevOutboxController). These tests read the links from there, as a user would
// from their inbox, and open them in the browser.
async function latestLink(page: Page, email: string, marker: string): Promise<string> {
  const response = await page.request.get(`/api/v1/dev/outbox?to=${encodeURIComponent(email)}`)
  expect(response.status()).toBe(200)
  const mails: { subject: string; text: string }[] = await response.json()
  const mail = mails.find(m => m.text.includes(marker))
  expect(mail, `an email with ${marker}`).toBeTruthy()
  const link = mail!.text.split(/\s+/).find(word => word.includes(marker))!
  // The link points at the dev server's address; open the same path here
  const url = new URL(link)
  return url.pathname + url.search
}

test.describe('Links sent by email', () => {
  test('signing up asks to confirm the email, and the link confirms it', async ({ page }) => {
    const user = newUser()
    await register(page, user)
    await expect(page.getByText(`Please confirm your email: we sent a link to ${user.email}.`)).toBeVisible()

    await page.goto(await latestLink(page, user.email, '/confirm-email?token='))
    await expect(page.getByRole('heading', { name: 'Email confirmed' })).toBeVisible()
    await page.getByRole('link', { name: 'Go to the dashboard' }).click()
    await expect(page).toHaveURL(/\/dashboard$/)
    await expect(page.getByText(/Please confirm your email/)).toHaveCount(0)

    // The same link a second time does nothing
    await page.goto(await latestLink(page, user.email, '/confirm-email?token='))
    await expect(page.getByText(/This confirmation link is invalid, used or expired/)).toBeVisible()
  })

  test('the confirmation can be sent again from the banner', async ({ page }) => {
    await register(page, newUser())
    await page.getByRole('button', { name: 'Send it again' }).click()
    await expect(page.getByText('Sent. Check your inbox and the spam folder.')).toBeVisible()
  })

  test('a forgotten password can be reset with the emailed link', async ({ page }) => {
    const user = newUser()
    await register(page, user)
    await page.getByRole('button', { name: 'Sign out' }).click()
    await expect(page).toHaveURL(/\/login$/)

    await page.getByRole('link', { name: 'Forgot password?' }).click()
    await page.getByLabel('Email address').fill(user.email)
    await page.getByRole('button', { name: 'Send reset link' }).click()
    await expect(page.getByText(/If an account uses that email, a reset link is on its way/)).toBeVisible()

    await page.goto(await latestLink(page, user.email, '/reset-password?token='))
    await page.getByLabel('New password', { exact: true }).fill('N3w!Password')
    await page.getByLabel('New password again').fill('N3w!Password')
    await page.getByRole('button', { name: 'Save new password' }).click()
    await expect(page).toHaveURL(/\/dashboard$/)
    await expect(page.getByText('Password changed. You are signed in.')).toBeVisible()

    await page.getByRole('button', { name: 'Sign out' }).click()
    await expect(page).toHaveURL(/\/login$/)
    await logIn(page, user.email, user.password)
    await expect(page.getByText('Invalid email or password. Please try again.')).toBeVisible()
    await logIn(page, user.email, 'N3w!Password')
    await expect(page).toHaveURL(/\/dashboard$/)
  })

  test('an email without an account gets the same answer', async ({ page }) => {
    await page.goto('/forgot-password')
    await page.getByLabel('Email address').fill(`nobody-${Date.now()}@example.com`)
    await page.getByRole('button', { name: 'Send reset link' }).click()
    await expect(page.getByText(/If an account uses that email, a reset link is on its way/)).toBeVisible()
  })
})
