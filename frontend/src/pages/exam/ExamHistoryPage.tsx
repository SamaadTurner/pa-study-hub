import { useState, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import { getExamHistory } from '@/api/exam'
import type { ExamHistorySummary, ApiError } from '@/types'
import LoadingSpinner from '@/components/common/LoadingSpinner'
import Button from '@/components/common/Button'
import { formatDate } from '@/utils/formatDate'
import { formatDuration } from '@/utils/formatDuration'
import { getCategoryLabel } from '@/utils/categoryColors'

type SortKey = 'completedAt' | 'scorePercent' | 'questionCount' | 'durationSeconds'
type SortDir = 'asc' | 'desc'

const BAND_COLORS: Record<string, string> = {
  EXCELLENT: 'bg-emerald-100 text-emerald-700',
  PASS:      'bg-green-100 text-green-700',
  NEAR_PASS: 'bg-yellow-100 text-yellow-700',
  FAIL:      'bg-red-100 text-red-700',
}

const BAND_LABELS: Record<string, string> = {
  EXCELLENT: 'Excellent',
  PASS:      'Pass',
  NEAR_PASS: 'Near Pass',
  FAIL:      'Fail',
}

function SortIcon({ active, dir }: { active: boolean; dir: SortDir }) {
  if (!active) return <span className="text-gray-300 ml-1">↕</span>
  return <span className="text-primary-600 ml-1">{dir === 'asc' ? '↑' : '↓'}</span>
}

export default function ExamHistoryPage() {
  const navigate = useNavigate()

  const [history, setHistory] = useState<ExamHistorySummary[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError]     = useState<string | null>(null)
  const [sortKey, setSortKey] = useState<SortKey>('completedAt')
  const [sortDir, setSortDir] = useState<SortDir>('desc')

  useEffect(() => {
    getExamHistory()
      .then(setHistory)
      .catch((err: ApiError) => setError(err?.detail ?? 'Failed to load history'))
      .finally(() => setLoading(false))
  }, [])

  function handleSort(key: SortKey) {
    if (key === sortKey) {
      setSortDir(d => d === 'asc' ? 'desc' : 'asc')
    } else {
      setSortKey(key)
      setSortDir('desc')
    }
  }

  const sorted = [...history].sort((a, b) => {
    let av: number | string = a[sortKey] ?? ''
    let bv: number | string = b[sortKey] ?? ''
    if (sortKey === 'completedAt') {
      av = new Date(a.completedAt).getTime()
      bv = new Date(b.completedAt).getTime()
    }
    if (av < bv) return sortDir === 'asc' ? -1 : 1
    if (av > bv) return sortDir === 'asc' ? 1 : -1
    return 0
  })

  if (loading) {
    return (
      <div className="flex items-center justify-center py-24">
        <LoadingSpinner message="Loading history..." size="lg" />
      </div>
    )
  }

  if (error) {
    return (
      <div className="max-w-xl mx-auto text-center py-16">
        <p className="text-5xl mb-4">⚠️</p>
        <h2 className="text-xl font-bold text-gray-900 mb-2">{error}</h2>
        <Button onClick={() => navigate('/exam')}>Back to Exam Setup</Button>
      </div>
    )
  }

  return (
    <div className="max-w-3xl mx-auto space-y-6">

      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">Exam History</h1>
          <p className="text-sm text-gray-500 mt-0.5">
            {history.length} {history.length === 1 ? 'exam' : 'exams'} completed
          </p>
        </div>
        <Button onClick={() => navigate('/exam')}>New Exam →</Button>
      </div>

      {history.length === 0 ? (
        <div className="bg-white border border-gray-200 rounded-xl p-12 text-center">
          <p className="text-4xl mb-3">📋</p>
          <h2 className="text-lg font-semibold text-gray-900 mb-1">No exams yet</h2>
          <p className="text-sm text-gray-500 mb-4">Take a practice exam to see your history here.</p>
          <Button onClick={() => navigate('/exam')}>Start Practice Exam</Button>
        </div>
      ) : (
        <div className="bg-white border border-gray-200 rounded-xl overflow-hidden">
          {/* Table header */}
          <div className="grid grid-cols-5 gap-4 px-5 py-3 bg-gray-50 border-b border-gray-200 text-xs font-semibold text-gray-500 uppercase tracking-wide">
            <button
              className="text-left flex items-center col-span-1"
              onClick={() => handleSort('completedAt')}
            >
              Date <SortIcon active={sortKey === 'completedAt'} dir={sortDir} />
            </button>
            <button
              className="text-right flex items-center justify-end"
              onClick={() => handleSort('scorePercent')}
            >
              Score <SortIcon active={sortKey === 'scorePercent'} dir={sortDir} />
            </button>
            <div className="text-center">Band</div>
            <button
              className="text-right flex items-center justify-end"
              onClick={() => handleSort('questionCount')}
            >
              Questions <SortIcon active={sortKey === 'questionCount'} dir={sortDir} />
            </button>
            <button
              className="text-right flex items-center justify-end"
              onClick={() => handleSort('durationSeconds')}
            >
              Duration <SortIcon active={sortKey === 'durationSeconds'} dir={sortDir} />
            </button>
          </div>

          {/* Rows */}
          {sorted.map(exam => {
            const bandClass = BAND_COLORS[exam.performanceBand] ?? 'bg-gray-100 text-gray-600'
            const bandLabel = BAND_LABELS[exam.performanceBand] ?? exam.performanceBand
            return (
              <button
                key={exam.sessionId}
                onClick={() => navigate(`/exam/${exam.sessionId}/results`)}
                className="w-full grid grid-cols-5 gap-4 px-5 py-4 border-b border-gray-100
                  last:border-0 hover:bg-gray-50 transition-colors text-left"
              >
                <div className="col-span-1">
                  <p className="text-sm font-medium text-gray-900">{formatDate(exam.completedAt)}</p>
                  {exam.categoryFilter && (
                    <p className="text-xs text-gray-400 mt-0.5">
                      {getCategoryLabel(exam.categoryFilter)}
                    </p>
                  )}
                </div>
                <div className="text-right">
                  <span className="text-sm font-bold text-gray-900">{exam.scorePercent}%</span>
                </div>
                <div className="flex justify-center">
                  <span className={`px-2 py-0.5 rounded-full text-xs font-medium ${bandClass}`}>
                    {bandLabel}
                  </span>
                </div>
                <div className="text-right">
                  <span className="text-sm text-gray-700">{exam.questionCount}</span>
                </div>
                <div className="text-right">
                  <span className="text-sm text-gray-700">{formatDuration(exam.durationSeconds)}</span>
                </div>
              </button>
            )
          })}
        </div>
      )}
    </div>
  )
}
