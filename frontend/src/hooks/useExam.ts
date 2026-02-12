import { useState, useCallback } from 'react'
import type { ExamSession, StartExamRequest } from '@/types'
import * as examApi from '@/api/exam'
import { useNotification } from '@/contexts/NotificationContext'
import type { ApiError } from '@/types'

export function useExam() {
  const [session, setSession] = useState<ExamSession | null>(null)
  const [loading, setLoading] = useState(false)
  const [error, setError]     = useState<string | null>(null)
  const { notify }            = useNotification()

  const startExam = useCallback(
    async (req: StartExamRequest): Promise<ExamSession | null> => {
      setLoading(true)
      setError(null)
      try {
        const data = await examApi.startExam(req)
        setSession(data)
        return data
      } catch (err) {
        const msg = (err as ApiError)?.detail ?? 'Failed to start exam'
        setError(msg)
        notify('error', msg)
        return null
      } finally {
        setLoading(false)
      }
    },
    [notify]
  )

  const completeExam = useCallback(
    async (sessionId: string) => {
      try {
        await examApi.completeExam(sessionId)
      } catch (err) {
        notify('error', (err as ApiError)?.detail ?? 'Failed to complete exam')
      }
    },
    [notify]
  )

  return { session, loading, error, startExam, completeExam }
}
