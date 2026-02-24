import { useState, useEffect } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { getExamResults } from '@/api/exam'
import { explainAnswer } from '@/api/ai'
import type { ExamResult, ApiError } from '@/types'
import LoadingSpinner from '@/components/common/LoadingSpinner'
import Button from '@/components/common/Button'
import Modal from '@/components/common/Modal'
import { getCategoryLabel, CATEGORY_HEX_COLORS } from '@/utils/categoryColors'
import { formatDuration } from '@/utils/formatDuration'
import { useNotification } from '@/contexts/NotificationContext'
import {
  BarChart,
  Bar,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
  Cell,
} from 'recharts'

const BAND_STYLES: Record<string, { bg: string; text: string; border: string; label: string }> = {
  EXCELLENT: { bg: 'bg-emerald-50', text: 'text-emerald-700', border: 'border-emerald-200', label: 'Excellent' },
  PASS:      { bg: 'bg-green-50',   text: 'text-green-700',   border: 'border-green-200',   label: 'Pass' },
  NEAR_PASS: { bg: 'bg-yellow-50',  text: 'text-yellow-700',  border: 'border-yellow-200',  label: 'Near Pass' },
  FAIL:      { bg: 'bg-red-50',     text: 'text-red-700',     border: 'border-red-200',     label: 'Fail' },
}
const DEFAULT_BAND = { bg: 'bg-gray-50', text: 'text-gray-700', border: 'border-gray-200', label: 'Complete' }

