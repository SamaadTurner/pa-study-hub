import { useState } from 'react'
import { useNavigate, Link } from 'react-router-dom'
import { generateFlashcards } from '@/api/ai'
import { createDeck, createCard } from '@/api/decks'
import type { GeneratedFlashcard, ApiError } from '@/types'
import Button from '@/components/common/Button'
import LoadingSpinner from '@/components/common/LoadingSpinner'
import { useNotification } from '@/contexts/NotificationContext'
import { getCategoryLabel } from '@/utils/categoryColors'

const CATEGORIES = [
  'CARDIOLOGY', 'PULMONOLOGY', 'GASTROENTEROLOGY', 'MUSCULOSKELETAL',
  'NEUROLOGY', 'PSYCHIATRY', 'DERMATOLOGY', 'EENT', 'ENDOCRINOLOGY',
  'HEMATOLOGY', 'INFECTIOUS_DISEASE', 'NEPHROLOGY', 'REPRODUCTIVE',
  'PEDIATRICS', 'EMERGENCY_MEDICINE', 'PHARMACOLOGY', 'ANATOMY'
]

const CARD_COUNTS = [5, 10, 15, 20]

export default function AiGeneratorPage() {
  const navigate = useNavigate()
  const { notify } = useNotification()

  const [topic, setTopic] = useState('')
  const [category, setCategory] = useState('CARDIOLOGY')
  const [count, setCount] = useState(10)
  const [generating, setGenerating] = useState(false)
  const [generatedCards, setGeneratedCards] = useState<GeneratedFlashcard[]>([])
  const [saving, setSaving] = useState(false)

  async function handleGenerate() {
    if (!topic.trim()) {
      notify('error', 'Please enter lecture notes or topic content')
      return
    }

    setGenerating(true)
    try {
      const result = await generateFlashcards({ topic, category, count })
      setGeneratedCards(result.flashcards)
      notify('success', `Generated ${result.generatedCount} flashcards with Claude AI`)
    } catch (err) {
      notify('error', (err as ApiError)?.detail ?? 'Failed to generate flashcards')
    } finally {
      setGenerating(false)
    }
  }

  async function handleSaveToDeck() {
    if (generatedCards.length === 0) return

    setSaving(true)
    try {
      // Create new deck
      const deckTitle = `${getCategoryLabel(category)} — AI Generated`
      const deck = await createDeck({
        title: deckTitle,
        description: `Generated from: ${topic.substring(0, 100)}...`,
        category,
        isPublic: false
      })

      // Create all cards in the deck
      for (const card of generatedCards) {
        await createCard(deck.id, {
          front: card.front,
          back: card.back,
          hint: card.hint,
          tags: card.tags
        })
      }

      notify('success', `Saved ${generatedCards.length} cards to deck: ${deckTitle}`)
      navigate(`/decks/${deck.id}`)
    } catch (err) {
      notify('error', (err as ApiError)?.detail ?? 'Failed to save flashcards')
    } finally {
      setSaving(false)
    }
  }

  function handleEditCard(index: number, field: keyof GeneratedFlashcard, value: string) {
    setGeneratedCards(prev => {
      const updated = [...prev]
      if (field === 'tags') {
        updated[index] = { ...updated[index], tags: value.split(',').map(t => t.trim()).filter(Boolean) }
      } else {
        updated[index] = { ...updated[index], [field]: value }
      }
      return updated
    })
  }

  function handleDeleteCard(index: number) {
    setGeneratedCards(prev => prev.filter((_, i) => i !== index))
  }

  return (
    <div className="max-w-4xl mx-auto space-y-8">
      {/* Header */}
      <div className="space-y-3">
        <div>
          <h1 className="text-3xl font-bold text-gray-900">AI Flashcard Generator ✨</h1>
          <p className="text-gray-600 mt-2">
            Paste your lecture notes or textbook content, and Claude AI will generate PANCE-targeted flashcards for you
          </p>
        </div>

        {/* AI Tools Navigation */}
        <div className="flex gap-2">
          <Link
            to="/ai"
            className="flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-xs font-medium
              text-gray-600 hover:bg-gray-100 border border-gray-200"
          >
            💬 Chat
          </Link>
          <Link
            to="/ai/generate"
            className="flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-xs font-medium
              bg-primary-50 text-primary-700 border border-primary-200"
          >
            🃏 Generate Flashcards
          </Link>
          <Link
            to="/ai/study-plan"
            className="flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-xs font-medium
              text-gray-600 hover:bg-gray-100 border border-gray-200"
          >
            📅 Study Plan
          </Link>
        </div>
      </div>

      {/* Input Form */}
      <div className="bg-white border border-gray-200 rounded-xl p-6 space-y-4">
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-2">
            Lecture Notes or Topic Content
          </label>
          <textarea
            value={topic}
            onChange={e => setTopic(e.target.value)}
            placeholder="Paste your lecture notes, textbook section, or any study material here..."
            rows={10}
            className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm
              focus:outline-none focus:ring-2 focus:ring-primary-500 focus:border-transparent
              resize-none"
          />
          <p className="text-xs text-gray-500 mt-1">
            {topic.length} characters — longer content may produce better flashcards
          </p>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">
              Category
            </label>
            <select
              value={category}
              onChange={e => setCategory(e.target.value)}
              className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm
                focus:outline-none focus:ring-2 focus:ring-primary-500"
            >
              {CATEGORIES.map(cat => (
                <option key={cat} value={cat}>{getCategoryLabel(cat)}</option>
              ))}
            </select>
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">
              Number of Cards
            </label>
            <div className="flex gap-2">
              {CARD_COUNTS.map(c => (
                <button
                  key={c}
                  onClick={() => setCount(c)}
                  className={`flex-1 px-4 py-2 rounded-lg text-sm font-medium transition-colors
                    ${count === c
                      ? 'bg-primary-600 text-white'
                      : 'bg-gray-100 text-gray-700 hover:bg-gray-200'
                    }`}
                >
                  {c}
                </button>
              ))}
            </div>
          </div>
        </div>

        <Button
          onClick={handleGenerate}
          disabled={generating || !topic.trim()}
          className="w-full"
        >
          {generating ? (
            <>
              <LoadingSpinner size="sm" className="mr-2" />
              Claude is generating flashcards...
            </>
          ) : (
            'Generate with Claude AI ✨'
          )}
        </Button>
      </div>

      {/* Generated Cards */}
      {generatedCards.length > 0 && (
        <div className="space-y-4">
          <div className="flex items-center justify-between">
            <h2 className="text-xl font-bold text-gray-900">
              Generated Flashcards ({generatedCards.length})
            </h2>
            <div className="flex gap-3">
              <Button
                variant="secondary"
                onClick={() => handleGenerate()}
                disabled={generating}
              >
                Regenerate
              </Button>
              <Button
                onClick={handleSaveToDeck}
                disabled={saving}
              >
                {saving ? 'Saving...' : 'Save All to New Deck'}
              </Button>
            </div>
          </div>

          <div className="space-y-3">
            {generatedCards.map((card, index) => (
              <GeneratedCardPreview
                key={index}
                card={card}
                onEdit={(field, value) => handleEditCard(index, field, value)}
                onDelete={() => handleDeleteCard(index)}
              />
            ))}
          </div>
        </div>
      )}

      {/* Empty State */}
      {!generating && generatedCards.length === 0 && (
        <div className="text-center py-16">
          <p className="text-6xl mb-4">💡</p>
          <p className="text-gray-500">
            Enter your lecture notes above and click generate to get started
          </p>
        </div>
      )}
    </div>
  )
}

