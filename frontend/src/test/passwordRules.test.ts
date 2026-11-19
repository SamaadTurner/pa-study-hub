import { describe, it, expect } from 'vitest'
import { SPECIAL_CHARACTERS, firstUnmetRequirement } from '@/utils/passwordRules'

describe('password rules', () => {
  it('accept a password that meets every rule', () => {
    expect(firstUnmetRequirement('Str0ng!Passw0rd')).toBeNull()
  })

  it('name the first rule a password breaks', () => {
    expect(firstUnmetRequirement('Sh0rt!')).toBe('At least 8 characters')
    expect(firstUnmetRequirement('nouppercase1!')).toBe('Contains uppercase letter')
    expect(firstUnmetRequirement('NoSpecial123')).toBe('Contains a special character')
  })

  it('use the same special characters as the server', () => {
    // Same string as PasswordPolicy.SPECIAL_CHARS in the backend
    expect(SPECIAL_CHARACTERS).toBe('!@#$%^&*()_+-=[]|;\':",./<>?')
    expect(firstUnmetRequirement('Under_score1')).toBeNull()
    expect(firstUnmetRequirement('Dash-dash12')).toBeNull()
    // The server does not accept braces, so neither does the page
    expect(firstUnmetRequirement('Brace{Brace1')).toBe('Contains a special character')
  })
})
