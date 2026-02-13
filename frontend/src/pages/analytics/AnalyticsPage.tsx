import { useState, useEffect } from 'react'
import { getAnalytics, getActivityHistory } from '@/api/progress'
import type { ProgressDashboard, DailyProgressPoint } from '@/types'
import LoadingSpinner from '@/components/common/LoadingSpinner'
import { getCategoryLabel, CATEGORY_HEX_COLORS } from '@/utils/categoryColors'
import { formatDate } from '@/utils/formatDate'
import {
  BarChart,
  Bar,
  LineChart,
  Line,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
  Cell,
  ReferenceLine,
} from 'recharts'

// ── Category accuracy ───────────────────────────────────────────────────────
function CategoryAccuracyChart({ data }: { data: Record<string, number> }) {
  const chartData = Object.entries(data)
    .map(([cat, acc]) => ({
      name: getCategoryLabel(cat),
      accuracy: Math.round(acc),
      fill: CATEGORY_HEX_COLORS[cat] ?? '#6b7280',
    }))
    .sort((a, b) => b.accuracy - a.accuracy)

  if (chartData.length === 0) {
    return (
      <p className="text-sm text-gray-400 text-center py-8">
        No category data yet — complete some reviews first.
      </p>
    )
  }

  return (
    <ResponsiveContainer width="100%" height={Math.max(220, chartData.length * 38)}>
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
        <Tooltip formatter={(value: number) => [`${value}%`, 'Accuracy']} />
        <ReferenceLine x={70} stroke="#ef4444" strokeDasharray="4 2" strokeWidth={1.5} />
        <Bar dataKey="accuracy" radius={[0, 4, 4, 0]}>
          {chartData.map((entry, i) => (
            <Cell key={i} fill={entry.fill} />
          ))}
        </Bar>
      </BarChart>
    </ResponsiveContainer>
  )
}

// ── Accuracy trend line ─────────────────────────────────────────────────────
function AccuracyTrendChart({ data }: { data: DailyProgressPoint[] }) {
  const chartData = data
    .filter(d => d.cardsReviewed > 0)
    .map(d => ({
      date: formatDate(d.date),
      accuracy: Math.round(d.accuracy),
      cards: d.cardsReviewed,
    }))

  if (chartData.length < 2) {
    return (
      <p className="text-sm text-gray-400 text-center py-8">
        Not enough data yet — study for multiple days to see a trend.
      </p>
    )
  }

  return (
    <ResponsiveContainer width="100%" height={180}>
      <LineChart data={chartData} margin={{ top: 4, right: 16, bottom: 0, left: 0 }}>
        <CartesianGrid strokeDasharray="3 3" />
        <XAxis
          dataKey="date"
          tick={{ fontSize: 10 }}
          interval="preserveStartEnd"
        />
        <YAxis
          domain={[0, 100]}
          tickFormatter={v => `${v}%`}
          tick={{ fontSize: 10 }}
          width={36}
        />
        <Tooltip formatter={(value: number) => [`${value}%`, 'Accuracy']} />
        <ReferenceLine y={70} stroke="#ef4444" strokeDasharray="4 2" strokeWidth={1.5} />
        <Line
          type="monotone"
          dataKey="accuracy"
          stroke="#6366f1"
          strokeWidth={2}
          dot={{ r: 3, fill: '#6366f1' }}
          activeDot={{ r: 5 }}
        />
      </LineChart>
    </ResponsiveContainer>
  )
}

