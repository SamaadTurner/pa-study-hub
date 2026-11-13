// ---- Auth ------------------------------------------------------------------

export interface User {
  id: string
  email: string
  firstName: string
  lastName: string
  role: 'STUDENT' | 'ADMIN'
}

export interface AuthTokens {
  accessToken: string
  refreshToken: string
  expiresIn: number
}

export interface LoginRequest {
  email: string
  password: string
}

export interface RegisterRequest {
  email: string
  password: string
  firstName: string
  lastName: string
}

// ---- Flashcards ------------------------------------------------------------

export interface Deck {
  id: string
  title: string
  description: string
  category: string
  isPublic: boolean
  cardCount: number
  dueCardCount: number
  avgEaseFactor: number
  createdAt: string
  updatedAt: string
}

export interface Card {
  id: string
  deckId: string
  front: string
  back: string
  hint?: string
  tags: string[]
  easeFactor: number
  interval: number
  repetitions: number
  nextReviewDate: string
  createdAt: string
}

export interface ReviewCard {
  id: string
  deckId: string
  front: string
  back: string
  hint?: string
  tags: string[]
}

export interface ReviewResult {
  cardId: string
  quality: number        // 0-5
  newInterval: number
  newEaseFactor: number
  nextReviewDate: string
}

// ---- Exam ------------------------------------------------------------------

export type DifficultyLevel = 'EASY' | 'MEDIUM' | 'HARD'
export type ExamStatus = 'IN_PROGRESS' | 'COMPLETED' | 'ABANDONED'
export type PerformanceBand = 'EXCELLENT' | 'GOOD' | 'PASSING' | 'NEEDS_IMPROVEMENT'

export interface StartExamRequest {
  questionCount: number
  timeLimitMinutes?: number
  categoryFilter?: string
  difficultyFilter?: DifficultyLevel
}

// The exam types below use the same field names as the backend's JSON.

export interface AnswerOption {
  id: string
  text: string
  isCorrect?: boolean | null  // only sent with the results, never during the exam
}

export interface ExamQuestion {
  id: string
  stem: string
  clinicalVignette: string | null
  category: string
  answerOptions: AnswerOption[]
  selectedOptionId: string | null  // the option picked so far, if any
}

export interface ExamSession {
  id: string
  categoryFilter: string | null
  questionCount: number
  answeredCount: number
  timeLimitMinutes: number             // 0 means no time limit
  timeRemainingSeconds: number | null  // null when there is no time limit
  status: ExamStatus
  startedAt: string
  questions: ExamQuestion[]
}

// One question on the results page, with the right answer and explanation
export interface ExamAnswerDetail {
  questionId: string
  stem: string
  clinicalVignette: string | null
  answerOptions: AnswerOption[]
  selectedOptionId: string | null  // null when the question was not answered
  isCorrect: boolean
  explanation: string
  timeSpentSeconds: number | null
}

export interface ExamResult {
  sessionId: string
  rawScore: number
  totalQuestions: number
  scorePercent: number
  performanceBand: PerformanceBand
  performanceMessage: string
  categoryBreakdown: Record<string, number>
  avgTimePerQuestionSeconds: number
  durationSeconds: number
  completedAt: string
  answerDetails: ExamAnswerDetail[]
}

// The history also lists exams that were abandoned or are still in progress.
// Those have no score yet, so the score fields can be null.
export interface ExamHistorySummary {
  id: string
  status: ExamStatus
  questionCount: number
  score: number | null
  scorePercent: number | null
  performanceBand: PerformanceBand | null
  categoryFilter: string | null
  startedAt: string
  completedAt: string | null
  durationSeconds: number | null
}

// ---- Progress / Dashboard --------------------------------------------------

export interface DailyProgressPoint {
  date: string
  cardsReviewed: number
  examQuestions: number
  accuracy: number
  studyMinutes: number
  goalMet: boolean
}

export interface ProgressDashboard {
  currentStreak: number
  longestStreak: number
  totalStudyDays: number
  totalCardsReviewed: number
  overallAccuracy: number
  categoryAccuracy: Record<string, number>
  last30DaysActivity: DailyProgressPoint[]
  todayGoal: GoalProgress
}

export interface GoalProgress {
  targetCardsPerDay: number
  targetMinutesPerDay: number
  todayCardsReviewed: number
  todayMinutesStudied: number
  cardGoalMet: boolean
  minuteGoalMet: boolean
}

export interface UpdateGoalRequest {
  targetCardsPerDay: number
  targetMinutesPerDay: number
}

// ---- Common ----------------------------------------------------------------

export interface ApiError {
  status: number
  title: string
  detail: string
  fieldErrors?: Record<string, string>
}

export interface PaginatedResponse<T> {
  content: T[]
  totalElements: number
  totalPages: number
  number: number
  size: number
}
