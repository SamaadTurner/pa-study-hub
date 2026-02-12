import { useState, useEffect, useRef, useCallback } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { getDueCards, submitReview } from '@/api/decks'
import type { ReviewCard } from '@/types'
import LoadingSpinner from '@/components/common/LoadingSpinner'
import ProgressBar from '@/components/common/ProgressBar'
import FlashcardViewer from '@/components/flashcards/FlashcardViewer'
import ReviewRatingButtons from '@/components/flashcards/ReviewRatingButtons'
import ReviewSummary from '@/components/flashcards/ReviewSummary'
import EmptyState from '@/components/common/EmptyState'
import { useNotification } from '@/contexts/NotificationContext'

interface RatingCount {
  again: number
  hard: number
  good: number
  easy: number
}

type ReviewState = 'loading' | 'reviewing' | 'done' | 'empty' | 'error'

export default function ReviewPage() {
  const { deckId }  = useParams<{ deckId: string }>()
  const navigate    = useNavigate()
  const { notify }  = useNotification()

  const [cards, setCards]         = useState<ReviewCard[]>([])
  const [currentIndex, setCurrentIndex] = useState(0)
  const [isFlipped, setIsFlipped] = useState(false)
  const [submitting, setSubmitting] = useState(false)
  const [state, setState]         = useState<ReviewState>('loading')

  const startTimeRef    = useRef<number>(Date.now())
  const sessionStartRef = useRef<number>(Date.now())
  const [ratingCounts, setRatingCounts] = useState<RatingCount>({
    again: 0, hard: 0, good: 0, easy: 0,
  })

  useEffect(() => {
    if (!deckId) return
    getDueCards(deckId)
      .then(data => {
        if (data.length === 0) {
          setState('empty')
        } else {
          setCards(data)
          setState('reviewing')
          sessionStartRef.current = Date.now()
        }
      })
      .catch(() => setState('error'))
  }, [deckId])

  // Reset flip + timer when card changes
  useEffect(() => {
    setIsFlipped(false)
    startTimeRef.current = Date.now()
  }, [currentIndex])

  const handleRate = useCallback(
    async (quality: 1 | 2 | 4 | 5) => {
      const card = cards[currentIndex]
      if (!card || submitting) return

      setSubmitting(true)
      try {
        await submitReview(card.id, quality)
      } catch {
        notify('error', 'Failed to submit review. Your progress may not be saved.')
      } finally {
        setSubmitting(false)
      }

      // Update rating counts
      setRatingCounts(prev => {
        const key = quality === 1 ? 'again' : quality === 2 ? 'hard' : quality === 4 ? 'good' : 'easy'
        return { ...prev, [key]: prev[key] + 1 }
      })

      // Advance to next card or finish
      if (currentIndex + 1 >= cards.length) {
        setState('done')
      } else {
        setCurrentIndex(i => i + 1)
      }
    },
    [cards, currentIndex, submitting, notify]
  )

  // Keyboard shortcuts
  useEffect(() => {
    const handleKey = (e: KeyboardEvent) => {
      if (state !== 'reviewing') return
      if (e.key === ' ' || e.key === 'Enter') {
        e.preventDefault()
        if (!isFlipped) setIsFlipped(true)
      }
      if (isFlipped && !submitting) {
        if (e.key === '1') handleRate(1)
        if (e.key === '2') handleRate(2)
        if (e.key === '3') handleRate(4)
        if (e.key === '4') handleRate(5)
      }
    }
    window.addEventListener('keydown', handleKey)
    return () => window.removeEventListener('keydown', handleKey)
  }, [state, isFlipped, submitting, handleRate])

  const totalReviewed = ratingCounts.again + ratingCounts.hard + ratingCounts.good + ratingCounts.easy
  const durationSeconds = Math.round((Date.now() - sessionStartRef.current) / 1000)
  const progressPct = cards.length > 0
    ? Math.round((currentIndex / cards.length) * 100)
    : 0

  if (state === 'loading') {
    return (
      <div className="flex items-center justify-center py-24">
        <LoadingSpinner message="Loading cards due for review..." size="lg" />
      </div>
    )
  }

  if (state === 'error') {
    return (
      <EmptyState icon="⚠️" title="Failed to load cards" message="Please try again."
        action={{ label: 'Go Back', onClick: () => navigate(`/decks/${deckId}`) }}
      />
    )
  }

  if (state === 'empty') {
    return (
      <div className="max-w-md mx-auto text-center py-16">
        <div className="text-6xl mb-4">🎉</div>
        <h2 className="text-2xl font-bold text-gray-900">All caught up!</h2>
        <p className="text-gray-500 mt-2 mb-6">
          No cards are due for review in this deck right now.
        </p>
        <div className="flex gap-3 justify-center">
          <button
            onClick={() => navigate(`/decks/${deckId}`)}
            className="px-4 py-2 border border-gray-300 text-gray-700 rounded-lg text-sm
              font-medium hover:bg-gray-50 transition-colors"
          >
            View Deck
          </button>
          <button
            onClick={() => navigate('/decks')}
            className="px-4 py-2 bg-primary-600 text-white rounded-lg text-sm
              font-medium hover:bg-primary-700 transition-colors"
          >
            All Decks
          </button>
        </div>
      </div>
    )
  }

  if (state === 'done') {
    return (
      <ReviewSummary
        deckId={deckId!}
        totalReviewed={totalReviewed}
        durationSeconds={durationSeconds}
        ratingCounts={ratingCounts}
      />
    )
  }

  // Reviewing state
  const currentCard = cards[currentIndex]

  return (
    <div className="max-w-2xl mx-auto space-y-6">
      {/* Progress header */}
      <div className="flex items-center justify-between text-sm text-gray-500">
        <span>
          Card <span className="font-semibold text-gray-900">{currentIndex + 1}</span>{' '}
          of <span className="font-semibold text-gray-900">{cards.length}</span>
        </span>
        <button
          onClick={() => navigate(`/decks/${deckId}`)}
          className="text-gray-400 hover:text-gray-600 transition-colors"
        >
          ✕ Exit
        </button>
      </div>

      <ProgressBar value={progressPct} size="sm" color="primary" />

      {/* Flashcard */}
      <FlashcardViewer
        card={currentCard}
        isFlipped={isFlipped}
        onFlip={() => setIsFlipped(f => !f)}
      />

      {/* Rating buttons — only show after flip */}
      {isFlipped ? (
        <ReviewRatingButtons onRate={handleRate} disabled={submitting} />
      ) : (
        <div className="text-center">
          <button
            onClick={() => setIsFlipped(true)}
            className="px-8 py-3 bg-white border-2 border-gray-200 rounded-xl text-sm
              font-medium text-gray-700 hover:border-primary-300 hover:bg-primary-50
              transition-all shadow-sm"
          >
            Reveal Answer
          </button>
          <p className="text-xs text-gray-400 mt-2">or press Space / Enter</p>
        </div>
      )}

      {/* Keyboard shortcuts hint */}
      {isFlipped && (
        <p className="text-center text-xs text-gray-400">
          Keyboard: <kbd className="bg-gray-100 px-1 rounded">1</kbd> Again &nbsp;
          <kbd className="bg-gray-100 px-1 rounded">2</kbd> Hard &nbsp;
          <kbd className="bg-gray-100 px-1 rounded">3</kbd> Good &nbsp;
          <kbd className="bg-gray-100 px-1 rounded">4</kbd> Easy
        </p>
      )}
    </div>
  )
}
