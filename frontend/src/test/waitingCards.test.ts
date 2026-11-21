import { describe, expect, it } from 'vitest'
import { WAITING_CARDS, randomStart, wakeProgress } from '@/utils/waitingCards'

describe('waiting cards', () => {
  it('has both kinds, each short enough to read in a few seconds, and no repeats', () => {
    expect(WAITING_CARDS.some(c => c.kind === 'fact')).toBe(true)
    expect(WAITING_CARDS.some(c => c.kind === 'review')).toBe(true)
    const texts = WAITING_CARDS.map(c => (c.kind === 'fact' ? c.text : c.question + c.answer))
    expect(new Set(texts).size).toBe(texts.length)
    for (const card of WAITING_CARDS) {
      const words = (card.kind === 'fact' ? card.text : `${card.question} ${card.answer}`).split(/\s+/).length
      expect(words, JSON.stringify(card)).toBeLessThanOrEqual(35)
      if (card.kind === 'review') expect(card.answer.trim()).not.toBe('')
    }
  })

  it('starts somewhere in the list', () => {
    for (let i = 0; i < 50; i++) {
      const start = randomStart()
      expect(start).toBeGreaterThanOrEqual(0)
      expect(start).toBeLessThan(WAITING_CARDS.length)
    }
  })

  it('fills the bar over the usual wake-up time but never all the way on its own', () => {
    expect(wakeProgress(0)).toBe(0)
    expect(wakeProgress(47)).toBe(49)
    expect(wakeProgress(95)).toBe(95)
    expect(wakeProgress(300)).toBe(95)
  })
})
