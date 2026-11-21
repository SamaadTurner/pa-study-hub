import { useEffect, useState } from 'react'
import { slowRequests, SLOW_AFTER_MS } from '@/utils/serverStatus'
import { WAITING_CARDS, randomStart, wakeProgress } from '@/utils/waitingCards'

// A new card every 12 seconds; a review question shows its answer after 5
const CARD_MS = 12_000
const ANSWER_AFTER_MS = 5_000

/**
 * Shown while a request has been waiting a few seconds, which on the free plan
 * almost always means the server is waking up (measured: about 95 seconds).
 *
 * Waits feel shorter when people can see progress, know why they wait and have
 * something to look at, so it shows all three: a progress bar with the seconds,
 * the reason, and a PA fact or quick review question that changes every 12 s.
 */
export default function ServerWakeBanner() {
  const [slow, setSlow] = useState(slowRequests.isSlow())
  const [seconds, setSeconds] = useState(0)
  const [cardIndex, setCardIndex] = useState(randomStart)
  const [showAnswer, setShowAnswer] = useState(false)

  useEffect(() => slowRequests.subscribe(setSlow), [])

  // Seconds since the request started (the banner appears after SLOW_AFTER_MS)
  useEffect(() => {
    if (!slow) {
      setSeconds(0)
      return
    }
    const started = Date.now() - SLOW_AFTER_MS
    const update = () => setSeconds(Math.round((Date.now() - started) / 1000))
    update()
    const timer = setInterval(update, 1000)
    return () => clearInterval(timer)
  }, [slow])

  // Next card every CARD_MS, and the answer of a review card after ANSWER_AFTER_MS
  useEffect(() => {
    if (!slow) return
    setShowAnswer(false)
    const answer = setTimeout(() => setShowAnswer(true), ANSWER_AFTER_MS)
    const next = setTimeout(() => setCardIndex(i => (i + 1) % WAITING_CARDS.length), CARD_MS)
    return () => { clearTimeout(answer); clearTimeout(next) }
  }, [slow, cardIndex])

  if (!slow) return null

  const card = WAITING_CARDS[cardIndex]
  const progress = wakeProgress(seconds)

  return (
    <div className="fixed top-0 inset-x-0 z-50 px-3 pt-3 pointer-events-none">
      <div className="pointer-events-auto max-w-xl mx-auto bg-white border border-primary-200 shadow-lg rounded-xl p-4">
        {/* Only this sentence is read out, once; the counter and cards would be noise */}
        <p role="status" aria-live="polite" className="text-sm font-semibold text-gray-900">
          Waking up the server
          <span className="font-normal text-gray-600"> · {seconds} s</span>
        </p>
        <div className="mt-2 h-2 rounded-full bg-gray-100 overflow-hidden" aria-hidden="true">
          <div className="h-full bg-primary-600 transition-all duration-1000 ease-linear" style={{ width: `${progress}%` }} />
        </div>
        <p className="mt-1 text-xs text-gray-600">
          It sleeps after 15 minutes without visitors and usually takes 1 to 2 minutes to start. Your page will load by itself.
        </p>

        <div className="mt-3 rounded-lg bg-primary-50 px-3 py-2" aria-live="off">
          <p className="text-xs font-semibold uppercase tracking-wide text-primary-700">
            {card.kind === 'fact' ? 'Did you know?' : 'Quick review'}
          </p>
          {card.kind === 'fact' ? (
            <p className="text-sm text-gray-800">{card.text}</p>
          ) : (
            <>
              <p className="text-sm font-medium text-gray-900">{card.question}</p>
              <p className={`text-sm text-gray-800 transition-opacity duration-500 ${showAnswer ? 'opacity-100' : 'opacity-0'}`}>
                {card.answer}
              </p>
            </>
          )}
        </div>
      </div>
    </div>
  )
}
