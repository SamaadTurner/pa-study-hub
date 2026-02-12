import { ReactNode } from 'react'

interface Props {
  children: ReactNode
  className?: string
  header?: ReactNode
  footer?: ReactNode
  padding?: boolean
}

export default function Card({ children, className = '', header, footer, padding = true }: Props) {
  return (
    <div className={`bg-white rounded-xl border border-gray-200 shadow-sm ${className}`}>
      {header && (
        <div className="px-6 py-4 border-b border-gray-200 font-semibold text-gray-900">
          {header}
        </div>
      )}
      <div className={padding ? 'p-6' : ''}>{children}</div>
      {footer && (
        <div className="px-6 py-4 border-t border-gray-200 bg-gray-50 rounded-b-xl">
          {footer}
        </div>
      )}
    </div>
  )
}
