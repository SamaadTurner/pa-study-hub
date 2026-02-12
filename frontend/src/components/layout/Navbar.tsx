import { Link, useLocation } from 'react-router-dom'
import { useAuth } from '@/contexts/AuthContext'

interface NavItem {
  label: string
  path: string
  icon: string
}

const NAV_ITEMS: NavItem[] = [
  { label: 'Dashboard',   path: '/dashboard', icon: '🏠' },
  { label: 'Flashcards',  path: '/decks',     icon: '🃏' },
  { label: 'Exams',       path: '/exam',      icon: '📝' },
  { label: 'Analytics',   path: '/analytics', icon: '📊' },
  { label: 'AI Tutor',    path: '/ai',        icon: '✨' },
]

export default function Navbar() {
  const { user, logout } = useAuth()
  const location = useLocation()

  return (
    <nav className="bg-white border-b border-gray-200 sticky top-0 z-40">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex items-center justify-between h-16">
          {/* Logo */}
          <Link to="/dashboard" className="flex items-center gap-2 font-bold text-primary-700 text-lg">
            <span className="text-2xl">🩺</span>
            <span className="hidden sm:block">PA Study Hub</span>
          </Link>

          {/* Nav links */}
          <div className="hidden md:flex items-center gap-1">
            {NAV_ITEMS.map(item => {
              const isActive = location.pathname.startsWith(item.path)
              return (
                <Link
                  key={item.path}
                  to={item.path}
                  className={`flex items-center gap-1.5 px-3 py-2 rounded-lg text-sm font-medium
                    transition-colors
                    ${isActive
                      ? 'bg-primary-50 text-primary-700'
                      : 'text-gray-600 hover:bg-gray-100 hover:text-gray-900'
                    }`}
                >
                  <span>{item.icon}</span>
                  {item.label}
                </Link>
              )
            })}
          </div>

          {/* User menu */}
          <div className="flex items-center gap-3">
            {user && (
              <span className="hidden sm:block text-sm text-gray-600">
                {user.firstName}
              </span>
            )}
            <button
              onClick={logout}
              className="text-sm text-gray-500 hover:text-gray-900 px-3 py-2 rounded-lg
                hover:bg-gray-100 transition-colors font-medium"
            >
              Sign out
            </button>
          </div>
        </div>

        {/* Mobile nav */}
        <div className="md:hidden flex gap-1 pb-2 overflow-x-auto">
          {NAV_ITEMS.map(item => {
            const isActive = location.pathname.startsWith(item.path)
            return (
              <Link
                key={item.path}
                to={item.path}
                className={`flex items-center gap-1 px-3 py-1.5 rounded-lg text-xs font-medium
                  whitespace-nowrap transition-colors
                  ${isActive
                    ? 'bg-primary-50 text-primary-700'
                    : 'text-gray-600 hover:bg-gray-100'
                  }`}
              >
                <span>{item.icon}</span>
                {item.label}
              </Link>
            )
          })}
        </div>
      </div>
    </nav>
  )
}
