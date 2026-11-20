import { useEffect, useState } from 'react'
import { getPicture } from '@/api/decks'

// One download per picture while the app is open. The boxes of one picture are
// separate cards, so a study session shows the same picture many times in a row.
const cache = new Map<string, Promise<string>>()

function loadPicture(imageId: string): Promise<string> {
  let url = cache.get(imageId)
  if (!url) {
    url = getPicture(imageId).then(blob => URL.createObjectURL(blob))
    // A failed download may work next time (the server was waking up)
    url.catch(() => cache.delete(imageId))
    cache.set(imageId, url)
  }
  return url
}

// The picture as a URL an <img> can show, or null while it loads
export function usePicture(imageId: string | null | undefined): { url: string | null; failed: boolean } {
  const [url, setUrl] = useState<string | null>(null)
  const [failed, setFailed] = useState(false)

  useEffect(() => {
    if (!imageId) return
    let ignore = false
    setUrl(null)
    setFailed(false)
    loadPicture(imageId)
      .then(loaded => { if (!ignore) setUrl(loaded) })
      .catch(() => { if (!ignore) setFailed(true) })
    return () => { ignore = true }
  }, [imageId])

  return { url, failed }
}