interface GeneratedCardPreviewProps {
  card: GeneratedFlashcard
  onEdit: (field: keyof GeneratedFlashcard, value: string) => void
  onDelete: () => void
}

function GeneratedCardPreview({ card, onEdit, onDelete }: GeneratedCardPreviewProps) {
  const [isEditing, setIsEditing] = useState(false)

  if (isEditing) {
    return (
      <div className="bg-white border border-gray-200 rounded-xl p-5 space-y-3">
        <div>
          <label className="block text-xs font-medium text-gray-700 mb-1">Front (Question)</label>
          <textarea
            value={card.front}
            onChange={e => onEdit('front', e.target.value)}
            rows={2}
            className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm
              focus:outline-none focus:ring-2 focus:ring-primary-500"
          />
        </div>
        <div>
          <label className="block text-xs font-medium text-gray-700 mb-1">Back (Answer)</label>
          <textarea
            value={card.back}
            onChange={e => onEdit('back', e.target.value)}
            rows={3}
            className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm
              focus:outline-none focus:ring-2 focus:ring-primary-500"
          />
        </div>
        <div>
          <label className="block text-xs font-medium text-gray-700 mb-1">Hint (optional)</label>
          <input
            type="text"
            value={card.hint ?? ''}
            onChange={e => onEdit('hint', e.target.value)}
            className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm
              focus:outline-none focus:ring-2 focus:ring-primary-500"
          />
        </div>
        <div>
          <label className="block text-xs font-medium text-gray-700 mb-1">Tags (comma-separated)</label>
          <input
            type="text"
            value={card.tags.join(', ')}
            onChange={e => onEdit('tags', e.target.value)}
            className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm
              focus:outline-none focus:ring-2 focus:ring-primary-500"
          />
        </div>
        <div className="flex gap-2">
          <Button onClick={() => setIsEditing(false)} size="sm" className="flex-1">
            Done Editing
          </Button>
          <Button onClick={onDelete} variant="secondary" size="sm">
            Delete
          </Button>
        </div>
      </div>
    )
  }

  return (
    <div className="bg-white border border-gray-200 rounded-xl p-5 hover:border-gray-300 transition-colors">
      <div className="space-y-3">
        <div>
          <p className="text-xs font-medium text-gray-500 mb-1">FRONT</p>
          <p className="text-sm text-gray-900">{card.front}</p>
        </div>
        <div className="border-t border-gray-100 pt-3">
          <p className="text-xs font-medium text-gray-500 mb-1">BACK</p>
          <p className="text-sm text-gray-700">{card.back}</p>
        </div>
        {card.hint && (
          <div className="border-t border-gray-100 pt-3">
            <p className="text-xs font-medium text-gray-500 mb-1">HINT</p>
            <p className="text-sm text-gray-600 italic">{card.hint}</p>
          </div>
        )}
        {card.tags.length > 0 && (
          <div className="flex gap-2 flex-wrap">
            {card.tags.map((tag, i) => (
              <span
                key={i}
                className="px-2 py-1 bg-gray-100 text-gray-700 text-xs rounded-md"
              >
                {tag}
              </span>
            ))}
          </div>
        )}
        <div className="flex gap-2 pt-2">
          <Button onClick={() => setIsEditing(true)} variant="secondary" size="sm">
            Edit
          </Button>
        </div>
      </div>
    </div>
  )
}
