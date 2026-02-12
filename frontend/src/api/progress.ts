import apiClient from './client'
import type {
  ProgressDashboard,
  GoalProgress,
  UpdateGoalRequest,
  DailyProgressPoint,
} from '@/types'

interface StreakInfo {
  currentStreak: number
  longestStreak: number
  totalStudyDays: number
  lastStudyDate: string
}

interface CalendarDay {
  date: string
  minutesStudied: number
  goalMet: boolean
}

export async function getDashboard(): Promise<ProgressDashboard> {
  const { data } = await apiClient.get<ProgressDashboard>('/progress/dashboard')
  return data
}

export async function getStreak(): Promise<StreakInfo> {
  const { data } = await apiClient.get<StreakInfo>('/progress/streak')
  return data
}

export async function getStreakCalendar(): Promise<CalendarDay[]> {
  const { data } = await apiClient.get<CalendarDay[]>('/progress/streak/calendar')
  return data
}

export async function getGoals(): Promise<GoalProgress> {
  const { data } = await apiClient.get<GoalProgress>('/progress/goals')
  return data
}

export async function updateGoals(req: UpdateGoalRequest): Promise<GoalProgress> {
  const { data } = await apiClient.put<GoalProgress>('/progress/goals', req)
  return data
}

export async function getTodayProgress(): Promise<GoalProgress> {
  const { data } = await apiClient.get<GoalProgress>('/progress/goals/today')
  return data
}

export async function getAnalytics(): Promise<ProgressDashboard> {
  const { data } = await apiClient.get<ProgressDashboard>('/progress/analytics')
  return data
}

export async function getActivityHistory(days = 30): Promise<DailyProgressPoint[]> {
  const { data } = await apiClient.get<DailyProgressPoint[]>(`/progress/activity?days=${days}`)
  return data
}
