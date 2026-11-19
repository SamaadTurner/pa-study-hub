// The password rules, the same ones PasswordPolicy.java checks on the server.
// Used by the register page and the password form on the profile page.

// Exactly the characters PasswordPolicy.SPECIAL_CHARS accepts. The page used to
// have its own list, which let "{" through (the server then refused it) and
// blocked "_" and "-" (which the server accepts).
export const SPECIAL_CHARACTERS = '!@#$%^&*()_+-=[]|;\':",./<>?'
export interface PasswordRequirement {
  label: string
  test: (pw: string) => boolean
}

export const PASSWORD_REQUIREMENTS: PasswordRequirement[] = [
  { label: 'At least 8 characters',             test: pw => pw.length >= 8 },
  { label: 'Contains uppercase letter',          test: pw => /[A-Z]/.test(pw) },
  { label: 'Contains lowercase letter',          test: pw => /[a-z]/.test(pw) },
  { label: 'Contains a number',                  test: pw => /\d/.test(pw) },
  { label: 'Contains a special character',       test: pw => [...pw].some(c => SPECIAL_CHARACTERS.includes(c)) },
]

// The first rule the password breaks, or null when it meets all of them
export function firstUnmetRequirement(password: string): string | null {
  const failed = PASSWORD_REQUIREMENTS.find(rule => !rule.test(password))
  return failed ? failed.label : null
}
