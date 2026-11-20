import { test, expect, type Page } from '@playwright/test'
import AxeBuilder from '@axe-core/playwright'
import { newUser, register, createDeck, openDeck } from './helpers'

// A 400 x 300 PNG made by the browser itself, so the test needs no picture file
async function makePng(page: Page): Promise<Buffer> {
  const dataUrl = await page.evaluate(() => {
    const canvas = document.createElement('canvas')
    canvas.width = 400
    canvas.height = 300
    const context = canvas.getContext('2d')!
    context.fillStyle = '#f1f5f9'
    context.fillRect(0, 0, 400, 300)
    context.fillStyle = '#b91c1c'
    context.fillRect(60, 60, 120, 90)
    return canvas.toDataURL('image/png')
  })
  return Buffer.from(dataUrl.split(',')[1], 'base64')
}

async function choosePicture(page: Page) {
  const png = await makePng(page)
  await page.getByLabel('Choose a picture').setInputFiles({ name: 'heart.png', mimeType: 'image/png', buffer: png })
  await expect(page.getByAltText('The picture to cover')).toBeVisible()
}

// Drags across the picture from one spot to another (fractions of its size)
async function drawBox(page: Page, from: [number, number], to: [number, number]) {
  const surface = page.getByRole('application')
  // The mouse can only press inside the window, so the whole picture must be on screen
  await surface.evaluate(element => element.scrollIntoView({ block: 'center' }))
  const box = (await surface.boundingBox())!
  expect(box.y).toBeGreaterThanOrEqual(0)
  expect(box.y + box.height).toBeLessThanOrEqual(page.viewportSize()!.height)
  await page.mouse.move(box.x + box.width * from[0], box.y + box.height * from[1])
  await page.mouse.down()
  await page.mouse.move(box.x + box.width * to[0], box.y + box.height * to[1], { steps: 5 })
  await page.mouse.up()
}

// axe (WCAG 2.1 A and AA) on the new picture screens
async function expectNoViolations(page: Page, screen: string) {
  const results = await new AxeBuilder({ page }).withTags(['wcag2a', 'wcag2aa', 'wcag21a', 'wcag21aa']).analyze()
  expect(results.violations.map(v => `${v.id}: ${v.nodes.map(n => n.target.join(' ')).join(' | ')}`), screen).toEqual([])
}

async function startPictureCard(page: Page) {
  await page.getByRole('button', { name: /Add a Card|\+ Add Card/ }).first().click()
  await expect(page).toHaveURL(/\/cards\/new$/)
  await page.getByText('Picture', { exact: true }).click()
}

test.describe('Picture cards', () => {
  test('boxes drawn on a picture become cards that hide one part each', async ({ page }) => {
    await register(page, newUser())
    const title = `Heart ${Date.now()}`
    await createDeck(page, title)
    await openDeck(page, title)
    await startPictureCard(page)
    await choosePicture(page)

    await drawBox(page, [0.1, 0.15], [0.5, 0.5])
    await drawBox(page, [0.6, 0.6], [0.9, 0.85])
    // A tap is not a box
    await drawBox(page, [0.55, 0.1], [0.555, 0.105])
    await expect(page.getByText('Makes 2 cards.')).toBeVisible()

    // A third box from the keyboard, then removed again
    await page.getByRole('button', { name: 'Add box' }).click()
    await page.keyboard.press('ArrowRight')
    await expect(page.getByText('Makes 3 cards.')).toBeVisible()
    await page.getByRole('button', { name: 'Remove box 3' }).click()

    await expectNoViolations(page, 'picture editor')
    await page.getByLabel('Answer for box 1').fill('Left atrium')
    await page.getByRole('textbox', { name: /^Question/ }).fill('Name the chamber')
    await page.getByRole('button', { name: 'Save Cards' }).click()

    await expect(page).toHaveURL(/\/decks\/[0-9a-f-]{36}$/)
    await expect(page.getByText('2 cards').first()).toBeVisible()
    await expect(page.getByText('Picture, box 1')).toBeVisible()
    await expect(page.getByText('Picture, box 2')).toBeVisible()

    // Studying: the box is covered, then opened up with its answer
    await page.getByRole('button', { name: /Study/ }).first().click()
    await expect(page.getByText('Name the chamber')).toBeVisible()
    await expect(page.getByAltText('Picture for this card')).toBeVisible()
    await expect(page.getByText('Answer: Left atrium')).toBeHidden()
    await page.getByRole('button', { name: 'Reveal Answer' }).click()
    await expect(page.getByText('Answer: Left atrium')).toBeVisible()
    await expectNoViolations(page, 'picture card answer')
  })

  test('a picture and at least one box are needed, and only pictures are taken', async ({ page }) => {
    await register(page, newUser())
    const title = `Checks ${Date.now()}`
    await createDeck(page, title)
    await openDeck(page, title)
    await startPictureCard(page)

    await page.getByRole('button', { name: 'Save Cards' }).click()
    await expect(page.getByRole('alert').filter({ hasText: 'Choose a picture first' })).toBeVisible()

    await page.getByLabel('Choose a picture').setInputFiles({ name: 'notes.txt', mimeType: 'text/plain', buffer: Buffer.from('hello') })
    await expect(page.getByText('Choose a picture file (PNG or JPEG).')).toBeVisible()

    await choosePicture(page)
    await page.getByRole('button', { name: 'Save Cards' }).click()
    await expect(page.getByText('Draw at least one box over the part to hide')).toBeVisible()
  })

  test("another student sees the picture of a public deck and can copy it", async ({ page, browser }) => {
    const ownerContext = await browser.newContext()
    const ownerPage = await ownerContext.newPage()
    await register(ownerPage, newUser())
    const title = `Public heart ${Date.now()}`
    await createDeck(ownerPage, title, { isPublic: true })
    await openDeck(ownerPage, title)
    await startPictureCard(ownerPage)
    await choosePicture(ownerPage)
    await drawBox(ownerPage, [0.2, 0.2], [0.4, 0.4])
    await ownerPage.getByLabel('Answer for box 1').fill('Apex')
    await ownerPage.getByRole('button', { name: 'Save Cards' }).click()
    await expect(ownerPage.getByText('Picture, box 1')).toBeVisible()
    const deckPath = new URL(ownerPage.url()).pathname
    await ownerContext.close()

    await register(page, newUser())
    await page.goto(deckPath)
    await page.getByText('Picture, box 1').click()
    const picture = page.getByAltText('Picture for this card')
    await expect(picture).toBeVisible()
    // The picture really loaded (the visitor is allowed to see it)
    await expect.poll(() => picture.evaluate(img => (img as HTMLImageElement).naturalWidth)).toBeGreaterThan(0)
    await expect(page.getByText('Answer: Apex')).toBeVisible()

    await page.getByRole('button', { name: 'Copy to my decks' }).click()
    await expect(page.getByText('Deck copied to My Decks')).toBeVisible()
    await page.getByText('Picture, box 1').click()
    await expect.poll(() => page.getByAltText('Picture for this card').evaluate(img => (img as HTMLImageElement).naturalWidth))
      .toBeGreaterThan(0)
  })
})
