import { useState, useEffect, useRef } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { createCard, createClozeCards, createImageCards, updateCard, getCards, uploadPicture } from '@/api/decks'
import type { Card, CardType, OcclusionBox } from '@/types'
import Button from '@/components/common/Button'
import LoadingSpinner from '@/components/common/LoadingSpinner'
import ClozeView from '@/components/flashcards/ClozeView'
import PictureBoxEditor from '@/components/flashcards/PictureBoxEditor'
import PictureWithBoxes from '@/components/flashcards/PictureWithBoxes'
import { shrinkPicture } from '@/utils/pictures'
import { useNotification } from '@/contexts/NotificationContext'
import { clozeNumbers, clozeProblem, nextClozeNumber, wrapAsBlank } from '@/utils/cloze'
import type { ApiError } from '@/types'

interface FormState {
  front: string     // the question, or a cloze card's text
  back: string      // the answer, or a cloze card's optional extra notes
  hint: string
  tagsRaw: string   // comma-separated string
}

const DEFAULT_FORM: FormState = { front: '', back: '', hint: '', tagsRaw: '' }

interface FieldErrors {
  front?: string
  back?: string
  picture?: string
}

// A picture chosen in the editor, already made small, not uploaded yet
interface LocalPicture {
  blob: Blob
  url: string
}

