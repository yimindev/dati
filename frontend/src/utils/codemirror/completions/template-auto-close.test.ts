// frontend/src/utils/codemirror/completions/template-auto-close.test.ts
import { describe, it, expect } from 'vitest'
import { detectIfClose } from './template-auto-close'

describe('detectIfClose', () => {
  it('returns indent when }} completes {{#if condition', () => {
    const doc = '{{#if status}}'
    expect(detectIfClose(doc, doc.length)).toBe('')
  })

  it('returns indent with leading whitespace preserved', () => {
    const doc = '  {{#if status}}'
    expect(detectIfClose(doc, doc.length)).toBe('  ')
  })

  it('returns null when cursor is not preceded by }}', () => {
    const doc = '{{#if status'
    expect(detectIfClose(doc, doc.length)).toBeNull()
  })

  it('returns null for regular {{var}} (not an if directive)', () => {
    const doc = '{{dept_id}}'
    expect(detectIfClose(doc, doc.length)).toBeNull()
  })

  it('returns null when {{/if}} already exists right after', () => {
    expect(detectIfClose('{{#if status}}{{/if}}', 16)).toBeNull()
  })

  it('treats IF as case-insensitive', () => {
    const doc = '{{#IF status}}'
    expect(detectIfClose(doc, doc.length)).toBe('')
  })

  it('treats /IF in guard as case-insensitive', () => {
    expect(detectIfClose('{{#if status}}{{/IF}}', 16)).toBeNull()
  })

  it('returns null for {{#where}} (only if triggers auto-close)', () => {
    const doc = '{{#where}}'
    expect(detectIfClose(doc, doc.length)).toBeNull()
  })

  it('returns indent for multiline document', () => {
    const doc = `SELECT *\n  {{#if status}}`
    expect(detectIfClose(doc, doc.length)).toBe('  ')
  })

  it('returns null with cursor at position 0', () => {
    expect(detectIfClose('', 0)).toBeNull()
  })

  it('returns null with cursor at position 1', () => {
    expect(detectIfClose('x', 1)).toBeNull()
  })

  it('returns null when {{/if}} with spaces appears right after', () => {
    expect(detectIfClose('{{#if status}}{{ /if }}', 16)).toBeNull()
  })

  it('allows dot in variable name: {{#if table.col}}', () => {
    const doc = '{{#if table.col}}'
    expect(detectIfClose(doc, doc.length)).toBe('')
  })

  // ---- mid-line {{#if (not at line start) ----
  it('detects {{#if}} mid-line after SQL', () => {
    const doc = 'SELECT * FROM users {{#if status}}'
    expect(detectIfClose(doc, doc.length)).toBe('')
  })

  it('detects {{#if}} mid-line with WHERE clause', () => {
    const doc = 'WHERE dept_id = {{dept}} {{#if status}}'
    expect(detectIfClose(doc, doc.length)).toBe('')
  })

  it('returns null for mid-line {{#where}} (not if)', () => {
    const doc = 'SELECT * FROM users {{#where}}'
    expect(detectIfClose(doc, doc.length)).toBeNull()
  })

  it('detects mid-line {{#if}} preserving indent from SQL', () => {
    const doc = `SELECT *\n  FROM users\n  {{#if status}}`
    expect(detectIfClose(doc, doc.length)).toBe('  ')
  })
})
