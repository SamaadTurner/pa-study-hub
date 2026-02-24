import { useState, useMemo, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import { getPublicDecks, cloneDeck } from '@/api/decks'
import type { Deck, ApiError } from '@/types'
import LoadingSpinner from '@/components/common/LoadingSpinner'
import EmptyState from '@/components/common/EmptyState'
import CategoryFilter from '@/components/flashcards/CategoryFilter'
import Button from '@/components/common/Button'
import Badge from '@/components/common/Badge'
import { useNotification } from '@/contexts/NotificationContext'
import { getCategoryLabel } from '@/utils/categoryColors'

export default function PublicDecksPage() {
  const navigate = useNavigate()
  const { notify } = useNotification()

  const [decks, setDecks] = useState<Deck[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [search, setSearch] = useState('')
  const [selectedCategory, setSelectedCategory] = useState<string | null>(null)
  const [cloningDeckId, setCloningDeckId] = useState<string | null>(null)

  useEffect(() => {
    loadPublicDecks()
  }, [])

  async function loadPublicDecks() {
    try {
      const data = await getPublicDecks()
      setDecks(data)
    } catch (err) {
      setError((err as ApiError)?.detail ?? 'Failed to load public decks')
    } finally {
      setLoading(false)
    }
  }

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

  async function handleClone(deckId: string) {
    setCloningDeckId(deckId)
    try {
      const clonedDeck = await cloneDeck(deckId)
      notify('success', `Cloned deck: ${clonedDeck.title}`)
      navigate(`/decks/${clonedDeck.id}`)
    } catch (err) {
      notify('error', (err as ApiError)?.detail ?? 'Failed to clone deck')
    } finally {
      setCloningDeckId(null)
    }
  }

  if (loading) {
    return (
      <div className="flex items-center justify-center py-24">
        <LoadingSpinner message="Loading public decks..." size="lg" />
      </div>
    )
  }

  if (error) {
    return (
      <EmptyState
        icon="⚠️"
        title="Failed to load public decks"
        message={error}
        action={{ label: 'Try Again', onClick: loadPublicDecks }}
      />
    )
  }

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">Browse Public Decks</h1>
          <p className="text-gray-500 text-sm mt-0.5">
            Discover and clone decks created by other PA students
          </p>
        </div>
        <Button variant="secondary" onClick={() => navigate('/decks')}>
          My Decks
        </Button>
      </div>

      {/* Search + filter */}
      <div className="space-y-3">
        <input
          type="search"
          placeholder="Search public decks..."
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
          icon="🔍"
          title={decks.length === 0 ? 'No public decks available' : 'No decks match your search'}
          message={
            decks.length === 0
              ? 'Be the first to create a public deck!'
              : 'Try a different search term or category.'
          }
        />
      ) : (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
          {filtered.map(deck => (
            <PublicDeckCard
              key={deck.id}
              deck={deck}
              onClone={handleClone}
              isCloning={cloningDeckId === deck.id}
            />
          ))}
        </div>
      )}
    </div>
  )
}

interface PublicDeckCardProps {
  deck: Deck
  onClone: (deckId: string) => void
  isCloning: boolean
}

function PublicDeckCard({ deck, onClone, isCloning }: PublicDeckCardProps) {
  return (
    <div className="bg-white border border-gray-200 rounded-xl p-5 hover:shadow-md transition-shadow">
      <div className="flex items-start justify-between gap-2 mb-3">
        <h3 className="font-semibold text-gray-900 text-base leading-tight">
          {deck.title}
        </h3>
        <Badge category={deck.category} />
      </div>

      {deck.description && (
        <p className="text-sm text-gray-600 mb-4 line-clamp-2">
          {deck.description}
        </p>
      )}

      <div className="flex items-center justify-between text-sm text-gray-500 mb-4">
        <span>{deck.cardCount} {deck.cardCount === 1 ? 'card' : 'cards'}</span>
        <span className="text-xs">{getCategoryLabel(deck.category)}</span>
      </div>

      <Button
        onClick={() => onClone(deck.id)}
        disabled={isCloning}
        size="sm"
        className="w-full"
      >
        {isCloning ? 'Cloning...' : 'Clone to My Decks'}
      </Button>
    </div>
  )
}
