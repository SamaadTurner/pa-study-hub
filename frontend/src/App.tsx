import { lazy, Suspense } from 'react'
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'
import { AuthProvider } from '@/contexts/AuthContext'
import { NotificationProvider } from '@/contexts/NotificationContext'
import ProtectedRoute from '@/components/common/ProtectedRoute'
import AppLayout from '@/components/layout/AppLayout'
import ErrorBoundary from '@/components/common/ErrorBoundary'

import LoginPage        from '@/pages/auth/LoginPage'
import RegisterPage     from '@/pages/auth/RegisterPage'

// The login and register pages are part of the main bundle, because one of them
// is the first thing a visitor sees. Every page behind the login is its own file
// that the browser fetches the first time the page is opened (AppLayout shows a
// spinner meanwhile). That keeps the first load small: the chart library is
// bigger than the rest of the app, and only the analytics and exam results
// pages use it.
const DashboardPage    = lazy(() => import('@/pages/dashboard/DashboardPage'))
const DecksPage        = lazy(() => import('@/pages/decks/DecksPage'))
const DeckDetailPage   = lazy(() => import('@/pages/decks/DeckDetailPage'))
const ReviewPage       = lazy(() => import('@/pages/decks/ReviewPage'))
const CardEditorPage   = lazy(() => import('@/pages/decks/CardEditorPage'))
const PublicDecksPage  = lazy(() => import('@/pages/decks/PublicDecksPage'))
const ExamSetupPage    = lazy(() => import('@/pages/exam/ExamSetupPage'))
const ExamPage         = lazy(() => import('@/pages/exam/ExamPage'))
const ExamResultsPage  = lazy(() => import('@/pages/exam/ExamResultsPage'))
const ExamHistoryPage  = lazy(() => import('@/pages/exam/ExamHistoryPage'))
const MyQuestionsPage  = lazy(() => import('@/pages/exam/MyQuestionsPage'))
const QuestionEditorPage = lazy(() => import('@/pages/exam/QuestionEditorPage'))
const SharedQuestionsPage = lazy(() => import('@/pages/exam/SharedQuestionsPage'))
const AnalyticsPage    = lazy(() => import('@/pages/analytics/AnalyticsPage'))
const PrivacyPage      = lazy(() => import('@/pages/legal/PrivacyPage'))
const ProfilePage      = lazy(() => import('@/pages/profile/ProfilePage'))

export default function App() {
  return (
    <ErrorBoundary>
      <BrowserRouter>
        <AuthProvider>
          <NotificationProvider>
            <Routes>
              {/* Public routes */}
              <Route path="/login"    element={<LoginPage />} />
              <Route path="/register" element={<RegisterPage />} />
              {/* Lazy, but outside AppLayout, so it needs its own Suspense */}
              <Route path="/privacy"  element={<Suspense fallback={null}><PrivacyPage /></Suspense>} />

              {/* Protected routes */}
              <Route element={<ProtectedRoute />}>
                <Route element={<AppLayout />}>
                  <Route index element={<Navigate to="/dashboard" replace />} />
                  <Route path="/dashboard"                            element={<DashboardPage />} />
                  <Route path="/decks"                                element={<DecksPage />} />
                  <Route path="/decks/public"                         element={<PublicDecksPage />} />
                  <Route path="/decks/:deckId"                        element={<DeckDetailPage />} />
                  <Route path="/decks/:deckId/review"                 element={<ReviewPage />} />
                  <Route path="/decks/:deckId/cards/new"              element={<CardEditorPage />} />
                  <Route path="/decks/:deckId/cards/:cardId/edit"     element={<CardEditorPage />} />
                  {/* exam/history and exam/questions must come before exam/:sessionId so they match first */}
                  <Route path="/exam/history"                         element={<ExamHistoryPage />} />
                  <Route path="/exam/questions"                       element={<MyQuestionsPage />} />
                  <Route path="/exam/shared"                          element={<SharedQuestionsPage />} />
                  <Route path="/exam/questions/new"                   element={<QuestionEditorPage />} />
                  <Route path="/exam/questions/:questionId/edit"      element={<QuestionEditorPage />} />
                  <Route path="/exam"                                 element={<ExamSetupPage />} />
                  <Route path="/exam/:sessionId"                      element={<ExamPage />} />
                  <Route path="/exam/:sessionId/results"              element={<ExamResultsPage />} />
                  <Route path="/analytics"                            element={<AnalyticsPage />} />
                  <Route path="/profile"                              element={<ProfilePage />} />
                </Route>
              </Route>

              {/* Fallback */}
              <Route path="*" element={<Navigate to="/dashboard" replace />} />
            </Routes>
          </NotificationProvider>
        </AuthProvider>
      </BrowserRouter>
    </ErrorBoundary>
  )
}
