import { useState, useEffect } from 'react'
import { Link } from 'react-router-dom'
import { getStudyPlan } from '@/api/ai'
import { getAnalytics } from '@/api/progress'
import type { StudyPlanResponse, ApiError } from '@/types'
import Button from '@/components/common/Button'
import LoadingSpinner from '@/components/common/LoadingSpinner'
import { useNotification } from '@/contexts/NotificationContext'
import { getCategoryLabel } from '@/utils/categoryColors'

export default function AiStudyPlanPage() {
  const { notify } = useNotification()

  const [loadingAnalytics, setLoadingAnalytics] = useState(true)
  const [weakCategories, setWeakCategories] = useState<string[]>([])
  const [daysUntilExam, setDaysUntilExam] = useState(30)
  const [dailyMinutes, setDailyMinutes] = useState(60)
  const [generating, setGenerating] = useState(false)
  const [studyPlan, setStudyPlan] = useState<StudyPlanResponse | null>(null)

  useEffect(() => {
    loadWeakCategories()
  }, [])

  async function loadWeakCategories() {
    try {
      const analytics = await getAnalytics()
      // Get categories with accuracy < 70%, sorted by accuracy ascending
      const weak = Object.entries(analytics.categoryAccuracy)
        .filter(([_, accuracy]) => accuracy < 70)
        .sort((a, b) => a[1] - b[1])
        .map(([category]) => category)
        .slice(0, 5) // Top 5 weakest

      setWeakCategories(weak.length > 0 ? weak : Object.keys(analytics.categoryAccuracy).slice(0, 3))
    } catch (err) {
      // If analytics not available, use default categories
      setWeakCategories(['CARDIOLOGY', 'PHARMACOLOGY', 'ANATOMY'])
    } finally {
      setLoadingAnalytics(false)
    }
  }

  async function handleGenerate() {
    if (weakCategories.length === 0) {
      notify('error', 'Please select at least one category to focus on')
      return
    }

    setGenerating(true)
    try {
      const plan = await getStudyPlan({ weakCategories, daysUntilExam, dailyMinutes })
      setStudyPlan(plan)
      notify('success', 'Claude generated your personalized study plan')
    } catch (err) {
      notify('error', (err as ApiError)?.detail ?? 'Failed to generate study plan')
    } finally {
      setGenerating(false)
    }
  }

  function toggleCategory(category: string) {
    setWeakCategories(prev =>
      prev.includes(category)
        ? prev.filter(c => c !== category)
        : [...prev, category]
    )
  }

  if (loadingAnalytics) {
    return (
      <div className="flex items-center justify-center py-24">
        <LoadingSpinner message="Loading your performance data..." size="lg" />
      </div>
    )
  }

  return (
    <div className="max-w-4xl mx-auto space-y-8">
      {/* Header */}
      <div className="space-y-3">
        <div>
          <h1 className="text-3xl font-bold text-gray-900">AI Study Plan Generator ✨</h1>
          <p className="text-gray-600 mt-2">
            Get a personalized study schedule based on your weak areas and available study time
          </p>
        </div>

        {/* AI Tools Navigation */}
        <div className="flex gap-2">
          <Link
            to="/ai"
            className="flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-xs font-medium
              text-gray-600 hover:bg-gray-100 border border-gray-200"
          >
            💬 Chat
          </Link>
          <Link
            to="/ai/generate"
            className="flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-xs font-medium
              text-gray-600 hover:bg-gray-100 border border-gray-200"
          >
            🃏 Generate Flashcards
          </Link>
          <Link
            to="/ai/study-plan"
            className="flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-xs font-medium
              bg-primary-50 text-primary-700 border border-primary-200"
          >
            📅 Study Plan
          </Link>
        </div>
      </div>

      {/* Configuration */}
      {!studyPlan && (
        <div className="bg-white border border-gray-200 rounded-xl p-6 space-y-6">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-3">
              Focus Categories (select your weak areas)
            </label>
            <div className="grid grid-cols-2 md:grid-cols-3 gap-2">
              {[
                'CARDIOLOGY', 'PULMONOLOGY', 'GASTROENTEROLOGY', 'MUSCULOSKELETAL',
                'NEUROLOGY', 'PSYCHIATRY', 'DERMATOLOGY', 'EENT', 'ENDOCRINOLOGY',
                'HEMATOLOGY', 'INFECTIOUS_DISEASE', 'NEPHROLOGY', 'REPRODUCTIVE',
                'PEDIATRICS', 'EMERGENCY_MEDICINE', 'PHARMACOLOGY', 'ANATOMY'
              ].map(cat => (
                <button
                  key={cat}
                  onClick={() => toggleCategory(cat)}
                  className={`px-3 py-2 rounded-lg text-xs font-medium transition-colors text-left
                    ${weakCategories.includes(cat)
                      ? 'bg-primary-600 text-white'
                      : 'bg-gray-100 text-gray-700 hover:bg-gray-200'
                    }`}
                >
                  {getCategoryLabel(cat)}
                </button>
              ))}
            </div>
            {weakCategories.length > 0 && (
              <p className="text-xs text-gray-500 mt-2">
                Selected {weakCategories.length} {weakCategories.length === 1 ? 'category' : 'categories'}
              </p>
            )}
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">
                Days Until Exam
              </label>
              <input
                type="number"
                min="1"
                max="365"
                value={daysUntilExam}
                onChange={e => setDaysUntilExam(parseInt(e.target.value) || 30)}
                className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm
                  focus:outline-none focus:ring-2 focus:ring-primary-500"
              />
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">
                Daily Study Time (minutes)
              </label>
              <input
                type="number"
                min="15"
                max="480"
                step="15"
                value={dailyMinutes}
                onChange={e => setDailyMinutes(parseInt(e.target.value) || 60)}
                className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm
                  focus:outline-none focus:ring-2 focus:ring-primary-500"
              />
            </div>
          </div>

          <Button
            onClick={handleGenerate}
            disabled={generating || weakCategories.length === 0}
            className="w-full"
          >
            {generating ? (
              <>
                <LoadingSpinner size="sm" className="mr-2" />
                Claude is creating your study plan...
              </>
            ) : (
              'Generate Study Plan with Claude AI ✨'
            )}
          </Button>
        </div>
      )}

      {/* Generated Study Plan */}
      {studyPlan && (
        <div className="space-y-4">
          <div className="flex items-center justify-between">
            <h2 className="text-xl font-bold text-gray-900">Your Personalized Study Plan</h2>
            <Button
              variant="secondary"
              onClick={() => setStudyPlan(null)}
            >
              Generate New Plan
            </Button>
          </div>

          <div className="bg-white border border-gray-200 rounded-xl p-6">
            <div className="grid grid-cols-3 gap-4 mb-6 pb-6 border-b border-gray-200">
              <div className="text-center">
                <p className="text-2xl font-bold text-gray-900">{studyPlan.daysUntilExam}</p>
                <p className="text-xs text-gray-500 mt-1">Days until exam</p>
              </div>
              <div className="text-center">
                <p className="text-2xl font-bold text-gray-900">{studyPlan.dailyMinutes}m</p>
                <p className="text-xs text-gray-500 mt-1">Daily study time</p>
              </div>
              <div className="text-center">
                <p className="text-2xl font-bold text-gray-900">{studyPlan.weakCategories.length}</p>
                <p className="text-xs text-gray-500 mt-1">Focus areas</p>
              </div>
            </div>

            <div className="mb-4">
              <h3 className="text-sm font-semibold text-gray-700 mb-2">Focus Categories:</h3>
              <div className="flex flex-wrap gap-2">
                {studyPlan.weakCategories.map(cat => (
                  <span
                    key={cat}
                    className="px-3 py-1 bg-primary-100 text-primary-700 text-xs font-medium rounded-full"
                  >
                    {getCategoryLabel(cat)}
                  </span>
                ))}
              </div>
            </div>

            <div className="prose prose-sm max-w-none">
              <h3 className="text-sm font-semibold text-gray-700 mb-3">Study Schedule:</h3>
              <div className="whitespace-pre-wrap text-sm text-gray-700 leading-relaxed">
                {studyPlan.studyPlan}
              </div>
            </div>
          </div>

          <div className="bg-blue-50 border border-blue-200 rounded-xl p-4">
            <p className="text-sm text-blue-800">
              <strong>Pro tip:</strong> Stick to this plan consistently, review difficult topics in spaced intervals,
              and take practice exams to track your progress. You can regenerate this plan anytime as your weak areas change.
            </p>
          </div>
        </div>
      )}

      {/* Empty State */}
      {!generating && !studyPlan && (
        <div className="text-center py-12">
          <p className="text-6xl mb-4">📅</p>
          <p className="text-gray-500">
            Configure your study parameters above to get a personalized plan from Claude
          </p>
        </div>
      )}
    </div>
  )
}
