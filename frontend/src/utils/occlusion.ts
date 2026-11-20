import type { OcclusionBox } from '@/types'

// Same limits as the server (OcclusionBoxes.java)
export const MAX_BOXES = 20
export const MAX_LABEL_LENGTH = 100
// A drag smaller than this is a tap, not a box (the server's minimum is 0.01)
export const MIN_BOX_SIZE = 0.02

export interface Point {
  x: number
  y: number
}

const clamp = (value: number, min: number, max: number) => Math.min(max, Math.max(min, value))

// Keeps numbers short in the saved JSON and avoids 0.30000000000000004
const round = (value: number) => Math.round(value * 10000) / 10000

// The box between two corners, whichever way the user dragged
export function boxFromPoints(a: Point, b: Point): OcclusionBox {
  const left = clamp(Math.min(a.x, b.x), 0, 1)
  const top = clamp(Math.min(a.y, b.y), 0, 1)
  const right = clamp(Math.max(a.x, b.x), 0, 1)
  const bottom = clamp(Math.max(a.y, b.y), 0, 1)
  return { x: round(left), y: round(top), width: round(right - left), height: round(bottom - top) }
}

export function isBigEnough(box: OcclusionBox): boolean {
  return box.width >= MIN_BOX_SIZE && box.height >= MIN_BOX_SIZE
}

// Moves a box, keeping all of it on the picture
export function moveBox(box: OcclusionBox, dx: number, dy: number): OcclusionBox {
  return {
    ...box,
    x: round(clamp(box.x + dx, 0, 1 - box.width)),
    y: round(clamp(box.y + dy, 0, 1 - box.height)),
  }
}

// Grows or shrinks a box from its top left corner, keeping it on the picture
export function resizeBox(box: OcclusionBox, dw: number, dh: number): OcclusionBox {
  return {
    ...box,
    width: round(clamp(box.width + dw, MIN_BOX_SIZE, 1 - box.x)),
    height: round(clamp(box.height + dh, MIN_BOX_SIZE, 1 - box.y)),
  }
}

// A box in the middle of the picture, for adding one without a mouse
export function centeredBox(): OcclusionBox {
  return { x: 0.35, y: 0.45, width: 0.3, height: 0.1 }
}

// Where a pointer is on an element, as fractions of its size
export function pointIn(rect: { left: number; top: number; width: number; height: number }, clientX: number, clientY: number): Point {
  return {
    x: clamp((clientX - rect.left) / rect.width, 0, 1),
    y: clamp((clientY - rect.top) / rect.height, 0, 1),
  }
}
