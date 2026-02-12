import { useNavigate } from 'react-router-dom'
import Button from '@/components/common/Button'
import type { Deck } from '@/types'

interface Props {
  decks: Deck[]
}

export default function CardsDueWidget({ decks }: Props) {
  const navigate = useNavigate()
  const totalDue = decks.reduce((sum, d) => sum + d.dueCardCount, 0)
  const deckWithMostDue = decks.reduce<Deck | null>(
    (best, d) => (!best || d.dueCardCount > best.dueCardCount ? d : best),
    null
  )

  if (totalDue === 0) {
    return (
      <div className="bg-green-50 border border-green-200 rounded-xl p-5 flex items-center gap-4">
        <span className="text-3xl">🎉</span>
        <div>
          <p className="font-semibold text-green-900">All caught up!</p>
          <p className="text-sm text-green-700">No cards due for review right now.</p>
        </div>
      </div>
    )
  }

  return (
    <div className="bg-primary-50 border border-primary-200 rounded-xl p-5">
      <div className="flex items-start justify-between gap-3">
        <div className="flex items-center gap-3">
          <span className="text-3xl">🃏</span>
          <div>
            <p className="text-2xl font-bold text-gray-900 leading-none">{totalDue}</p>
            <p className="text-sm text-gray-600 mt-0.5">
              {totalDue === 1 ? 'card' : 'cards'} due for review
            </p>
          </div>
        </div>
        {deckWithMostDue && deckWithMostDue.dueCardCount > 0 && (
          <Button
            size="sm"
            onClick={() => navigate(`/decks/${deckWithMostDue.id}/review`)}
          >
            Study Now
          </Button>
        )}
      </div>
      {deckWithMostDue && decks.filter(d => d.dueCardCount > 0).length > 1 && (
        <p className="text-xs text-gray-500 mt-3">
          Most due: <span className="font-medium text-gray-700">{deckWithMostDue.title}</span>{' '}
          ({deckWithMostDue.dueCardCount} cards)
        </p>
      )}
    </div>
  )
}
