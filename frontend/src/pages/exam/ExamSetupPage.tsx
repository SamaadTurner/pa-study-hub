import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { startExam } from '@/api/exam'
import { getQuestionCounts } from '@/api/questions'
import { ALL_CATEGORIES, getCategoryLabel } from '@/utils/categoryColors'
import { TASK_AREAS } from '@/utils/taskAreas'
import Button from '@/components/common/Button'
import { useNotification } from '@/contexts/NotificationContext'
import type { ApiError, QuestionCounts } from '@/types'

const QUESTION_COUNTS = [5, 10, 20, 30]

// Value used for the "All categories" choice
const ALL = ''
const TIME_LIMITS = [
  { label: 'Untimed', minutes: 0 },
  { label: '15 min', minutes: 15 },
  { label: '30 min', minutes: 30 },
  { label: '60 min', minutes: 60 },
]

export default function ExamSetupPage() {
  const navigate   = useNavigate()
  const { notify } = useNotification()

  // The backend takes one category or none (= all of them). This used to be a list
  // of checkboxes, but picking two or more quietly gave an exam from every category.
  const [category, setCategory]                     = useState<string>(ALL)
  const [questionCount, setQuestionCount]           = useState(10)
  const [timeLimitMinutes, setTimeLimitMinutes]      = useState(0)
  const [customCount, setCustomCount]               = useState('')
  const [useCustomCount, setUseCustomCount]         = useState(false)
  const [followBlueprint, setFollowBlueprint]       = useState(false)
  const [includeShared, setIncludeShared]           = useState(false)
  const [counts, setCounts]                         = useState<QuestionCounts | null>(null)
  const [loading, setLoading]                       = useState(false)
  const [error, setError]                           = useState<string | null>(null)

  const effectiveCount = useCustomCount
    ? Math.min(100, Math.max(1, parseInt(customCount) || 0))
    : questionCount

  // How many questions each topic has for this user, so a topic without enough
  // questions shows it before Start is pressed. Loaded again when shared questions
  // are switched on or off. Until it arrives nothing is greyed out.
  useEffect(() => {
    let ignore = false
    getQuestionCounts(includeShared)
      .then(result => { if (!ignore) setCounts(result) })
      .catch(() => { if (!ignore) setCounts(null) })
    return () => { ignore = true }
  }, [includeShared])

  function countFor(cat: string): number | null {
    if (!counts) return null
    return cat === ALL ? counts.total : counts.byCategory[cat] ?? 0
  }

  // A topic picked earlier can run empty when shared questions are switched off
  useEffect(() => {
    if (counts && category !== ALL && (counts.byCategory[category] ?? 0) === 0) {
      setCategory(ALL)
    }
  }, [counts, category])

  const available = countFor(category)
  const notEnough = available !== null && available < effectiveCount

  async function handleStart() {
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
        categoryFilter: category === ALL ? undefined : category,
        followBlueprint,
        includeSharedQuestions: includeShared,
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
      <div className="flex items-start justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">Practice Exam</h1>
          <p className="text-gray-500 text-sm mt-1">
            Configure your exam and test your PANCE readiness
          </p>
        </div>
        <Button variant="secondary" onClick={() => navigate('/exam/questions')}>
          My Questions
        </Button>
      </div>

      {/* Categories */}
      <section className="bg-white border border-gray-200 rounded-xl p-6 space-y-4">
        <div className="flex items-center justify-between">
          <h2 className="text-sm font-semibold text-gray-900">Category</h2>
        </div>
        {/* On a phone a dropdown, where the grid took 9 rows */}
        <div className="sm:hidden">
          <label htmlFor="exam-topic" className="sr-only">Topic</label>
          <select
            id="exam-topic"
            value={category}
            onChange={e => setCategory(e.target.value)}
            className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm bg-white
              focus:outline-none focus:ring-2 focus:ring-primary-500"
          >
            {[ALL, ...ALL_CATEGORIES].map(cat => {
              const count = countFor(cat)
              return (
                <option key={cat} value={cat} disabled={count === 0}>
                  {cat === ALL ? 'All categories' : getCategoryLabel(cat)}{count !== null ? ` (${count})` : ''}
                </option>
              )
            })}
          </select>
        </div>
        <div className="hidden sm:grid grid-cols-3 gap-2">
          {[ALL, ...ALL_CATEGORIES].map(cat => {
            const checked = category === cat
            const count = countFor(cat)
            const empty = count === 0
            return (
              <label
                key={cat}
                className={`flex items-center gap-2.5 px-3 py-2 rounded-lg border transition-colors text-sm
                  ${empty
                    ? 'border-gray-100 text-gray-500 opacity-60 cursor-not-allowed'
                    : checked
                      ? 'border-primary-400 bg-primary-50 text-primary-900 cursor-pointer'
                      : 'border-gray-200 text-gray-700 hover:border-gray-300 hover:bg-gray-50 cursor-pointer'
                  }`}
              >
                <input
                  type="radio"
                  name="category"
                  checked={checked}
                  disabled={empty}
                  onChange={() => setCategory(cat)}
                  className="border-gray-300 text-primary-600 focus:ring-primary-500"
                />
                <span className="leading-tight">
                  {cat === ALL ? 'All categories' : getCategoryLabel(cat)}
                  {count !== null && <span className="text-xs text-gray-600"> ({count})</span>}
                </span>
              </label>
            )
          })}
        </div>
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

      {/* Question mix */}
      <section className="bg-white border border-gray-200 rounded-xl p-6 space-y-3">
        <h2 className="text-sm font-semibold text-gray-900">Question Mix</h2>
        <div className="space-y-2">
          <label className="flex items-start gap-2.5 text-sm text-gray-700 cursor-pointer">
            <input
              type="radio"
              name="mix"
              checked={!followBlueprint}
              onChange={() => setFollowBlueprint(false)}
              className="mt-0.5 border-gray-300 text-primary-600 focus:ring-primary-500"
            />
            <span>Random</span>
          </label>
          <label className="flex items-start gap-2.5 text-sm text-gray-700 cursor-pointer">
            <input
              type="radio"
              name="mix"
              checked={followBlueprint}
              onChange={() => setFollowBlueprint(true)}
              className="mt-0.5 border-gray-300 text-primary-600 focus:ring-primary-500"
            />
            <span>
              PANCE blueprint mix
              <span className="block text-xs text-gray-500">
                Same share of each task as the real exam, as far as the question bank allows
              </span>
            </span>
          </label>
        </div>
        {followBlueprint && (
          <ul className="grid grid-cols-1 sm:grid-cols-2 gap-x-6 gap-y-1 text-xs text-gray-500 pt-1">
            {TASK_AREAS.map(area => (
              <li key={area.value} className="flex justify-between">
                <span>{area.label}</span>
                <span className="tabular-nums">{area.percent}%</span>
              </li>
            ))}
          </ul>
        )}
        <label className="flex items-start gap-2.5 text-sm text-gray-700 cursor-pointer pt-3 border-t border-gray-100">
          <input
            type="checkbox"
            checked={includeShared}
            onChange={e => setIncludeShared(e.target.checked)}
            className="mt-0.5 rounded border-gray-300 text-primary-600 focus:ring-primary-500"
          />
          <span>
            Include questions other users shared
            <span className="block text-xs text-gray-500">
              Written by other students and not checked by anyone. Your own questions are always included.
            </span>
          </span>
        </label>
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
      {notEnough && (
        <p className="text-sm text-amber-800 bg-amber-50 border border-amber-200 rounded-lg px-4 py-3" role="status">
          {available === 0
            ? 'There are no questions for this choice yet. Pick another topic, or write your own on the My Questions page.'
            : `Only ${available} ${available === 1 ? 'question' : 'questions'} for this choice. Pick ${available} or fewer, or another topic.`}
        </p>
      )}
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
              {category === ALL ? 'all categories' : getCategoryLabel(category)}
            </span>{' '}
            · <span className="font-medium">
              {timeLimitMinutes === 0 ? 'untimed' : `${timeLimitMinutes} min`}
            </span>
          </p>
          <p className="text-xs text-gray-500">
            {followBlueprint
              ? 'Questions picked to follow the 2025 PANCE blueprint task mix'
              : 'Questions picked at random from the starter questions and yours'}
            {includeShared && ', plus shared questions'}
          </p>
        </div>
        <Button onClick={handleStart} loading={loading} size="lg" disabled={notEnough}>
          Start Exam →
        </Button>
      </div>
    </div>
  )
}
