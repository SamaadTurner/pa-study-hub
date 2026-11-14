import apiClient from './client'
import type { PaginatedResponse } from '@/types'

// The most items the backend gives out in one request
const PAGE_SIZE = 100

// Loads a whole list from an endpoint that answers in pages, by asking for one
// page after the other until the backend says that was the last one.
//
// This is for lists that belong to one user and stay small, like their own decks
// or the cards of one deck. The pages that show them search and count on the
// whole list, so they need all of it. A list that can grow without limit (the
// public decks) is loaded a page at a time instead.
export async function fetchAllPages<T>(url: string): Promise<T[]> {
  const items: T[] = []
  let page = 0
  let morePages = true

  while (morePages) {
    const { data } = await apiClient.get<PaginatedResponse<T>>(url, {
      params: { page, size: PAGE_SIZE },
    })
    items.push(...(data.content || []))

    // Only go on when the backend clearly says there is more. An answer without
    // the "last" field ends the loop instead of asking forever.
    morePages = data.last === false
    page += 1
  }

  return items
}
