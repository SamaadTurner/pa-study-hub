import apiClient from './client'
import type { Card, Deck, ReviewCard, ReviewResult } from '@/types'

// ---- Decks -----------------------------------------------------------------

export async function getDecks(): Promise<Deck[]> {
  const { data } = await apiClient.get<Deck[]>('/decks')
  return data
}

export async function getPublicDecks(): Promise<Deck[]> {
  const { data } = await apiClient.get<Deck[]>('/decks/public')
  return data
}

export async function getDeck(deckId: string): Promise<Deck> {
  const { data } = await apiClient.get<Deck>(`/decks/${deckId}`)
  return data
}

export async function createDeck(payload: { title: string; description: string; category: string; isPublic: boolean }): Promise<Deck> {
  const { data } = await apiClient.post<Deck>('/decks', payload)
  return data
}

export async function updateDeck(deckId: string, payload: Partial<{ title: string; description: string; isPublic: boolean }>): Promise<Deck> {
  const { data } = await apiClient.put<Deck>(`/decks/${deckId}`, payload)
  return data
}

export async function deleteDeck(deckId: string): Promise<void> {
  await apiClient.delete(`/decks/${deckId}`)
}

export async function cloneDeck(deckId: string): Promise<Deck> {
  const { data } = await apiClient.post<Deck>(`/decks/${deckId}/clone`)
  return data
}

// ---- Cards -----------------------------------------------------------------

export async function getCards(deckId: string): Promise<Card[]> {
  const { data } = await apiClient.get<Card[]>(`/decks/${deckId}/cards`)
  return data
}

export async function createCard(deckId: string, payload: { front: string; back: string; hint?: string; tags?: string[] }): Promise<Card> {
  const { data } = await apiClient.post<Card>(`/decks/${deckId}/cards`, payload)
  return data
}

export async function updateCard(cardId: string, payload: Partial<{ front: string; back: string; hint?: string; tags?: string[] }>): Promise<Card> {
  const { data } = await apiClient.put<Card>(`/cards/${cardId}`, payload)
  return data
}

export async function deleteCard(cardId: string): Promise<void> {
  await apiClient.delete(`/cards/${cardId}`)
}

// ---- Review ----------------------------------------------------------------

export async function getDueCards(deckId: string): Promise<ReviewCard[]> {
  const { data } = await apiClient.get<ReviewCard[]>(`/decks/${deckId}/review`)
  return data
}

export async function submitReview(cardId: string, quality: number): Promise<ReviewResult> {
  const { data } = await apiClient.post<ReviewResult>(`/cards/${cardId}/review`, { quality })
  return data
}
