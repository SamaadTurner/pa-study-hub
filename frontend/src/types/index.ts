// ---- Auth ------------------------------------------------------------------

export interface User {
  id: string
  email: string
  firstName: string
  lastName: string
  role: 'STUDENT' | 'ADMIN'
  paSchoolName?: string | null
  graduationYear?: number | null
  hasPassword: boolean    // false for an account made with Google that has no password yet
  googleLinked: boolean
  emailVerified: boolean  // false until the confirmation link is used
  timeZone?: string | null  // the zone streaks and goals use, from the device
}

export interface AuthTokens {
  accessToken: string
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
  botCheckToken?: string  // from the Turnstile widget, when the check is on
}

// ---- Flashcards ------------------------------------------------------------

export interface Deck {
  id: string
  userId?: string  // the owner; another user's public deck can be studied and copied, not changed
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

// BASIC = question and answer; CLOZE = fill in the blank ({{c1::answer}} in front);
// IMAGE = a picture with boxes, the card asks for the part under one box
export type CardType = 'BASIC' | 'CLOZE' | 'IMAGE'

// A box over a picture. Position and size are fractions of the picture (0 to 1),
// so the box stays on the same spot at any size.
export interface OcclusionBox {
  x: number
  y: number
  width: number
  height: number
  label?: string
}

export interface Card {
  id: string
  deckId: string
  front: string
  back: string
  cardType: CardType
  clozeIndex: number | null  // for a cloze card, the blank number it asks for (picture card: the box number)
  imageId: string | null     // picture cards only
  boxes: OcclusionBox[]      // picture cards only
  pictureGroup: string | null  // picture cards only: the same for every card of one picture
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
  cardType: CardType
  clozeIndex: number | null
  imageId: string | null
  boxes: OcclusionBox[]
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

// What a question tests, the second dimension of the PANCE blueprint
export type TaskArea =
  | 'HISTORY_AND_PHYSICAL'
  | 'DIAGNOSTIC_STUDIES'
  | 'MOST_LIKELY_DIAGNOSIS'
  | 'HEALTH_MAINTENANCE'
  | 'CLINICAL_INTERVENTION'
  | 'PHARMACEUTICAL_THERAPEUTICS'
  | 'BASIC_SCIENCE'
  | 'PROFESSIONAL_PRACTICE'

export interface StartExamRequest {
  questionCount: number
  timeLimitMinutes?: number
  categoryFilter?: string
  difficultyFilter?: DifficultyLevel
  followBlueprint?: boolean  // pick questions in the blueprint's task area percentages
  includeSharedQuestions?: boolean  // also pick questions other users wrote and shared
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
  taskArea: TaskArea
  answerOptions: AnswerOption[]
  selectedOptionId: string | null  // the option picked so far, if any
}

export interface ExamSession {
  id: string
  categoryFilter: string | null
  followsBlueprint: boolean
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
  taskArea: TaskArea
  answerOptions: AnswerOption[]
  selectedOptionId: string | null  // null when the question was not answered
  isCorrect: boolean
  explanation: string
  timeSpentSeconds: number | null
  source: QuestionSource
  reportedByYou: boolean  // only for questions shared by another user
}

// Who wrote a question, seen from the user who took the exam
export type QuestionSource = 'STARTER' | 'YOU' | 'ANOTHER_USER'

// ---- Questions the user writes ----------------------------------------------

export interface WrittenAnswerOption {
  id?: string
  text: string
  isCorrect: boolean
}

export interface QuestionRequest {
  clinicalVignette?: string | null
  stem: string
  answerOptions: WrittenAnswerOption[]
  explanation: string
  category: string
  taskArea: TaskArea
  difficulty: DifficultyLevel
  isPublic: boolean
}

export type ReportReason = 'WRONG_ANSWER' | 'UNCLEAR' | 'OTHER'

// A report on one of your shared questions. Who sent it is never shown.
export interface QuestionReport {
  reason: ReportReason
  note: string | null
  createdAt: string
}

// A question another user shared, on the browse page
export interface SharedQuestion {
  id: string
  clinicalVignette: string | null
  stem: string
  answerOptions: WrittenAnswerOption[]
  explanation: string
  category: string
  taskArea: TaskArea
  difficulty: DifficultyLevel
}

// How many questions an exam can pick from, per category (categories with none are left out)
export interface QuestionCounts {
  total: number
  byCategory: Record<string, number>
}

export interface WrittenQuestion extends QuestionRequest {
  id: string
  reports: QuestionReport[]
  earlierReportCount: number  // reports the earlier versions had; they still count
  hiddenFromShared: boolean   // left out of other people's exams because of reports
  createdAt: string
  updatedAt: string
}

// Right answers out of questions for one task area. Unanswered questions count as wrong.
export interface TaskAreaScore {
  taskArea: TaskArea
  correct: number
  total: number
  percent: number
}

export interface ExamResult {
  sessionId: string
  rawScore: number
  totalQuestions: number
  scorePercent: number
  performanceBand: PerformanceBand
  performanceMessage: string
  categoryBreakdown: Record<string, number>
  taskAreaBreakdown: TaskAreaScore[]  // in blueprint order, only areas in this exam
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
  followsBlueprint: boolean
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
  totalExamQuestions: number  // questions in finished practice exams
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
  last: boolean  // true when there are no more pages after this one
}
