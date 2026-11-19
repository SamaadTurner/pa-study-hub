import { describe, it, expect } from 'vitest'
import {
  emptyQuestionForm,
  formFromQuestion,
  hasErrors,
  removeOption,
  toQuestionRequest,
  validateQuestionForm,
  type QuestionForm,
} from '@/utils/questionForm'
import type { WrittenQuestion } from '@/types'

function filledForm(): QuestionForm {
  return {
    ...emptyQuestionForm(),
    stem: 'Which drug is first line?',
    options: ['Metformin', 'Insulin', 'Glipizide', ''],
    correctIndex: 0,
    explanation: 'Metformin is first line for type 2 diabetes.',
    category: 'ENDOCRINOLOGY',
    taskArea: 'PHARMACEUTICAL_THERAPEUTICS',
  }
}

describe('validateQuestionForm', () => {
  it('accepts a complete form, empty option rows included', () => {
    expect(hasErrors(validateQuestionForm(filledForm()))).toBe(false)
  })

  it('asks for the question, explanation, category and task area', () => {
    const errors = validateQuestionForm(emptyQuestionForm())
    expect(errors.stem).toBe('The question is required')
    expect(errors.explanation).toBe('The explanation is required')
    expect(errors.category).toBe('Pick a category')
    expect(errors.taskArea).toBe('Pick a task area')
    expect(errors.options).toBe('Write at least 2 answer options')
  })

  it('needs a right answer, and it cannot be an empty row', () => {
    expect(validateQuestionForm({ ...filledForm(), correctIndex: null }).options).toBe('Mark the right answer')
    expect(validateQuestionForm({ ...filledForm(), correctIndex: 3 }).options).toBe('Mark the right answer')
  })

  it('refuses two options with the same text, ignoring case and spaces', () => {
    const form = { ...filledForm(), options: ['Metformin', ' metformin ', 'Insulin'] }
    expect(validateQuestionForm(form).options).toBe('Two answer options have the same text')
  })

  it('refuses text that is too long', () => {
    const form = { ...filledForm(), stem: 'x'.repeat(3001), clinicalVignette: 'x'.repeat(1001) }
    const errors = validateQuestionForm(form)
    expect(errors.stem).toBe('The question must be 3000 characters or less')
    expect(errors.clinicalVignette).toBe('The patient scenario must be 1000 characters or less')
  })
})

describe('toQuestionRequest', () => {
  it('trims the text, leaves out empty rows and marks the right option', () => {
    const request = toQuestionRequest({ ...filledForm(), stem: '  Which drug?  ', correctIndex: 1 })
    expect(request.stem).toBe('Which drug?')
    expect(request.clinicalVignette).toBeNull()
    expect(request.answerOptions).toEqual([
      { text: 'Metformin', isCorrect: false },
      { text: 'Insulin', isCorrect: true },
      { text: 'Glipizide', isCorrect: false },
    ])
  })

  it('keeps the right answer when an empty row comes before it', () => {
    const form = { ...filledForm(), options: ['A', '', 'C'], correctIndex: 2 }
    expect(toQuestionRequest(form).answerOptions).toEqual([
      { text: 'A', isCorrect: false },
      { text: 'C', isCorrect: true },
    ])
  })
})

describe('removeOption', () => {
  it('moves the right-answer mark along with its option', () => {
    const form = { ...filledForm(), correctIndex: 2 }
    const after = removeOption(form, 0)
    expect(after.options).toEqual(['Insulin', 'Glipizide', ''])
    expect(after.correctIndex).toBe(1)
  })

  it('clears the mark when the right option is removed', () => {
    expect(removeOption(filledForm(), 0).correctIndex).toBeNull()
  })

  it('leaves the mark alone when a later option is removed', () => {
    expect(removeOption(filledForm(), 2).correctIndex).toBe(0)
  })
})

describe('formFromQuestion', () => {
  it('fills the form from a saved question', () => {
    const question: WrittenQuestion = {
      id: 'q1',
      clinicalVignette: null,
      stem: 'Which drug?',
      answerOptions: [
        { id: 'o1', text: 'A', isCorrect: false },
        { id: 'o2', text: 'B', isCorrect: true },
      ],
      explanation: 'Because.',
      category: 'CARDIOLOGY',
      taskArea: 'CLINICAL_INTERVENTION',
      difficulty: 'HARD',
      isPublic: true,
      reports: [],
      hiddenFromShared: false,
      createdAt: '2026-11-19T10:00:00',
      updatedAt: '2026-11-19T10:00:00',
    }
    const form = formFromQuestion(question)
    expect(form.clinicalVignette).toBe('')
    expect(form.options).toEqual(['A', 'B'])
    expect(form.correctIndex).toBe(1)
    expect(form.isPublic).toBe(true)
    expect(form.difficulty).toBe('HARD')
  })
})
