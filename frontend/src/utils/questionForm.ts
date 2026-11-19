import type { DifficultyLevel, QuestionRequest, TaskArea, WrittenQuestion } from '@/types'

// The rules match QuestionRequest.java on the backend, so most mistakes are
// shown next to the field before anything is sent.
export const MIN_OPTIONS = 2
export const MAX_OPTIONS = 5
export const MAX_VIGNETTE = 1000
export const MAX_STEM = 3000
export const MAX_OPTION = 1000
export const MAX_EXPLANATION = 2000

export interface QuestionForm {
  clinicalVignette: string
  stem: string
  options: string[]
  correctIndex: number | null  // which option is the right one
  explanation: string
  category: string             // '' until one is picked
  taskArea: TaskArea | ''
  difficulty: DifficultyLevel
  isPublic: boolean
}

export interface QuestionFormErrors {
  clinicalVignette?: string
  stem?: string
  options?: string
  explanation?: string
  category?: string
  taskArea?: string
}

export function emptyQuestionForm(): QuestionForm {
  return {
    clinicalVignette: '',
    stem: '',
    options: ['', '', '', ''],
    correctIndex: null,
    explanation: '',
    category: '',
    taskArea: '',
    difficulty: 'MEDIUM',
    isPublic: false,
  }
}

export function formFromQuestion(question: WrittenQuestion): QuestionForm {
  const correctIndex = question.answerOptions.findIndex(option => option.isCorrect)
  return {
    clinicalVignette: question.clinicalVignette ?? '',
    stem: question.stem,
    options: question.answerOptions.map(option => option.text),
    correctIndex: correctIndex >= 0 ? correctIndex : null,
    explanation: question.explanation,
    category: question.category,
    taskArea: question.taskArea,
    difficulty: question.difficulty,
    isPublic: question.isPublic,
  }
}

// Removes one option row and keeps the right-answer mark on the same option
export function removeOption(form: QuestionForm, index: number): QuestionForm {
  const options = form.options.filter((_, i) => i !== index)
  let correctIndex = form.correctIndex
  if (correctIndex === index) {
    correctIndex = null
  } else if (correctIndex !== null && correctIndex > index) {
    correctIndex = correctIndex - 1
  }
  return { ...form, options, correctIndex }
}

export function validateQuestionForm(form: QuestionForm): QuestionFormErrors {
  const errors: QuestionFormErrors = {}

  if (form.clinicalVignette.length > MAX_VIGNETTE) {
    errors.clinicalVignette = `The patient scenario must be ${MAX_VIGNETTE} characters or less`
  }

  if (!form.stem.trim()) errors.stem = 'The question is required'
  else if (form.stem.length > MAX_STEM) errors.stem = `The question must be ${MAX_STEM} characters or less`

  if (!form.explanation.trim()) errors.explanation = 'The explanation is required'
  else if (form.explanation.length > MAX_EXPLANATION) {
    errors.explanation = `The explanation must be ${MAX_EXPLANATION} characters or less`
  }

  if (!form.category) errors.category = 'Pick a category'
  if (!form.taskArea) errors.taskArea = 'Pick a task area'

  errors.options = checkOptions(form)
  if (!errors.options) delete errors.options

  return errors
}

// Empty option rows are left out when saving, so they are not mistakes
function checkOptions(form: QuestionForm): string | undefined {
  const filled = form.options.map(text => text.trim()).filter(Boolean)
  if (filled.length < MIN_OPTIONS) return `Write at least ${MIN_OPTIONS} answer options`
  if (form.options.some(text => text.length > MAX_OPTION)) {
    return `An answer option must be ${MAX_OPTION} characters or less`
  }

  const lowerCase = filled.map(text => text.toLowerCase())
  if (new Set(lowerCase).size !== lowerCase.length) return 'Two answer options have the same text'

  if (form.correctIndex === null || !form.options[form.correctIndex]?.trim()) {
    return 'Mark the right answer'
  }
  return undefined
}

export function hasErrors(errors: QuestionFormErrors): boolean {
  return Object.keys(errors).length > 0
}

// The request the backend expects. Only call this when the form has no errors.
export function toQuestionRequest(form: QuestionForm): QuestionRequest {
  const answerOptions = form.options
    .map((text, index) => ({ text: text.trim(), isCorrect: index === form.correctIndex }))
    .filter(option => option.text !== '')

  return {
    clinicalVignette: form.clinicalVignette.trim() || null,
    stem: form.stem.trim(),
    answerOptions,
    explanation: form.explanation.trim(),
    category: form.category,
    taskArea: form.taskArea as TaskArea,
    difficulty: form.difficulty,
    isPublic: form.isPublic,
  }
}
