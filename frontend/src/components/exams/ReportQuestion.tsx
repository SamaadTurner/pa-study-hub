import { useState, FormEvent } from 'react'
import { reportQuestion } from '@/api/questions'
import Button from '@/components/common/Button'
import type { ApiError, ReportReason } from '@/types'

const REASONS: { value: ReportReason; label: string }[] = [
  { value: 'WRONG_ANSWER', label: 'The marked answer is wrong' },
  { value: 'UNCLEAR', label: 'The question is unclear' },
  { value: 'OTHER', label: 'Something else' },
]

interface Props {
  questionId: string
  alreadyReported: boolean
}

/**
 * "Report this question" under a question another user shared. With 3 reports the
 * question is left out of other people's exams, and its author sees the reports.
 */
export default function ReportQuestion({ questionId, alreadyReported }: Props) {
  const [open, setOpen]         = useState(false)
  const [reason, setReason]     = useState<ReportReason>('WRONG_ANSWER')
  const [note, setNote]         = useState('')
  const [sending, setSending]   = useState(false)
  const [reported, setReported] = useState(alreadyReported)
  const [error, setError]       = useState<string | null>(null)

  if (reported) {
    return <p className="text-xs text-gray-500">You reported this question. Thank you.</p>
  }

  if (!open) {
    return (
      <button type="button" onClick={() => setOpen(true)} className="text-xs text-gray-500 hover:text-red-600 hover:underline">
        Report this question
      </button>
    )
  }

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    setError(null)
    setSending(true)
    try {
      await reportQuestion(questionId, reason, note)
      setReported(true)
    } catch (err) {
      const apiError = err as ApiError
      // Reported in another tab already: same result for the user
      if (apiError?.status === 409 && apiError.detail === 'You already reported this question') {
        setReported(true)
      } else {
        setError(apiError?.detail ?? 'Could not send the report')
      }
    } finally {
      setSending(false)
    }
  }

  return (
    <form onSubmit={handleSubmit} className="border border-gray-200 rounded-lg p-4 space-y-3" aria-label="Report this question">
      <fieldset className="space-y-1">
        <legend className="text-sm font-medium text-gray-700 mb-1">What is wrong with it?</legend>
        {REASONS.map(option => (
          <label key={option.value} className="flex items-center gap-2 text-sm text-gray-700 cursor-pointer">
            <input type="radio" name={`reason-${questionId}`} checked={reason === option.value}
              onChange={() => setReason(option.value)} className="border-gray-300 text-primary-600" />
            {option.label}
          </label>
        ))}
      </fieldset>
      <div>
        <label htmlFor={`note-${questionId}`} className="block text-sm font-medium text-gray-700 mb-1">
          Note for the author <span className="text-gray-500 font-normal">(optional)</span>
        </label>
        <textarea id={`note-${questionId}`} rows={2} maxLength={300} value={note} onChange={e => setNote(e.target.value)}
          className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-primary-500" />
      </div>
      {error && <p className="text-sm text-red-600" role="alert">{error}</p>}
      <div className="flex gap-2">
        <Button type="submit" size="sm" loading={sending}>Send report</Button>
        <Button type="button" size="sm" variant="secondary" onClick={() => setOpen(false)}>Cancel</Button>
      </div>
    </form>
  )
}
