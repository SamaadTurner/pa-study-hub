import apiClient from './client'
import type { Card, Deck, ReviewCard, ReviewResult } from '@/types'

// Backend response types (match Java DTOs)
interface DeckResponse {
  id: string
  userId?: string
  title: string
  description: string
  category: string
  isPublic: boolean
  cardCount: number
  cardsToReview: number  // Backend uses "cardsToReview" instead of "dueCardCount"
  createdAt: string
  updatedAt: string
}

interface ReviewScheduleInfo {
  nextReviewDate: string
  interval: number
  easeFactor: number
  repetitions: number
}

interface CardResponse {
  id: string
  deckId: string
  front: string
  back: string
  hint?: string
  imageUrl?: string
  tags: string[]
  reviewSchedule?: ReviewScheduleInfo  // Backend nests review info
  createdAt: string
  updatedAt?: string
}

interface ReviewResponse {
  nextReviewDate: string
  interval: number
  easeFactor: number
  repetitions: number
  message?: string
}

// Mapping functions
function mapDeck(response: DeckResponse): Deck {
  return {
    id: response.id,
    title: response.title,
    description: response.description,
    category: response.category,
    isPublic: response.isPublic,
    cardCount: response.cardCount,
    dueCardCount: response.cardsToReview,  // Map field name
    avgEaseFactor: 2.5,  // Default value since backend doesn't provide this
    createdAt: response.createdAt,
    updatedAt: response.updatedAt
  }
}

function mapCard(response: CardResponse): Card {
  return {
    id: response.id,
    deckId: response.deckId,
    front: response.front,
    back: response.back,
    hint: response.hint,
    tags: response.tags || [],
    easeFactor: response.reviewSchedule?.easeFactor ?? 2.5,
    interval: response.reviewSchedule?.interval ?? 0,
    repetitions: response.reviewSchedule?.repetitions ?? 0,
    nextReviewDate: response.reviewSchedule?.nextReviewDate ?? new Date().toISOString(),
    createdAt: response.createdAt
  }
}

function mapReviewResult(response: ReviewResponse, cardId: string, quality: number): ReviewResult {
  return {
    cardId,
    quality,
    newInterval: response.interval,
    newEaseFactor: response.easeFactor,
    nextReviewDate: response.nextReviewDate
  }
}

// ---- Decks -----------------------------------------------------------------

export async function getDecks(): Promise<Deck[]> {
  const { data } = await apiClient.get<{ content: DeckResponse[] }>('/decks')
  const decks = data.content || []
  return decks.map(mapDeck)
}

export async function getPublicDecks(): Promise<Deck[]> {
  const { data } = await apiClient.get<{ content: DeckResponse[] }>('/decks/public')
  const decks = data.content || []
  return decks.map(mapDeck)
}

export async function getDeck(deckId: string): Promise<Deck> {
  const { data } = await apiClient.get<DeckResponse>(`/decks/${deckId}`)
  return mapDeck(data)
}

export async function createDeck(payload: { title: string; description: string; category: string; isPublic: boolean }): Promise<Deck> {
  const { data } = await apiClient.post<DeckResponse>('/decks', payload)
  return mapDeck(data)
}

export async function updateDeck(deckId: string, payload: Partial<{ title: string; description: string; isPublic: boolean }>): Promise<Deck> {
  const { data } = await apiClient.put<DeckResponse>(`/decks/${deckId}`, payload)
  return mapDeck(data)
}

export async function deleteDeck(deckId: string): Promise<void> {
  await apiClient.delete(`/decks/${deckId}`)
}

export async function cloneDeck(deckId: string): Promise<Deck> {
  const { data } = await apiClient.post<DeckResponse>(`/decks/${deckId}/clone`)
  return mapDeck(data)
}

// ---- Cards -----------------------------------------------------------------

export async function getCards(deckId: string): Promise<Card[]> {
  const { data } = await apiClient.get<{ content: CardResponse[] }>(`/decks/${deckId}/cards`)
  const cards = data.content || []
  return cards.map(mapCard)
}

export async function createCard(deckId: string, payload: { front: string; back: string; hint?: string; tags?: string[] }): Promise<Card> {
  const { data } = await apiClient.post<CardResponse>(`/decks/${deckId}/cards`, payload)
  return mapCard(data)
}

export async function updateCard(cardId: string, payload: Partial<{ front: string; back: string; hint?: string; tags?: string[] }>): Promise<Card> {
  const { data } = await apiClient.put<CardResponse>(`/cards/${cardId}`, payload)
  return mapCard(data)
}

export async function deleteCard(cardId: string): Promise<void> {
  await apiClient.delete(`/cards/${cardId}`)
}

// ---- Review ----------------------------------------------------------------

export async function getDueCards(deckId: string): Promise<ReviewCard[]> {
  const { data } = await apiClient.get<CardResponse[]>(`/decks/${deckId}/review`)
  // Map CardResponse to ReviewCard (only include needed fields)
  return data.map(card => ({
    id: card.id,
    deckId: card.deckId,
    front: card.front,
    // the answer, shown when the card is flipped
    back: card.back,
    hint: card.hint,
    tags: card.tags || []
  }))
}

// timeSpentSeconds is how long the card was on screen. The backend adds it to
// today's study time, which is what the daily minutes goal is measured against.
export async function submitReview(
  cardId: string,
  quality: number,
  timeSpentSeconds: number
): Promise<ReviewResult> {
  const { data } = await apiClient.post<ReviewResponse>(`/cards/${cardId}/review`, {
    quality,
    timeSpentSeconds,
  })
  return mapReviewResult(data, cardId, quality)
}
