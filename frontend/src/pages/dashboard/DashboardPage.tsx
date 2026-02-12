import { useAuth } from '@/contexts/AuthContext'
import { useProgress } from '@/hooks/useProgress'
import { useDecks } from '@/hooks/useDecks'
import { getGreeting } from '@/utils/formatDate'
import LoadingSpinner from '@/components/common/LoadingSpinner'
import StreakWidget from '@/components/dashboard/StreakWidget'
import DailyGoalProgress from '@/components/dashboard/DailyGoalProgress'
import CardsDueWidget from '@/components/dashboard/CardsDueWidget'
import WeakAreasAlert from '@/components/dashboard/WeakAreasAlert'
import RecentActivity from '@/components/dashboard/RecentActivity'
import QuickActions from '@/components/dashboard/QuickActions'

export default function DashboardPage() {
  const { user }               = useAuth()
  const { dashboard, loading: progressLoading } = useProgress()
  const { decks, loading: decksLoading }        = useDecks()

  const isLoading = progressLoading || decksLoading

  if (isLoading) {
    return (
      <div className="flex items-center justify-center py-24">
        <LoadingSpinner message="Loading your dashboard..." size="lg" />
      </div>
    )
  }

  const greeting = `${getGreeting()}, ${user?.firstName ?? 'there'}!`

  return (
    <div className="space-y-6">
      {/* Header */}
      <div>
        <h1 className="text-2xl font-bold text-gray-900">{greeting}</h1>
        <p className="text-gray-500 text-sm mt-1">
          {new Date().toLocaleDateString('en-US', {
            weekday: 'long',
            month: 'long',
            day: 'numeric',
          })}
        </p>
      </div>

      {/* Top row: streak + goal progress + cards due */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
        {dashboard ? (
          <StreakWidget
            currentStreak={dashboard.currentStreak}
            longestStreak={dashboard.longestStreak}
            totalStudyDays={dashboard.totalStudyDays}
          />
        ) : (
          <StreakWidget currentStreak={0} longestStreak={0} totalStudyDays={0} />
        )}

        {dashboard?.todayGoal ? (
          <DailyGoalProgress goal={dashboard.todayGoal} />
        ) : (
          <DailyGoalProgress
            goal={{
              targetCardsPerDay: 20,
              targetMinutesPerDay: 30,
              todayCardsReviewed: 0,
              todayMinutesStudied: 0,
              cardGoalMet: false,
              minuteGoalMet: false,
            }}
          />
        )}

        <CardsDueWidget decks={decks} />
      </div>

      {/* Quick actions */}
      <QuickActions />

      {/* Second row: weak areas + recent activity */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-4">
        {dashboard?.categoryAccuracy && Object.keys(dashboard.categoryAccuracy).length > 0 && (
          <WeakAreasAlert categoryAccuracy={dashboard.categoryAccuracy} />
        )}

        {dashboard?.last30DaysActivity && (
          <RecentActivity activity={dashboard.last30DaysActivity} />
        )}
      </div>

      {/* No data state */}
      {!dashboard && !progressLoading && (
        <div className="bg-blue-50 border border-blue-200 rounded-xl p-6 text-center">
          <p className="text-2xl mb-2">👋</p>
          <p className="font-semibold text-gray-900">Welcome to PA Study Hub!</p>
          <p className="text-sm text-gray-600 mt-1">
            Start by creating a flashcard deck or taking a practice exam.
          </p>
        </div>
      )}
    </div>
  )
}
