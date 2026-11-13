import { test, expect } from '@playwright/test'
import { newUser, register, logIn, fillRegisterForm } from './helpers'

test.describe('Accounts', () => {
  test('a new user can register and lands on the dashboard', async ({ page }) => {
    const user = newUser('Jordan')

    await register(page, user)

    // The dashboard greets the user by first name and their name is in the top bar
    await expect(page.getByRole('heading', { level: 1 })).toContainText('Jordan')
    await expect(page.getByRole('button', { name: 'Sign out' })).toBeVisible()
  })

  test('a user can sign out and log back in', async ({ page }) => {
    const user = newUser()
    await register(page, user)

    await page.getByRole('button', { name: 'Sign out' }).click()
    await expect(page).toHaveURL(/\/login$/)

    await logIn(page, user.email, user.password)

    await expect(page).toHaveURL(/\/dashboard$/)
    await expect(page.getByRole('heading', { level: 1 })).toContainText(user.firstName)
  })

  test('the user stays logged in after reloading the page', async ({ page }) => {
    const user = newUser()
    await register(page, user)

    await page.reload()

    await expect(page).toHaveURL(/\/dashboard$/)
    await expect(page.getByRole('button', { name: 'Sign out' })).toBeVisible()
  })

  test('a wrong password shows an error and does not log in', async ({ page }) => {
    const user = newUser()
    await register(page, user)
    await page.getByRole('button', { name: 'Sign out' }).click()

    await logIn(page, user.email, 'Wr0ng!Passw0rd')

    await expect(page.getByText('Invalid email or password. Please try again.')).toBeVisible()
    await expect(page).toHaveURL(/\/login$/)
  })

  test('pages behind the login send a visitor to the login page', async ({ page }) => {
    await page.goto('/decks')
    await expect(page).toHaveURL(/\/login$/)

    await page.goto('/exam')
    await expect(page).toHaveURL(/\/login$/)
  })

  test('after signing out the app pages are closed again', async ({ page }) => {
    const user = newUser()
    await register(page, user)
    await page.getByRole('button', { name: 'Sign out' }).click()
    await expect(page).toHaveURL(/\/login$/)

    await page.goto('/dashboard')

    await expect(page).toHaveURL(/\/login$/)
  })

  test('registering with passwords that do not match shows an error', async ({ page }) => {
    const user = newUser()

    await fillRegisterForm(page, user, 'Different!Passw0rd')
    await page.getByRole('button', { name: 'Create account' }).click()

    await expect(page.getByText('Passwords do not match')).toBeVisible()
    await expect(page).toHaveURL(/\/register$/)
  })

  test('the same email cannot be registered twice', async ({ page }) => {
    const user = newUser()
    await register(page, user)
    await page.getByRole('button', { name: 'Sign out' }).click()

    await fillRegisterForm(page, user)
    await page.getByRole('button', { name: 'Create account' }).click()

    await expect(page.getByText('An account with this email already exists. Try logging in instead.')).toBeVisible()
    await expect(page).toHaveURL(/\/register$/)
  })
})
