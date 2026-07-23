import apiClient from './client'
import type { ProgressDashboard, GoalProgress, UpdateGoalRequest, DailyProgressPoint } from '@/types'

// Backend response types (match Java DTOs)
interface GoalProgressResponse {
  targetCardsPerDay: number
  targetMinutesPerDay: number
  cardsReviewedToday: number        // Backend uses different naming
  minutesStudiedToday: number       // Backend uses different naming
  cardGoalMet: boolean
  timeGoalMet: boolean              // Backend uses "timeGoalMet" instead of "minuteGoalMet"
  cardGoalPercent?: number
  timeGoalPercent?: number
}

interface DailyProgressPointResponse {
  date: string
  cardsReviewed: number
  minutesStudied: number            // Backend uses "minutesStudied" instead of "studyMinutes"
  accuracy: number
  goalMet: boolean
}

interface ProgressDashboardResponse {
  currentStreak: number
  longestStreak: number
  totalStudyDays: number
  totalStudyMinutes?: number
  totalCardsReviewed: number
  overallAccuracy: number
  weakestCategory?: string
  categoryAccuracy: Record<string, number>
  last30DaysActivity: DailyProgressPointResponse[]
  todayGoal: GoalProgressResponse
}

// Mapping functions to convert backend response to frontend types
function mapGoalProgress(response: GoalProgressResponse): GoalProgress {
  return {
    targetCardsPerDay: response.targetCardsPerDay,
    targetMinutesPerDay: response.targetMinutesPerDay,
    todayCardsReviewed: response.cardsReviewedToday,      // Map field name
    todayMinutesStudied: response.minutesStudiedToday,    // Map field name
    cardGoalMet: response.cardGoalMet,
    minuteGoalMet: response.timeGoalMet                   // Map field name
  }
}

function mapDailyProgressPoint(response: DailyProgressPointResponse): DailyProgressPoint {
  return {
    date: response.date,
    cardsReviewed: response.cardsReviewed,
    accuracy: response.accuracy,
    studyMinutes: response.minutesStudied,                // Map field name
    goalMet: response.goalMet
  }
}

function mapProgressDashboard(response: ProgressDashboardResponse): ProgressDashboard {
  return {
    currentStreak: response.currentStreak,
    longestStreak: response.longestStreak,
    totalStudyDays: response.totalStudyDays,
    totalCardsReviewed: response.totalCardsReviewed,
    overallAccuracy: response.overallAccuracy,
    categoryAccuracy: response.categoryAccuracy,
    last30DaysActivity: response.last30DaysActivity.map(mapDailyProgressPoint),
    todayGoal: mapGoalProgress(response.todayGoal)
  }
}

export async function getDashboard(): Promise<ProgressDashboard> {
  const { data } = await apiClient.get<ProgressDashboardResponse>('/progress/dashboard')
  return mapProgressDashboard(data)
}

export async function getGoalProgress(): Promise<GoalProgress> {
  const { data } = await apiClient.get<GoalProgressResponse>('/progress/goal')
  return mapGoalProgress(data)
}

export async function updateGoal(req: UpdateGoalRequest): Promise<GoalProgress> {
  const { data } = await apiClient.put<GoalProgressResponse>('/progress/goal', req)
  return mapGoalProgress(data)
}
