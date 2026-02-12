import CircularProgress from '@/components/common/CircularProgress'
import type { GoalProgress } from '@/types'

interface Props {
  goal: GoalProgress
}

export default function DailyGoalProgress({ goal }: Props) {
  const cardPct   = goal.targetCardsPerDay   > 0
    ? Math.min(100, Math.round((goal.todayCardsReviewed / goal.targetCardsPerDay) * 100))
    : 100
  const minutePct = goal.targetMinutesPerDay > 0
    ? Math.min(100, Math.round((goal.todayMinutesStudied / goal.targetMinutesPerDay) * 100))
    : 100

  return (
    <div className="bg-white border border-gray-200 rounded-xl p-5">
      <h3 className="text-sm font-semibold text-gray-700 mb-4">Today's Goals</h3>
      <div className="flex items-center justify-around gap-2">
        <CircularProgress
          value={minutePct}
          size={88}
          label="Minutes"
          sublabel={`${goal.todayMinutesStudied} / ${goal.targetMinutesPerDay}`}
        />
        <CircularProgress
          value={cardPct}
          size={88}
          label="Cards"
          sublabel={`${goal.todayCardsReviewed} / ${goal.targetCardsPerDay}`}
        />
      </div>
      {goal.cardGoalMet && goal.minuteGoalMet && (
        <div className="mt-4 text-center">
          <span className="inline-flex items-center gap-1 bg-green-100 text-green-800
            text-xs font-medium px-3 py-1 rounded-full">
            🎉 Daily goal complete!
          </span>
        </div>
      )}
    </div>
  )
}
