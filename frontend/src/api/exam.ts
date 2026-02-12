import apiClient from './client'
import type {
  ExamSession,
  ExamQuestion,
  ExamResult,
  ExamHistorySummary,
  StartExamRequest,
} from '@/types'

interface AnswerRequest {
  questionId: string
  selectedOptionId: string
  timeSpentSeconds: number
}

interface AnswerResponse {
  isCorrect: boolean
  correctOptionId: string
  explanation: string
  runningScore: { correct: number; total: number }
}

interface NextQuestionResponse {
  question: ExamQuestion
  questionNumber: number
  totalQuestions: number
  timeRemainingSeconds: number | null
}

export async function startExam(req: StartExamRequest): Promise<ExamSession> {
  const { data } = await apiClient.post<ExamSession>('/exams/start', req)
  return data
}

export async function getNextQuestion(sessionId: string): Promise<NextQuestionResponse> {
  const { data } = await apiClient.get<NextQuestionResponse>(`/exams/${sessionId}/next`)
  return data
}

export async function submitAnswer(sessionId: string, req: AnswerRequest): Promise<AnswerResponse> {
  const { data } = await apiClient.post<AnswerResponse>(`/exams/${sessionId}/answer`, req)
  return data
}

export async function completeExam(sessionId: string): Promise<void> {
  await apiClient.post(`/exams/${sessionId}/complete`)
}

export async function getExamResults(sessionId: string): Promise<ExamResult> {
  const { data } = await apiClient.get<ExamResult>(`/exams/${sessionId}/results`)
  return data
}

export async function getExamHistory(): Promise<ExamHistorySummary[]> {
  const { data } = await apiClient.get<ExamHistorySummary[]>('/exams/history')
  return data
}

export async function getExamStats(): Promise<Record<string, unknown>> {
  const { data } = await apiClient.get('/exams/stats')
  return data
}
