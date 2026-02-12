import { useNavigate } from 'react-router-dom'

interface QuickAction {
  label: string
  icon: string
  path: string
  description: string
  color: string
}

const ACTIONS: QuickAction[] = [
  {
    label: 'Review Flashcards',
    icon: '🃏',
    path: '/decks',
    description: 'Continue spaced repetition',
    color: 'hover:bg-blue-50 hover:border-blue-300',
  },
  {
    label: 'Start Practice Exam',
    icon: '📝',
    path: '/exam',
    description: 'PANCE-style questions',
    color: 'hover:bg-purple-50 hover:border-purple-300',
  },
  {
    label: 'Create New Deck',
    icon: '➕',
    path: '/decks?create=true',
    description: 'Add your own flashcards',
    color: 'hover:bg-green-50 hover:border-green-300',
  },
  {
    label: 'AI Tutor',
    icon: '✨',
    path: '/ai',
    description: 'Chat or generate cards',
    color: 'hover:bg-yellow-50 hover:border-yellow-300',
  },
]

export default function QuickActions() {
  const navigate = useNavigate()

  return (
    <div className="bg-white border border-gray-200 rounded-xl p-5">
      <h3 className="text-sm font-semibold text-gray-700 mb-3">Quick Actions</h3>
      <div className="grid grid-cols-2 sm:grid-cols-4 gap-3">
        {ACTIONS.map(action => (
          <button
            key={action.path}
            onClick={() => navigate(action.path)}
            className={`flex flex-col items-center gap-2 p-3 rounded-xl border border-gray-200
              transition-all text-center ${action.color}`}
          >
            <span className="text-2xl">{action.icon}</span>
            <div>
              <p className="text-xs font-semibold text-gray-900 leading-tight">{action.label}</p>
              <p className="text-xs text-gray-500 leading-tight mt-0.5">{action.description}</p>
            </div>
          </button>
        ))}
      </div>
    </div>
  )
}
