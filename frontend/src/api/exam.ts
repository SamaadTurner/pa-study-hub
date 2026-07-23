import apiClient from './client'
import type {
  ExamSession,
  ExamResult,
  ExamHistorySummary,
  StartExamRequest,
} from '@/types'

interface SubmitAnswerRequest {
  selectedOptionId: string
  timeSpentSeconds: number
}

// Response from backend when starting/getting exam session
interface ExamSessionResponse {
  id: string
  categoryFilter: string | null
  questionCount: number
  answeredCount: number
  timeLimit: number | null
  status: 'IN_PROGRESS' | 'COMPLETED' | 'ABANDONED'
  startedAt: string
  questions: Array<{
    id: string
    clinicalVignette: string
    questionText: string
    options: Array<{ id: string; text: string }>
    category: string
    correctOptionId?: string
    userAnswer?: { selectedOptionId: string; isCorrect: boolean }
  }>
}

// Response from backend when getting exam results
interface ExamResultResponse {
  sessionId: string
  score: number
  percentageScore: number
  totalQuestions: number
  correctCount: number
  categoryBreakdown: Record<string, number>
  completedAt: string
  timeTaken: number
  performanceMessage?: string
}

export async function startExam(req: StartExamRequest): Promise<ExamSession> {
  const { data } = await apiClient.post<ExamSessionResponse>('/exams', req)
  return mapExamSessionResponse(data)
}

export async function getExamSession(sessionId: string): Promise<ExamSession> {
  const { data } = await apiClient.get<ExamSessionResponse>(`/exams/${sessionId}`)
  return mapExamSessionResponse(data)
}

export async function submitAnswer(
  sessionId: string,
  questionId: string,
  req: SubmitAnswerRequest
): Promise<ExamSession> {
  const { data } = await apiClient.post<ExamSessionResponse>(
    `/exams/${sessionId}/questions/${questionId}/answer`,
    req
  )
  return mapExamSessionResponse(data)
}

export async function completeExam(sessionId: string): Promise<ExamResult> {
  const { data } = await apiClient.post<ExamResultResponse>(`/exams/${sessionId}/complete`)
  return mapExamResultResponse(data)
}

export async function getExamResult(sessionId: string): Promise<ExamResult> {
  const { data } = await apiClient.get<ExamResultResponse>(`/exams/${sessionId}/result`)
  return mapExamResultResponse(data)
}

export async function abandonExam(sessionId: string): Promise<void> {
  await apiClient.post(`/exams/${sessionId}/abandon`)
}

export async function getExamHistory(page = 0, size = 20): Promise<ExamHistorySummary[]> {
  const { data } = await apiClient.get<{ content: ExamHistorySummary[] }>(
    `/exams/history?page=${page}&size=${size}`
  )
  return data.content || data
}

// Helper function to map backend response to frontend type
function mapExamSessionResponse(response: ExamSessionResponse): ExamSession {
  return {
    id: response.id,
    categoryFilter: response.categoryFilter,
    questionCount: response.questionCount,
    answeredCount: response.answeredCount,
    timeLimit: response.timeLimit,
    status: response.status,
    startedAt: response.startedAt,
    questions: response.questions.map(q => ({
      id: q.id,
      clinicalVignette: q.clinicalVignette,
      questionText: q.questionText,
      options: q.options,
      category: q.category,
      correctOptionId: q.correctOptionId,
      userAnswer: q.userAnswer,
    })),
  }
}

// Helper function to map backend result response to frontend type
function mapExamResultResponse(response: ExamResultResponse): ExamResult {
  return {
    sessionId: response.sessionId,
    score: response.score,
    percentageScore: response.percentageScore,
    totalQuestions: response.totalQuestions,
    correctCount: response.correctCount,
    incorrectCount: response.totalQuestions - response.correctCount,
    categoryBreakdown: response.categoryBreakdown,
    completedAt: response.completedAt,
    timeTaken: response.timeTaken,
    performanceMessage: response.performanceMessage || '',
  }
}
