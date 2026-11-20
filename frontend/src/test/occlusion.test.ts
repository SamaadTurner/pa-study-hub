import { describe, expect, it } from 'vitest'
import { boxFromPoints, isBigEnough, moveBox, pointIn, resizeBox } from '@/utils/occlusion'
import { shrunkSize } from '@/utils/pictures'

describe('picture boxes', () => {
  it('makes the same box whichever way the user drags', () => {
    const down = boxFromPoints({ x: 0.2, y: 0.3 }, { x: 0.5, y: 0.4 })
    const up = boxFromPoints({ x: 0.5, y: 0.4 }, { x: 0.2, y: 0.3 })
    expect(down).toEqual({ x: 0.2, y: 0.3, width: 0.3, height: 0.1 })
    expect(up).toEqual(down)
  })

  it('keeps a drawn box on the picture', () => {
    expect(boxFromPoints({ x: -0.2, y: 0.9 }, { x: 0.1, y: 1.4 })).toEqual({ x: 0, y: 0.9, width: 0.1, height: 0.1 })
  })

  it('treats a tap as no box', () => {
    expect(isBigEnough(boxFromPoints({ x: 0.5, y: 0.5 }, { x: 0.505, y: 0.6 }))).toBe(false)
    expect(isBigEnough({ x: 0, y: 0, width: 0.02, height: 0.02 })).toBe(true)
  })

  it('moves a box but never off the picture', () => {
    const box = { x: 0.8, y: 0.1, width: 0.15, height: 0.1, label: 'Aorta' }
    expect(moveBox(box, 0.01, 0)).toEqual({ ...box, x: 0.81 })
    expect(moveBox(box, 0.5, -0.5)).toEqual({ ...box, x: 0.85, y: 0 })
  })

  it('resizes a box within the picture and not below the minimum', () => {
    const box = { x: 0.9, y: 0.5, width: 0.05, height: 0.05 }
    expect(resizeBox(box, 0.5, 0)).toEqual({ ...box, width: 0.1 })
    expect(resizeBox(box, 0, -1)).toEqual({ ...box, height: 0.02 })
  })

  it('turns a pointer position into fractions of the element', () => {
    const rect = { left: 100, top: 50, width: 400, height: 200 }
    expect(pointIn(rect, 300, 100)).toEqual({ x: 0.5, y: 0.25 })
    expect(pointIn(rect, 0, 999)).toEqual({ x: 0, y: 1 })
  })
})

describe('picture size before upload', () => {
  it('shrinks the longest side to 1600 pixels and keeps the shape', () => {
    expect(shrunkSize(4000, 3000)).toEqual({ width: 1600, height: 1200 })
    expect(shrunkSize(1000, 5000)).toEqual({ width: 320, height: 1600 })
  })

  it('leaves small pictures as they are', () => {
    expect(shrunkSize(800, 600)).toEqual({ width: 800, height: 600 })
  })
})
