import apiClient from './client'
import { fetchAllPages } from './pagination'
import type { Card, CardType, Deck, OcclusionBox, PaginatedResponse, ReviewCard, ReviewResult } from '@/types'

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
  cardType?: CardType
  clozeIndex?: number | null
  imageId?: string | null
  boxes?: OcclusionBox[] | null
  pictureGroup?: string | null
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
    userId: response.userId,
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
    cardType: response.cardType ?? 'BASIC',
    clozeIndex: response.clozeIndex ?? null,
    imageId: response.imageId ?? null,
    boxes: response.boxes ?? [],
    pictureGroup: response.pictureGroup ?? null,
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

// All of the user's own decks. The backend answers in pages, and this used to
// take only the first one, so a user never saw more than their 20 newest decks.
export async function getDecks(): Promise<Deck[]> {
  const decks = await fetchAllPages<DeckResponse>('/decks')
  return decks.map(mapDeck)
}

export const PUBLIC_DECKS_PAGE_SIZE = 20

export interface PublicDeckPage {
  decks: Deck[]
  totalDecks: number  // how many public decks match, counting the pages not loaded yet
  hasMore: boolean
}

// One page of the public deck list. The search and the category are sent to the
// backend, so they look through every public deck and not only the ones on screen.
export async function getPublicDecks(
  options: { search?: string; category?: string | null; page?: number } = {}
): Promise<PublicDeckPage> {
  const params: Record<string, string | number> = {
    page: options.page ?? 0,
    size: PUBLIC_DECKS_PAGE_SIZE,
  }
  if (options.search) params.search = options.search
  if (options.category) params.category = options.category

  const { data } = await apiClient.get<PaginatedResponse<DeckResponse>>('/decks/public', { params })
  return {
    decks: (data.content || []).map(mapDeck),
    totalDecks: data.totalElements,
    hasMore: !data.last,
  }
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

// All cards of a deck, in the order they were added (not only the first 50)
export async function getCards(deckId: string): Promise<Card[]> {
  const cards = await fetchAllPages<CardResponse>(`/decks/${deckId}/cards`)
  return cards.map(mapCard)
}

export async function createCard(deckId: string, payload: { front: string; back: string; hint?: string; tags?: string[] }): Promise<Card> {
  const { data } = await apiClient.post<CardResponse>(`/decks/${deckId}/cards`, payload)
  return mapCard(data)
}

// Fill-in-the-blank cards: one text makes one card per blank number
export async function createClozeCards(
  deckId: string,
  payload: { text: string; extra?: string; tags?: string[] },
): Promise<Card[]> {
  const { data } = await apiClient.post<CardResponse[]>(`/decks/${deckId}/cloze-cards`, payload)
  return data.map(mapCard)
}

// Uploads a picture for picture cards. Nothing uses it until createImageCards.
export async function uploadPicture(picture: Blob): Promise<{ id: string; width: number; height: number }> {
  const form = new FormData()
  form.append('file', picture, 'picture.jpg')
  // The client sends JSON by default; FormData needs its own type (axios adds the boundary)
  const { data } = await apiClient.post('/images', form, { headers: { 'Content-Type': 'multipart/form-data' } })
  return data
}

// One card per box
export async function createImageCards(
  deckId: string,
  payload: { imageId: string; prompt?: string; extra?: string; boxes: OcclusionBox[]; tags?: string[] },
): Promise<Card[]> {
  const { data } = await apiClient.post<CardResponse[]>(`/decks/${deckId}/image-cards`, payload)
  return data.map(mapCard)
}

// Changes the boxes of all cards of one picture. A box with a cardId keeps that card
// (and its study history); one without gets a new card; cards left out are deleted.
export async function updatePictureCards(
  cardId: string,
  payload: { prompt?: string; extra?: string; tags?: string[]; boxes: (OcclusionBox & { cardId?: string })[] },
): Promise<Card[]> {
  const { data } = await apiClient.put<CardResponse[]>(`/cards/${cardId}/picture`, payload)
  return data.map(mapCard)
}

// The picture as a blob, because an <img> tag can't send the sign-in token
export async function getPicture(imageId: string): Promise<Blob> {
  const { data } = await apiClient.get<Blob>(`/images/${imageId}`, { responseType: 'blob' })
  return data
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
    cardType: card.cardType ?? 'BASIC',
    clozeIndex: card.clozeIndex ?? null,
    imageId: card.imageId ?? null,
    boxes: card.boxes ?? [],
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
