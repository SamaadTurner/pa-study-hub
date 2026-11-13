import { useState, useEffect, useRef } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { getExamSession, submitAnswer, completeExam, abandonExam } from '@/api/exam'
import type { ExamSession, ApiError } from '@/types'
import LoadingSpinner from '@/components/common/LoadingSpinner'
import ProgressBar from '@/components/common/ProgressBar'
import AnswerOptions from '@/components/exams/AnswerOptions'
import ExamTimer from '@/components/exams/ExamTimer'
import Button from '@/components/common/Button'
import { useNotification } from '@/contexts/NotificationContext'
import { getCategoryLabel } from '@/utils/categoryColors'

type PageState = 'loading' | 'ready' | 'finishing' | 'error'

// The exam works like the real thing: you answer the questions without being told
// whether you were right, and the answers and explanations come on the results page.
export default function ExamPage() {
  const { sessionId } = useParams<{ sessionId: string }>()
  const navigate      = useNavigate()
  const { notify }    = useNotification()

  const [state, setState]               = useState<PageState>('loading')
  const [session, setSession]           = useState<ExamSession | null>(null)
  const [currentIndex, setCurrentIndex] = useState(0)
  // The option picked for each question: questionId -> optionId
  const [picks, setPicks]               = useState<Record<string, string>>({})
  // Picks the server already has, so an unchanged answer is not sent again
  const [savedPicks, setSavedPicks]     = useState<Record<string, string>>({})
  const [saving, setSaving]             = useState(false)

  const questionStartRef = useRef<number>(Date.now())
  const timeUpHandledRef = useRef(false)

  // Load the exam once. The questions are kept in state after that.
  useEffect(() => {
    if (!sessionId) return

    getExamSession(sessionId)
      .then(data => {
        if (data.status === 'COMPLETED') {
          navigate(`/exam/${sessionId}/results`, { replace: true })
          return
        }
        if (data.status === 'ABANDONED') {
          navigate('/exam', { replace: true })
          return
        }

        // The time ran out while the page was closed: score it with what was answered
        if (data.timeRemainingSeconds === 0) {
          completeExam(sessionId)
            .catch(() => { /* the results page shows the error if this failed */ })
            .finally(() => navigate(`/exam/${sessionId}/results`, { replace: true }))
          return
        }

        const alreadyPicked: Record<string, string> = {}
        for (const question of data.questions) {
          if (question.selectedOptionId) {
            alreadyPicked[question.id] = question.selectedOptionId
          }
        }

        // Coming back to an exam: continue at the first question without an answer
        const firstUnanswered = data.questions.findIndex(q => !q.selectedOptionId)

        setSession(data)
        setPicks(alreadyPicked)
        setSavedPicks(alreadyPicked)
        setCurrentIndex(firstUnanswered === -1 ? data.questions.length - 1 : firstUnanswered)
        setState('ready')
      })
      .catch(() => setState('error'))
  }, [sessionId, navigate])

  // Restart the per-question clock whenever a different question is shown
  useEffect(() => {
    questionStartRef.current = Date.now()
  }, [currentIndex])

  // Sends the pick for the current question, unless the server already has it.
  // Returns false if saving failed, so the caller can stay on the question.
  async function saveCurrentAnswer(): Promise<boolean> {
    if (!session || !sessionId) return false
    const question = session.questions[currentIndex]
    const picked = picks[question.id]

    if (!picked || picked === savedPicks[question.id]) {
      return true
    }

    setSaving(true)
    try {
      await submitAnswer(sessionId, question.id, {
        selectedOptionId: picked,
        timeSpentSeconds: Math.round((Date.now() - questionStartRef.current) / 1000),
      })
      setSavedPicks(prev => ({ ...prev, [question.id]: picked }))
      return true
    } catch (err) {
      notify('error', (err as ApiError)?.detail ?? 'Failed to save your answer')
      return false
    } finally {
      setSaving(false)
    }
  }

  async function goToQuestion(index: number) {
    const saved = await saveCurrentAnswer()
    if (saved) setCurrentIndex(index)
  }

  async function finishExam() {
    if (!sessionId) return
    setState('finishing')
    try {
      await completeExam(sessionId)
      navigate(`/exam/${sessionId}/results`, { replace: true })
    } catch (err) {
      notify('error', (err as ApiError)?.detail ?? 'Failed to finish the exam')
      setState('ready')
    }
  }

  async function handleFinish() {
    if (!session) return
    const saved = await saveCurrentAnswer()
    if (!saved) return

    const unanswered = session.questions.filter(q => !picks[q.id]).length
    if (unanswered > 0) {
      const ok = window.confirm(
        `You have ${unanswered} unanswered ${unanswered === 1 ? 'question' : 'questions'}. ` +
        'Unanswered questions count as wrong. Finish anyway?'
      )
      if (!ok) return
    }
    await finishExam()
  }

  async function handleTimeUp() {
    // The timer can fire more than once, only finish the exam the first time
    if (timeUpHandledRef.current) return
    timeUpHandledRef.current = true

    notify('warning', "Time's up! Submitting your exam...")
    await saveCurrentAnswer()
    await finishExam()
  }

  async function handleExit() {
    if (!sessionId) return
    const ok = window.confirm('Leave this exam? It will be abandoned and will not be scored.')
    if (!ok) return

    try {
      await abandonExam(sessionId)
    } catch {
      // Leaving anyway. The exam just stays in the history as in progress.
    }
    navigate('/exam')
  }

  if (state === 'loading' || state === 'finishing') {
    return (
      <div className="flex items-center justify-center py-24">
        <LoadingSpinner message={state === 'finishing' ? 'Submitting exam...' : 'Loading exam...'} size="lg" />
      </div>
    )
  }

  if (state === 'error' || !session || session.questions.length === 0) {
    return (
      <div className="max-w-xl mx-auto text-center py-16">
        <p className="text-5xl mb-4">⚠️</p>
        <h2 className="text-xl font-bold text-gray-900 mb-2">This exam could not be loaded</h2>
        <Button onClick={() => navigate('/exam')}>Back to Exam Setup</Button>
      </div>
    )
  }

  const question       = session.questions[currentIndex]
  const totalQuestions = session.questions.length
  const answeredCount  = session.questions.filter(q => picks[q.id]).length
  const isLastQuestion = currentIndex === totalQuestions - 1
  const progressPct    = Math.round((answeredCount / totalQuestions) * 100)

  return (
    <div className="max-w-2xl mx-auto space-y-6">
      {/* Top bar */}
      <div className="flex items-center justify-between gap-4">
        <div className="flex items-center gap-3">
          <span className="text-sm font-medium text-gray-700">
            Question <span className="text-gray-900 font-bold">{currentIndex + 1}</span>
            <span className="text-gray-400"> / {totalQuestions}</span>
          </span>
          <span className="text-xs bg-gray-100 text-gray-600 px-2 py-0.5 rounded-full">
            {getCategoryLabel(question.category)}
          </span>
        </div>
        <div className="flex items-center gap-2">
          {session.timeRemainingSeconds !== null && (
            <ExamTimer
              initialSeconds={session.timeRemainingSeconds}
              onExpire={handleTimeUp}
            />
          )}
          <button
            onClick={handleExit}
            className="text-xs text-gray-400 hover:text-gray-600 transition-colors px-2 py-1"
          >
            Exit
          </button>
        </div>
      </div>

      <div>
        <ProgressBar value={progressPct} size="sm" color="primary" />
        <p className="text-xs text-gray-400 mt-1">
          {answeredCount} of {totalQuestions} answered
        </p>
      </div>

      {/* Question */}
      <div className="bg-white border border-gray-200 rounded-xl p-6 space-y-4">
        {question.clinicalVignette && (
          <div className="text-sm text-gray-700 leading-relaxed bg-blue-50 border-l-4 border-blue-400 p-4 rounded">
            <p className="font-semibold text-blue-900 mb-2">Clinical Vignette:</p>
            <p>{question.clinicalVignette}</p>
          </div>
        )}
        <p className="text-gray-900 text-base leading-relaxed font-medium">
          {question.stem}
        </p>
      </div>

      {/* Answer options. correctId stays null, the right answer is only shown in the results. */}
      <AnswerOptions
        options={question.answerOptions}
        selectedId={picks[question.id] ?? null}
        correctId={null}
        onSelect={optionId => setPicks(prev => ({ ...prev, [question.id]: optionId }))}
        disabled={saving}
      />

      {/* Action buttons */}
      <div className="flex items-center justify-between">
        <Button
          variant="secondary"
          onClick={() => goToQuestion(currentIndex - 1)}
          disabled={currentIndex === 0 || saving}
        >
          ← Previous
        </Button>
        {isLastQuestion ? (
          <Button onClick={handleFinish} loading={saving}>
            Finish Exam
          </Button>
        ) : (
          <Button onClick={() => goToQuestion(currentIndex + 1)} loading={saving}>
            {picks[question.id] ? 'Next Question →' : 'Skip →'}
          </Button>
        )}
      </div>
    </div>
  )
}
