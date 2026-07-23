import { useState, useEffect, useRef, useCallback } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { getExamSession, submitAnswer, completeExam } from '@/api/exam'
import type { ExamQuestion } from '@/types'
import LoadingSpinner from '@/components/common/LoadingSpinner'
import ProgressBar from '@/components/common/ProgressBar'
import AnswerOptions from '@/components/exams/AnswerOptions'
import ExamTimer from '@/components/exams/ExamTimer'
import Button from '@/components/common/Button'
import { useNotification } from '@/contexts/NotificationContext'
import { getCategoryLabel } from '@/utils/categoryColors'
import type { ApiError } from '@/types'

interface AnswerFeedback {
  isCorrect: boolean
  correctOptionId: string
  explanation: string
  runningScore: { correct: number; total: number }
}

type ExamState = 'loading' | 'question' | 'answered' | 'submitting_next' | 'done' | 'error'

export default function ExamPage() {
  const { sessionId } = useParams<{ sessionId: string }>()
  const navigate      = useNavigate()
  const { notify }    = useNotification()

  const [state, setState]             = useState<ExamState>('loading')
  const [question, setQuestion]       = useState<ExamQuestion | null>(null)
  const [questionNumber, setQNumber]  = useState(1)
  const [totalQuestions, setTotal]    = useState(0)
  const [timeRemaining, setTimeLeft]  = useState<number | null>(null)
  const [selectedId, setSelectedId]   = useState<string | null>(null)
  const [feedback, setFeedback]       = useState<AnswerFeedback | null>(null)
  const [submitting, setSubmitting]   = useState(false)
  const [timerExpired, setTimerExpired] = useState(false)

  const questionStartRef = useRef<number>(Date.now())

  const loadNextQuestion = useCallback(async () => {
    if (!sessionId) return
    setState('loading')
    setSelectedId(null)
    setFeedback(null)
    try {
      const session = await getExamSession(sessionId)
      // Find next unanswered question
      const nextQ = session.questions.find(q => !q.userAnswer)
      if (!nextQ) {
        // All questions answered, go to results
        navigate(`/exam/${sessionId}/results`, { replace: true })
        return
      }
      setQuestion(nextQ)
      const answered = session.questions.filter(q => q.userAnswer).length
      setQNumber(answered + 1)
      setTotal(session.questionCount)
      setTimeLeft(session.timeLimit ? session.timeLimit * 60 : null)
      questionStartRef.current = Date.now()
      setState('question')
    } catch (err) {
      const apiErr = err as ApiError
      // If no more questions, navigate to results
      if (apiErr?.status === 404 || apiErr?.detail?.includes('complete')) {
        navigate(`/exam/${sessionId}/results`, { replace: true })
      } else {
        setState('error')
      }
    }
  }, [sessionId, navigate])

  useEffect(() => { loadNextQuestion() }, [loadNextQuestion])

  async function handleSubmitAnswer() {
    if (!selectedId || !question || !sessionId || submitting) return
    setSubmitting(true)
    const timeSpent = Math.round((Date.now() - questionStartRef.current) / 1000)
    try {
      const updatedSession = await submitAnswer(sessionId, question.id, {
        selectedOptionId: selectedId,
        timeSpentSeconds: timeSpent,
      })
      // Extract feedback from the updated question
      const answeredQ = updatedSession.questions.find(q => q.id === question.id)
      if (answeredQ?.userAnswer && answeredQ.correctOptionId) {
        setFeedback({
          isCorrect: answeredQ.userAnswer.isCorrect,
          correctOptionId: answeredQ.correctOptionId,
          explanation: '', // Backend doesn't return this in answer response
          runningScore: {
            correct: updatedSession.questions.filter(q => q.userAnswer?.isCorrect).length,
            total: updatedSession.answeredCount,
          },
        })
      }
      setState('answered')
    } catch (err) {
      notify('error', (err as ApiError)?.detail ?? 'Failed to submit answer')
    } finally {
      setSubmitting(false)
    }
  }

  async function handleNext() {
    if (!sessionId) return
    // Check if this was the last question
    if (questionNumber >= totalQuestions) {
      setState('submitting_next')
      try {
        await completeExam(sessionId)
      } catch { /* already completed is fine */ }
      navigate(`/exam/${sessionId}/results`, { replace: true })
    } else {
      loadNextQuestion()
    }
  }

  async function handleTimerExpire() {
    if (!sessionId || timerExpired) return
    setTimerExpired(true)
    notify('warning', "Time's up! Submitting your exam...")
    try {
      await completeExam(sessionId)
    } catch { /* ignore */ }
    navigate(`/exam/${sessionId}/results`, { replace: true })
  }

  async function handleAbandon() {
    if (!sessionId) return
    try {
      await completeExam(sessionId)
    } catch { /* ignore */ }
    navigate('/exam')
  }

  const progressPct = totalQuestions > 0
    ? Math.round(((questionNumber - 1) / totalQuestions) * 100)
    : 0

  if (state === 'loading' || state === 'submitting_next') {
    return (
      <div className="flex items-center justify-center py-24">
        <LoadingSpinner message={state === 'submitting_next' ? 'Submitting exam...' : 'Loading question...'} size="lg" />
      </div>
    )
  }

  if (state === 'error') {
    return (
      <div className="max-w-xl mx-auto text-center py-16">
        <p className="text-5xl mb-4">⚠️</p>
        <h2 className="text-xl font-bold text-gray-900 mb-2">Something went wrong</h2>
        <Button onClick={() => navigate('/exam')}>Back to Exam Setup</Button>
      </div>
    )
  }

  if (!question) return null

  return (
    <div className="max-w-2xl mx-auto space-y-6">
      {/* Top bar */}
      <div className="flex items-center justify-between gap-4">
        <div className="flex items-center gap-3">
          <span className="text-sm font-medium text-gray-700">
            Question <span className="text-gray-900 font-bold">{questionNumber}</span>
            <span className="text-gray-400"> / {totalQuestions}</span>
          </span>
          <span className="text-xs bg-gray-100 text-gray-600 px-2 py-0.5 rounded-full">
            {getCategoryLabel(question.category)}
          </span>
        </div>
        <div className="flex items-center gap-2">
          {timeRemaining !== null && timeRemaining > 0 && (
            <ExamTimer
              initialSeconds={timeRemaining}
              onExpire={handleTimerExpire}
              paused={state === 'answered'}
            />
          )}
          <button
            onClick={handleAbandon}
            className="text-xs text-gray-400 hover:text-gray-600 transition-colors px-2 py-1"
          >
            Exit
          </button>
        </div>
      </div>

      <ProgressBar value={progressPct} size="sm" color="primary" />

      {/* Question */}
      <div className="bg-white border border-gray-200 rounded-xl p-6 space-y-4">
        {question.clinicalVignette && (
          <div className="text-sm text-gray-700 leading-relaxed bg-blue-50 border-l-4 border-blue-400 p-4 rounded">
            <p className="font-semibold text-blue-900 mb-2">Clinical Vignette:</p>
            <p>{question.clinicalVignette}</p>
          </div>
        )}
        <p className="text-gray-900 text-base leading-relaxed font-medium">
          {question.questionText}
        </p>
      </div>

      {/* Answer options */}
      <AnswerOptions
        options={question.options}
        selectedId={selectedId}
        correctId={feedback?.correctOptionId ?? null}
        onSelect={id => {
          if (state === 'question') setSelectedId(id)
        }}
        disabled={state === 'answered' || submitting}
      />

      {/* Explanation (shown after answering) */}
      {feedback && (
        <div className={`rounded-xl border p-5 ${
          feedback.isCorrect
            ? 'bg-green-50 border-green-200'
            : 'bg-red-50 border-red-200'
        }`}>
          <div className="flex items-center gap-2 mb-2">
            <span className="text-lg">{feedback.isCorrect ? '✅' : '❌'}</span>
            <span className={`text-sm font-semibold ${feedback.isCorrect ? 'text-green-800' : 'text-red-800'}`}>
              {feedback.isCorrect ? 'Correct!' : 'Incorrect'}
            </span>
            <span className="ml-auto text-xs text-gray-500">
              Score: {feedback.runningScore.correct}/{feedback.runningScore.total}
            </span>
          </div>
          <p className="text-sm text-gray-700 leading-relaxed">{feedback.explanation}</p>
        </div>
      )}

      {/* Action buttons */}
      <div className="flex items-center justify-between">
        {state === 'question' ? (
          <Button
            onClick={handleSubmitAnswer}
            loading={submitting}
            disabled={!selectedId}
            className="ml-auto"
          >
            Submit Answer
          </Button>
        ) : (
          <Button onClick={handleNext} className="ml-auto">
            {questionNumber >= totalQuestions ? 'View Results →' : 'Next Question →'}
          </Button>
        )}
      </div>
    </div>
  )
}
