import { useState } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { useCards } from '@/hooks/useCards'
import { useDecks } from '@/hooks/useDecks'
import type { Card } from '@/types'
import LoadingSpinner from '@/components/common/LoadingSpinner'
import EmptyState from '@/components/common/EmptyState'
import Button from '@/components/common/Button'
import Badge from '@/components/common/Badge'
import { ConfirmModal } from '@/components/common/Modal'
import { getCategoryBadgeVariant, getCategoryLabel } from '@/utils/categoryColors'
import { formatRelativeDate } from '@/utils/formatDate'
import { useNotification } from '@/contexts/NotificationContext'
import type { ApiError } from '@/types'

export default function DeckDetailPage() {
  const { deckId }  = useParams<{ deckId: string }>()
  const navigate    = useNavigate()
  const { notify }  = useNotification()

  const { decks }                                 = useDecks()
  const { cards, loading, error, deleteCard }     = useCards(deckId!)

  const deck = decks.find(d => d.id === deckId)

  const [deleteTarget, setDeleteTarget]   = useState<Card | null>(null)
  const [deleting, setDeleting]           = useState(false)
  const [expandedCard, setExpandedCard]   = useState<string | null>(null)

  async function handleDeleteCard() {
    if (!deleteTarget) return
    setDeleting(true)
    try {
      await deleteCard(deleteTarget.id)
      setDeleteTarget(null)
    } catch (err) {
      notify('error', (err as ApiError)?.detail ?? 'Failed to delete card')
    } finally {
      setDeleting(false)
    }
  }

  if (loading) {
    return (
      <div className="flex items-center justify-center py-24">
        <LoadingSpinner message="Loading cards..." size="lg" />
      </div>
    )
  }

  if (error) {
    return (
      <EmptyState icon="⚠️" title="Failed to load cards" message={error}
        action={{ label: 'Go Back', onClick: () => navigate('/decks') }}
      />
    )
  }

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-start justify-between gap-4">
        <div>
          <button
            onClick={() => navigate('/decks')}
            className="text-sm text-primary-600 hover:underline mb-2 flex items-center gap-1"
          >
            ← Decks
          </button>
          <div className="flex items-center gap-3 flex-wrap">
            <h1 className="text-2xl font-bold text-gray-900">{deck?.title ?? 'Deck'}</h1>
            {deck && (
              <Badge variant={getCategoryBadgeVariant(deck.category)}>
                {getCategoryLabel(deck.category)}
              </Badge>
            )}
          </div>
          {deck?.description && (
            <p className="text-gray-500 text-sm mt-1">{deck.description}</p>
          )}
          <p className="text-gray-400 text-xs mt-1">
            {cards.length} {cards.length === 1 ? 'card' : 'cards'}
            {deck && deck.dueCardCount > 0 && (
              <span className="text-orange-600 ml-2">· {deck.dueCardCount} due for review</span>
            )}
          </p>
        </div>
        <div className="flex items-center gap-2">
          {deck && deck.dueCardCount > 0 && (
            <Button
              onClick={() => navigate(`/decks/${deckId}/review`)}
              size="sm"
            >
              Study ({deck.dueCardCount})
            </Button>
          )}
          <Button
            variant="secondary"
            size="sm"
            onClick={() => navigate(`/decks/${deckId}/cards/new`)}
          >
            + Add Card
          </Button>
        </div>
      </div>

      {/* Card list */}
      {cards.length === 0 ? (
        <EmptyState
          icon="📝"
          title="No cards yet"
          message="Add your first card to start studying this deck."
          action={{
            label: 'Add a Card',
            onClick: () => navigate(`/decks/${deckId}/cards/new`),
          }}
        />
      ) : (
        <div className="space-y-3">
          {cards.map((card, idx) => (
            <div
              key={card.id}
              className="bg-white border border-gray-200 rounded-xl overflow-hidden"
            >
              {/* Card header */}
              <button
                className="w-full px-5 py-4 flex items-start justify-between gap-4 text-left hover:bg-gray-50 transition-colors"
                onClick={() => setExpandedCard(expandedCard === card.id ? null : card.id)}
              >
                <div className="flex items-start gap-3 flex-1 min-w-0">
                  <span className="text-xs text-gray-400 font-mono mt-0.5 flex-shrink-0">
                    {String(idx + 1).padStart(2, '0')}
                  </span>
                  <p className="text-sm text-gray-900 font-medium leading-snug line-clamp-2">
                    {card.front}
                  </p>
                </div>
                <div className="flex items-center gap-2 flex-shrink-0">
                  {card.tags.length > 0 && (
                    <span className="text-xs text-gray-400">{card.tags.length} tags</span>
                  )}
                  <span className="text-gray-400 text-sm">
                    {expandedCard === card.id ? '▲' : '▼'}
                  </span>
                </div>
              </button>

              {/* Expanded content */}
              {expandedCard === card.id && (
                <div className="px-5 pb-4 border-t border-gray-100 space-y-3">
                  <div>
                    <p className="text-xs font-semibold text-gray-500 uppercase tracking-wide mb-1">
                      Answer
                    </p>
                    <p className="text-sm text-gray-800 leading-relaxed whitespace-pre-wrap">
                      {card.back}
                    </p>
                  </div>

                  {card.hint && (
                    <div>
                      <p className="text-xs font-semibold text-gray-500 uppercase tracking-wide mb-1">
                        Hint
                      </p>
                      <p className="text-sm text-gray-600 italic">{card.hint}</p>
                    </div>
                  )}

                  {card.tags.length > 0 && (
                    <div className="flex flex-wrap gap-1">
                      {card.tags.map(tag => (
                        <span
                          key={tag}
                          className="text-xs bg-gray-100 text-gray-600 px-2 py-0.5 rounded-full"
                        >
                          {tag}
                        </span>
                      ))}
                    </div>
                  )}

                  <div className="text-xs text-gray-400">
                    Next review: {formatRelativeDate(card.nextReviewDate)}
                    {' · '}Interval: {card.interval}d
                  </div>

                  {/* Card actions */}
                  <div className="flex gap-2 pt-1">
                    <Button
                      size="sm"
                      variant="secondary"
                      onClick={() => navigate(`/decks/${deckId}/cards/${card.id}/edit`)}
                    >
                      Edit
                    </Button>
                    <Button
                      size="sm"
                      variant="danger"
                      onClick={() => setDeleteTarget(card)}
                    >
                      Delete
                    </Button>
                  </div>
                </div>
              )}
            </div>
          ))}
        </div>
      )}

      <ConfirmModal
        isOpen={!!deleteTarget}
        onClose={() => setDeleteTarget(null)}
        onConfirm={handleDeleteCard}
        title="Delete Card"
        message={`Delete this card? "${deleteTarget?.front.slice(0, 60)}${(deleteTarget?.front.length ?? 0) > 60 ? '…' : ''}"`}
        confirmLabel="Delete"
        confirmVariant="danger"
        loading={deleting}
      />
    </div>
  )
}
