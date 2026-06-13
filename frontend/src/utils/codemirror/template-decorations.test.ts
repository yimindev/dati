// frontend/src/utils/codemirror/template-decorations.test.ts
import { describe, it, expect } from 'vitest'
import { detectUnclosedRanges } from './template-decorations'

describe('detectUnclosedRanges', () => {
  // ---- closed: no errors ----
  it('{{a}} {{b}} → all closed', () => {
    expect(detectUnclosedRanges('{{a}} {{b}}')).toEqual([])
  })

  it('\\{{escaped}} {{real}} → escaped ignored, real closed', () => {
    expect(detectUnclosedRanges('\\{{escaped}} {{real}}')).toEqual([])
  })

  it('{{#if status}} text → closed directive', () => {
    expect(detectUnclosedRanges('{{#if status}} text')).toEqual([])
  })

  it('{{/if}} → closed', () => {
    expect(detectUnclosedRanges('{{/if}}')).toEqual([])
  })

  it('{{{var}}} → triple brace closed', () => {
    expect(detectUnclosedRanges('{{{var}}}')).toEqual([])
  })

  it('plain text → no errors', () => {
    expect(detectUnclosedRanges('text without braces')).toEqual([])
  })

  it('}} only closing → no errors', () => {
    expect(detectUnclosedRanges('}} only closing')).toEqual([])
  })

  // ---- variable: error stops at first invalid char (space) ----
  it('{{sta fsdfsd → only {{sta, stops at space', () => {
    expect(detectUnclosedRanges('{{sta fsdfsd')).toEqual([{ from: 0, to: 5 }])
  })

  it('{{var:default txt → stops after :default', () => {
    expect(detectUnclosedRanges('{{var:default txt')).toEqual([{ from: 0, to: 13 }])
  })

  it('{{table.col works → dot is valid', () => {
    expect(detectUnclosedRanges('{{table.col')).toEqual([{ from: 0, to: 11 }])
  })

  it('{{var_col → underscore is valid', () => {
    expect(detectUnclosedRanges('{{var_col')).toEqual([{ from: 0, to: 9 }])
  })

  // ---- directive opening: error stops at first non-varname char ----
  it('{{#if status extra → stops at second space', () => {
    expect(detectUnclosedRanges('{{#if status extra')).toEqual([{ from: 0, to: 12 }])
  })

  it('{{#if sta → whole line (all valid)', () => {
    expect(detectUnclosedRanges('{{#if sta')).toEqual([{ from: 0, to: 9 }])
  })

  it('{{#where cond → stops after condition', () => {
    expect(detectUnclosedRanges('{{#where cond')).toEqual([{ from: 0, to: 13 }])
  })

  // ---- directive closing: error covers the whole tag ----
  it('{{/if → unclosed closing tag', () => {
    expect(detectUnclosedRanges('{{/if')).toEqual([{ from: 0, to: 5 }])
  })

  it('{{/where → unclosed closing tag', () => {
    expect(detectUnclosedRanges('{{/where')).toEqual([{ from: 0, to: 8 }])
  })

  // ---- mixed: only first unclosed, second closed ----
  it('{{sta aaa {{xi}} → only {{sta is unclosed', () => {
    expect(detectUnclosedRanges('{{sta aaa {{xi}}')).toEqual([{ from: 0, to: 5 }])
  })

  it('{{a}} {{b}} {{c {{d}} → only {{c is unclosed', () => {
    expect(detectUnclosedRanges('{{a}} {{b}} {{c {{d}}')).toEqual([{ from: 12, to: 15 }])
  })

  // ---- raw variable: triple brace ----
  it('{{{table unclosed raw variable', () => {
    expect(detectUnclosedRanges('{{{table')).toEqual([{ from: 0, to: 8 }])
  })

  it('{{{table:def unclosed raw with default', () => {
    expect(detectUnclosedRanges('{{{table:def')).toEqual([{ from: 0, to: 12 }])
  })

  // ---- bare/unclosed minimum ----
  it('bare {{ → minimal error', () => {
    expect(detectUnclosedRanges('{{')).toEqual([{ from: 0, to: 2 }])
  })
})
