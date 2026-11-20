import { readFileSync } from 'node:fs'
import path from 'node:path'
import { describe, expect, it } from 'vitest'
import securityHeaders from '../../security-headers.json'

// The same security headers are written in three places that can't share a file:
// render.yaml (the live site), nginx (docker compose) and vite preview (which reads
// the JSON). These checks fail when a copy no longer matches security-headers.json.
const root = path.resolve(__dirname, '../../..')
const headers = Object.entries(securityHeaders)

describe('security headers', () => {
  it('render.yaml sends every header with the same value', () => {
    // Line endings differ between Windows and Linux checkouts
    const render = readFileSync(path.join(root, 'render.yaml'), 'utf-8').replace(/\r\n/g, '\n')
    for (const [name, value] of headers) {
      // YAML single quotes write a ' as ''
      expect(render, name).toContain(`name: ${name}\n        value: '${value.replace(/'/g, "''")}'`)
    }
  })

  it('nginx sends every header with the same value, and both places that need them include it', () => {
    const conf = readFileSync(path.join(root, 'frontend/nginx-security-headers.conf'), 'utf-8')
    for (const [name, value] of headers) {
      expect(conf, name).toContain(`add_header ${name} "${value}" always;`)
    }
    const nginx = readFileSync(path.join(root, 'frontend/nginx.conf'), 'utf-8')
    expect(nginx.match(/include \/etc\/nginx\/security-headers\.conf;/g)).toHaveLength(2)
  })

  it('the policy allows Google sign-in and blocks framing and plugins', () => {
    const csp = securityHeaders['Content-Security-Policy']
    expect(csp).toContain("script-src 'self' https://accounts.google.com/gsi/client")
    expect(csp).toContain('frame-src https://accounts.google.com/gsi/')
    expect(csp).toContain("frame-ancestors 'none'")
    expect(csp).toContain("object-src 'none'")
    expect(csp).not.toContain("'unsafe-eval'")
    expect(csp).not.toMatch(/script-src[^;]*'unsafe-inline'/)
  })
})
