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

export interface AnswerOption {
  id: string
  text: string
  isCorrect?: boolean  // only present after exam completion
}

export interface ExamQuestion {
  id: string
  stem: string
  category: string
  difficulty: DifficultyLevel
  options: AnswerOption[]
}

export interface ExamSession {
  id: string
  questionCount: number
  timeLimitMinutes: number
  categoryFilter?: string
  difficultyFilter?: DifficultyLevel
  status: ExamStatus
  questions: ExamQuestion[]
  startedAt: string
  completedAt?: string
}

export interface ExamResult {
  sessionId: string
  rawScore: number
  totalQuestions: number
  scorePercent: number
  performanceBand: PerformanceBand
  categoryBreakdown: Record<string, number>
  avgTimePerQuestionSeconds: number
  incorrectCount: number
  durationSeconds: number
  completedAt: string
}

export interface ExamHistorySummary {
  sessionId: string
  questionCount: number
  scorePercent: number
  performanceBand: PerformanceBand
  categoryFilter?: string
  durationSeconds: number
  completedAt: string
}

// ---- Progress / Dashboard --------------------------------------------------

export interface DailyProgressPoint {
  date: string
  cardsReviewed: number
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

// ---- AI --------------------------------------------------------------------

export interface GenerateFlashcardsRequest {
  topic: string
  category: string
  count: number
}

export interface GeneratedFlashcard {
  front: string
  back: string
  hint?: string
  tags: string[]
}

export interface GenerateFlashcardsResponse {
  topic: string
  category: string
  requestedCount: number
  generatedCount: number
  flashcards: GeneratedFlashcard[]
}

export interface ChatMessage {
  role: 'user' | 'assistant'
  content: string
}

export interface ChatRequest {
  message: string
  sessionId?: string
}

export interface ChatResponse {
  sessionId: string
  userMessage: string
  assistantReply: string
  turnNumber: number
}

export interface StudyPlanRequest {
  weakCategories: string[]
  daysUntilExam: number
  dailyMinutes: number
}

export interface StudyPlanResponse {
  weakCategories: string[]
  daysUntilExam: number
  dailyMinutes: number
  studyPlan: string
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
