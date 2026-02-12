import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'
import { AuthProvider } from '@/contexts/AuthContext'
import { NotificationProvider } from '@/contexts/NotificationContext'
import ProtectedRoute from '@/components/common/ProtectedRoute'
import AppLayout from '@/components/layout/AppLayout'
import ErrorBoundary from '@/components/common/ErrorBoundary'

import LoginPage        from '@/pages/auth/LoginPage'
import RegisterPage     from '@/pages/auth/RegisterPage'
import DashboardPage    from '@/pages/dashboard/DashboardPage'
import DecksPage        from '@/pages/decks/DecksPage'
import DeckDetailPage   from '@/pages/decks/DeckDetailPage'
import ReviewPage       from '@/pages/decks/ReviewPage'
import CardEditorPage   from '@/pages/decks/CardEditorPage'
import ExamSetupPage    from '@/pages/exam/ExamSetupPage'
import ExamPage         from '@/pages/exam/ExamPage'
import ExamResultsPage  from '@/pages/exam/ExamResultsPage'
import ExamHistoryPage  from '@/pages/exam/ExamHistoryPage'
import AnalyticsPage    from '@/pages/analytics/AnalyticsPage'
import AiChatPage       from '@/pages/ai/AiChatPage'

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

              {/* Protected routes */}
              <Route element={<ProtectedRoute />}>
                <Route element={<AppLayout />}>
                  <Route index element={<Navigate to="/dashboard" replace />} />
                  <Route path="/dashboard"                            element={<DashboardPage />} />
                  <Route path="/decks"                                element={<DecksPage />} />
                  <Route path="/decks/:deckId"                        element={<DeckDetailPage />} />
                  <Route path="/decks/:deckId/review"                 element={<ReviewPage />} />
                  <Route path="/decks/:deckId/cards/new"              element={<CardEditorPage />} />
                  <Route path="/decks/:deckId/cards/:cardId/edit"     element={<CardEditorPage />} />
                  {/* exam/history must come before exam/:sessionId so it matches first */}
                  <Route path="/exam/history"                         element={<ExamHistoryPage />} />
                  <Route path="/exam"                                 element={<ExamSetupPage />} />
                  <Route path="/exam/:sessionId"                      element={<ExamPage />} />
                  <Route path="/exam/:sessionId/results"              element={<ExamResultsPage />} />
                  <Route path="/analytics"                            element={<AnalyticsPage />} />
                  <Route path="/ai"                                   element={<AiChatPage />} />
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
