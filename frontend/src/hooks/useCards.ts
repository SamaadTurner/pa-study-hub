import { useState, useEffect, useCallback } from 'react'
import type { Card } from '@/types'
import * as decksApi from '@/api/decks'
import { useNotification } from '@/contexts/NotificationContext'
import type { ApiError } from '@/types'

export function useCards(deckId: string) {
  const [cards, setCards]     = useState<Card[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError]     = useState<string | null>(null)
  const { notify }            = useNotification()

  const fetchCards = useCallback(async () => {
    if (!deckId) return
    setLoading(true)
    setError(null)
    try {
      const data = await decksApi.getCards(deckId)
      setCards(data)
    } catch (err) {
      const msg = (err as ApiError)?.detail ?? 'Failed to load cards'
      setError(msg)
    } finally {
      setLoading(false)
    }
  }, [deckId])

  useEffect(() => { fetchCards() }, [fetchCards])

  const createCard = useCallback(
    async (payload: { front: string; back: string; hint?: string; tags?: string[] }) => {
      const card = await decksApi.createCard(deckId, payload)
      setCards(prev => [...prev, card])
      notify('success', 'Card added!')
      return card
    },
    [deckId, notify]
  )

  const updateCard = useCallback(
    async (cardId: string, payload: Partial<{ front: string; back: string; hint?: string; tags?: string[] }>) => {
      const updated = await decksApi.updateCard(cardId, payload)
      setCards(prev => prev.map(c => c.id === cardId ? updated : c))
      notify('success', 'Card updated.')
      return updated
    },
    [notify]
  )

  const deleteCard = useCallback(
    async (cardId: string) => {
      await decksApi.deleteCard(cardId)
      setCards(prev => prev.filter(c => c.id !== cardId))
      notify('success', 'Card deleted.')
    },
    [notify]
  )

  return { cards, loading, error, refetch: fetchCards, createCard, updateCard, deleteCard }
}
