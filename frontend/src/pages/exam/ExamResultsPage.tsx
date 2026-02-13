import { useState, useEffect } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { getExamResults } from '@/api/exam'
import type { ExamResult, ApiError } from '@/types'
import LoadingSpinner from '@/components/common/LoadingSpinner'
import Button from '@/components/common/Button'
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

  const [result, setResult]   = useState<ExamResult | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError]     = useState<string | null>(null)

  useEffect(() => {
    if (!sessionId) return
    getExamResults(sessionId)
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
    </div>
  )
}
