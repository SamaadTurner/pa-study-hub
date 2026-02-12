interface Props {
  page: number         // 0-indexed (Spring pagination)
  totalPages: number
  totalElements: number
  pageSize: number
  onPageChange: (page: number) => void
}

export default function Pagination({
  page,
  totalPages,
  totalElements,
  pageSize,
  onPageChange,
}: Props) {
  if (totalPages <= 1) return null

  const start = page * pageSize + 1
  const end   = Math.min((page + 1) * pageSize, totalElements)

  const pages = Array.from({ length: totalPages }, (_, i) => i)
  const visible = pages.filter(p => Math.abs(p - page) <= 2)

  return (
    <div className="flex items-center justify-between mt-4">
      <p className="text-sm text-gray-600">
        Showing <span className="font-medium">{start}</span>–<span className="font-medium">{end}</span>{' '}
        of <span className="font-medium">{totalElements}</span>
      </p>

      <nav className="flex items-center gap-1" aria-label="Pagination">
        <PagerButton
          onClick={() => onPageChange(page - 1)}
          disabled={page === 0}
          aria-label="Previous page"
        >
          ←
        </PagerButton>

        {visible[0] > 0 && (
          <>
            <PagerButton onClick={() => onPageChange(0)}>1</PagerButton>
            {visible[0] > 1 && <span className="px-2 text-gray-400">…</span>}
          </>
        )}

        {visible.map(p => (
          <PagerButton
            key={p}
            onClick={() => onPageChange(p)}
            active={p === page}
          >
            {p + 1}
          </PagerButton>
        ))}

        {visible[visible.length - 1] < totalPages - 1 && (
          <>
            {visible[visible.length - 1] < totalPages - 2 && (
              <span className="px-2 text-gray-400">…</span>
            )}
            <PagerButton onClick={() => onPageChange(totalPages - 1)}>
              {totalPages}
            </PagerButton>
          </>
        )}

        <PagerButton
          onClick={() => onPageChange(page + 1)}
          disabled={page === totalPages - 1}
          aria-label="Next page"
        >
          →
        </PagerButton>
      </nav>
    </div>
  )
}

function PagerButton({
  children,
  onClick,
  disabled = false,
  active = false,
  ...rest
}: {
  children: React.ReactNode
  onClick: () => void
  disabled?: boolean
  active?: boolean
  'aria-label'?: string
}) {
  return (
    <button
      onClick={onClick}
      disabled={disabled}
      {...rest}
      className={`min-w-[2rem] h-8 px-2 rounded text-sm font-medium transition-colors
        ${active
          ? 'bg-primary-600 text-white'
          : 'text-gray-700 hover:bg-gray-100 disabled:opacity-40 disabled:cursor-not-allowed'
        }`}
    >
      {children}
    </button>
  )
}
