import { useState, useEffect, useCallback } from 'react'
import type { Deck } from '@/types'
import * as decksApi from '@/api/decks'
import { useNotification } from '@/contexts/NotificationContext'
import type { ApiError } from '@/types'

export function useDecks() {
  const [decks, setDecks]     = useState<Deck[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError]     = useState<string | null>(null)
  const { notify }            = useNotification()

  const fetchDecks = useCallback(async () => {
    setLoading(true)
    setError(null)
    try {
      const data = await decksApi.getDecks()
      setDecks(data)
    } catch (err) {
      const msg = (err as ApiError)?.detail ?? 'Failed to load decks'
      setError(msg)
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => { fetchDecks() }, [fetchDecks])

  const createDeck = useCallback(
    async (payload: { title: string; description: string; category: string; isPublic: boolean }) => {
      const deck = await decksApi.createDeck(payload)
      setDecks(prev => [deck, ...prev])
      notify('success', `Deck "${deck.title}" created!`)
      return deck
    },
    [notify]
  )

  const deleteDeck = useCallback(
    async (deckId: string) => {
      await decksApi.deleteDeck(deckId)
      setDecks(prev => prev.filter(d => d.id !== deckId))
      notify('success', 'Deck deleted.')
    },
    [notify]
  )

  const updateDeck = useCallback(
    async (deckId: string, payload: Partial<{ title: string; description: string; isPublic: boolean }>) => {
      const updated = await decksApi.updateDeck(deckId, payload)
      setDecks(prev => prev.map(d => d.id === deckId ? updated : d))
      notify('success', 'Deck updated.')
      return updated
    },
    [notify]
  )

  return { decks, loading, error, refetch: fetchDecks, createDeck, deleteDeck, updateDeck }
}
