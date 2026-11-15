import { useState } from 'react'
import Button from '@/components/common/Button'
import { explainAnswer } from '@/api/ai'
import type { ApiError } from '@/types'

interface Props {
  sessionId: string
  questionId: string
  wasCorrect: boolean
  // Called after a request, so the page can update how many requests are left
  onUsedRequest: () => void
}

// The "Explain" button under one question on the results page, and the answer
// once it arrives. Each question asks for its own explanation, only when clicked,
// because every request counts against the daily limit.
export default function ExplainAnswer({ sessionId, questionId, wasCorrect, onUsedRequest }: Props) {
  const [explanation, setExplanation] = useState<string | null>(null)
  const [loading, setLoading]         = useState(false)
  const [error, setError]             = useState<string | null>(null)

  async function handleClick() {
    setLoading(true)
    setError(null)
    try {
      setExplanation(await explainAnswer(sessionId, questionId))
    } catch (err) {
      setError((err as ApiError)?.detail ?? 'Could not get an explanation. Please try again.')
    } finally {
      setLoading(false)
      onUsedRequest()
    }
  }

  if (explanation) {
    return (
      <div className="bg-violet-50 border border-violet-200 rounded-lg p-4">
        <p className="text-xs font-semibold text-violet-700 uppercase tracking-wide mb-1">AI explanation</p>
        <p className="text-sm text-gray-800 leading-relaxed whitespace-pre-wrap">{explanation}</p>
        <p className="text-xs text-gray-500 mt-2">
          Written by an AI model from the explanation above. It can be wrong.
        </p>
      </div>
    )
  }

  return (
    <div className="space-y-2">
      <Button variant="secondary" size="sm" onClick={handleClick} loading={loading}>
        {wasCorrect ? 'Explain this question' : 'Explain my mistake'}
      </Button>
      {error && (
        <p role="alert" className="text-sm text-red-700">{error}</p>
      )}
    </div>
  )
}
