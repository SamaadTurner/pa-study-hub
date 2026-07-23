import apiClient from './client'
import type {
  GenerateFlashcardsRequest,
  GenerateFlashcardsResponse,
  ChatRequest,
  ChatResponse,
  StudyPlanRequest,
  StudyPlanResponse,
} from '@/types'

interface ExplainRequest {
  questionText: string
  selectedAnswer: string
  correctAnswer: string
  explanation: string
  category: string
}

interface ExplainResponse {
  explanation: string
}

export async function generateFlashcards(
  req: GenerateFlashcardsRequest
): Promise<GenerateFlashcardsResponse> {
  const { data } = await apiClient.post<GenerateFlashcardsResponse>('/ai/flashcards', req)
  return data
}

export async function explainAnswer(req: ExplainRequest): Promise<ExplainResponse> {
  const { data } = await apiClient.post<ExplainResponse>('/ai/explain', req)
  return data
}

export async function getStudyPlan(req: StudyPlanRequest): Promise<StudyPlanResponse> {
  const { data } = await apiClient.post<StudyPlanResponse>('/ai/study-plan', req)
  return data
}

export async function sendChatMessage(req: ChatRequest): Promise<ChatResponse> {
  const { data } = await apiClient.post<ChatResponse>('/ai/chat', req)
  return data
}
