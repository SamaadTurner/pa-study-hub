import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { getSharedQuestions } from '@/api/questions'
import Badge from '@/components/common/Badge'
import Button from '@/components/common/Button'
import LoadingSpinner from '@/components/common/LoadingSpinner'
import { ALL_CATEGORIES, getCategoryBadgeVariant, getCategoryLabel } from '@/utils/categoryColors'
import { getTaskAreaLabel } from '@/utils/taskAreas'
import type { ApiError, SharedQuestion } from '@/types'

const LETTERS = ['A', 'B', 'C', 'D', 'E']

function SharedQuestionCard({ question }: { question: SharedQuestion }) {
  const [showAnswer, setShowAnswer] = useState(false)
  return (
    <li className="bg-white border border-gray-200 rounded-xl p-5 space-y-3">
      <div className="flex flex-wrap items-center gap-2">
        <Badge variant={getCategoryBadgeVariant(question.category)}>{getCategoryLabel(question.category)}</Badge>
        <span className="text-xs text-gray-500">{getTaskAreaLabel(question.taskArea)}</span>
      </div>
      {question.clinicalVignette && (
        <p className="text-sm text-gray-700 bg-blue-50 border-l-4 border-blue-400 p-3 rounded">{question.clinicalVignette}</p>
      )}
      <p className="text-sm font-medium text-gray-900">{question.stem}</p>
      <ol className="text-sm text-gray-700 space-y-1">
        {question.answerOptions.map((option, index) => (
          <li key={option.id ?? index}
            className={showAnswer && option.isCorrect ? 'text-green-700 font-medium' : ''}>
            {LETTERS[index]}. {option.text}{showAnswer && option.isCorrect ? ' (right answer)' : ''}
          </li>
        ))}
      </ol>
      {showAnswer ? (
        <p className="text-sm text-gray-600 bg-gray-50 border border-gray-200 rounded-lg p-3">{question.explanation}</p>
      ) : (
        <button type="button" onClick={() => setShowAnswer(true)} className="text-sm text-primary-600 hover:underline">
          Show answer and explanation
        </button>
      )}
    </li>
  )
}

/**
 * Questions other users shared, by topic. They are only used in your exams when you
 * tick "Include questions other users shared" on the setup page.
 */
export default function SharedQuestionsPage() {
  const navigate = useNavigate()
  const [category, setCategory]   = useState<string>('')
  const [questions, setQuestions] = useState<SharedQuestion[]>([])
  const [total, setTotal]         = useState(0)
  const [page, setPage]           = useState(0)
  const [isLast, setIsLast]       = useState(true)
  const [loading, setLoading]     = useState(true)
  const [error, setError]         = useState<string | null>(null)

  // A new category starts again at the first page
  useEffect(() => {
    let ignore = false
    setLoading(true)
    setError(null)
    getSharedQuestions(category || null, 0)
      .then(result => {
        if (ignore) return
        setQuestions(result.content)
        setTotal(result.totalElements)
        setPage(0)
        setIsLast(result.last !== false)
      })
      .catch((err: ApiError) => { if (!ignore) setError(err?.detail ?? 'Could not load shared questions') })
      .finally(() => { if (!ignore) setLoading(false) })
    return () => { ignore = true }
  }, [category])

  async function loadMore() {
    try {
      const result = await getSharedQuestions(category || null, page + 1)
      setQuestions(current => [...current, ...result.content])
      setPage(page + 1)
      setIsLast(result.last !== false)
    } catch (err) {
      setError((err as ApiError)?.detail ?? 'Could not load more questions')
    }
  }

  return (
    <div className="max-w-3xl mx-auto space-y-6">
      <div>
        <button onClick={() => navigate('/exam/questions')} className="text-sm text-primary-600 hover:underline mb-2">
          ← Back to My Questions
        </button>
        <h1 className="text-2xl font-bold text-gray-900">Shared Questions</h1>
        <p className="text-sm text-gray-500 mt-1">
          Written by other students and not checked by anyone. Tick "Include questions other users shared"
          on the exam setup page to get them in your exams.
        </p>
      </div>

      <div>
        <label htmlFor="shared-category" className="block text-sm font-medium text-gray-700 mb-1">Topic</label>
        <select id="shared-category" value={category} onChange={e => setCategory(e.target.value)}
          className="w-full sm:w-72 px-3 py-2 border border-gray-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-primary-500">
          <option value="">All topics</option>
          {ALL_CATEGORIES.map(cat => <option key={cat} value={cat}>{getCategoryLabel(cat)}</option>)}
        </select>
      </div>

      {loading ? (
        <div className="flex justify-center py-16"><LoadingSpinner message="Loading shared questions..." /></div>
      ) : error ? (
        <p className="text-sm text-red-600" role="alert">{error}</p>
      ) : questions.length === 0 ? (
        <p className="text-sm text-gray-500 bg-white border border-gray-200 rounded-xl p-8 text-center">
          Nobody has shared a question {category ? `in ${getCategoryLabel(category)} ` : ''}yet.
        </p>
      ) : (
        <>
          <p className="text-sm text-gray-500">Showing {questions.length} of {total}</p>
          <ul className="space-y-3">
            {questions.map(question => <SharedQuestionCard key={question.id} question={question} />)}
          </ul>
          {!isLast && (
            <div className="flex justify-center">
              <Button variant="secondary" onClick={loadMore}>Load more</Button>
            </div>
          )}
        </>
      )}
    </div>
  )
}
