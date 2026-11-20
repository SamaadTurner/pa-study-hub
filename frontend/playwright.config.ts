import { defineConfig, devices } from '@playwright/test'
import path from 'path'
import { fileURLToPath } from 'url'

// End-to-end tests: a real browser clicking through the real frontend and backend.
//
// The only thing that has to be running already is a PostgreSQL database the
// backend can reach. The backend and the frontend are started from here if they
// are not up yet. The backend reads its database settings from the usual
// DB_HOST, DB_PORT, DB_NAME, DB_USER and DB_PASSWORD environment variables
// (application.yml has the defaults).

// The backend is started with the Gradle wrapper in the folder above this one.
// The full path is used because the wrapper has a different file name on Windows
// and Windows does not always look in the current folder for a command.
const repoRoot = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..')
const gradle = path.join(repoRoot, process.platform === 'win32' ? 'gradlew.bat' : 'gradlew')

// The specs that also run in WebKit and on an iPhone-sized screen
const MAIN_FLOWS = /(^|[\\/])(auth|decks|review|exams|cloze)\.spec\.ts$/

export default defineConfig({
  testDir: './e2e',

  // Every test registers its own user, so the tests don't share any data
  // and can safely run at the same time.
  fullyParallel: true,

  forbidOnly: !!process.env.CI,
  retries: process.env.CI ? 1 : 0,
  reporter: [['list'], ['html', { open: 'never' }]],

  use: {
    baseURL: 'http://127.0.0.1:5173',
    trace: 'on-first-retry',
    screenshot: 'only-on-failure',
  },

  projects: [
    { name: 'chromium', use: { ...devices['Desktop Chrome'] } },
    // Most students will open the app on a phone, many on an iPhone, whose browsers
    // all use Safari's engine (WebKit). These two run the main flows; the whole
    // suite runs in Chromium.
    { name: 'webkit', use: { ...devices['Desktop Safari'] }, testMatch: MAIN_FLOWS },
    { name: 'iphone', use: { ...devices['iPhone 13'] }, testMatch: MAIN_FLOWS },
  ],

  webServer: [
    {
      // --no-daemon so that stopping the tests also stops the backend
      command: `"${gradle}" :app:bootRun --no-daemon`,
      cwd: repoRoot,
      // The "dev" profile creates the demo account that auth.spec.ts logs in with,
      // and has a JWT secret for local use. These are added to the normal environment.
      env: { SPRING_PROFILES_ACTIVE: 'dev' },
      url: 'http://127.0.0.1:8080/actuator/health',
      reuseExistingServer: !process.env.CI,
      timeout: 180_000,
    },
    {
      command: 'npm run dev -- --host 127.0.0.1 --strictPort',
      url: 'http://127.0.0.1:5173',
      reuseExistingServer: !process.env.CI,
      timeout: 60_000,
    },
  ],
})
