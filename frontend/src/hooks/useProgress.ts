import { useState, useEffect, useCallback } from 'react'
import type { ProgressDashboard, GoalProgress } from '@/types'
import * as progressApi from '@/api/progress'
import type { ApiError } from '@/types'

export function useProgress() {
  const [dashboard, setDashboard] = useState<ProgressDashboard | null>(null)
  const [loading, setLoading]     = useState(true)
  const [error, setError]         = useState<string | null>(null)

  const fetchDashboard = useCallback(async () => {
    setLoading(true)
    setError(null)
    try {
      const data = await progressApi.getDashboard()
      setDashboard(data)
    } catch (err) {
      const msg = (err as ApiError)?.detail ?? 'Failed to load progress data'
      setError(msg)
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => { fetchDashboard() }, [fetchDashboard])

  return { dashboard, loading, error, refetch: fetchDashboard }
}

export function useTodayProgress() {
  const [progress, setProgress] = useState<GoalProgress | null>(null)
  const [loading, setLoading]   = useState(true)
  const [error, setError]       = useState<string | null>(null)

  useEffect(() => {
    progressApi.getTodayProgress()
      .then(setProgress)
      .catch(err => setError((err as ApiError)?.detail ?? 'Failed to load today\'s progress'))
      .finally(() => setLoading(false))
  }, [])

  return { progress, loading, error }
}
