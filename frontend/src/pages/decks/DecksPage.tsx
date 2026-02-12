import { useState, useMemo, useEffect } from 'react'
import { useSearchParams } from 'react-router-dom'
import { useDecks } from '@/hooks/useDecks'
import type { Deck } from '@/types'
import LoadingSpinner from '@/components/common/LoadingSpinner'
import EmptyState from '@/components/common/EmptyState'
import DeckCard from '@/components/flashcards/DeckCard'
import CategoryFilter from '@/components/flashcards/CategoryFilter'
import DeckFormModal from '@/components/flashcards/DeckFormModal'
import Button from '@/components/common/Button'
import { useNotification } from '@/contexts/NotificationContext'
import type { ApiError } from '@/types'

export default function DecksPage() {
  const { decks, loading, error, createDeck, deleteDeck, updateDeck } = useDecks()
  const { notify } = useNotification()
  const [searchParams, setSearchParams] = useSearchParams()

  const [search, setSearch]         = useState('')
  const [selectedCategory, setSelectedCategory] = useState<string | null>(
    searchParams.get('category')
  )
  const [modalOpen, setModalOpen]   = useState(searchParams.get('create') === 'true')
  const [editingDeck, setEditingDeck] = useState<Deck | null>(null)

  // Clear the ?create=true param after opening
  useEffect(() => {
    if (searchParams.get('create') === 'true') {
      setSearchParams({}, { replace: true })
    }
  }, []) // eslint-disable-line react-hooks/exhaustive-deps

  const availableCategories = useMemo(
    () => [...new Set(decks.map(d => d.category))].sort(),
    [decks]
  )

  const filtered = useMemo(() => {
    return decks.filter(d => {
      const matchesSearch = !search ||
        d.title.toLowerCase().includes(search.toLowerCase()) ||
        (d.description ?? '').toLowerCase().includes(search.toLowerCase())
      const matchesCategory = !selectedCategory || d.category === selectedCategory
      return matchesSearch && matchesCategory
    })
  }, [decks, search, selectedCategory])

  async function handleCreate(data: { title: string; description: string; category: string; isPublic: boolean }) {
    try {
      await createDeck(data)
    } catch (err) {
      notify('error', (err as ApiError)?.detail ?? 'Failed to create deck')
      throw err
    }
  }

  async function handleUpdate(data: { title: string; description: string; category: string; isPublic: boolean }) {
    if (!editingDeck) return
    try {
      await updateDeck(editingDeck.id, data)
      setEditingDeck(null)
    } catch (err) {
      notify('error', (err as ApiError)?.detail ?? 'Failed to update deck')
      throw err
    }
  }

  function openEdit(deck: Deck) {
    setEditingDeck(deck)
    setModalOpen(true)
  }

  function closeModal() {
    setModalOpen(false)
    setEditingDeck(null)
  }

  if (loading) {
    return (
      <div className="flex items-center justify-center py-24">
        <LoadingSpinner message="Loading your decks..." size="lg" />
      </div>
    )
  }

  if (error) {
    return (
      <EmptyState
        icon="⚠️"
        title="Failed to load decks"
        message={error}
        action={{ label: 'Try Again', onClick: () => window.location.reload() }}
      />
    )
  }

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">Flashcard Decks</h1>
          <p className="text-gray-500 text-sm mt-0.5">
            {decks.length} {decks.length === 1 ? 'deck' : 'decks'} total
          </p>
        </div>
        <Button onClick={() => setModalOpen(true)}>+ Create Deck</Button>
      </div>

      {/* Search + filter */}
      <div className="space-y-3">
        <input
          type="search"
          placeholder="Search decks..."
          value={search}
          onChange={e => setSearch(e.target.value)}
          className="w-full max-w-sm px-4 py-2 border border-gray-300 rounded-lg text-sm
            focus:outline-none focus:ring-2 focus:ring-primary-500"
        />
        <CategoryFilter
          selected={selectedCategory}
          onChange={setSelectedCategory}
          availableCategories={availableCategories}
        />
      </div>

      {/* Deck grid */}
      {filtered.length === 0 ? (
        <EmptyState
          icon="🃏"
          title={decks.length === 0 ? 'No decks yet' : 'No decks match your search'}
          message={
            decks.length === 0
              ? 'Create your first flashcard deck to start studying.'
              : 'Try a different search term or category.'
          }
          action={
            decks.length === 0
              ? { label: 'Create your first deck', onClick: () => setModalOpen(true) }
              : undefined
          }
        />
      ) : (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
          {filtered.map(deck => (
            <DeckCard
              key={deck.id}
              deck={deck}
              onDelete={deleteDeck}
              onEdit={openEdit}
            />
          ))}
        </div>
      )}

      {/* Create / Edit modal */}
      <DeckFormModal
        isOpen={modalOpen}
        onClose={closeModal}
        onSubmit={editingDeck ? handleUpdate : handleCreate}
        editDeck={editingDeck}
      />
    </div>
  )
}
