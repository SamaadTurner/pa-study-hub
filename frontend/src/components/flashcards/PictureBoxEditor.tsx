import { useRef, useState } from 'react'
import type { KeyboardEvent, PointerEvent } from 'react'
import type { OcclusionBox } from '@/types'
import Button from '@/components/common/Button'
import {
  MAX_BOXES, MAX_LABEL_LENGTH, boxFromPoints, centeredBox, isBigEnough, moveBox, pointIn, resizeBox,
  type Point,
} from '@/utils/occlusion'

interface Props {
  imageUrl: string
  boxes: OcclusionBox[]
  onChange: (boxes: OcclusionBox[]) => void
}

// What a pointer that is down is doing: drawing a new box, or moving one
type Drag =
  | { kind: 'draw'; start: Point; current: Point }
  | { kind: 'move'; index: number; start: Point; original: OcclusionBox }

const STEP = 0.01

/**
 * Draw boxes over the parts of a picture to hide. Drag on the picture to draw a
 * box, drag a box to move it. Without a mouse: Add box, then the arrow keys move
 * the selected box, Shift and the arrows change its size, Delete removes it.
 */
export default function PictureBoxEditor({ imageUrl, boxes, onChange }: Props) {
  const surfaceRef = useRef<HTMLDivElement>(null)
  const [drag, setDrag] = useState<Drag | null>(null)
  const [selected, setSelected] = useState<number | null>(null)
  const full = boxes.length >= MAX_BOXES

  function pointer(event: PointerEvent): Point {
    return pointIn(surfaceRef.current!.getBoundingClientRect(), event.clientX, event.clientY)
  }

  function handlePointerDown(event: PointerEvent<HTMLDivElement>) {
    if (event.button !== 0) return
    const start = pointer(event)
    const target = (event.target as HTMLElement).closest('[data-box]')
    // Focus for the arrow keys, but without the browser scrolling the picture into
    // view: a scroll in the middle of a drag put the box somewhere else
    event.currentTarget.focus({ preventScroll: true })
    event.currentTarget.setPointerCapture(event.pointerId)
    if (target) {
      const index = Number(target.getAttribute('data-box'))
      setSelected(index)
      setDrag({ kind: 'move', index, start, original: boxes[index] })
    } else if (!full) {
      setDrag({ kind: 'draw', start, current: start })
    }
  }

  function handlePointerMove(event: PointerEvent<HTMLDivElement>) {
    if (!drag) return
    const now = pointer(event)
    if (drag.kind === 'draw') {
      setDrag({ ...drag, current: now })
    } else {
      const moved = moveBox(drag.original, now.x - drag.start.x, now.y - drag.start.y)
      onChange(boxes.map((box, i) => (i === drag.index ? moved : box)))
    }
  }

  function handlePointerUp() {
    if (drag?.kind === 'draw') {
      const box = boxFromPoints(drag.start, drag.current)
      // A tap on the picture is not a box
      if (isBigEnough(box)) {
        onChange([...boxes, { ...box, label: '' }])
        setSelected(boxes.length)
      }
    }
    setDrag(null)
  }

  function handleKeyDown(event: KeyboardEvent<HTMLDivElement>) {
    if (selected === null || !boxes[selected]) return
    const moves: Record<string, [number, number]> = {
      ArrowLeft: [-STEP, 0], ArrowRight: [STEP, 0], ArrowUp: [0, -STEP], ArrowDown: [0, STEP],
    }
    if (moves[event.key]) {
      event.preventDefault()
      const [dx, dy] = moves[event.key]
      const box = boxes[selected]
      const changed = event.shiftKey ? resizeBox(box, dx, dy) : moveBox(box, dx, dy)
      onChange(boxes.map((b, i) => (i === selected ? changed : b)))
    } else if (event.key === 'Delete' || event.key === 'Backspace') {
      event.preventDefault()
      remove(selected)
    }
  }

  function addBox() {
    onChange([...boxes, { ...centeredBox(), label: '' }])
    setSelected(boxes.length)
    surfaceRef.current?.focus({ preventScroll: true })
  }

  function remove(index: number) {
    onChange(boxes.filter((_, i) => i !== index))
    setSelected(null)
  }

  function setLabel(index: number, label: string) {
    onChange(boxes.map((box, i) => (i === index ? { ...box, label } : box)))
  }

  const preview = drag?.kind === 'draw' ? boxFromPoints(drag.start, drag.current) : null

  return (
    <div className="space-y-3">
      <p className="text-xs text-gray-600">
        Drag on the picture to cover each part you want to learn. Every box becomes its own card.
        Drag a box to move it. With a keyboard: Add box, then arrow keys move it, Shift + arrows resize it, Delete removes it.
      </p>

      <div
        ref={surfaceRef}
        tabIndex={0}
        role="application"
        aria-label={`Picture with ${boxes.length} ${boxes.length === 1 ? 'box' : 'boxes'}${selected !== null ? `, box ${selected + 1} selected` : ''}`}
        className="relative w-full select-none rounded-lg border border-gray-300 focus:outline-none focus:ring-2 focus:ring-primary-500 cursor-crosshair"
        // Drawing must not scroll the page on a phone
        style={{ touchAction: 'none' }}
        // The normal focus on press would scroll the page; handlePointerDown focuses instead
        onMouseDown={e => e.preventDefault()}
        onPointerDown={handlePointerDown}
        onPointerMove={handlePointerMove}
        onPointerUp={handlePointerUp}
        onPointerCancel={() => setDrag(null)}
        onKeyDown={handleKeyDown}
      >
        <img src={imageUrl} alt="The picture to cover" className="block w-full h-auto rounded-lg pointer-events-none" draggable={false} />
        {boxes.map((box, i) => (
          <div
            key={i}
            data-box={i}
            className={`absolute flex items-center justify-center text-white text-sm font-bold cursor-move
              ${selected === i ? 'bg-orange-500/90 border-2 border-orange-800' : 'bg-slate-600/80 border border-slate-800'}`}
            style={{ left: `${box.x * 100}%`, top: `${box.y * 100}%`, width: `${box.width * 100}%`, height: `${box.height * 100}%` }}
          >
            {i + 1}
          </div>
        ))}
        {preview && (
          <div
            className="absolute border-2 border-dashed border-orange-600 bg-orange-300/40 pointer-events-none"
            style={{ left: `${preview.x * 100}%`, top: `${preview.y * 100}%`, width: `${preview.width * 100}%`, height: `${preview.height * 100}%` }}
          />
        )}
      </div>

      <div className="flex flex-wrap items-center gap-3">
        <Button type="button" variant="secondary" size="sm" onClick={addBox} disabled={full}>Add box</Button>
        <span className="text-xs text-gray-600">
          {full ? `${MAX_BOXES} boxes is the most for one picture.`
            : boxes.length === 0 ? 'No boxes yet.'
            : `Makes ${boxes.length} ${boxes.length === 1 ? 'card' : 'cards'}.`}
        </span>
      </div>

      {boxes.length > 0 && (
        <ol className="space-y-2">
          {boxes.map((box, i) => (
            <li key={i} className={`flex items-center gap-2 rounded-lg p-2 ${selected === i ? 'bg-orange-50' : ''}`}>
              <button
                type="button"
                onClick={() => { setSelected(i); surfaceRef.current?.focus() }}
                className="w-16 flex-shrink-0 text-left text-sm font-semibold text-gray-700 hover:underline"
                aria-label={`Select box ${i + 1}`}
              >
                Box {i + 1}
              </button>
              <input
                type="text"
                value={box.label ?? ''}
                onChange={e => setLabel(i, e.target.value)}
                maxLength={MAX_LABEL_LENGTH}
                aria-label={`Answer for box ${i + 1}`}
                placeholder="Answer (what is under the box)"
                className="flex-1 min-w-0 px-3 py-1.5 border border-gray-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-primary-500"
              />
              <button
                type="button"
                onClick={() => remove(i)}
                className="text-sm text-red-700 hover:underline flex-shrink-0"
                aria-label={`Remove box ${i + 1}`}
              >
                Remove
              </button>
            </li>
          ))}
        </ol>
      )}
    </div>
  )
}