export default function CardEditorPage() {
  const { deckId, cardId } = useParams<{ deckId: string; cardId: string }>()
  const navigate           = useNavigate()
  const { notify }         = useNotification()
  const isEditing          = !!cardId

  const [form, setForm]         = useState<FormState>(DEFAULT_FORM)
  const [cardType, setCardType] = useState<CardType>('BASIC')
  // For an existing cloze card: the blank number it asks for
  const [clozeIndex, setClozeIndex] = useState<number | null>(null)
  // Picture cards: the new picture and its boxes, or an existing card's saved picture
  const [picture, setPicture]   = useState<LocalPicture | null>(null)
  const [boxes, setBoxes]       = useState<OcclusionBox[]>([])
  const [savedImageId, setSavedImageId] = useState<string | null>(null)
  const [preparing, setPreparing] = useState(false)
  const [errors, setErrors]     = useState<FieldErrors>({})
  const [loading, setLoading]   = useState(isEditing)
  const [saving, setSaving]     = useState(false)
  const textRef = useRef<HTMLTextAreaElement>(null)

  const isCloze = cardType === 'CLOZE'
  const isPicture = cardType === 'IMAGE'

  // The local preview URL is freed when the picture changes or the page closes
  useEffect(() => {
    return () => { if (picture) URL.revokeObjectURL(picture.url) }
  }, [picture])

  async function choosePicture(file: File | undefined) {
    if (!file) return
    setPreparing(true)
    setErrors(e => ({ ...e, picture: undefined }))
    try {
      const blob = await shrinkPicture(file)
      setPicture({ blob, url: URL.createObjectURL(blob) })
      setBoxes([])
    } catch (err) {
      setErrors(e => ({ ...e, picture: (err as Error).message }))
    } finally {
      setPreparing(false)
    }
  }

  // Load existing card data when editing
  useEffect(() => {
    if (!isEditing || !deckId) return

    // React can run this effect twice (it does in development), which means two
    // requests. Only the latest one may fill the form. Without this, the second
    // answer could arrive after the user had started typing and put the old text back.
    let ignore = false

    getCards(deckId)
      .then(cards => {
        if (ignore) return
        const card = cards.find((c: Card) => c.id === cardId)
        if (card) {
          setForm({
            front:    card.front,
            back:     card.back,
            hint:     card.hint ?? '',
            tagsRaw:  card.tags.join(', '),
          })
          setCardType(card.cardType)
          setClozeIndex(card.clozeIndex)
          setSavedImageId(card.imageId)
          setBoxes(card.boxes)
        }
      })
      .catch(() => {
        if (!ignore) notify('error', 'Failed to load card')
      })
      .finally(() => {
        // The form stays hidden until the request that counts has finished
        if (!ignore) setLoading(false)
      })

    return () => { ignore = true }
  }, [isEditing, deckId, cardId, notify])

  function validate(): boolean {
    const next: FieldErrors = {}
    if (isPicture) {
      // The question is optional on a picture card; a default is used
      if (form.front.length > 2000) next.front = 'Must be 2000 characters or less'
      if (!isEditing && !picture) next.picture = 'Choose a picture first'
      else if (!isEditing && boxes.length === 0) next.picture = 'Draw at least one box over the part to hide'
    } else if (!form.front.trim()) next.front = isCloze ? 'The text is required' : 'Question is required'
    else if (form.front.length > 2000) next.front = 'Must be 2000 characters or less'
    else if (isCloze) {
      const problem = clozeProblem(form.front)
      if (problem) next.front = problem
      else if (clozeIndex !== null && !clozeNumbers(form.front).includes(clozeIndex)) {
        next.front = `This card asks for blank c${clozeIndex}, so the text needs a {{c${clozeIndex}::...}} blank`
      }
    }
    if (!isCloze && !isPicture && !form.back.trim()) next.back = 'Answer is required'
    else if (form.back.length > 5000) next.back = 'Must be 5000 characters or less'
    setErrors(next)
    return Object.keys(next).length === 0
  }

  function parseTags(raw: string): string[] {
    return raw
      .split(',')
      .map(t => t.trim().toLowerCase())
      .filter(Boolean)
  }

  // Wraps the words selected in the text box in a new blank
  function makeBlank() {
    const box = textRef.current
    if (!box) return
    const { selectionStart, selectionEnd } = box
    if (selectionStart === selectionEnd) {
      setErrors(e => ({ ...e, front: 'Select the words to hide first' }))
      return
    }
    setForm(f => ({ ...f, front: wrapAsBlank(f.front, selectionStart, selectionEnd, nextClozeNumber(f.front)) }))
    setErrors(e => ({ ...e, front: undefined }))
  }

  async function handleSave(addAnother = false) {
    if (!validate() || !deckId) return
    setSaving(true)
    try {
      const tags = parseTags(form.tagsRaw)
      if (isEditing && cardId) {
        await updateCard(cardId, {
          front: form.front.trim(),
          back:  form.back.trim(),
          hint:  isCloze || isPicture ? undefined : form.hint.trim() || undefined,
          tags,
        })
        notify('success', 'Card updated!')
        navigate(`/decks/${deckId}`)
        return
      }

      let message: string
      if (isPicture && picture) {
        const uploaded = await uploadPicture(picture.blob)
        const created = await createImageCards(deckId, {
          imageId: uploaded.id, prompt: form.front.trim(), extra: form.back.trim(),
          boxes: boxes.map(box => ({ ...box, label: box.label?.trim() ?? '' })), tags,
        })
        message = created.length === 1 ? '1 card added!' : `${created.length} cards added!`
      } else if (isCloze) {
        const created = await createClozeCards(deckId, { text: form.front.trim(), extra: form.back.trim(), tags })
        message = created.length === 1 ? '1 card added!' : `${created.length} cards added!`
      } else {
        await createCard(deckId, { front: form.front.trim(), back: form.back.trim(), hint: form.hint.trim() || undefined, tags })
        message = 'Card added!'
      }
      if (addAnother) {
        setForm(DEFAULT_FORM)
        setPicture(null)
        setBoxes([])
        setErrors({})
        notify('success', `${message} Add another.`)
      } else {
        notify('success', message)
        navigate(`/decks/${deckId}`)
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

  const numbers = isCloze ? clozeNumbers(form.front) : []

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

      {/* Card type, only when adding: an existing card keeps its type */}
      {!isEditing && (
        <fieldset className="flex gap-2" aria-label="Card type">
          {([['BASIC', 'Question and answer'], ['CLOZE', 'Fill in the blank'], ['IMAGE', 'Picture']] as const).map(([value, label]) => (
            <label key={value}
              className={`px-4 py-2 rounded-lg border text-sm font-medium cursor-pointer
                ${cardType === value ? 'border-primary-500 bg-primary-50 text-primary-700' : 'border-gray-200 text-gray-700'}`}>
              <input type="radio" name="card-type" className="sr-only" checked={cardType === value}
                onChange={() => { setCardType(value); setErrors({}) }} />
              {label}
            </label>
          ))}
        </fieldset>
      )}

      {/* Form */}
      <div className="bg-white border border-gray-200 rounded-xl p-6 space-y-5">
        {isPicture && (
          <div>
            <p className="block text-sm font-semibold text-gray-700 mb-1">
              Picture {!isEditing && <span className="text-red-600">*</span>}
            </p>
            {isEditing ? (
              <>
                <PictureWithBoxes imageId={savedImageId} boxes={boxes} activeIndex={clozeIndex} revealed={false} />
                <p className="mt-2 text-xs text-gray-600">
                  This card asks for box {clozeIndex}. You can change the question, notes and tags here.
                </p>
              </>
            ) : (
              <>
                <label className="inline-flex items-center gap-2 px-4 py-2 border border-gray-300 rounded-lg text-sm font-medium text-gray-700 cursor-pointer hover:bg-gray-50 focus-within:ring-2 focus-within:ring-primary-500">
                  {preparing ? 'Preparing picture...' : picture ? 'Choose another picture' : 'Choose a picture'}
                  <input
                    type="file"
                    accept="image/*"
                    className="sr-only"
                    aria-label="Choose a picture"
                    disabled={preparing}
                    onChange={e => { choosePicture(e.target.files?.[0]); e.target.value = '' }}
                  />
                </label>
                <p className="mt-1 text-xs text-gray-600">
                  An anatomy diagram, an ECG strip or an X-ray. It is made smaller before upload.
                  Anyone who can see this deck can see the picture.
                </p>
                {picture && (
                  <div className="mt-3">
                    <PictureBoxEditor imageUrl={picture.url} boxes={boxes} onChange={next => { setBoxes(next); setErrors(e => ({ ...e, picture: undefined })) }} />
                  </div>
                )}
              </>
            )}
            {errors.picture && <p role="alert" className="mt-2 text-xs text-red-600">{errors.picture}</p>}
          </div>
        )}

        {isPicture ? (
          <div>
            <label htmlFor="card-front" className="block text-sm font-semibold text-gray-700 mb-1">
              Question <span className="text-gray-500 font-normal">(optional, shown above the picture)</span>
            </label>
            <input
              id="card-front"
              type="text"
              value={form.front}
              onChange={e => setForm(f => ({ ...f, front: e.target.value }))}
              className={`w-full px-4 py-2.5 border rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-primary-500
                ${errors.front ? 'border-red-400 bg-red-50' : 'border-gray-300'}`}
              placeholder="What is under the highlighted box?"
              maxLength={2000}
            />
            {errors.front && <p className="mt-1 text-xs text-red-600">{errors.front}</p>}
          </div>
        ) : isCloze ? (
          <div>
            <label htmlFor="cloze-text" className="block text-sm font-semibold text-gray-700 mb-1">
              Text with blanks <span className="text-red-600">*</span>
            </label>
            <p className="text-xs text-gray-500 mb-2">
              Select the words to hide and press Make blank, or type {'{{c1::answer}}'}. Each number becomes its own
              card; use the same number for blanks that belong together. Add a hint with {'{{c1::answer::hint}}'}.
              {clozeIndex !== null && ` This card asks for blank c${clozeIndex}.`}
            </p>
            <textarea
              id="cloze-text"
              ref={textRef}
              value={form.front}
              onChange={e => setForm(f => ({ ...f, front: e.target.value }))}
              rows={4}
              className={`w-full px-4 py-3 border rounded-lg text-sm focus:outline-none
                focus:ring-2 focus:ring-primary-500 resize-y font-mono
                ${errors.front ? 'border-red-400 bg-red-50' : 'border-gray-300'}`}
              placeholder="{{c1::Metoprolol}} is a {{c2::beta blocker}}"
              maxLength={2000}
            />
            <div className="mt-2 flex flex-wrap items-center gap-3">
              <Button type="button" variant="secondary" size="sm" onClick={makeBlank}>Make blank</Button>
              {!isEditing && numbers.length > 0 && (
                <span className="text-xs text-gray-500">
                  Makes {numbers.length} {numbers.length === 1 ? 'card' : 'cards'} ({numbers.map(n => `c${n}`).join(', ')})
                </span>
              )}
            </div>
            {errors.front && <p className="mt-1 text-xs text-red-600">{errors.front}</p>}
            {numbers.length > 0 && (
              <div className="mt-3 text-sm text-gray-800 bg-gray-50 border border-gray-200 rounded-lg p-3">
                <p className="text-xs font-semibold text-gray-500 uppercase tracking-wide mb-1">
                  Preview{clozeIndex === null ? ` (card c${numbers[0]})` : ''}
                </p>
                <ClozeView text={form.front} index={clozeIndex ?? numbers[0]} revealed={false} />
              </div>
            )}
          </div>
        ) : (
          <div>
            <label htmlFor="card-front" className="block text-sm font-semibold text-gray-700 mb-1">
              Question (Front) <span className="text-red-600">*</span>
            </label>
            <textarea
              id="card-front"
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
            <p className="mt-1 text-xs text-gray-500">{form.front.length}/2000</p>
          </div>
        )}

        {/* Back, or a cloze card's extra notes */}
        <div>
          <label htmlFor="card-back" className="block text-sm font-semibold text-gray-700 mb-1">
            {isCloze || isPicture ? <>Extra notes <span className="text-gray-500 font-normal">(optional, shown with the answer)</span></>
              : <>Answer (Back) <span className="text-red-600">*</span></>}
          </label>
          <textarea
            id="card-back"
            value={form.back}
            onChange={e => setForm(f => ({ ...f, back: e.target.value }))}
            rows={isCloze || isPicture ? 3 : 6}
            className={`w-full px-4 py-3 border rounded-lg text-sm focus:outline-none
              focus:ring-2 focus:ring-primary-500 resize-y
              ${errors.back ? 'border-red-400 bg-red-50' : 'border-gray-300'}`}
            placeholder={isPicture ? 'Optional notes shown with every answer' : isCloze ? 'Cardioselective: mostly beta-1' : 'Dyspnea, orthopnea, PND, fatigue, pulmonary edema (crackles/rales). Key finding: S3 gallop.'}
            maxLength={5000}
          />
          {errors.back && <p className="mt-1 text-xs text-red-600">{errors.back}</p>}
        </div>

        {/* Hint (basic cards; a cloze card puts its hint inside the blank) */}
        {!isCloze && !isPicture && (
          <div>
            <label htmlFor="card-hint" className="block text-sm font-medium text-gray-700 mb-1">
              Hint <span className="text-gray-500 font-normal">(optional)</span>
            </label>
            <input
              id="card-hint"
              type="text"
              value={form.hint}
              onChange={e => setForm(f => ({ ...f, hint: e.target.value }))}
              className="w-full px-4 py-2.5 border border-gray-300 rounded-lg text-sm
                focus:outline-none focus:ring-2 focus:ring-primary-500"
              placeholder="Think about what happens when the left ventricle fails to pump..."
              maxLength={500}
            />
          </div>
        )}

        {/* Tags */}
        <div>
          <label htmlFor="card-tags" className="block text-sm font-medium text-gray-700 mb-1">
            Tags <span className="text-gray-500 font-normal">(optional, comma-separated)</span>
          </label>
          <input
            id="card-tags"
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
            {isEditing ? 'Save Changes' : isCloze || isPicture ? 'Save Cards' : 'Save Card'}
          </Button>
        </div>
      </div>
    </div>
  )
}
