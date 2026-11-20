// Pictures are stored in the database, so they are made small in the browser
// before upload: at most 1600 pixels on the longest side, saved as JPEG. That is
// sharp enough for an ECG or an X-ray on any screen and usually 100 to 300 KB.
// Saving it again also drops the photo's metadata (camera, GPS location).
export const MAX_PICTURE_SIDE = 1600
export const MAX_UPLOAD_BYTES = 1024 * 1024
const JPEG_QUALITY = 0.85

// The size to draw a picture at so its longest side is at most maxSide
export function shrunkSize(width: number, height: number, maxSide = MAX_PICTURE_SIDE): { width: number; height: number } {
  const scale = Math.min(1, maxSide / Math.max(width, height))
  return { width: Math.max(1, Math.round(width * scale)), height: Math.max(1, Math.round(height * scale)) }
}

function loadImage(url: string): Promise<HTMLImageElement> {
  return new Promise((resolve, reject) => {
    const image = new Image()
    image.onload = () => resolve(image)
    image.onerror = () => reject(new Error('This file could not be opened as a picture. Try a PNG or JPEG.'))
    image.src = url
  })
}

function toJpeg(canvas: HTMLCanvasElement, quality: number): Promise<Blob | null> {
  return new Promise(resolve => canvas.toBlob(resolve, 'image/jpeg', quality))
}

export async function shrinkPicture(file: File): Promise<Blob> {
  if (!file.type.startsWith('image/')) {
    throw new Error('Choose a picture file (PNG or JPEG).')
  }
  const url = URL.createObjectURL(file)
  try {
    const image = await loadImage(url)
    const size = shrunkSize(image.naturalWidth, image.naturalHeight)
    const canvas = document.createElement('canvas')
    canvas.width = size.width
    canvas.height = size.height
    const context = canvas.getContext('2d')
    if (!context) throw new Error('This browser could not prepare the picture.')
    // JPEG has no transparency; a white background keeps diagrams readable
    context.fillStyle = '#ffffff'
    context.fillRect(0, 0, size.width, size.height)
    context.drawImage(image, 0, 0, size.width, size.height)

    let blob = await toJpeg(canvas, JPEG_QUALITY)
    // A very detailed picture can still be big; one more try at lower quality
    if (blob && blob.size > MAX_UPLOAD_BYTES) blob = await toJpeg(canvas, 0.6)
    if (!blob) throw new Error('This browser could not prepare the picture.')
    if (blob.size > MAX_UPLOAD_BYTES) throw new Error('This picture is too detailed to upload. Try cropping it first.')
    return blob
  } finally {
    URL.revokeObjectURL(url)
  }
}
