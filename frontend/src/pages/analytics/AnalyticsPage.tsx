import { useProgress } from '@/hooks/useProgress'
import type { DailyProgressPoint } from '@/types'
import LoadingSpinner from '@/components/common/LoadingSpinner'
import EmptyState from '@/components/common/EmptyState'
import { getCategoryLabel, CATEGORY_HEX_COLORS } from '@/utils/categoryColors'
import { formatShortDate } from '@/utils/formatDate'
import { itemsStudied, activityLevel, describeDay, type ActivityLevel } from '@/utils/activity'
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
        No category data yet. Review some cards or finish a practice exam first.
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
  const daysStudied = data.filter(d => itemsStudied(d) > 0).length

  // Every day gets a place on the chart, so the dates are evenly spaced. A day
  // without studying has no accuracy, and the line passes straight over it.
  const chartData = data.map(d => ({
    date: formatShortDate(d.date),
    accuracy: itemsStudied(d) > 0 ? Math.round(d.accuracy) : null,
  }))

  if (daysStudied < 2) {
    return (
      <p className="text-sm text-gray-400 text-center py-8">
        Not enough data yet. Study on two or more days to see a trend.
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
          minTickGap={24}
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
          connectNulls
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
const LEVEL_COLORS: Record<ActivityLevel, string> = {
  none:    'bg-gray-100',
  light:   'bg-primary-100',
  active:  'bg-primary-300',
  goalMet: 'bg-primary-500',
}

// One square per day, oldest first, exactly the days the backend sent. The page
// used to work out 35 dates of its own, but the backend only has the last 30,
// so the first five squares could never be filled in.
function ActivityHeatmap({ data }: { data: DailyProgressPoint[] }) {
  return (
    <div>
      <div className="grid grid-cols-10 gap-1.5">
        {data.map(point => (
          <div
            key={point.date}
            role="img"
            aria-label={describeDay(point)}
            title={describeDay(point)}
            className={`h-7 rounded ${LEVEL_COLORS[activityLevel(point)]} cursor-default transition-opacity hover:opacity-75`}
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
  const { dashboard, loading, error, refetch } = useProgress()

  if (loading) {
    return (
      <div className="flex items-center justify-center py-24">
        <LoadingSpinner message="Loading analytics..." size="lg" />
      </div>
    )
  }

  // Without this the page showed 0% and empty charts when the request failed,
  // which looks the same as an account that has never studied.
  if (error) {
    return (
      <EmptyState
        icon="⚠️"
        title="Failed to load analytics"
        message={error}
        action={{ label: 'Try Again', onClick: refetch }}
      />
    )
  }

  const activity = dashboard?.last30DaysActivity ?? []
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
        <div className="flex items-center justify-between mb-4">
          <h2 className="text-sm font-semibold text-gray-900">Study Activity, Last 30 Days</h2>
          {activity.length > 0 && (
            <span className="text-xs text-gray-400">
              {formatShortDate(activity[0].date)} to today, oldest first
            </span>
          )}
        </div>
        <ActivityHeatmap data={activity} />
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
