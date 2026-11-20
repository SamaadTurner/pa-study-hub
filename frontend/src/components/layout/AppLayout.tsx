import { Suspense } from 'react'
import { Outlet } from 'react-router-dom'
import ConfirmEmailBanner from './ConfirmEmailBanner'
import Navbar from './Navbar'
import LoadingSpinner from '@/components/common/LoadingSpinner'

export default function AppLayout() {
  return (
    <div className="min-h-screen bg-gray-50">
      <Navbar />
      <ConfirmEmailBanner />
      <main className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-6">
        {/* The pages are loaded on demand (see App.tsx). Suspense sits here, inside
            the layout, so the top bar stays on screen while a page is fetched. */}
        <Suspense
          fallback={
            <div className="flex justify-center py-16">
              <LoadingSpinner message="Loading page..." />
            </div>
          }
        >
          <Outlet />
        </Suspense>
      </main>
    </div>
  )
}
