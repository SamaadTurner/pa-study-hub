import { useState, useEffect, useRef } from 'react'
import { useNavigate } from 'react-router-dom'
import { getPublicDecks, cloneDeck } from '@/api/decks'
import type { Deck, ApiError } from '@/types'
import LoadingSpinner from '@/components/common/LoadingSpinner'
import EmptyState from '@/components/common/EmptyState'
import CategoryFilter from '@/components/flashcards/CategoryFilter'
import Button from '@/components/common/Button'
import Badge, { CATEGORY_BADGE_VARIANT } from '@/components/common/Badge'
import { useNotification } from '@/contexts/NotificationContext'
import { ALL_CATEGORIES, getCategoryLabel } from '@/utils/categoryColors'

// How long to wait after the last key press before searching
const SEARCH_DELAY_MS = 300

export default function PublicDecksPage() {
  const navigate = useNavigate()
  const { notify } = useNotification()

  // The decks loaded so far. "Load more" adds the next page to the end.
  const [decks, setDecks] = useState<Deck[]>([])
  const [totalDecks, setTotalDecks] = useState(0)
  const [lastLoadedPage, setLastLoadedPage] = useState(0)
  const [hasMore, setHasMore] = useState(false)

  const [loading, setLoading] = useState(true)
  const [loadingMore, setLoadingMore] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [retryCount, setRetryCount] = useState(0)

  // searchInput is what is in the box. search is what was last sent to the
  // backend, and only changes once the user stops typing for a moment.
  const [searchInput, setSearchInput] = useState('')
  const [search, setSearch] = useState('')
  const [selectedCategory, setSelectedCategory] = useState<string | null>(null)
  const [cloningDeckId, setCloningDeckId] = useState<string | null>(null)

  // Goes up every time a new search starts. A "Load more" answer that arrives
  // after the search has changed belongs to the old list and is thrown away.
  const searchNumber = useRef(0)

  useEffect(() => {
    const timer = setTimeout(() => setSearch(searchInput.trim()), SEARCH_DELAY_MS)
    return () => clearTimeout(timer)
  }, [searchInput])

  // Load the first page again whenever the search or the category changes
  useEffect(() => {
    let ignore = false
    searchNumber.current += 1
    setLoading(true)
    setError(null)

    getPublicDecks({ search, category: selectedCategory, page: 0 })
      .then(result => {
        if (ignore) return
        setDecks(result.decks)
        setTotalDecks(result.totalDecks)
        setHasMore(result.hasMore)
        setLastLoadedPage(0)
      })
      .catch(err => {
        if (!ignore) setError((err as ApiError)?.detail ?? 'Failed to load public decks')
      })
      .finally(() => {
        if (!ignore) setLoading(false)
      })

    // If the search changes before the answer is back, the old answer is ignored
    return () => { ignore = true }
  }, [search, selectedCategory, retryCount])

  async function handleLoadMore() {
    const startedDuringSearch = searchNumber.current
    setLoadingMore(true)
    try {
      const result = await getPublicDecks({
        search,
        category: selectedCategory,
        page: lastLoadedPage + 1,
      })
      if (startedDuringSearch !== searchNumber.current) return

      // If someone published a deck since the first page was loaded, every deck
      // has moved down one place and the next page starts with a deck that is
      // already on screen. Those are skipped.
      setDecks(current => {
        const alreadyShown = new Set(current.map(d => d.id))
        return [...current, ...result.decks.filter(d => !alreadyShown.has(d.id))]
      })
      setTotalDecks(result.totalDecks)
      setHasMore(result.hasMore)
      setLastLoadedPage(lastLoadedPage + 1)
    } catch (err) {
      notify('error', (err as ApiError)?.detail ?? 'Failed to load more decks')
    } finally {
      setLoadingMore(false)
    }
  }

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

  const isFiltering = search !== '' || selectedCategory !== null

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

      {/* Search + filter. These stay on screen while results load, so the search
          box keeps its focus while the user is typing. */}
      <div className="space-y-3">
        <input
          type="search"
          placeholder="Search public decks..."
          aria-label="Search public decks"
          value={searchInput}
          onChange={e => setSearchInput(e.target.value)}
          className="w-full max-w-sm px-4 py-2 border border-gray-300 rounded-lg text-sm
            focus:outline-none focus:ring-2 focus:ring-primary-500"
        />
        <CategoryFilter
          selected={selectedCategory}
          onChange={setSelectedCategory}
          availableCategories={ALL_CATEGORIES}
        />
      </div>

      {/* Results */}
      {error ? (
        <EmptyState
          icon="⚠️"
          title="Failed to load public decks"
          message={error}
          action={{ label: 'Try Again', onClick: () => setRetryCount(retryCount + 1) }}
        />
      ) : loading && decks.length === 0 ? (
        <div className="flex items-center justify-center py-24">
          <LoadingSpinner message="Loading public decks..." size="lg" />
        </div>
      ) : decks.length === 0 ? (
        <EmptyState
          icon="🔍"
          title={isFiltering ? 'No decks match your search' : 'No public decks available'}
          message={
            isFiltering
              ? 'Try a different search term or category.'
              : 'Be the first to create a public deck!'
          }
        />
      ) : (
        // The old results stay visible, a little faded, until the new ones arrive
        <div className={`space-y-4 ${loading ? 'opacity-60' : ''}`} aria-busy={loading}>
          <p className="text-sm text-gray-500">
            Showing {decks.length} of {totalDecks} {totalDecks === 1 ? 'deck' : 'decks'}
          </p>

          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
            {decks.map(deck => (
              <PublicDeckCard
                key={deck.id}
                deck={deck}
                onClone={handleClone}
                isCloning={cloningDeckId === deck.id}
              />
            ))}
          </div>

          {hasMore && (
            <div className="flex justify-center pt-2">
              <Button variant="secondary" onClick={handleLoadMore} disabled={loadingMore || loading}>
                {loadingMore ? 'Loading...' : 'Load more'}
              </Button>
            </div>
          )}
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
        <Badge variant={CATEGORY_BADGE_VARIANT[deck.category] || 'gray'}>
          {getCategoryLabel(deck.category)}
        </Badge>
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
