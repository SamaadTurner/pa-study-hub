import { useCallback, useEffect, useState } from 'react'
import { getAiStatus, type AiStatus } from '@/api/ai'

const OFF: AiStatus = { enabled: false, model: null, remainingToday: null }

// Whether the AI buttons should be shown. If the status can't be loaded the
// buttons stay hidden: the rest of the page works the same without them.
export function useAiStatus() {
  const [status, setStatus] = useState<AiStatus>(OFF)

  const refresh = useCallback(async () => {
    try {
      setStatus(await getAiStatus())
    } catch {
      setStatus(OFF)
    }
  }, [])

  useEffect(() => {
    let ignore = false
    getAiStatus()
      .then(loaded => { if (!ignore) setStatus(loaded) })
      .catch(() => { if (!ignore) setStatus(OFF) })
    return () => { ignore = true }
  }, [])

  return { ...status, refresh }
}
