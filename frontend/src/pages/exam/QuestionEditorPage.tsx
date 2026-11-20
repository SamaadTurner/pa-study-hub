import { useState, useEffect } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { createQuestion, getMyQuestion, updateQuestion } from '@/api/questions'
import Button from '@/components/common/Button'
import LoadingSpinner from '@/components/common/LoadingSpinner'
import { useNotification } from '@/contexts/NotificationContext'
import { ALL_CATEGORIES, getCategoryLabel } from '@/utils/categoryColors'
import { TASK_AREAS } from '@/utils/taskAreas'
import {
  MAX_EXPLANATION,
  MAX_OPTIONS,
  MAX_STEM,
  MAX_VIGNETTE,
  MIN_OPTIONS,
  emptyQuestionForm,
  formFromQuestion,
  hasErrors,
  removeOption,
  toQuestionRequest,
  validateQuestionForm,
  type QuestionForm,
  type QuestionFormErrors,
} from '@/utils/questionForm'
import type { ApiError, DifficultyLevel } from '@/types'

const LETTERS = ['A', 'B', 'C', 'D', 'E']
const DIFFICULTIES: { value: DifficultyLevel; label: string }[] = [
  { value: 'EASY', label: 'Easy' },
  { value: 'MEDIUM', label: 'Medium' },
  { value: 'HARD', label: 'Hard' },
]

const inputClass = (hasError: boolean) =>
  `w-full px-4 py-2.5 border rounded-lg text-sm focus:outline-none focus:ring-2
   focus:ring-primary-500 ${hasError ? 'border-red-400 bg-red-50' : 'border-gray-300'}`

