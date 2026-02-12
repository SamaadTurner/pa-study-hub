import { useState, useEffect } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { createCard, updateCard, getCards } from '@/api/decks'
import type { Card } from '@/types'
import Button from '@/components/common/Button'
import LoadingSpinner from '@/components/common/LoadingSpinner'
import { useNotification } from '@/contexts/NotificationContext'
import type { ApiError } from '@/types'

interface FormState {
  front: string
  back: string
  hint: string
  tagsRaw: string   // comma-separated string
}

const DEFAULT_FORM: FormState = { front: '', back: '', hint: '', tagsRaw: '' }

interface FieldErrors {
  front?: string
  back?: string
}

export default function CardEditorPage() {
  const { deckId, cardId } = useParams<{ deckId: string; cardId: string }>()
  const navigate           = useNavigate()
  const { notify }         = useNotification()
  const isEditing          = !!cardId

  const [form, setForm]       = useState<FormState>(DEFAULT_FORM)
  const [errors, setErrors]   = useState<FieldErrors>({})
  const [loading, setLoading] = useState(isEditing)
  const [saving, setSaving]   = useState(false)

  // Load existing card data when editing
  useEffect(() => {
    if (!isEditing || !deckId) return
    getCards(deckId)
      .then(cards => {
        const card = cards.find((c: Card) => c.id === cardId)
        if (card) {
          setForm({
            front:    card.front,
            back:     card.back,
            hint:     card.hint ?? '',
            tagsRaw:  card.tags.join(', '),
          })
        }
      })
      .catch(() => notify('error', 'Failed to load card'))
      .finally(() => setLoading(false))
  }, [isEditing, deckId, cardId, notify])

  function validate(): boolean {
    const next: FieldErrors = {}
    if (!form.front.trim())          next.front = 'Question is required'
    else if (form.front.length > 2000) next.front = 'Question must be 2000 characters or less'
    if (!form.back.trim())           next.back  = 'Answer is required'
    else if (form.back.length > 5000) next.back = 'Answer must be 5000 characters or less'
    setErrors(next)
    return Object.keys(next).length === 0
  }

  function parseTags(raw: string): string[] {
    return raw
      .split(',')
      .map(t => t.trim().toLowerCase())
      .filter(Boolean)
  }

  async function handleSave(addAnother = false) {
    if (!validate() || !deckId) return
    setSaving(true)
    try {
      const payload = {
        front: form.front.trim(),
        back:  form.back.trim(),
        hint:  form.hint.trim() || undefined,
        tags:  parseTags(form.tagsRaw),
      }

      if (isEditing && cardId) {
        await updateCard(cardId, payload)
        notify('success', 'Card updated!')
        navigate(`/decks/${deckId}`)
      } else {
        await createCard(deckId, payload)
        if (addAnother) {
          setForm(DEFAULT_FORM)
          setErrors({})
          notify('success', 'Card added! Add another.')
        } else {
          notify('success', 'Card added!')
          navigate(`/decks/${deckId}`)
        }
      }
    } catch (err) {
      notify('error', (err as ApiError)?.detail ?? 'Failed to save card')
    } finally {
      setSaving(false)
    }
  }

  if (loading) {
    return (
      <div className="flex items-center justify-center py-24">
        <LoadingSpinner message="Loading card..." size="lg" />
      </div>
    )
  }

  return (
    <div className="max-w-2xl mx-auto space-y-6">
      {/* Header */}
      <div>
        <button
          onClick={() => navigate(`/decks/${deckId}`)}
          className="text-sm text-primary-600 hover:underline mb-2 flex items-center gap-1"
        >
          ← Back to Deck
        </button>
        <h1 className="text-2xl font-bold text-gray-900">
          {isEditing ? 'Edit Card' : 'Add New Card'}
        </h1>
      </div>

      {/* Form */}
      <div className="bg-white border border-gray-200 rounded-xl p-6 space-y-5">
        {/* Front */}
        <div>
          <label className="block text-sm font-semibold text-gray-700 mb-1">
            Question (Front) <span className="text-red-500">*</span>
          </label>
          <textarea
            value={form.front}
            onChange={e => setForm(f => ({ ...f, front: e.target.value }))}
            rows={4}
            className={`w-full px-4 py-3 border rounded-lg text-sm focus:outline-none
              focus:ring-2 focus:ring-primary-500 resize-y
              ${errors.front ? 'border-red-400 bg-red-50' : 'border-gray-300'}`}
            placeholder="What are the classic symptoms of left-sided heart failure?"
            maxLength={2000}
          />
          {errors.front && <p className="mt-1 text-xs text-red-600">{errors.front}</p>}
          <p className="mt-1 text-xs text-gray-400">{form.front.length}/2000</p>
        </div>

        {/* Back */}
        <div>
          <label className="block text-sm font-semibold text-gray-700 mb-1">
            Answer (Back) <span className="text-red-500">*</span>
          </label>
          <textarea
            value={form.back}
            onChange={e => setForm(f => ({ ...f, back: e.target.value }))}
            rows={6}
            className={`w-full px-4 py-3 border rounded-lg text-sm focus:outline-none
              focus:ring-2 focus:ring-primary-500 resize-y
              ${errors.back ? 'border-red-400 bg-red-50' : 'border-gray-300'}`}
            placeholder="Dyspnea, orthopnea, PND, fatigue, pulmonary edema (crackles/rales). Key finding: S3 gallop."
            maxLength={5000}
          />
          {errors.back && <p className="mt-1 text-xs text-red-600">{errors.back}</p>}
          <p className="mt-1 text-xs text-gray-400">{form.back.length}/5000</p>
        </div>

        {/* Hint */}
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">
            Hint <span className="text-gray-400 font-normal">(optional)</span>
          </label>
          <input
            type="text"
            value={form.hint}
            onChange={e => setForm(f => ({ ...f, hint: e.target.value }))}
            className="w-full px-4 py-2.5 border border-gray-300 rounded-lg text-sm
              focus:outline-none focus:ring-2 focus:ring-primary-500"
            placeholder="Think about what happens when the left ventricle fails to pump..."
            maxLength={500}
          />
        </div>

        {/* Tags */}
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">
            Tags <span className="text-gray-400 font-normal">(optional, comma-separated)</span>
          </label>
          <input
            type="text"
            value={form.tagsRaw}
            onChange={e => setForm(f => ({ ...f, tagsRaw: e.target.value }))}
            className="w-full px-4 py-2.5 border border-gray-300 rounded-lg text-sm
              focus:outline-none focus:ring-2 focus:ring-primary-500"
            placeholder="heart-failure, ejection-fraction, bnp"
          />
          {/* Tag preview */}
          {form.tagsRaw && (
            <div className="flex flex-wrap gap-1 mt-2">
              {parseTags(form.tagsRaw).map(tag => (
                <span
                  key={tag}
                  className="text-xs bg-gray-100 text-gray-600 px-2 py-0.5 rounded-full"
                >
                  {tag}
                </span>
              ))}
            </div>
          )}
        </div>
      </div>

      {/* Actions */}
      <div className="flex items-center justify-between gap-3">
        <Button variant="secondary" onClick={() => navigate(`/decks/${deckId}`)}>
          Cancel
        </Button>
        <div className="flex gap-2">
          {!isEditing && (
            <Button
              variant="secondary"
              loading={saving}
              onClick={() => handleSave(true)}
            >
              Save & Add Another
            </Button>
          )}
          <Button
            loading={saving}
            onClick={() => handleSave(false)}
          >
            {isEditing ? 'Save Changes' : 'Save Card'}
          </Button>
        </div>
      </div>
    </div>
  )
}

