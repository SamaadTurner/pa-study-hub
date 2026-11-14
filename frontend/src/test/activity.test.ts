import { describe, it, expect } from 'vitest'
import type { DailyProgressPoint } from '@/types'
import { itemsStudied, activityLevel, describeDay } from '@/utils/activity'

// A day with nothing studied. Each test changes only what it is about.
function day(changes: Partial<DailyProgressPoint> = {}): DailyProgressPoint {
  return {
    date: '2026-11-12',
    cardsReviewed: 0,
    examQuestions: 0,
    accuracy: 0,
    studyMinutes: 0,
    goalMet: false,
    ...changes,
  }
}

describe('itemsStudied', () => {
  it('adds flashcards and exam questions', () => {
    expect(itemsStudied(day({ cardsReviewed: 3, examQuestions: 5 }))).toBe(8)
  })

  it('is 0 for a day without studying', () => {
    expect(itemsStudied(day())).toBe(0)
  })
})

describe('activityLevel', () => {
  it('is "none" when nothing was studied', () => {
    expect(activityLevel(day())).toBe('none')
  })

  it('is "light" for a few cards', () => {
    expect(activityLevel(day({ cardsReviewed: 9 }))).toBe('light')
  })

  it('counts a day with only a practice exam', () => {
    // This day used to show as "no activity" because only cards were looked at
    expect(activityLevel(day({ examQuestions: 5 }))).toBe('light')
  })

  it('is "active" from 10 items, cards and exam questions together', () => {
    expect(activityLevel(day({ cardsReviewed: 10 }))).toBe('active')
    expect(activityLevel(day({ cardsReviewed: 4, examQuestions: 6 }))).toBe('active')
  })

  it('is "goalMet" when the daily goal was reached, whatever the count', () => {
    expect(activityLevel(day({ cardsReviewed: 25, goalMet: true }))).toBe('goalMet')
  })
})

describe('describeDay', () => {
  it('says so when nothing was studied', () => {
    expect(describeDay(day())).toBe('Nov 12: no activity')
  })

  it('lists cards, exam questions and accuracy', () => {
    const text = describeDay(day({ cardsReviewed: 3, examQuestions: 10, accuracy: 69.6 }))
    expect(text).toBe('Nov 12: 3 cards, 10 exam questions, 70% accuracy')
  })

  it('uses the singular for one card and one question', () => {
    const text = describeDay(day({ cardsReviewed: 1, examQuestions: 1, accuracy: 100 }))
    expect(text).toBe('Nov 12: 1 card, 1 exam question, 100% accuracy')
  })

  it('leaves out what was not done that day', () => {
    expect(describeDay(day({ examQuestions: 5, accuracy: 80 }))).toBe('Nov 12: 5 exam questions, 80% accuracy')
    expect(describeDay(day({ cardsReviewed: 12, accuracy: 50 }))).toBe('Nov 12: 12 cards, 50% accuracy')
  })
})
