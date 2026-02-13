import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { startExam } from '@/api/exam'
import { ALL_CATEGORIES, getCategoryLabel } from '@/utils/categoryColors'
import Button from '@/components/common/Button'
import { useNotification } from '@/contexts/NotificationContext'
import type { ApiError } from '@/types'

const QUESTION_COUNTS = [10, 20, 30, 50]
const TIME_LIMITS = [
  { label: 'Untimed', minutes: 0 },
  { label: '15 min', minutes: 15 },
  { label: '30 min', minutes: 30 },
  { label: '60 min', minutes: 60 },
]

export default function ExamSetupPage() {
  const navigate   = useNavigate()
  const { notify } = useNotification()

  const [selectedCategories, setSelectedCategories] = useState<string[]>([])
  const [questionCount, setQuestionCount]           = useState(20)
  const [timeLimitMinutes, setTimeLimitMinutes]      = useState(0)
  const [customCount, setCustomCount]               = useState('')
  const [useCustomCount, setUseCustomCount]         = useState(false)
  const [loading, setLoading]                       = useState(false)
  const [error, setError]                           = useState<string | null>(null)

  function toggleCategory(cat: string) {
    setSelectedCategories(prev =>
      prev.includes(cat) ? prev.filter(c => c !== cat) : [...prev, cat]
    )
  }

  function selectAll() {
    setSelectedCategories([...ALL_CATEGORIES])
  }

  function clearAll() {
    setSelectedCategories([])
  }

  const effectiveCount = useCustomCount
    ? Math.min(100, Math.max(1, parseInt(customCount) || 0))
    : questionCount

  async function handleStart() {
    if (selectedCategories.length === 0) {
      setError('Select at least one category.')
      return
    }
    if (effectiveCount < 1) {
      setError('Enter at least 1 question.')
      return
    }
    setError(null)
    setLoading(true)
    try {
      const session = await startExam({
        questionCount: effectiveCount,
        timeLimitMinutes: timeLimitMinutes || undefined,
        categoryFilter: selectedCategories.length === 1 ? selectedCategories[0] : undefined,
      })
      navigate(`/exam/${session.id}`)
    } catch (err) {
      notify('error', (err as ApiError)?.detail ?? 'Failed to start exam')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="max-w-2xl mx-auto space-y-8">
      {/* Header */}
      <div>
        <h1 className="text-2xl font-bold text-gray-900">Practice Exam</h1>
        <p className="text-gray-500 text-sm mt-1">
          Configure your exam and test your PANCE readiness
        </p>
      </div>

      {/* Categories */}
      <section className="bg-white border border-gray-200 rounded-xl p-6 space-y-4">
        <div className="flex items-center justify-between">
          <h2 className="text-sm font-semibold text-gray-900">
            Categories <span className="text-red-500">*</span>
          </h2>
          <div className="flex gap-3">
            <button
              onClick={selectAll}
              className="text-xs text-primary-600 font-medium hover:underline"
            >
              Select All
            </button>
            <button
              onClick={clearAll}
              className="text-xs text-gray-400 hover:text-gray-600 font-medium"
            >
              Clear
            </button>
          </div>
        </div>
        <div className="grid grid-cols-2 sm:grid-cols-3 gap-2">
          {ALL_CATEGORIES.map(cat => {
            const checked = selectedCategories.includes(cat)
            return (
              <label
                key={cat}
                className={`flex items-center gap-2.5 px-3 py-2 rounded-lg border cursor-pointer
                  transition-colors text-sm
                  ${checked
                    ? 'border-primary-400 bg-primary-50 text-primary-900'
                    : 'border-gray-200 text-gray-700 hover:border-gray-300 hover:bg-gray-50'
                  }`}
              >
                <input
                  type="checkbox"
                  checked={checked}
                  onChange={() => toggleCategory(cat)}
                  className="rounded border-gray-300 text-primary-600 focus:ring-primary-500"
                />
                <span className="leading-tight">{getCategoryLabel(cat)}</span>
              </label>
            )
          })}
        </div>
        {selectedCategories.length > 0 && (
          <p className="text-xs text-primary-700 font-medium">
            {selectedCategories.length} of {ALL_CATEGORIES.length} categories selected
          </p>
        )}
      </section>

      {/* Question Count */}
      <section className="bg-white border border-gray-200 rounded-xl p-6 space-y-3">
        <h2 className="text-sm font-semibold text-gray-900">Number of Questions</h2>
        <div className="flex flex-wrap gap-2">
          {QUESTION_COUNTS.map(n => (
            <button
              key={n}
              onClick={() => { setQuestionCount(n); setUseCustomCount(false) }}
              className={`px-4 py-2 rounded-lg border text-sm font-medium transition-colors
                ${!useCustomCount && questionCount === n
                  ? 'border-primary-500 bg-primary-50 text-primary-700'
                  : 'border-gray-200 text-gray-700 hover:border-gray-300'
                }`}
            >
              {n}
            </button>
          ))}
          <button
            onClick={() => setUseCustomCount(true)}
            className={`px-4 py-2 rounded-lg border text-sm font-medium transition-colors
              ${useCustomCount
                ? 'border-primary-500 bg-primary-50 text-primary-700'
                : 'border-gray-200 text-gray-700 hover:border-gray-300'
              }`}
          >
            Custom
          </button>
        </div>
        {useCustomCount && (
          <input
            type="number"
            min={1}
            max={100}
            value={customCount}
            onChange={e => setCustomCount(e.target.value)}
            placeholder="Enter number (1–100)"
            className="w-40 px-3 py-2 border border-gray-300 rounded-lg text-sm
              focus:outline-none focus:ring-2 focus:ring-primary-500"
            autoFocus
          />
        )}
      </section>

      {/* Time Limit */}
      <section className="bg-white border border-gray-200 rounded-xl p-6 space-y-3">
        <h2 className="text-sm font-semibold text-gray-900">Time Limit</h2>
        <div className="flex flex-wrap gap-2">
          {TIME_LIMITS.map(t => (
            <button
              key={t.minutes}
              onClick={() => setTimeLimitMinutes(t.minutes)}
              className={`px-4 py-2 rounded-lg border text-sm font-medium transition-colors
                ${timeLimitMinutes === t.minutes
                  ? 'border-primary-500 bg-primary-50 text-primary-700'
                  : 'border-gray-200 text-gray-700 hover:border-gray-300'
                }`}
            >
              {t.label}
            </button>
          ))}
        </div>
        {timeLimitMinutes > 0 && (
          <p className="text-xs text-gray-500">
            ~{Math.round(timeLimitMinutes / effectiveCount * 60)} seconds per question
          </p>
        )}
      </section>

      {/* Summary + Error */}
      {error && (
        <p className="text-sm text-red-600 bg-red-50 border border-red-200 rounded-lg px-4 py-3">
          {error}
        </p>
      )}

      <div className="bg-gray-50 border border-gray-200 rounded-xl px-5 py-4 flex items-center justify-between">
        <div className="text-sm text-gray-600 space-y-0.5">
          <p>
            <span className="font-medium">{effectiveCount}</span> questions ·{' '}
            <span className="font-medium">
              {selectedCategories.length === 0
                ? 'no categories'
                : selectedCategories.length === ALL_CATEGORIES.length
                ? 'all categories'
                : `${selectedCategories.length} categories`}
            </span>{' '}
            · <span className="font-medium">
              {timeLimitMinutes === 0 ? 'untimed' : `${timeLimitMinutes} min`}
            </span>
          </p>
          <p className="text-xs text-gray-400">
            Questions randomly selected from the PANCE question bank
          </p>
        </div>
        <Button onClick={handleStart} loading={loading} size="lg">
          Start Exam →
        </Button>
      </div>
    </div>
  )
}
