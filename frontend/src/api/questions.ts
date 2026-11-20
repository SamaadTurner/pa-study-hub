import apiClient from './client'
import { fetchAllPages } from './pagination'
import type {
  PaginatedResponse,
  QuestionCounts,
  QuestionRequest,
  ReportReason,
  SharedQuestion,
  WrittenQuestion,
} from '@/types'

// Practice exam questions the user wrote. Only the author can load one,
// because the response includes the right answer.

export function getMyQuestions(): Promise<WrittenQuestion[]> {
  return fetchAllPages<WrittenQuestion>('/questions/mine')
}

export async function getMyQuestion(questionId: string): Promise<WrittenQuestion> {
  const { data } = await apiClient.get<WrittenQuestion>(`/questions/${questionId}`)
  return data
}

export async function createQuestion(req: QuestionRequest): Promise<WrittenQuestion> {
  const { data } = await apiClient.post<WrittenQuestion>('/questions', req)
  return data
}

// A question that was already used in an exam comes back with a new id
export async function updateQuestion(questionId: string, req: QuestionRequest): Promise<WrittenQuestion> {
  const { data } = await apiClient.put<WrittenQuestion>(`/questions/${questionId}`, req)
  return data
}

export async function deleteQuestion(questionId: string): Promise<void> {
  await apiClient.delete(`/questions/${questionId}`)
}

// Reports a question another user shared. Allowed once, after it was in one of your finished exams.
export async function reportQuestion(questionId: string, reason: ReportReason, note: string): Promise<void> {
  await apiClient.post(`/questions/${questionId}/reports`, { reason, note: note.trim() || null })
}

// One page of the questions other users shared, newest first
export async function getSharedQuestions(
  category: string | null,
  search: string,
  page: number,
): Promise<PaginatedResponse<SharedQuestion>> {
  const { data } = await apiClient.get<PaginatedResponse<SharedQuestion>>('/questions/shared', {
    params: { category: category ?? undefined, search: search.trim() || undefined, page, size: 20 },
  })
  return data
}

// How many questions an exam can pick from, per category
export async function getQuestionCounts(includeShared: boolean): Promise<QuestionCounts> {
  const { data } = await apiClient.get<QuestionCounts>('/questions/counts', { params: { includeShared } })
  return data
}
