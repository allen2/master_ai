// frontend/src/utils/markdown.js
import { marked } from 'marked'
import DOMPurify from 'dompurify'

marked.setOptions({ breaks: true })

/**
 * 将 Markdown 文本渲染为安全的 HTML 字符串。
 * @param {string} text Markdown 原文
 * @returns {string} 经过 DOMPurify 净化的 HTML
 */
export function renderMarkdown(text) {
  if (!text) {
    return ''
  }
  const html = marked.parse(text)
  return DOMPurify.sanitize(html)
}
