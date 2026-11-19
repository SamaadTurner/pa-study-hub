import { test, expect } from '@playwright/test'
import { newUser, register, logIn } from './helpers'

test.describe('Profile', () => {
  test('details can be changed and are kept after a reload', async ({ page }) => {
    await register(page, newUser('Jordan'))
    await page.getByRole('link', { name: /Profile/ }).first().click()
    await expect(page.getByRole('heading', { name: 'Profile' })).toBeVisible()

    await page.getByLabel('First name').fill('Jordy')
    await page.getByLabel(/PA school/).fill('MEDEX Northwest')
    await page.getByLabel(/Graduation year/).fill('2028')
    await page.getByRole('button', { name: 'Save details' }).click()
    await expect(page.getByText('Profile saved')).toBeVisible()

    await page.reload()
    await expect(page.getByLabel('First name')).toHaveValue('Jordy')
    await expect(page.getByLabel(/PA school/)).toHaveValue('MEDEX Northwest')
    await expect(page.getByLabel(/Graduation year/)).toHaveValue('2028')
  })

  test('the password can be changed, and this browser stays signed in', async ({ page }) => {
    const user = newUser()
    await register(page, user)
    await page.goto('/profile')
    const newPassword = 'N3w!Password'

    await page.getByLabel('Current password').fill('Wr0ng!Passw0rd')
    await page.getByLabel('New password', { exact: true }).fill(newPassword)
    await page.getByLabel('New password again').fill(newPassword)
    await page.getByRole('button', { name: 'Change password' }).click()
    await expect(page.getByRole('alert')).toHaveText('Your current password is not right.')

    await page.getByLabel('Current password').fill(user.password)
    await page.getByRole('button', { name: 'Change password' }).click()
    await expect(page.getByText('Password changed. Other devices are signed out.')).toBeVisible()

    // Still signed in here, also after a reload
    await page.reload()
    await expect(page.getByRole('heading', { name: 'Profile' })).toBeVisible()

    await page.getByRole('button', { name: 'Sign out' }).click()
    await expect(page).toHaveURL(/\/login$/)
    await logIn(page, user.email, newPassword)
    await expect(page).toHaveURL(/\/dashboard$/)
  })

  test('a new password that breaks the rules is caught on the page', async ({ page }) => {
    const user = newUser()
    await register(page, user)
    await page.goto('/profile')

    await page.getByLabel('Current password').fill(user.password)
    await page.getByLabel('New password', { exact: true }).fill('alllowercase')
    await page.getByLabel('New password again').fill('alllowercase')
    await page.getByRole('button', { name: 'Change password' }).click()
    await expect(page.getByRole('alert')).toHaveText('New password: contains uppercase letter')
  })

  test('closing the account deletes it, and the email can be used again', async ({ page }) => {
    const user = newUser()
    await register(page, user)
    await page.goto('/profile')

    await page.getByLabel('Your password').fill('Wr0ng!Passw0rd')
    page.once('dialog', dialog => dialog.accept())
    await page.getByRole('button', { name: 'Delete my account' }).click()
    await expect(page.getByRole('alert')).toContainText('To close your account, enter your password')

    await page.getByLabel('Your password').fill(user.password)
    page.once('dialog', dialog => dialog.accept())
    await page.getByRole('button', { name: 'Delete my account' }).click()
    await expect(page).toHaveURL(/\/login$/)

    await logIn(page, user.email, user.password)
    await expect(page.getByText('Invalid email or password. Please try again.')).toBeVisible()

    // The same email can register again
    await register(page, user)
    await expect(page).toHaveURL(/\/dashboard$/)
  })

  test('Google can be added from the profile', async ({ page }) => {
    // Same stand-in for Google as in google.spec.ts, and the server's answer is faked
    await page.route('**/api/v1/auth/google/status', route =>
      route.fulfill({ json: { enabled: true, clientId: 'test-client.apps.googleusercontent.com' } }))
    await page.route('https://accounts.google.com/gsi/client', route => route.fulfill({
      contentType: 'application/javascript',
      body: `window.google = { accounts: { id: {
        initialize: function (o) { window.__cb = o.callback },
        renderButton: function (p) { var b = document.createElement('button'); b.type = 'button';
          b.textContent = 'Continue with Google (test)'; b.onclick = function () { window.__cb({ credential: 'fake' }) };
          p.appendChild(b) } } } }`,
    }))
    const user = newUser()
    await register(page, user)
    await page.route('**/api/v1/users/me/google', async route => {
      expect(route.request().postDataJSON()).toEqual({ credential: 'fake' })
      const me = await page.request.get('/api/v1/users/me', { headers: route.request().headers() })
      await route.fulfill({ json: { ...(await me.json()), googleLinked: true } })
    })

    await page.goto('/profile')
    await page.getByRole('button', { name: 'Continue with Google (test)' }).click()
    await expect(page.getByText('Google sign-in added')).toBeVisible()
    await expect(page.getByText('Google sign-in is on for this account.')).toBeVisible()
    await expect(page.getByRole('button', { name: 'Remove Google sign-in' })).toBeVisible()
  })
})
