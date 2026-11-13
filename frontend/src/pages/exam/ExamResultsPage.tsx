import { useState, useEffect } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { getExamResult } from '@/api/exam'
import type { ExamResult, ExamAnswerDetail, ApiError } from '@/types'
import LoadingSpinner from '@/components/common/LoadingSpinner'
import Button from '@/components/common/Button'
import AnswerOptions from '@/components/exams/AnswerOptions'
import { getCategoryLabel, CATEGORY_HEX_COLORS } from '@/utils/categoryColors'
import { formatDuration } from '@/utils/formatDuration'
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

// The band comes from the backend, these are only the colors and labels for it
const BAND_STYLES: Record<string, { bg: string; text: string; border: string; label: string }> = {
  EXCELLENT:         { bg: 'bg-emerald-50', text: 'text-emerald-700', border: 'border-emerald-200', label: 'Excellent' },
  GOOD:              { bg: 'bg-green-50',   text: 'text-green-700',   border: 'border-green-200',   label: 'Good' },
  PASSING:           { bg: 'bg-yellow-50',  text: 'text-yellow-700',  border: 'border-yellow-200',  label: 'Borderline' },
  NEEDS_IMPROVEMENT: { bg: 'bg-red-50',     text: 'text-red-700',     border: 'border-red-200',     label: 'Needs improvement' },
}
const DEFAULT_BAND = { bg: 'bg-gray-50', text: 'text-gray-700', border: 'border-gray-200', label: 'Complete' }

// One question in the review list: what was picked, what was right, and why
function QuestionReview({ detail, number }: { detail: ExamAnswerDetail; number: number }) {
  const correctOption = detail.answerOptions.find(option => option.isCorrect)
  const wasAnswered   = detail.selectedOptionId !== null

  let badgeText  = 'Not answered'
  let badgeClass = 'bg-gray-100 text-gray-600'
  if (wasAnswered && detail.isCorrect) {
    badgeText  = 'Correct'
    badgeClass = 'bg-green-100 text-green-700'
  } else if (wasAnswered) {
    badgeText  = 'Incorrect'
    badgeClass = 'bg-red-100 text-red-700'
  }

  return (
    <div className="bg-white border border-gray-200 rounded-xl p-6 space-y-4">
      <div className="flex items-center justify-between">
        <span className="text-sm font-semibold text-gray-900">Question {number}</span>
        <span className={`px-2 py-0.5 rounded-full text-xs font-medium ${badgeClass}`}>
          {badgeText}
        </span>
      </div>

      {detail.clinicalVignette && (
        <p className="text-sm text-gray-700 leading-relaxed bg-blue-50 border-l-4 border-blue-400 p-4 rounded">
          {detail.clinicalVignette}
        </p>
      )}
      <p className="text-gray-900 text-sm leading-relaxed font-medium">{detail.stem}</p>

      <AnswerOptions
        options={detail.answerOptions}
        selectedId={detail.selectedOptionId}
        correctId={correctOption ? correctOption.id : null}
        onSelect={() => { /* read-only in the review */ }}
        disabled
      />

      <div className="bg-gray-50 border border-gray-200 rounded-lg p-4">
        <p className="text-xs font-semibold text-gray-500 uppercase tracking-wide mb-1">Explanation</p>
        <p className="text-sm text-gray-700 leading-relaxed">{detail.explanation}</p>
      </div>
    </div>
  )
}

export default function ExamResultsPage() {
  const { sessionId } = useParams<{ sessionId: string }>()
  const navigate = useNavigate()

  const [result, setResult]   = useState<ExamResult | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError]     = useState<string | null>(null)

  useEffect(() => {
    if (!sessionId) return
    getExamResult(sessionId)
      .then(setResult)
      .catch((err: ApiError) => setError(err?.detail ?? 'Failed to load results'))
      .finally(() => setLoading(false))
  }, [sessionId])

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

  const unansweredCount = result.answerDetails.filter(d => d.selectedOptionId === null).length
  const incorrectCount  = result.totalQuestions - result.rawScore - unansweredCount

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
        {result.performanceMessage && (
          <p className="text-sm text-gray-700 mb-4">{result.performanceMessage}</p>
        )}
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
              {formatDuration(Math.round(result.avgTimePerQuestionSeconds))}
            </p>
            <p className="text-xs text-gray-500 mt-0.5">Avg / answer</p>
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
          <p className="text-3xl font-bold text-gray-900">{incorrectCount}</p>
          <p className="text-xs text-gray-500 mt-1">Incorrect answers</p>
        </div>
        <div className="bg-white border border-gray-200 rounded-xl p-5 text-center">
          <p className="text-3xl font-bold text-gray-900">{unansweredCount}</p>
          <p className="text-xs text-gray-500 mt-1">Not answered</p>
        </div>
      </div>

      {/* ── Question review ───────────────────────────────────────────── */}
      <section className="space-y-4">
        <h2 className="text-sm font-semibold text-gray-900">Review your answers</h2>
        {result.answerDetails.map((detail, index) => (
          <QuestionReview key={detail.questionId} detail={detail} number={index + 1} />
        ))}
      </section>

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
            New Exam →
          </Button>
        </div>
      </div>
    </div>
  )
}
