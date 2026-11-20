import { useState } from 'react'
import type { ReviewCard } from '@/types'
import ClozeView from './ClozeView'
import PictureWithBoxes from './PictureWithBoxes'

interface Props {
  card: ReviewCard
  isFlipped: boolean
  onFlip: () => void
}

export default function FlashcardViewer({ card, isFlipped, onFlip }: Props) {
  const [showHint, setShowHint] = useState(false)
  const isCloze = card.cardType === 'CLOZE'

  // A picture card does not flip: a picture can be taller than the flip card, and
  // the answer is the same picture with the box opened up.
  if (card.cardType === 'IMAGE') {
    return (
      <div className="w-full max-w-2xl mx-auto select-none">
        <div
          className={`rounded-2xl border-2 shadow-lg p-4 sm:p-6 cursor-pointer
            ${isFlipped ? 'bg-primary-50 border-primary-200' : 'bg-white border-gray-200'}`}
          onClick={onFlip}
        >
          <p className={`text-xs font-semibold uppercase tracking-widest mb-2 ${isFlipped ? 'text-primary-700' : 'text-gray-500'}`}>
            {isFlipped ? 'Answer' : 'Picture'}
          </p>
          <p className="text-lg font-medium text-gray-900 mb-3">{card.front}</p>
          <PictureWithBoxes imageId={card.imageId} boxes={card.boxes} activeIndex={card.clozeIndex} revealed={isFlipped} />
          {isFlipped && card.back && <p className="mt-3 text-sm text-gray-600 whitespace-pre-wrap">{card.back}</p>}
          {!isFlipped && <p className="mt-3 text-xs text-gray-500 text-center">Click to reveal answer</p>}
        </div>
      </div>
    )
  }

  return (
    <div className="w-full max-w-2xl mx-auto select-none">
      {/* Flip card container — CSS 3D flip */}
      {/* Clicking or tapping the card flips it. It is not a button on purpose: the
          hint button sits inside it, and keyboard users flip with Space or the
          Reveal Answer button below. */}
      <div
        className="relative cursor-pointer"
        style={{ perspective: '1200px' }}
        onClick={onFlip}
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
            style={{
              backfaceVisibility: 'hidden',
              WebkitBackfaceVisibility: 'hidden',
              // Safari (every iPhone browser) still sends taps to a face turned away,
              // so a tap on Show Hint hit the hidden answer side. The face that is
              // turned away takes no taps or clicks.
              pointerEvents: isFlipped ? 'none' : 'auto',
            }}
          >
            <p className="text-xs font-semibold text-gray-500 uppercase tracking-widest mb-4">
              {isCloze ? 'Fill in the blank' : 'Question'}
            </p>
            <p className="text-xl font-medium text-gray-900 leading-relaxed">
              {isCloze ? <ClozeView text={card.front} index={card.clozeIndex} revealed={false} /> : card.front}
            </p>

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
              <p className="absolute bottom-4 text-xs text-gray-500">Click to reveal answer</p>
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
              pointerEvents: isFlipped ? 'auto' : 'none',
            }}
          >
            <p className="text-xs font-semibold text-primary-700 uppercase tracking-widest mb-4">
              Answer
            </p>
            {isCloze ? (
              <>
                <p className="text-lg text-gray-900 leading-relaxed">
                  <ClozeView text={card.front} index={card.clozeIndex} revealed />
                </p>
                {card.back && <p className="mt-3 text-sm text-gray-600 whitespace-pre-wrap">{card.back}</p>}
              </>
            ) : (
              <p className="text-lg text-gray-900 leading-relaxed whitespace-pre-wrap">{card.back ?? ''}</p>
            )}

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
