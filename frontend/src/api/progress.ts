import apiClient from './client'
import type { ProgressDashboard, GoalProgress, UpdateGoalRequest } from '@/types'

export async function getDashboard(): Promise<ProgressDashboard> {
  const { data } = await apiClient.get<ProgressDashboard>('/progress/dashboard')
  return data
}

export async function getGoalProgress(): Promise<GoalProgress> {
  const { data } = await apiClient.get<GoalProgress>('/progress/goal')
  return data
}

export async function updateGoal(req: UpdateGoalRequest): Promise<GoalProgress> {
  const { data } = await apiClient.put<GoalProgress>('/progress/goal', req)
  return data
}
