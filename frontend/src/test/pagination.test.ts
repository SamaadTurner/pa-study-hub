import { describe, it, expect, vi, beforeEach } from 'vitest'
import apiClient from '@/api/client'
import { fetchAllPages } from '@/api/pagination'

// The real client would send requests. Here "get" is replaced so each test can
// say what the backend answers.
vi.mock('@/api/client', () => ({
  default: { get: vi.fn() },
}))

const get = vi.mocked(apiClient.get)

// What the backend sends for one page
function page(content: string[], last: boolean) {
  return { data: { content, last, totalElements: 0, totalPages: 0, number: 0, size: 100 } }
}

describe('fetchAllPages', () => {
  beforeEach(() => {
    get.mockReset()
  })

  it('returns the items of a single page', async () => {
    get.mockResolvedValueOnce(page(['a', 'b'], true))

    const items = await fetchAllPages<string>('/decks')

    expect(items).toEqual(['a', 'b'])
    expect(get).toHaveBeenCalledTimes(1)
    expect(get).toHaveBeenCalledWith('/decks', { params: { page: 0, size: 100 } })
  })

  it('asks for the next page until the backend says it was the last', async () => {
    get.mockResolvedValueOnce(page(['a', 'b'], false))
    get.mockResolvedValueOnce(page(['c', 'd'], false))
    get.mockResolvedValueOnce(page(['e'], true))

    const items = await fetchAllPages<string>('/decks')

    expect(items).toEqual(['a', 'b', 'c', 'd', 'e'])
    expect(get).toHaveBeenCalledTimes(3)
    expect(get).toHaveBeenLastCalledWith('/decks', { params: { page: 2, size: 100 } })
  })

  it('returns an empty list when there is nothing', async () => {
    get.mockResolvedValueOnce(page([], true))

    expect(await fetchAllPages<string>('/decks')).toEqual([])
  })

  it('stops after one request when the answer has no "last" field', async () => {
    get.mockResolvedValueOnce({ data: { content: ['a'] } })

    expect(await fetchAllPages<string>('/decks')).toEqual(['a'])
    expect(get).toHaveBeenCalledTimes(1)
  })

  it('passes an error on to the caller', async () => {
    get.mockResolvedValueOnce(page(['a'], false))
    get.mockRejectedValueOnce({ status: 500, detail: 'boom' })

    await expect(fetchAllPages<string>('/decks')).rejects.toEqual({ status: 500, detail: 'boom' })
  })
})
