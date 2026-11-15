import apiClient from './client'

// The optional AI features. The backend turns them off when it has no API key,
// and the status call says whether they are on.

export interface AiStatus {
  enabled: boolean
  model: string | null
  remainingToday: number | null
}

export interface SuggestedCard {
  front: string
  back: string
}

export async function getAiStatus(): Promise<AiStatus> {
  const { data } = await apiClient.get<AiStatus>('/ai/status')
  return data
}

// Suggests cards from notes. Nothing is saved: the user picks which ones to add.
export async function suggestFlashcards(notes: string, count: number): Promise<SuggestedCard[]> {
  const { data } = await apiClient.post<{ cards: SuggestedCard[] }>('/ai/flashcards', { notes, count })
  return data.cards
}

export async function explainAnswer(sessionId: string, questionId: string): Promise<string> {
  const { data } = await apiClient.post<{ explanation: string }>(
    `/ai/exams/${sessionId}/questions/${questionId}/explanation`
  )
  return data.explanation
}