// ── Activity heatmap ────────────────────────────────────────────────────────
function ActivityHeatmap({ data }: { data: DailyProgressPoint[] }) {
  const byDate = Object.fromEntries(data.map(d => [d.date, d]))

  // Build last 35 days grid (5 rows × 7 cols, newest at bottom-right)
  const today = new Date()
  const days: Array<{ date: string; point: DailyProgressPoint | null }> = []
  for (let i = 34; i >= 0; i--) {
    const d = new Date(today)
    d.setDate(d.getDate() - i)
    const key = `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
    days.push({ date: key, point: byDate[key] ?? null })
  }

  function cellColor(point: DailyProgressPoint | null): string {
    if (!point || point.cardsReviewed === 0) return 'bg-gray-100'
    if (point.goalMet) return 'bg-primary-500'
    if (point.cardsReviewed >= 10) return 'bg-primary-300'
    return 'bg-primary-100'
  }

  return (
    <div>
      <div className="grid grid-cols-7 gap-1.5">
        {days.map(({ date, point }) => (
          <div
            key={date}
            title={point
              ? `${date}: ${point.cardsReviewed} cards, ${Math.round(point.accuracy)}% accuracy`
              : date}
            className={`h-7 rounded ${cellColor(point)} cursor-default transition-opacity hover:opacity-75`}
          />
        ))}
      </div>
      <div className="flex items-center gap-3 mt-3 text-xs text-gray-500">
        <span className="flex items-center gap-1.5">
          <span className="inline-block w-3 h-3 rounded bg-gray-100" /> No activity
        </span>
        <span className="flex items-center gap-1.5">
          <span className="inline-block w-3 h-3 rounded bg-primary-100" /> Light
        </span>
        <span className="flex items-center gap-1.5">
          <span className="inline-block w-3 h-3 rounded bg-primary-300" /> Active
        </span>
        <span className="flex items-center gap-1.5">
          <span className="inline-block w-3 h-3 rounded bg-primary-500" /> Goal met
        </span>
      </div>
    </div>
  )
}

// ── Main page ───────────────────────────────────────────────────────────────
export default function AnalyticsPage() {
  const [dashboard, setDashboard]   = useState<ProgressDashboard | null>(null)
  const [activity, setActivity]     = useState<DailyProgressPoint[]>([])
  const [loading, setLoading]       = useState(true)

  useEffect(() => {
    Promise.all([getAnalytics(), getActivityHistory(30)])
      .then(([dash, act]) => {
        setDashboard(dash)
        setActivity(act)
      })
      .finally(() => setLoading(false))
  }, [])

  if (loading) {
    return (
      <div className="flex items-center justify-center py-24">
        <LoadingSpinner message="Loading analytics..." size="lg" />
      </div>
    )
  }

  const overallAcc = dashboard?.overallAccuracy ?? 0

  return (
    <div className="max-w-3xl mx-auto space-y-8">

      {/* Header */}
      <div>
        <h1 className="text-2xl font-bold text-gray-900">Analytics</h1>
        <p className="text-sm text-gray-500 mt-0.5">
          Track your study performance across all categories
        </p>
      </div>

      {/* Overall stats */}
      <div className="grid grid-cols-3 gap-4">
        <div className="bg-white border border-gray-200 rounded-xl p-5 text-center">
          <p className="text-3xl font-bold text-gray-900">{Math.round(overallAcc)}%</p>
          <p className="text-xs text-gray-500 mt-1">Overall accuracy</p>
        </div>
        <div className="bg-white border border-gray-200 rounded-xl p-5 text-center">
          <p className="text-3xl font-bold text-gray-900">{dashboard?.totalCardsReviewed ?? 0}</p>
          <p className="text-xs text-gray-500 mt-1">Cards reviewed</p>
        </div>
        <div className="bg-white border border-gray-200 rounded-xl p-5 text-center">
          <p className="text-3xl font-bold text-gray-900">{dashboard?.totalStudyDays ?? 0}</p>
          <p className="text-xs text-gray-500 mt-1">Study days</p>
        </div>
      </div>

      {/* Activity heatmap */}
      <section className="bg-white border border-gray-200 rounded-xl p-6">
        <h2 className="text-sm font-semibold text-gray-900 mb-4">Study Activity — Last 35 Days</h2>
        <ActivityHeatmap data={dashboard?.last30DaysActivity ?? []} />
      </section>

      {/* Accuracy trend */}
      <section className="bg-white border border-gray-200 rounded-xl p-6">
        <div className="flex items-center justify-between mb-4">
          <h2 className="text-sm font-semibold text-gray-900">Accuracy Trend</h2>
          <span className="text-xs text-gray-400">Red line = 70% passing threshold</span>
        </div>
        <AccuracyTrendChart data={activity} />
      </section>

      {/* Category accuracy */}
      <section className="bg-white border border-gray-200 rounded-xl p-6">
        <div className="flex items-center justify-between mb-4">
          <h2 className="text-sm font-semibold text-gray-900">Accuracy by Category</h2>
          <span className="text-xs text-gray-400">Red line = 70% threshold</span>
        </div>
        <CategoryAccuracyChart data={dashboard?.categoryAccuracy ?? {}} />
      </section>

    </div>
  )
}
