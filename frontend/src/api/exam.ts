import apiClient from './client'
import type {
  ExamSession,
  ExamResult,
  ExamHistorySummary,
  StartExamRequest,
} from '@/types'

// The types in '@/types' use the same field names as the backend's JSON, so the
// responses are returned as they are. This file used to map them to a second set
// of names that the backend never sent, which is why the exam pages showed nothing.

interface SubmitAnswerRequest {
  selectedOptionId: string
  timeSpentSeconds: number
}

export async function startExam(req: StartExamRequest): Promise<ExamSession> {
  const { data } = await apiClient.post<ExamSession>('/exams', req)
  return data
}

export async function getExamSession(sessionId: string): Promise<ExamSession> {
  const { data } = await apiClient.get<ExamSession>(`/exams/${sessionId}`)
  return data
}

export async function submitAnswer(
  sessionId: string,
  questionId: string,
  req: SubmitAnswerRequest
): Promise<ExamSession> {
  const { data } = await apiClient.post<ExamSession>(
    `/exams/${sessionId}/questions/${questionId}/answer`,
    req
  )
  return data
}

export async function completeExam(sessionId: string): Promise<ExamResult> {
  const { data } = await apiClient.post<ExamResult>(`/exams/${sessionId}/complete`)
  return data
}

export async function getExamResult(sessionId: string): Promise<ExamResult> {
  const { data } = await apiClient.get<ExamResult>(`/exams/${sessionId}/result`)
  return data
}

export async function abandonExam(sessionId: string): Promise<void> {
  await apiClient.post(`/exams/${sessionId}/abandon`)
}

export async function getExamHistory(page = 0, size = 20): Promise<ExamHistorySummary[]> {
  const { data } = await apiClient.get<{ content: ExamHistorySummary[] }>(
    `/exams/history?page=${page}&size=${size}`
  )
  return data.content
}
