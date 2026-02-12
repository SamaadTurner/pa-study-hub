import type { DailyProgressPoint } from '@/types'
import { formatShortDate } from '@/utils/formatDate'

interface Props {
  activity: DailyProgressPoint[]
}

export default function RecentActivity({ activity }: Props) {
  const recent = [...activity]
    .filter(a => a.cardsReviewed > 0 || a.studyMinutes > 0)
    .sort((a, b) => b.date.localeCompare(a.date))
    .slice(0, 5)

  if (recent.length === 0) {
    return (
      <div className="bg-white border border-gray-200 rounded-xl p-5">
        <h3 className="text-sm font-semibold text-gray-700 mb-3">Recent Activity</h3>
        <p className="text-sm text-gray-400 text-center py-4">No activity yet. Start studying!</p>
      </div>
    )
  }

  return (
    <div className="bg-white border border-gray-200 rounded-xl p-5">
      <h3 className="text-sm font-semibold text-gray-700 mb-3">Recent Activity</h3>
      <div className="space-y-3">
        {recent.map(day => (
          <div key={day.date} className="flex items-center justify-between">
            <div className="flex items-center gap-2">
              <div className={`w-2 h-2 rounded-full ${day.goalMet ? 'bg-green-500' : 'bg-gray-300'}`} />
              <span className="text-sm text-gray-700">{formatShortDate(day.date)}</span>
            </div>
            <div className="flex items-center gap-3 text-xs text-gray-500">
              {day.cardsReviewed > 0 && (
                <span>🃏 {day.cardsReviewed} cards</span>
              )}
              {day.studyMinutes > 0 && (
                <span>⏱ {day.studyMinutes}m</span>
              )}
              {day.accuracy > 0 && (
                <span className={`font-medium ${day.accuracy >= 70 ? 'text-green-600' : 'text-red-500'}`}>
                  {Math.round(day.accuracy)}%
                </span>
              )}
            </div>
          </div>
        ))}
      </div>
    </div>
  )
}
