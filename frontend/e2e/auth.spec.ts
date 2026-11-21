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
    // Sign out waits for the server to revoke the token, then goes to the login page
    await expect(page).toHaveURL(/\/login$/)

    await logIn(page, user.email, 'Wr0ng!Passw0rd')

    await expect(page.getByText('Invalid email or password. Please try again.')).toBeVisible()
    await expect(page).toHaveURL(/\/login$/)
  })

  test('the refresh token is a cookie the page cannot read, and signing out revokes it', async ({ page, context }) => {
    const signUp = page.waitForResponse(r => r.url().endsWith('/api/v1/auth/register'))
    await register(page, newUser())
    // What the server sends (WebKit on Windows does not report SameSite back to the test)
    const setCookie = (await (await signUp).allHeaders())['set-cookie']
    expect(setCookie).toMatch(/pa_refresh=[^;]+/)
    expect(setCookie).toContain('HttpOnly')
    expect(setCookie).toContain('SameSite=Strict')
    expect(setCookie).toContain('Path=/api/v1/auth')
    // Not in storage and not visible to scripts: only the browser holds it
    expect(await page.evaluate(() => localStorage.getItem('pa_refresh_token'))).toBeNull()
    expect(await page.evaluate(() => document.cookie)).not.toContain('pa_refresh')
    const cookie = (await context.cookies()).find(c => c.name === 'pa_refresh')
    expect(cookie).toBeTruthy()
    expect(cookie!.httpOnly).toBe(true)

    await page.getByRole('button', { name: 'Sign out' }).click()
    await expect(page).toHaveURL(/\/login$/)
    // The browser deleted it
    expect((await context.cookies()).find(c => c.name === 'pa_refresh')).toBeUndefined()

    // A copy of the token taken before signing out no longer gets new tokens
    const response = await page.request.post('/api/v1/auth/refresh', { headers: { Cookie: `pa_refresh=${cookie!.value}` } })
    expect(response.status()).toBe(401)
  })

  test('a student signed in with an older version of the app moves to the cookie without signing in again', async ({ page, context }) => {
    await register(page, newUser())
    // Rebuild the old state: the refresh token in storage, no cookie, an expired access token
    const cookie = (await context.cookies()).find(c => c.name === 'pa_refresh')!
    await context.clearCookies()
    await page.evaluate(token => {
      localStorage.setItem('pa_refresh_token', token)
      localStorage.setItem('pa_access_token', 'expired')
    }, cookie.value)

    await page.goto('/decks')
    await expect(page).toHaveURL(/\/decks$/)
    await expect(page.getByRole('button', { name: 'Sign out' })).toBeVisible()
    // The old copy is gone and the cookie took its place
    expect(await page.evaluate(() => localStorage.getItem('pa_refresh_token'))).toBeNull()
    expect((await context.cookies()).some(c => c.name === 'pa_refresh')).toBe(true)
  })

  test('after 5 wrong passwords the login is locked, even with the right password', async ({ page }) => {
    const user = newUser()
    await register(page, user)
    await page.getByRole('button', { name: 'Sign out' }).click()
    // Sign out waits for the server to revoke the token, then goes to the login page
    await expect(page).toHaveURL(/\/login$/)

    for (let attempt = 0; attempt < 5; attempt++) {
      await logIn(page, user.email, 'Wr0ng!Passw0rd')
      await expect(page.getByText('Invalid email or password. Please try again.')).toBeVisible()
    }

    await logIn(page, user.email, user.password)
    await expect(page.getByText('Too many failed logins. Try again in 15 minutes.')).toBeVisible()
    await expect(page).toHaveURL(/\/login$/)
  })

  test('an expired access token is refreshed once, even when several requests fail together', async ({ page }) => {
    await register(page, newUser())
    // Spoil the access token, as if it had run out. The dashboard then loads
    // several things at once and every one of them gets a 401.
    await page.evaluate(() => localStorage.setItem('pa_access_token', 'expired'))
    await page.goto('/dashboard')

    // One shared refresh: the user stays signed in. Before, each request refreshed
    // with the same token, the server took the second use for theft and signed
    // the user out everywhere.
    await expect(page.getByRole('button', { name: 'Sign out' })).toBeVisible()
    await page.goto('/decks')
    await expect(page).toHaveURL(/\/decks$/)
  })

  test('a signed-in student stays signed in while the server is waking up', async ({ page }) => {
    await register(page, newUser())
    // The first two "who am I" calls fail as if the sleeping server were not answering
    let failures = 0
    await page.route('**/api/v1/users/me', async route => {
      if (failures < 2) {
        failures++
        await route.abort('timedout')
      } else {
        await route.continue()
      }
    })

    await page.reload()
    // The app waits and asks again instead of signing the student out
    await expect(page.getByRole('button', { name: 'Sign out' })).toBeVisible({ timeout: 20_000 })
    await expect(page).toHaveURL(/\/dashboard$/)
    expect(failures).toBe(2)
  })

  test('a slow answer shows the waking-up banner until it arrives', async ({ page }) => {
    await register(page, newUser())
    // Hold the dashboard's data for 6 seconds, as a sleeping server would
    await page.route('**/api/v1/progress/dashboard', async route => {
      await new Promise(resolve => setTimeout(resolve, 6_000))
      await route.continue()
    })
    await page.goto('/dashboard')
    const banner = page.getByRole('status').filter({ hasText: 'Waking up the server' })
    await expect(banner).toBeVisible({ timeout: 6_000 })
    await expect(page.getByText('usually takes 1 to 2 minutes')).toBeVisible()
    // A PA fact or a quick review question while waiting
    await expect(page.getByText(/^(Did you know\?|Quick review)$/)).toBeVisible()
    // Gone once the answer is in
    await expect(banner).toBeHidden({ timeout: 10_000 })
  })

  test('when the host gives up on a waking server, reading requests try again and the page still loads', async ({ page }) => {
    await register(page, newUser())
    // The first two tries get the 520 Render's edge sends after about a minute
    let tries = 0
    await page.route('**/api/v1/progress/dashboard', async route => {
      tries++
      if (tries <= 2) {
        await route.fulfill({ status: 520, body: '' })
      } else {
        await route.continue()
      }
    })
    await page.goto('/dashboard')
    await expect(page.getByRole('status').filter({ hasText: 'Waking up the server' })).toBeVisible()
    await expect(page.getByText('Today\'s Goals')).toBeVisible({ timeout: 20_000 })
    // Two refused tries, then the real answer (the page may ask more than once)
    expect(tries).toBeGreaterThanOrEqual(3)
    await expect(page.getByText('An unexpected error occurred.')).toBeHidden()
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
    // Sign out waits for the server to revoke the token, then goes to the login page
    await expect(page).toHaveURL(/\/login$/)

    await fillRegisterForm(page, user)
    await page.getByRole('button', { name: 'Create account' }).click()

    await expect(page.getByText('An account with this email already exists. Try logging in instead.')).toBeVisible()
    await expect(page).toHaveURL(/\/register$/)
  })

  test('a server that is still starting up gets a clear message on login', async ({ page }) => {
    // What the free host answers while the backend is waking up
    await page.route('**/api/v1/auth/login', (route) => route.fulfill({ status: 503, body: 'starting' }))

    await logIn(page, 'someone@example.com', 'Str0ng!Passw0rd')

    await expect(page.getByText(/The server is not answering/)).toBeVisible()
    await expect(page).toHaveURL(/\/login$/)
  })

  test('the demo account from the dev profile can log in', async ({ page }) => {
    // The backend creates this account at startup, but only with the "dev" profile
    // (playwright.config.ts starts it that way). The test only looks: the account
    // is shared by every run, so it must not add decks or study anything.
    await logIn(page, 'demo@example.com', 'DemoStudent1!')

    await expect(page).toHaveURL(/\/dashboard$/)
    await expect(page.getByRole('heading', { level: 1 })).toContainText('Demo')
  })
})
