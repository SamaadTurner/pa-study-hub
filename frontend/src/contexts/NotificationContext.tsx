import {
  createContext,
  useContext,
  useState,
  useCallback,
  ReactNode,
} from 'react'

export type NotificationType = 'success' | 'error' | 'info' | 'warning'

export interface Notification {
  id: string
  type: NotificationType
  message: string
}

interface NotificationContextValue {
  notifications: Notification[]
  notify: (type: NotificationType, message: string) => void
  dismiss: (id: string) => void
}

const NotificationContext = createContext<NotificationContextValue | null>(null)

const TYPE_STYLES: Record<NotificationType, string> = {
  success: 'bg-green-50 text-green-800 border-green-200',
  error:   'bg-red-50 text-red-800 border-red-200',
  warning: 'bg-yellow-50 text-yellow-800 border-yellow-200',
  info:    'bg-blue-50 text-blue-800 border-blue-200',
}

const TYPE_ICONS: Record<NotificationType, string> = {
  success: '✓',
  error:   '✕',
  warning: '⚠',
  info:    'ℹ',
}

export function NotificationProvider({ children }: { children: ReactNode }) {
  const [notifications, setNotifications] = useState<Notification[]>([])

  const notify = useCallback((type: NotificationType, message: string) => {
    const id = crypto.randomUUID()
    setNotifications(prev => [...prev, { id, type, message }])
    setTimeout(() => {
      setNotifications(prev => prev.filter(n => n.id !== id))
    }, 4000)
  }, [])

  const dismiss = useCallback((id: string) => {
    setNotifications(prev => prev.filter(n => n.id !== id))
  }, [])

  return (
    <NotificationContext.Provider value={{ notifications, notify, dismiss }}>
      {children}
      {notifications.length > 0 && (
        <div className="fixed top-4 right-4 z-50 flex flex-col gap-2 w-80 pointer-events-none">
          {notifications.map(n => (
            <div
              key={n.id}
              className={`flex items-start gap-3 p-4 rounded-lg shadow-lg border text-sm
                font-medium animate-slide-up pointer-events-auto ${TYPE_STYLES[n.type]}`}
            >
              <span className="flex-shrink-0 font-bold">{TYPE_ICONS[n.type]}</span>
              <span className="flex-1">{n.message}</span>
              <button
                onClick={() => dismiss(n.id)}
                className="flex-shrink-0 opacity-60 hover:opacity-100 transition-opacity leading-none"
                aria-label="Dismiss notification"
              >
                ✕
              </button>
            </div>
          ))}
        </div>
      )}
    </NotificationContext.Provider>
  )
}

export function useNotification(): NotificationContextValue {
  const ctx = useContext(NotificationContext)
  if (!ctx) throw new Error('useNotification must be used within NotificationProvider')
  return ctx
}
