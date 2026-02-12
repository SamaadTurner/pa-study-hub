import { useState } from 'react'
import type { ReviewCard } from '@/types'

// The review endpoint returns front/hint/tags, but we also display back after flip.
// The backend sends back as part of the review card response even though the type
// definition omits it to prevent cheating. We cast to access it safely.
interface ReviewCardWithBack extends ReviewCard {
  back?: string
}

interface Props {
  card: ReviewCard
  isFlipped: boolean
  onFlip: () => void
}

export default function FlashcardViewer({ card, isFlipped, onFlip }: Props) {
  const [showHint, setShowHint] = useState(false)
  const cardWithBack = card as ReviewCardWithBack

  return (
    <div className="w-full max-w-2xl mx-auto select-none">
      {/* Flip card container — CSS 3D flip */}
      <div
        className="relative cursor-pointer"
        style={{ perspective: '1200px' }}
        onClick={onFlip}
        role="button"
        aria-label={isFlipped ? 'Card showing answer. Click to flip back.' : 'Card showing question. Click to reveal answer.'}
        tabIndex={0}
        onKeyDown={e => e.key === 'Enter' || e.key === ' ' ? onFlip() : undefined}
      >
        <div
          className="relative w-full transition-transform duration-500"
          style={{
            transformStyle: 'preserve-3d',
            transform: isFlipped ? 'rotateY(180deg)' : 'rotateY(0deg)',
            minHeight: '280px',
          }}
        >
          {/* Front face */}
          <div
            className="absolute inset-0 bg-white border-2 border-gray-200 rounded-2xl
              shadow-lg flex flex-col items-center justify-center p-8 text-center"
            style={{ backfaceVisibility: 'hidden', WebkitBackfaceVisibility: 'hidden' }}
          >
            <p className="text-xs font-semibold text-gray-400 uppercase tracking-widest mb-4">
              Question
            </p>
            <p className="text-xl font-medium text-gray-900 leading-relaxed">{card.front}</p>

            {card.hint && !showHint && (
              <button
                className="mt-6 text-sm text-primary-600 hover:text-primary-800 font-medium"
                onClick={e => { e.stopPropagation(); setShowHint(true) }}
              >
                Show Hint
              </button>
            )}
            {card.hint && showHint && (
              <div className="mt-6 bg-yellow-50 border border-yellow-200 rounded-lg px-4 py-2">
                <p className="text-sm text-yellow-800 italic">{card.hint}</p>
              </div>
            )}

            {!isFlipped && (
              <p className="absolute bottom-4 text-xs text-gray-300">Click to reveal answer</p>
            )}
          </div>

          {/* Back face */}
          <div
            className="absolute inset-0 bg-primary-50 border-2 border-primary-200 rounded-2xl
              shadow-lg flex flex-col items-center justify-center p-8 text-center"
            style={{
              backfaceVisibility: 'hidden',
              WebkitBackfaceVisibility: 'hidden',
              transform: 'rotateY(180deg)',
            }}
          >
            <p className="text-xs font-semibold text-primary-400 uppercase tracking-widest mb-4">
              Answer
            </p>
            <p className="text-lg text-gray-900 leading-relaxed whitespace-pre-wrap">{cardWithBack.back ?? ''}</p>

            {card.tags.length > 0 && (
              <div className="flex flex-wrap gap-1 justify-center mt-4">
                {card.tags.map(tag => (
                  <span
                    key={tag}
                    className="text-xs bg-primary-100 text-primary-700 px-2 py-0.5 rounded-full"
                  >
                    {tag}
                  </span>
                ))}
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  )
}
