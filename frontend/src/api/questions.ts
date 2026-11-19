import apiClient from './client'
import { fetchAllPages } from './pagination'
import type { QuestionRequest, WrittenQuestion } from '@/types'

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
