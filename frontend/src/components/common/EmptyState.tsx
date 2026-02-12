import { ReactNode } from 'react'
import Button from './Button'

interface Props {
  icon?: string
  title: string
  message?: string
  action?: {
    label: string
    onClick: () => void
  }
  children?: ReactNode
}

export default function EmptyState({ icon = '📭', title, message, action, children }: Props) {
  return (
    <div className="flex flex-col items-center justify-center py-16 px-4 text-center">
      <div className="text-5xl mb-4">{icon}</div>
      <h3 className="text-lg font-semibold text-gray-900 mb-2">{title}</h3>
      {message && <p className="text-sm text-gray-500 max-w-sm mb-6">{message}</p>}
      {action && (
        <Button onClick={action.onClick}>{action.label}</Button>
      )}
      {children}
    </div>
  )
}
