interface Props {
  message?: string
  size?: 'sm' | 'md' | 'lg'
}

const SIZE = {
  sm: 'h-5 w-5 border-2',
  md: 'h-8 w-8 border-2',
  lg: 'h-12 w-12 border-4',
}

export default function LoadingSpinner({ message, size = 'md' }: Props) {
  return (
    <div className="flex flex-col items-center gap-3">
      <div
        className={`${SIZE[size]} rounded-full border-primary-200 border-t-primary-600 animate-spin`}
      />
      {message && <p className="text-sm text-gray-500">{message}</p>}
    </div>
  )
}
