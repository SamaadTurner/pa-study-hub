import { test, expect } from '@playwright/test'
import { newUser, register, createDeck, openDeck } from './helpers'

test.describe('Fill-in-the-blank cards', () => {
  test('one text with two blanks makes two cards, and each hides only its own blank', async ({ page }) => {
    await register(page, newUser())
    const title = `Cloze deck ${Date.now()}`
    await createDeck(page, title)
    await openDeck(page, title)

    await page.getByRole('button', { name: /Add a Card|\+ Add Card/ }).first().click()
    await page.getByText('Fill in the blank', { exact: true }).click()

    // Make blank wraps the selected words
    const text = page.getByLabel(/Text with blanks/)
    await text.fill('Aspirin inhibits COX')
    await text.evaluate((box: HTMLTextAreaElement) => box.setSelectionRange(17, 20))
    await page.getByRole('button', { name: 'Make blank' }).click()
    await expect(text).toHaveValue('Aspirin inhibits {{c1::COX}}')

    await text.fill('{{c1::Aspirin}} irreversibly inhibits {{c2::COX::enzyme}}')
    await expect(page.getByText('Makes 2 cards (c1, c2)')).toBeVisible()
    await page.getByLabel(/Extra notes/).fill('Low dose prevents clots')
    await page.getByRole('button', { name: 'Save Cards' }).click()

    await expect(page).toHaveURL(/\/decks\/[0-9a-f-]{36}$/)
    await expect(page.getByText('Blank c1')).toBeVisible()
    await expect(page.getByText('Blank c2')).toBeVisible()

    // Studying: the first card hides "Aspirin" and shows "COX"
    await page.getByRole('button', { name: 'Study (2)' }).click()
    await expect(page.getByText('Fill in the blank', { exact: true }).first()).toBeVisible()
    await expect(page.getByText('[...]').first()).toBeVisible()
    await page.getByRole('button', { name: 'Reveal Answer', exact: true }).click()
    await expect(page.locator('mark', { hasText: 'Aspirin' })).toBeVisible()
    await expect(page.getByText('Low dose prevents clots')).toBeVisible()
    await page.getByRole('button', { name: /^Good/ }).click()

    // The second card hides "COX" behind its hint and shows "Aspirin"
    await expect(page.getByText('[enzyme]').first()).toBeVisible()
    await page.getByRole('button', { name: 'Reveal Answer', exact: true }).click()
    await expect(page.locator('mark', { hasText: 'COX' })).toBeVisible()
  })
})
