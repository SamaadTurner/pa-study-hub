import type { OcclusionBox } from '@/types'
import { usePicture } from '@/hooks/usePicture'

interface Props {
  // A saved picture (loaded from the server), or a local one (the editor's preview)
  imageId?: string | null
  imageUrl?: string | null
  boxes: OcclusionBox[]
  // The box this card asks for (1 = first); null covers every box the same way
  activeIndex: number | null
  revealed: boolean
  className?: string
}

/**
 * A picture card's picture. Every box covers its part of the picture. The box the
 * card asks for is orange with a question mark; once revealed it opens up, so the
 * part underneath shows with an outline and the label. The other boxes stay covered.
 */
export default function PictureWithBoxes({ imageId, imageUrl, boxes, activeIndex, revealed, className = '' }: Props) {
  const loaded = usePicture(imageUrl ? null : imageId)
  const src = imageUrl ?? loaded.url
  const active = activeIndex !== null ? boxes[activeIndex - 1] : undefined

  if (!src) {
    return (
      <div className={`flex items-center justify-center bg-gray-100 rounded-lg h-48 text-sm text-gray-600 ${className}`}>
        {loaded.failed ? 'The picture could not be loaded.' : 'Loading picture...'}
      </div>
    )
  }

  return (
    <figure className={className}>
      <div className="relative inline-block w-full">
        <img src={src} alt="Picture for this card" className="block w-full h-auto rounded-lg select-none" draggable={false} />
        {boxes.map((box, i) => {
          const isActive = i + 1 === activeIndex
          const open = isActive && revealed
          return (
            <div
              key={i}
              aria-hidden="true"
              className={`absolute flex items-center justify-center rounded-sm text-white font-bold
                ${open ? 'border-[3px] border-orange-500 bg-transparent'
                  : isActive ? 'bg-orange-500 border-2 border-orange-700'
                  : 'bg-slate-500 border border-slate-700'}`}
              style={{
                left: `${box.x * 100}%`,
                top: `${box.y * 100}%`,
                width: `${box.width * 100}%`,
                height: `${box.height * 100}%`,
              }}
            >
              {isActive && !revealed && '?'}
            </div>
          )
        })}
      </div>
      {/* What the boxes mean, for screen readers and as the answer for everyone */}
      {activeIndex !== null && (
        <figcaption className="mt-2 text-sm text-gray-700">
          {revealed ? (
            active?.label ? <>Answer: <strong className="text-gray-900">{active.label}</strong></>
              : 'The highlighted part is now uncovered.'
          ) : (
            <span className="sr-only">Box {activeIndex} of {boxes.length} is highlighted.</span>
          )}
        </figcaption>
      )}
    </figure>
  )
}
