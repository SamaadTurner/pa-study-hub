import { useState } from 'react'
import Modal from '@/components/common/Modal'
import Button from '@/components/common/Button'
import { suggestFlashcards, type SuggestedCard } from '@/api/ai'
import ClozeView from '@/components/flashcards/ClozeView'
import { clozeNumbers, clozeProblem } from '@/utils/cloze'
import type { ApiError, CardType } from '@/types'

const MAX_NOTES_LENGTH = 6000
const COUNT_CHOICES = [3, 5, 10]

// A suggestion the user can edit, and untick to leave it out
interface Draft extends SuggestedCard {
  keep: boolean
}

interface Props {
  isOpen: boolean
  onClose: () => void
  remainingToday: number | null
  // Saves one question-and-answer card. Called once for every one the user keeps.
  onAddCard: (card: { front: string; back: string }) => Promise<unknown>
  // Saves one fill-in-the-blank text (one card per blank number)
  onAddClozeText: (text: string, extra: string) => Promise<unknown>
  // Called after a request, so the page can update how many requests are left
  onUsedRequest: () => void
}

const textareaClass = `w-full px-3 py-2 border border-gray-300 rounded-lg text-sm
  focus:outline-none focus:ring-2 focus:ring-primary-500 resize-y`

// A suggestion is ready to add when it is complete: a question and an answer, or a
// text with a proper blank
function isComplete(draft: Draft): boolean {
  if (draft.cardType === 'CLOZE') return clozeProblem(draft.front) === null
  return draft.front.trim() !== '' && draft.back.trim() !== ''
}

