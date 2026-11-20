import { test, expect, type Page } from '@playwright/test'
import { newUser, register, createDeckWithCards } from './helpers'

// The server runs on UTC. A student's studying must count on their own calendar
// day. These two zones are 25 hours apart, so at any moment at least one of them
// is on a different date than UTC, and the test means something whenever it runs.
const ZONES = ['Pacific/Kiritimati', 'Pacific/Pago_Pago']

// Today's date as the analytics squares label it ("Nov 21"), in the browser's zone
async function todayLabel(page: Page): Promise<string> {
  return page.evaluate(() => new Date().toLocaleDateString('en-US', { month: 'short', day: 'numeric' }))
}

for (const zone of ZONES) {
  test.describe(`A student in ${zone}`, () => {
    test.use({ timezoneId: zone })

    test('studying counts for today where the student is', async ({ page }) => {
      await register(page, newUser())
      await createDeckWithCards(page, `Zone ${Date.now()}`, [{ front: 'Zone question', back: 'Zone answer' }])

      await page.getByRole('button', { name: /Study/ }).first().click()
      await page.getByRole('button', { name: 'Reveal Answer' }).click()
      await page.getByRole('button', { name: /^Good/ }).click()

      await page.goto('/analytics')
      const label = await todayLabel(page)
      await expect(page.getByRole('img', { name: new RegExp(`^${label}: 1 card`) })).toBeVisible()

      // The profile says which zone is used
      await page.goto('/profile')
      await expect(page.getByText(zone)).toBeVisible()
    })
  })
}