export default function ExamResultsPage() {
  const { sessionId } = useParams<{ sessionId: string }>()
  const navigate = useNavigate()
  const { notify } = useNotification()

  const [result, setResult]   = useState<ExamResult | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError]     = useState<string | null>(null)
  const [showAiHelp, setShowAiHelp] = useState(false)
  const [aiExplanation, setAiExplanation] = useState<string | null>(null)
  const [gettingExplanation, setGettingExplanation] = useState(false)
  const [helpForm, setHelpForm] = useState({
    questionText: '',
    selectedAnswer: '',
    correctAnswer: '',
    explanation: '',
    category: ''
  })

  useEffect(() => {
    if (!sessionId) return
    getExamResults(sessionId)
      .then(setResult)
      .catch((err: ApiError) => setError(err?.detail ?? 'Failed to load results'))
      .finally(() => setLoading(false))
  }, [sessionId])

  async function handleGetAiExplanation() {
    if (!helpForm.questionText || !helpForm.selectedAnswer || !helpForm.correctAnswer) {
      notify('error', 'Please fill in question, your answer, and correct answer')
      return
    }

    setGettingExplanation(true)
    try {
      const response = await explainAnswer({
        questionText: helpForm.questionText,
        selectedAnswer: helpForm.selectedAnswer,
        correctAnswer: helpForm.correctAnswer,
        explanation: helpForm.explanation,
        category: helpForm.category || 'GENERAL'
      })
      setAiExplanation(response.explanation)
      notify('success', 'Claude generated a personalized explanation')
    } catch (err) {
      notify('error', (err as ApiError)?.detail ?? 'Failed to get AI explanation')
    } finally {
      setGettingExplanation(false)
    }
  }

  function openAiHelp() {
    setShowAiHelp(true)
    setAiExplanation(null)
    // Pre-fill category if only one was tested
    const categories = Object.keys(result?.categoryBreakdown || {})
    if (categories.length === 1) {
      setHelpForm(prev => ({ ...prev, category: categories[0] }))
    }
  }

  if (loading) {
    return (
      <div className="flex items-center justify-center py-24">
        <LoadingSpinner message="Loading results..." size="lg" />
      </div>
    )
  }

  if (error || !result) {
    return (
      <div className="max-w-xl mx-auto text-center py-16">
        <p className="text-5xl mb-4">⚠️</p>
        <h2 className="text-xl font-bold text-gray-900 mb-2">
          {error ?? 'Results not found'}
        </h2>
        <Button onClick={() => navigate('/exam')}>Back to Exam Setup</Button>
      </div>
    )
  }

  const band = BAND_STYLES[result.performanceBand] ?? DEFAULT_BAND

  const chartData = Object.entries(result.categoryBreakdown)
    .map(([cat, pct]) => ({
      name: getCategoryLabel(cat),
      score: Math.round(pct),
      fill: CATEGORY_HEX_COLORS[cat] ?? '#6b7280',
    }))
    .sort((a, b) => b.score - a.score)

  return (
    <div className="max-w-2xl mx-auto space-y-8">

      {/* ── Score hero ────────────────────────────────────────────────── */}
      <div className={`rounded-2xl border p-8 text-center ${band.bg} ${band.border}`}>
        <p className={`text-7xl font-bold mb-2 ${band.text}`}>{result.scorePercent}%</p>
        <span className={`inline-block px-3 py-1 rounded-full text-sm font-semibold
          border ${band.border} ${band.text} mb-6`}>
          {band.label}
        </span>
        <div className="flex items-center justify-center gap-8 text-sm">
          <div className="text-center">
            <p className="text-2xl font-bold text-gray-900">{result.rawScore}/{result.totalQuestions}</p>
            <p className="text-xs text-gray-500 mt-0.5">Correct</p>
          </div>
          <div className="w-px h-10 bg-current opacity-20" />
          <div className="text-center">
            <p className="text-2xl font-bold text-gray-900">{formatDuration(result.durationSeconds)}</p>
            <p className="text-xs text-gray-500 mt-0.5">Total time</p>
          </div>
          <div className="w-px h-10 bg-current opacity-20" />
          <div className="text-center">
            <p className="text-2xl font-bold text-gray-900">
              {formatDuration(result.avgTimePerQuestionSeconds)}
            </p>
            <p className="text-xs text-gray-500 mt-0.5">Avg / question</p>
          </div>
        </div>
      </div>

      {/* ── Category breakdown ────────────────────────────────────────── */}
      {chartData.length > 0 && (
        <section className="bg-white border border-gray-200 rounded-xl p-6">
          <h2 className="text-sm font-semibold text-gray-900 mb-5">Performance by Category</h2>
          <ResponsiveContainer width="100%" height={Math.max(200, chartData.length * 44)}>
            <BarChart
              data={chartData}
              layout="vertical"
              margin={{ top: 0, right: 48, bottom: 0, left: 0 }}
            >
              <CartesianGrid strokeDasharray="3 3" horizontal={false} />
              <XAxis
                type="number"
                domain={[0, 100]}
                tickFormatter={v => `${v}%`}
                tick={{ fontSize: 11 }}
              />
              <YAxis
                type="category"
                dataKey="name"
                width={160}
                tick={{ fontSize: 11 }}
              />
              <Tooltip formatter={(value: number) => [`${value}%`, 'Score']} />
              <Bar dataKey="score" radius={[0, 4, 4, 0]}>
                {chartData.map((entry, i) => (
                  <Cell key={i} fill={entry.fill} />
                ))}
              </Bar>
            </BarChart>
          </ResponsiveContainer>
        </section>
      )}

      {/* ── Quick stats ───────────────────────────────────────────────── */}
      <div className="grid grid-cols-2 gap-4">
        <div className="bg-white border border-gray-200 rounded-xl p-5 text-center">
          <p className="text-3xl font-bold text-gray-900">{result.incorrectCount}</p>
          <p className="text-xs text-gray-500 mt-1">Incorrect answers</p>
        </div>
        <div className="bg-white border border-gray-200 rounded-xl p-5 text-center">
          <p className="text-3xl font-bold text-gray-900">
            {result.totalQuestions > 0
              ? Math.round(result.rawScore / result.totalQuestions * 100)
              : 0}%
          </p>
          <p className="text-xs text-gray-500 mt-1">Accuracy</p>
        </div>
      </div>

      {/* ── AI Help ───────────────────────────────────────────────────── */}
      {result.incorrectCount > 0 && (
        <div className="bg-gradient-to-r from-purple-50 to-blue-50 border border-purple-200 rounded-xl p-6">
          <div className="flex items-start justify-between gap-4">
            <div>
              <h3 className="text-sm font-semibold text-gray-900 mb-1 flex items-center gap-2">
                Need help understanding a question? ✨
              </h3>
              <p className="text-sm text-gray-600">
                Get a personalized explanation from Claude AI for any question you got wrong
              </p>
            </div>
            <Button onClick={openAiHelp} variant="secondary" size="sm">
              Get AI Help
            </Button>
          </div>
        </div>
      )}

      {/* ── Actions ───────────────────────────────────────────────────── */}
      <div className="flex items-center justify-between gap-4">
        <Button variant="secondary" onClick={() => navigate('/dashboard')}>
          ← Dashboard
        </Button>
        <div className="flex gap-3">
          <Button variant="secondary" onClick={() => navigate('/exam/history')}>
            View History
          </Button>
          <Button onClick={() => navigate('/exam')}>
            Retake Exam →
          </Button>
        </div>
      </div>

      {/* ── AI Help Modal ─────────────────────────────────────────────── */}
      <Modal
        isOpen={showAiHelp}
        onClose={() => setShowAiHelp(false)}
        title="Get AI Explanation ✨"
      >
        <div className="space-y-4">
          {!aiExplanation ? (
            <>
              <p className="text-sm text-gray-600">
                Paste the question you got wrong and Claude will explain why you got it wrong and help you understand the correct answer.
              </p>

              <div>
                <label className="block text-xs font-medium text-gray-700 mb-1">
                  Question Text *
                </label>
                <textarea
                  value={helpForm.questionText}
                  onChange={e => setHelpForm(prev => ({ ...prev, questionText: e.target.value }))}
                  rows={3}
                  placeholder="Paste the full question here..."
                  className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm
                    focus:outline-none focus:ring-2 focus:ring-primary-500"
                />
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block text-xs font-medium text-gray-700 mb-1">
                    Your Answer *
                  </label>
                  <input
                    type="text"
                    value={helpForm.selectedAnswer}
                    onChange={e => setHelpForm(prev => ({ ...prev, selectedAnswer: e.target.value }))}
                    placeholder="What you selected"
                    className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm
                      focus:outline-none focus:ring-2 focus:ring-primary-500"
                  />
                </div>

                <div>
                  <label className="block text-xs font-medium text-gray-700 mb-1">
                    Correct Answer *
                  </label>
                  <input
                    type="text"
                    value={helpForm.correctAnswer}
                    onChange={e => setHelpForm(prev => ({ ...prev, correctAnswer: e.target.value }))}
                    placeholder="The right answer"
                    className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm
                      focus:outline-none focus:ring-2 focus:ring-primary-500"
                  />
                </div>
              </div>

              <div>
                <label className="block text-xs font-medium text-gray-700 mb-1">
                  Explanation (optional)
                </label>
                <textarea
                  value={helpForm.explanation}
                  onChange={e => setHelpForm(prev => ({ ...prev, explanation: e.target.value }))}
                  rows={2}
                  placeholder="Original explanation if available..."
                  className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm
                    focus:outline-none focus:ring-2 focus:ring-primary-500"
                />
              </div>

              <div>
                <label className="block text-xs font-medium text-gray-700 mb-1">
                  Category
                </label>
                <select
                  value={helpForm.category}
                  onChange={e => setHelpForm(prev => ({ ...prev, category: e.target.value }))}
                  className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm
                    focus:outline-none focus:ring-2 focus:ring-primary-500"
                >
                  <option value="">Select category</option>
                  {Object.keys(result?.categoryBreakdown || {}).map(cat => (
                    <option key={cat} value={cat}>{getCategoryLabel(cat)}</option>
                  ))}
                </select>
              </div>

              <Button
                onClick={handleGetAiExplanation}
                disabled={gettingExplanation}
                className="w-full"
              >
                {gettingExplanation ? (
                  <>
                    <LoadingSpinner size="sm" className="mr-2" />
                    Claude is analyzing...
                  </>
                ) : (
                  'Get Personalized Explanation'
                )}
              </Button>
            </>
          ) : (
            <>
              <div className="bg-blue-50 border border-blue-200 rounded-lg p-4">
                <h4 className="text-sm font-semibold text-blue-900 mb-2">
                  Claude's Explanation:
                </h4>
                <div className="text-sm text-blue-900 whitespace-pre-wrap leading-relaxed">
                  {aiExplanation}
                </div>
              </div>

              <div className="flex gap-2">
                <Button
                  onClick={() => {
                    setAiExplanation(null)
                    setHelpForm({
                      questionText: '',
                      selectedAnswer: '',
                      correctAnswer: '',
                      explanation: '',
                      category: helpForm.category
                    })
                  }}
                  variant="secondary"
                  className="flex-1"
                >
                  Ask Another Question
                </Button>
                <Button
                  onClick={() => setShowAiHelp(false)}
                  className="flex-1"
                >
                  Done
                </Button>
              </div>
            </>
          )}
        </div>
      </Modal>
    </div>
  )
}
