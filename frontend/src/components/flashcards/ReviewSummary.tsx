import { useNavigate } from 'react-router-dom'
import Button from '@/components/common/Button'
import { formatDuration } from '@/utils/formatDuration'

interface RatingCount {
  again: number
  hard: number
  good: number
  easy: number
}

interface Props {
  deckId: string
  totalReviewed: number
  durationSeconds: number
  ratingCounts: RatingCount
}

export default function ReviewSummary({
  deckId,
  totalReviewed,
  durationSeconds,
  ratingCounts,
}: Props) {
  const navigate = useNavigate()

  const correctCount = ratingCounts.good + ratingCounts.easy
  const accuracy = totalReviewed > 0
    ? Math.round((correctCount / totalReviewed) * 100)
    : 0

  return (
    <div className="max-w-md mx-auto text-center space-y-6">
      <div>
        <div className="text-6xl mb-4">{accuracy >= 70 ? '🎉' : '💪'}</div>
        <h2 className="text-2xl font-bold text-gray-900">Session Complete!</h2>
        <p className="text-gray-500 mt-1">
          You reviewed {totalReviewed} {totalReviewed === 1 ? 'card' : 'cards'} in{' '}
          {formatDuration(durationSeconds)}
        </p>
      </div>

      {/* Accuracy */}
      <div className="bg-white border border-gray-200 rounded-xl p-6">
        <p className={`text-4xl font-bold ${accuracy >= 70 ? 'text-green-600' : 'text-orange-500'}`}>
          {accuracy}%
        </p>
        <p className="text-sm text-gray-500 mt-1">Accuracy ({correctCount}/{totalReviewed} correct)</p>
      </div>

      {/* Rating breakdown */}
      <div className="bg-white border border-gray-200 rounded-xl p-5">
        <p className="text-sm font-semibold text-gray-700 mb-3">Rating Breakdown</p>
        <div className="grid grid-cols-4 gap-3">
          <RatingCell label="Again" count={ratingCounts.again} color="text-red-600" bg="bg-red-50" />
          <RatingCell label="Hard"  count={ratingCounts.hard}  color="text-orange-600" bg="bg-orange-50" />
          <RatingCell label="Good"  count={ratingCounts.good}  color="text-green-600" bg="bg-green-50" />
          <RatingCell label="Easy"  count={ratingCounts.easy}  color="text-blue-600" bg="bg-blue-50" />
        </div>
      </div>

      {/* Actions */}
      <div className="flex flex-col sm:flex-row gap-3">
        <Button
          variant="secondary"
          className="flex-1"
          onClick={() => navigate(`/decks/${deckId}/review`)}
        >
          Review Again
        </Button>
        <Button
          className="flex-1"
          onClick={() => navigate('/dashboard')}
        >
          Back to Dashboard
        </Button>
      </div>
    </div>
  )
}

function RatingCell({
  label,
  count,
  color,
  bg,
}: {
  label: string
  count: number
  color: string
  bg: string
}) {
  return (
    <div className={`${bg} rounded-lg p-3 text-center`}>
      <p className={`text-xl font-bold ${color}`}>{count}</p>
      <p className="text-xs text-gray-500 mt-0.5">{label}</p>
    </div>
  )
}