export default function SuggestCardsModal({ isOpen, onClose, remainingToday, onAddCard, onAddClozeText, onUsedRequest }: Props) {
  const [notes, setNotes]         = useState('')
  const [cardType, setCardType]   = useState<CardType>('BASIC')
  const [count, setCount]         = useState(5)
  const [drafts, setDrafts]       = useState<Draft[]>([])
  const [suggesting, setSuggesting] = useState(false)
  const [saving, setSaving]       = useState(false)
  const [error, setError]         = useState<string | null>(null)

  const keptCount = drafts.filter(d => d.keep && isComplete(d)).length

  function close() {
    // Start fresh next time, so old suggestions are not added to another deck by mistake
    setNotes('')
    setDrafts([])
    setError(null)
    onClose()
  }

  async function handleSuggest() {
    setSuggesting(true)
    setError(null)
    try {
      const cards = await suggestFlashcards(notes, count, cardType)
      setDrafts(cards.map(card => ({ ...card, keep: true })))
    } catch (err) {
      setError((err as ApiError)?.detail ?? 'Could not get suggestions. Please try again.')
    } finally {
      setSuggesting(false)
      onUsedRequest()
    }
  }

  function updateDraft(index: number, change: Partial<Draft>) {
    setDrafts(prev => prev.map((draft, i) => (i === index ? { ...draft, ...change } : draft)))
  }

  async function handleAdd() {
    setSaving(true)
    setError(null)
    try {
      // One after the other, so the cards keep the order they were suggested in
      for (const draft of drafts) {
        if (!draft.keep || !isComplete(draft)) continue
        if (draft.cardType === 'CLOZE') {
          await onAddClozeText(draft.front.trim(), draft.back.trim())
        } else {
          await onAddCard({ front: draft.front.trim(), back: draft.back.trim() })
        }
      }
      close()
    } catch (err) {
      setError((err as ApiError)?.detail ?? 'Could not add the cards. Please try again.')
    } finally {
      setSaving(false)
    }
  }

  const outOfRequests = remainingToday !== null && remainingToday <= 0

  const actions = drafts.length === 0 ? (
    <>
      <Button variant="secondary" onClick={close}>Cancel</Button>
      <Button onClick={handleSuggest} loading={suggesting} disabled={!notes.trim() || outOfRequests}>
        Suggest cards
      </Button>
    </>
  ) : (
    <>
      <Button variant="secondary" onClick={() => setDrafts([])} disabled={saving}>
        Start over
      </Button>
      <Button onClick={handleAdd} loading={saving} disabled={keptCount === 0}>
        Add {keptCount} {keptCount === 1 ? 'card' : 'cards'}
      </Button>
    </>
  )

  return (
    <Modal isOpen={isOpen} onClose={close} title="Suggest cards from your notes" size="xl" actions={actions}>
      <div className="space-y-4 max-h-[70vh] overflow-y-auto pr-1">
        {drafts.length === 0 ? (
          <>
            <div>
              <label htmlFor="ai-notes" className="block text-sm font-medium text-gray-700 mb-1">
                Your notes
              </label>
              <textarea
                id="ai-notes"
                value={notes}
                onChange={e => setNotes(e.target.value)}
                maxLength={MAX_NOTES_LENGTH}
                rows={8}
                placeholder="Paste lecture notes or a textbook passage..."
                className={textareaClass}
              />
              <p className="text-xs text-gray-500 mt-1 text-right">
                {notes.length} / {MAX_NOTES_LENGTH}
              </p>
            </div>

            <div className="flex flex-wrap items-center gap-2" role="group" aria-label="Kind of card">
              <span className="text-sm text-gray-700">Kind of card:</span>
              {([['BASIC', 'Question and answer'], ['CLOZE', 'Fill in the blank']] as const).map(([value, label]) => (
                <button
                  key={value}
                  type="button"
                  onClick={() => setCardType(value)}
                  aria-pressed={cardType === value}
                  className={`px-3 py-1 rounded-lg text-sm border transition-colors ${
                    cardType === value
                      ? 'bg-primary-600 text-white border-primary-600'
                      : 'bg-white text-gray-700 border-gray-300 hover:bg-gray-50'
                  }`}
                >
                  {label}
                </button>
              ))}
            </div>

            <div className="flex items-center gap-2">
              <span className="text-sm text-gray-700">How many cards:</span>
              {COUNT_CHOICES.map(choice => (
                <button
                  key={choice}
                  type="button"
                  onClick={() => setCount(choice)}
                  aria-pressed={count === choice}
                  className={`px-3 py-1 rounded-lg text-sm border transition-colors ${
                    count === choice
                      ? 'bg-primary-600 text-white border-primary-600'
                      : 'bg-white text-gray-700 border-gray-300 hover:bg-gray-50'
                  }`}
                >
                  {choice}
                </button>
              ))}
            </div>

            {remainingToday !== null && (
              <p className="text-xs text-gray-500">
                {outOfRequests
                  ? 'You have used all your AI requests for today. They reset at midnight UTC.'
                  : `${remainingToday} AI ${remainingToday === 1 ? 'request' : 'requests'} left today.`}
              </p>
            )}
          </>
        ) : (
          <>
            {/* Medical facts from a model can be wrong, so nothing is added without a look */}
            <p className="text-sm text-amber-800 bg-amber-50 border border-amber-200 rounded-lg p-3">
              These cards were written by an AI model and can contain mistakes. Check each one,
              edit it if needed, and untick any you don't want.
            </p>

            {drafts.map((draft, index) => (
              <div key={index} className="border border-gray-200 rounded-lg p-4 space-y-2">
                <label className="flex items-center gap-2 text-sm font-medium text-gray-700">
                  <input
                    type="checkbox"
                    checked={draft.keep}
                    onChange={e => updateDraft(index, { keep: e.target.checked })}
                    className="rounded border-gray-300"
                  />
                  Keep card {index + 1}
                </label>
                {draft.cardType === 'CLOZE' ? (
                  <>
                    <textarea
                      aria-label={`Text with blanks of card ${index + 1}`}
                      value={draft.front}
                      onChange={e => updateDraft(index, { front: e.target.value })}
                      rows={2}
                      className={`${textareaClass} font-mono`}
                    />
                    {clozeProblem(draft.front) ? (
                      <p className="text-xs text-red-600">{clozeProblem(draft.front)}</p>
                    ) : (
                      <p className="text-sm text-gray-700">
                        <ClozeView text={draft.front} index={clozeNumbers(draft.front)[0]} revealed={false} />
                      </p>
                    )}
                    <textarea
                      aria-label={`Extra notes of card ${index + 1}`}
                      value={draft.back}
                      onChange={e => updateDraft(index, { back: e.target.value })}
                      rows={2}
                      placeholder="Extra notes (optional)"
                      className={textareaClass}
                    />
                  </>
                ) : (
                  <>
                    <textarea
                      aria-label={`Front of card ${index + 1}`}
                      value={draft.front}
                      onChange={e => updateDraft(index, { front: e.target.value })}
                      rows={2}
                      className={textareaClass}
                    />
                    <textarea
                      aria-label={`Back of card ${index + 1}`}
                      value={draft.back}
                      onChange={e => updateDraft(index, { back: e.target.value })}
                      rows={3}
                      className={textareaClass}
                    />
                  </>
                )}
              </div>
            ))}
          </>
        )}

        {error && (
          <p role="alert" className="text-sm text-red-700 bg-red-50 border border-red-200 rounded-lg p-3">
            {error}
          </p>
        )}
      </div>
    </Modal>
  )
}
