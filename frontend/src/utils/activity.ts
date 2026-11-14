import type { DailyProgressPoint } from '@/types'
import { formatShortDate } from '@/utils/formatDate'

// How busy a day was, for the colour of its square on the analytics page
export type ActivityLevel = 'none' | 'light' | 'active' | 'goalMet'

// From this many cards and questions a day counts as "active" instead of "light"
const ACTIVE_FROM = 10

// Everything studied on a day: flashcards plus practice exam questions.
// A day with only an exam is still a day of studying.
export function itemsStudied(point: DailyProgressPoint): number {
  return point.cardsReviewed + point.examQuestions
}

export function activityLevel(point: DailyProgressPoint): ActivityLevel {
  const items = itemsStudied(point)
  if (items === 0) return 'none'
  if (point.goalMet) return 'goalMet'
  if (items >= ACTIVE_FROM) return 'active'
  return 'light'
}

// One line about a day, for example "Nov 12: 3 cards, 10 exam questions, 70% accuracy"
export function describeDay(point: DailyProgressPoint): string {
  const day = formatShortDate(point.date)
  if (itemsStudied(point) === 0) {
    return `${day}: no activity`
  }

  const parts: string[] = []
  if (point.cardsReviewed > 0) {
    parts.push(`${point.cardsReviewed} ${point.cardsReviewed === 1 ? 'card' : 'cards'}`)
  }
  if (point.examQuestions > 0) {
    parts.push(`${point.examQuestions} exam ${point.examQuestions === 1 ? 'question' : 'questions'}`)
  }
  parts.push(`${Math.round(point.accuracy)}% accuracy`)
  return `${day}: ${parts.join(', ')}`
}
