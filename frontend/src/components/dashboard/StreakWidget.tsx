interface Props {
  currentStreak: number
  longestStreak: number
  totalStudyDays: number
}

export default function StreakWidget({ currentStreak, longestStreak, totalStudyDays }: Props) {
  return (
    <div className="bg-gradient-to-br from-orange-50 to-red-50 border border-orange-200 rounded-xl p-5">
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-3">
          <span className="text-4xl">🔥</span>
          <div>
            <p className="text-3xl font-bold text-gray-900 leading-none">{currentStreak}</p>
            <p className="text-sm text-gray-600 mt-0.5">
              {currentStreak === 1 ? 'day streak' : 'day streak'}
            </p>
          </div>
        </div>
        <div className="text-right">
          <p className="text-xs text-gray-500">Best</p>
          <p className="text-lg font-semibold text-orange-600">{longestStreak} days</p>
        </div>
      </div>
      <div className="mt-3 pt-3 border-t border-orange-200">
        <p className="text-xs text-gray-500">
          {totalStudyDays} total study {totalStudyDays === 1 ? 'day' : 'days'}
        </p>
      </div>
    </div>
  )
}
