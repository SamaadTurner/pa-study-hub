import { useState, useEffect, useCallback } from 'react'
import { useNavigate } from 'react-router-dom'
import { deleteQuestion, getMyQuestions } from '@/api/questions'
import Badge from '@/components/common/Badge'
import Button from '@/components/common/Button'
import EmptyState from '@/components/common/EmptyState'
import LoadingSpinner from '@/components/common/LoadingSpinner'
import { ConfirmModal } from '@/components/common/Modal'
import { useNotification } from '@/contexts/NotificationContext'
import { getCategoryBadgeVariant, getCategoryLabel } from '@/utils/categoryColors'
import { getTaskAreaLabel } from '@/utils/taskAreas'
import type { ApiError, ReportReason, WrittenQuestion } from '@/types'

const REPORT_REASON_LABELS: Record<ReportReason, string> = {
  WRONG_ANSWER: 'Marked answer is wrong',
  UNCLEAR: 'Unclear',
  OTHER: 'Other',
}

function shorten(text: string, length: number): string {
  return text.length > length ? `${text.slice(0, length)}…` : text
}

export default function MyQuestionsPage() {
  const navigate   = useNavigate()
  const { notify } = useNotification()

  const [questions, setQuestions]       = useState<WrittenQuestion[]>([])
  const [loading, setLoading]           = useState(true)
  const [error, setError]               = useState<string | null>(null)
  const [deleteTarget, setDeleteTarget] = useState<WrittenQuestion | null>(null)
  const [deleting, setDeleting]         = useState(false)

  const load = useCallback(() => {
    setLoading(true)
    setError(null)
    getMyQuestions()
      .then(setQuestions)
      .catch((err: ApiError) => setError(err?.detail ?? 'Could not load your questions'))
      .finally(() => setLoading(false))
  }, [])

  useEffect(() => { load() }, [load])

  async function handleDelete() {
    if (!deleteTarget) return
    setDeleting(true)
    try {
      await deleteQuestion(deleteTarget.id)
      setQuestions(current => current.filter(q => q.id !== deleteTarget.id))
      setDeleteTarget(null)
      notify('success', 'Question deleted')
    } catch (err) {
      notify('error', (err as ApiError)?.detail ?? 'Could not delete the question')
    } finally {
      setDeleting(false)
    }
  }

  if (loading) {
    return (
      <div className="flex items-center justify-center py-24">
        <LoadingSpinner message="Loading your questions..." size="lg" />
      </div>
    )
  }

  if (error) {
    return (
      <div className="max-w-xl mx-auto text-center py-16">
        <h1 className="text-xl font-bold text-gray-900 mb-4">{error}</h1>
        <Button onClick={load}>Try Again</Button>
      </div>
    )
  }

  const sharedCount = questions.filter(q => q.isPublic).length

  return (
    <div className="max-w-3xl mx-auto space-y-6">
      <div>
        <button onClick={() => navigate('/exam')} className="text-sm text-primary-600 hover:underline mb-2">
          ← Back to Exam Setup
        </button>
        <div className="flex items-center justify-between gap-4">
          <div>
            <h1 className="text-2xl font-bold text-gray-900">My Questions</h1>
            <p className="text-sm text-gray-500 mt-0.5">
              {questions.length} {questions.length === 1 ? 'question' : 'questions'}, {sharedCount} shared.
              Your questions are part of every exam you start.
            </p>
          </div>
          <div className="flex gap-2">
            <Button variant="secondary" onClick={() => navigate('/exam/shared')}>Browse shared</Button>
            <Button onClick={() => navigate('/exam/questions/new')}>+ Write Question</Button>
          </div>
        </div>
      </div>

      {questions.length === 0 ? (
        <div className="bg-white border border-gray-200 rounded-xl">
          <EmptyState
            icon="✍️"
            title="No questions yet"
            message="Write questions from your lectures and they show up in your practice exams next to the starter questions."
            action={{ label: 'Write Your First Question', onClick: () => navigate('/exam/questions/new') }}
          />
        </div>
      ) : (
        <ul className="space-y-3">
          {questions.map(question => (
            <li key={question.id} className="bg-white border border-gray-200 rounded-xl p-5">
              <div className="flex flex-wrap items-center gap-2 mb-2">
                <Badge variant={getCategoryBadgeVariant(question.category)}>
                  {getCategoryLabel(question.category)}
                </Badge>
                <span className="text-xs text-gray-500">{getTaskAreaLabel(question.taskArea)}</span>
                {question.isPublic && <Badge variant="teal">Shared</Badge>}
                {question.reports.length > 0 && (
                  <Badge variant="red">
                    {question.reports.length} {question.reports.length === 1 ? 'report' : 'reports'}
                  </Badge>
                )}
              </div>
              <p className="text-sm font-medium text-gray-900 leading-relaxed">{shorten(question.stem, 200)}</p>
              <p className="text-xs text-gray-500 mt-1">
                Right answer: {shorten(question.answerOptions.find(o => o.isCorrect)?.text ?? '', 80)}
              </p>
              {(question.reports.length > 0 || question.earlierReportCount > 0) && (
                <div className="mt-3 bg-red-50 border border-red-100 rounded-lg p-3 text-xs text-gray-700 space-y-1">
                  {question.hiddenFromShared && (
                    <p className="font-medium text-red-700">
                      Left out of other people's exams because of the reports. Fixing it saves a new
                      version that comes back, but the reports still count: one more report hides it again.
                    </p>
                  )}
                  {question.earlierReportCount > 0 && (
                    <p>
                      {question.earlierReportCount} {question.earlierReportCount === 1 ? 'report' : 'reports'} on
                      earlier versions, still counted.
                    </p>
                  )}
                  <ul className="space-y-1">
                    {question.reports.map((report, index) => (
                      <li key={index}>
                        <span className="font-medium">{REPORT_REASON_LABELS[report.reason]}</span>
                        {report.note && <span>: "{report.note}"</span>}
                      </li>
                    ))}
                  </ul>
                </div>
              )}
              <div className="flex gap-2 mt-3">
                <Button
                  variant="secondary"
                  size="sm"
                  onClick={() => navigate(`/exam/questions/${question.id}/edit`)}
                  aria-label={`Edit question: ${shorten(question.stem, 40)}`}
                >
                  Edit
                </Button>
                <Button
                  variant="ghost"
                  size="sm"
                  onClick={() => setDeleteTarget(question)}
                  aria-label={`Delete question: ${shorten(question.stem, 40)}`}
                >
                  Delete
                </Button>
              </div>
            </li>
          ))}
        </ul>
      )}

      <ConfirmModal
        isOpen={!!deleteTarget}
        onClose={() => setDeleteTarget(null)}
        onConfirm={handleDelete}
        title="Delete Question"
        message={`Delete this question? "${shorten(deleteTarget?.stem ?? '', 60)}" Past exam results keep it.`}
        confirmLabel="Delete"
        confirmVariant="danger"
        loading={deleting}
      />
    </div>
  )
}
