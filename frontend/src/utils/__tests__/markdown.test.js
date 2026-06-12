// frontend/src/utils/__tests__/markdown.test.js
import { describe, it, expect } from 'vitest'
import { renderMarkdown } from '../markdown.js'

describe('renderMarkdown', () => {
  it('空文本返回空字符串', () => {
    expect(renderMarkdown('')).toBe('')
    expect(renderMarkdown(null)).toBe('')
  })

  it('将标题和列表渲染为对应 HTML 标签', () => {
    const html = renderMarkdown('# 一、逆向拆解瓶颈\n- 要点一\n- 要点二')
    expect(html).toContain('<h1>')
    expect(html).toContain('一、逆向拆解瓶颈')
    expect(html).toContain('<li>要点一</li>')
  })

  it('过滤危险的 script 标签', () => {
    const html = renderMarkdown('正常内容<script>alert(1)</script>')
    expect(html).not.toContain('<script>')
    expect(html).toContain('正常内容')
  })
})