export default function QuestionEditorPage() {
  const { questionId } = useParams<{ questionId: string }>()
  const navigate       = useNavigate()
  const { notify }     = useNotification()
  const isEditing      = !!questionId

  const [form, setForm]       = useState<QuestionForm>(emptyQuestionForm)
  const [errors, setErrors]   = useState<QuestionFormErrors>({})
  const [loading, setLoading] = useState(isEditing)
  const [loadError, setLoadError] = useState<string | null>(null)
  const [saving, setSaving]   = useState(false)

  useEffect(() => {
    if (!questionId) return
    // Ignore a late answer, like in CardEditorPage
    let ignore = false
    getMyQuestion(questionId)
      .then(question => {
        if (!ignore) setForm(formFromQuestion(question))
      })
      .catch((err: ApiError) => {
        if (!ignore) setLoadError(err?.detail ?? 'Could not load the question')
      })
      .finally(() => {
        if (!ignore) setLoading(false)
      })
    return () => { ignore = true }
  }, [questionId])

  function update(changes: Partial<QuestionForm>) {
    setForm(current => ({ ...current, ...changes }))
  }

  function setOptionText(index: number, text: string) {
    setForm(current => ({
      ...current,
      options: current.options.map((old, i) => (i === index ? text : old)),
    }))
  }

  async function handleSave(writeAnother: boolean) {
    const found = validateQuestionForm(form)
    setErrors(found)
    if (hasErrors(found)) return

    setSaving(true)
    try {
      const request = toQuestionRequest(form)
      if (isEditing && questionId) {
        await updateQuestion(questionId, request)
        notify('success', 'Question saved')
        navigate('/exam/questions')
      } else {
        await createQuestion(request)
        if (writeAnother) {
          // Keep the category, task and sharing choice, they are often the same
          setForm({ ...emptyQuestionForm(), category: form.category, taskArea: form.taskArea,
            difficulty: form.difficulty, isPublic: form.isPublic })
          notify('success', 'Question saved. Write the next one.')
        } else {
          notify('success', 'Question saved')
          navigate('/exam/questions')
        }
      }
    } catch (err) {
      notify('error', (err as ApiError)?.detail ?? 'Could not save the question')
    } finally {
      setSaving(false)
    }
  }

  if (loading) {
    return (
      <div className="flex items-center justify-center py-24">
        <LoadingSpinner message="Loading question..." size="lg" />
      </div>
    )
  }

  if (loadError) {
    return (
      <div className="max-w-xl mx-auto text-center py-16">
        <h1 className="text-xl font-bold text-gray-900 mb-4">{loadError}</h1>
        <Button onClick={() => navigate('/exam/questions')}>Back to My Questions</Button>
      </div>
    )
  }

  return (
    <div className="max-w-2xl mx-auto space-y-6">
      <div>
        <button
          onClick={() => navigate('/exam/questions')}
          className="text-sm text-primary-600 hover:underline mb-2"
        >
          ← Back to My Questions
        </button>
        <h1 className="text-2xl font-bold text-gray-900">
          {isEditing ? 'Edit Question' : 'Write a Question'}
        </h1>
      </div>

      <div className="bg-white border border-gray-200 rounded-xl p-6 space-y-5">
        <div>
          <label htmlFor="vignette" className="block text-sm font-medium text-gray-700 mb-1">
            Patient scenario <span className="text-gray-500 font-normal">(optional)</span>
          </label>
          <textarea
            id="vignette"
            value={form.clinicalVignette}
            onChange={e => update({ clinicalVignette: e.target.value })}
            rows={3}
            maxLength={MAX_VIGNETTE}
            className={inputClass(!!errors.clinicalVignette) + ' resize-y'}
            placeholder="A 62-year-old man presents with crushing chest pain for 30 minutes..."
          />
          {errors.clinicalVignette && <p className="mt-1 text-xs text-red-600">{errors.clinicalVignette}</p>}
        </div>

        <div>
          <label htmlFor="stem" className="block text-sm font-semibold text-gray-700 mb-1">
            Question <span className="text-red-600">*</span>
          </label>
          <textarea
            id="stem"
            value={form.stem}
            onChange={e => update({ stem: e.target.value })}
            rows={3}
            maxLength={MAX_STEM}
            className={inputClass(!!errors.stem) + ' resize-y'}
            placeholder="Which of the following is the most likely diagnosis?"
          />
          {errors.stem && <p className="mt-1 text-xs text-red-600">{errors.stem}</p>}
        </div>

        <fieldset>
          <legend className="block text-sm font-semibold text-gray-700 mb-1">
            Answer options <span className="text-red-600">*</span>
          </legend>
          <p className="text-xs text-gray-500 mb-3">
            Mark the right one. Exams show the options in a random order.
          </p>
          <div className="space-y-2">
            {form.options.map((text, index) => (
              <div key={index} className="flex items-center gap-2">
                <input
                  type="radio"
                  name="correct"
                  checked={form.correctIndex === index}
                  onChange={() => update({ correctIndex: index })}
                  aria-label={`Option ${LETTERS[index]} is the right answer`}
                  className="border-gray-300 text-primary-600 focus:ring-primary-500"
                />
                <span className="w-4 text-sm font-medium text-gray-500">{LETTERS[index]}</span>
                <input
                  type="text"
                  value={text}
                  onChange={e => setOptionText(index, e.target.value)}
                  aria-label={`Answer option ${LETTERS[index]}`}
                  className={inputClass(false)}
                />
                {form.options.length > MIN_OPTIONS && (
                  <button
                    type="button"
                    onClick={() => setForm(current => removeOption(current, index))}
                    aria-label={`Remove option ${LETTERS[index]}`}
                    className="px-2 py-1 text-gray-500 hover:text-red-600 text-lg leading-none"
                  >
                    ×
                  </button>
                )}
              </div>
            ))}
          </div>
          {form.options.length < MAX_OPTIONS && (
            <button
              type="button"
              onClick={() => update({ options: [...form.options, ''] })}
              className="mt-2 text-sm text-primary-600 hover:underline"
            >
              + Add option
            </button>
          )}
          {errors.options && <p className="mt-2 text-xs text-red-600">{errors.options}</p>}
        </fieldset>

        <div>
          <label htmlFor="explanation" className="block text-sm font-semibold text-gray-700 mb-1">
            Explanation <span className="text-red-600">*</span>
          </label>
          <textarea
            id="explanation"
            value={form.explanation}
            onChange={e => update({ explanation: e.target.value })}
            rows={4}
            maxLength={MAX_EXPLANATION}
            className={inputClass(!!errors.explanation) + ' resize-y'}
            placeholder="Shown on the results page: why the right answer is right and the others are not."
          />
          {errors.explanation && <p className="mt-1 text-xs text-red-600">{errors.explanation}</p>}
        </div>

        <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
          <div>
            <label htmlFor="category" className="block text-sm font-semibold text-gray-700 mb-1">
              Category <span className="text-red-600">*</span>
            </label>
            <select
              id="category"
              value={form.category}
              onChange={e => update({ category: e.target.value })}
              className={inputClass(!!errors.category)}
            >
              <option value="">Pick a category</option>
              {ALL_CATEGORIES.map(category => (
                <option key={category} value={category}>{getCategoryLabel(category)}</option>
              ))}
            </select>
            {errors.category && <p className="mt-1 text-xs text-red-600">{errors.category}</p>}
          </div>

          <div>
            <label htmlFor="taskArea" className="block text-sm font-semibold text-gray-700 mb-1">
              Task area <span className="text-red-600">*</span>
            </label>
            <select
              id="taskArea"
              value={form.taskArea}
              onChange={e => update({ taskArea: e.target.value as QuestionForm['taskArea'] })}
              className={inputClass(!!errors.taskArea)}
            >
              <option value="">Pick a task area</option>
              {TASK_AREAS.map(area => (
                <option key={area.value} value={area.value}>{area.label}</option>
              ))}
            </select>
            {errors.taskArea && <p className="mt-1 text-xs text-red-600">{errors.taskArea}</p>}
          </div>
        </div>

        <fieldset>
          <legend className="block text-sm font-semibold text-gray-700 mb-2">Difficulty</legend>
          <div className="flex gap-4">
            {DIFFICULTIES.map(level => (
              <label key={level.value} className="flex items-center gap-2 text-sm text-gray-700 cursor-pointer">
                <input
                  type="radio"
                  name="difficulty"
                  checked={form.difficulty === level.value}
                  onChange={() => update({ difficulty: level.value })}
                  className="border-gray-300 text-primary-600 focus:ring-primary-500"
                />
                {level.label}
              </label>
            ))}
          </div>
        </fieldset>

        <label className="flex items-start gap-2.5 text-sm text-gray-700 cursor-pointer">
          <input
            type="checkbox"
            checked={form.isPublic}
            onChange={e => update({ isPublic: e.target.checked })}
            className="mt-0.5 rounded border-gray-300 text-primary-600 focus:ring-primary-500"
          />
          <span>
            Share with other users
            <span className="block text-xs text-gray-500">
              Others can get it in their exams if they choose to include shared questions.
              They never see who wrote it, and they cannot change it.
            </span>
          </span>
        </label>
      </div>

      <div className="flex items-center justify-between gap-3">
        <Button variant="secondary" onClick={() => navigate('/exam/questions')}>
          Cancel
        </Button>
        <div className="flex gap-2">
          {!isEditing && (
            <Button variant="secondary" loading={saving} onClick={() => handleSave(true)}>
              Save & Write Another
            </Button>
          )}
          <Button loading={saving} onClick={() => handleSave(false)}>
            {isEditing ? 'Save Changes' : 'Save Question'}
          </Button>
        </div>
      </div>
    </div>
  )
}
