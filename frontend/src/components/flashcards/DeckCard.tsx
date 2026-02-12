import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import type { Deck } from '@/types'
import Badge from '@/components/common/Badge'
import { ConfirmModal } from '@/components/common/Modal'
import { getCategoryBadgeVariant, getCategoryLabel } from '@/utils/categoryColors'

interface Props {
  deck: Deck
  onDelete: (deckId: string) => Promise<void>
  onEdit: (deck: Deck) => void
}

export default function DeckCard({ deck, onDelete, onEdit }: Props) {
  const navigate             = useNavigate()
  const [confirmOpen, setConfirmOpen] = useState(false)
  const [deleting, setDeleting]       = useState(false)

  async function handleDelete() {
    setDeleting(true)
    try {
      await onDelete(deck.id)
    } finally {
      setDeleting(false)
      setConfirmOpen(false)
    }
  }

  return (
    <>
      <div className="bg-white border border-gray-200 rounded-xl p-5 hover:shadow-md
        transition-shadow flex flex-col gap-3">
        {/* Header */}
        <div className="flex items-start justify-between gap-2">
          <h3 className="font-semibold text-gray-900 leading-snug flex-1">{deck.title}</h3>
          <Badge variant={getCategoryBadgeVariant(deck.category)}>
            {getCategoryLabel(deck.category)}
          </Badge>
        </div>

        {/* Description */}
        {deck.description && (
          <p className="text-sm text-gray-500 line-clamp-2">{deck.description}</p>
        )}

        {/* Stats */}
        <div className="flex items-center gap-4 text-sm text-gray-500">
          <span>📚 {deck.cardCount} {deck.cardCount === 1 ? 'card' : 'cards'}</span>
          {deck.dueCardCount > 0 && (
            <span className="text-orange-600 font-medium">
              🔔 {deck.dueCardCount} due
            </span>
          )}
          {deck.isPublic && (
            <span className="text-blue-600">🌐 Public</span>
          )}
        </div>

        {/* Actions */}
        <div className="flex items-center gap-2 mt-1">
          <button
            onClick={() => navigate(`/decks/${deck.id}/review`)}
            disabled={deck.cardCount === 0}
            className="flex-1 text-sm font-medium bg-primary-600 text-white rounded-lg
              px-3 py-2 hover:bg-primary-700 disabled:bg-gray-200 disabled:text-gray-400
              disabled:cursor-not-allowed transition-colors text-center"
          >
            Study {deck.dueCardCount > 0 ? `(${deck.dueCardCount})` : ''}
          </button>
          <button
            onClick={() => navigate(`/decks/${deck.id}`)}
            className="text-sm font-medium text-gray-600 hover:text-gray-900
              border border-gray-200 rounded-lg px-3 py-2 hover:bg-gray-50 transition-colors"
          >
            View
          </button>
          <button
            onClick={() => onEdit(deck)}
            className="text-sm font-medium text-gray-600 hover:text-gray-900
              border border-gray-200 rounded-lg px-3 py-2 hover:bg-gray-50 transition-colors"
          >
            Edit
          </button>
          <button
            onClick={() => setConfirmOpen(true)}
            className="text-sm font-medium text-red-600 hover:text-red-800
              border border-red-200 rounded-lg px-3 py-2 hover:bg-red-50 transition-colors"
          >
            Delete
          </button>
        </div>
      </div>

      <ConfirmModal
        isOpen={confirmOpen}
        onClose={() => setConfirmOpen(false)}
        onConfirm={handleDelete}
        title="Delete Deck"
        message={`Are you sure you want to delete "${deck.title}"? This will permanently delete all ${deck.cardCount} cards.`}
        confirmLabel="Delete"
        confirmVariant="danger"
        loading={deleting}
      />
    </>
  )
}
