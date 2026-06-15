// frontend/src/views/__tests__/MessageBoardView.test.js
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import { nextTick } from 'vue'
import MessageBoardView from '../MessageBoardView.vue'
import { messageBoardApi } from '../../api/index.js'

vi.mock('../../api/index.js', () => ({
  messageBoardApi: {
    list: vi.fn(),
    create: vi.fn(),
    remove: vi.fn(),
    like: vi.fn()
  }
}))

function makeMessage(overrides = {}) {
  return {
    id: 1,
    user_id: 1,
    nickname: '小明',
    content: '第一条留言',
    like_count: 0,
    liked_by_me: false,
    can_delete: true,
    created_at: '2026-06-15 10:00:00',
    ...overrides
  }
}

describe('MessageBoardView', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('加载时展示留言列表', async () => {
    messageBoardApi.list.mockResolvedValue({ list: [makeMessage()], total: 1, pageNum: 1, pageSize: 20 })

    const wrapper = mount(MessageBoardView)
    await flushPromises()

    expect(wrapper.text()).toContain('第一条留言')
    expect(wrapper.text()).toContain('小明')
  })

  it('发表留言成功后插入到列表顶部', async () => {
    messageBoardApi.list.mockResolvedValue({ list: [], total: 0, pageNum: 1, pageSize: 20 })
    messageBoardApi.create.mockResolvedValue(makeMessage({ id: 2, content: '新留言' }))

    const wrapper = mount(MessageBoardView)
    await flushPromises()

    wrapper.vm.content = '新留言'
    await wrapper.vm.handlePost()
    await flushPromises()

    expect(messageBoardApi.create).toHaveBeenCalledWith('新留言')
    expect(wrapper.text()).toContain('新留言')
  })

  it('点赞成功后更新点赞数和状态', async () => {
    messageBoardApi.list.mockResolvedValue({ list: [makeMessage()], total: 1, pageNum: 1, pageSize: 20 })
    messageBoardApi.like.mockResolvedValue({ id: 1, like_count: 1, liked_by_me: true })

    const wrapper = mount(MessageBoardView)
    await flushPromises()

    await wrapper.vm.handleLike(wrapper.vm.list[0])
    await flushPromises()

    expect(messageBoardApi.like).toHaveBeenCalledWith(1)
    expect(wrapper.vm.list[0].like_count).toBe(1)
    expect(wrapper.vm.list[0].liked_by_me).toBe(true)
  })

  it('点赞失败时回滚本地状态', async () => {
    messageBoardApi.list.mockResolvedValue({ list: [makeMessage()], total: 1, pageNum: 1, pageSize: 20 })
    messageBoardApi.like.mockRejectedValue(new Error('点赞失败'))

    const wrapper = mount(MessageBoardView)
    await flushPromises()

    await wrapper.vm.handleLike(wrapper.vm.list[0])
    await flushPromises()

    expect(wrapper.vm.list[0].like_count).toBe(0)
    expect(wrapper.vm.list[0].liked_by_me).toBe(false)
  })

  it('删除留言成功后从列表移除', async () => {
    messageBoardApi.list.mockResolvedValue({ list: [makeMessage()], total: 1, pageNum: 1, pageSize: 20 })
    messageBoardApi.remove.mockResolvedValue()

    const wrapper = mount(MessageBoardView)
    await flushPromises()

    await wrapper.vm.handleDelete(wrapper.vm.list[0])
    await flushPromises()

    expect(messageBoardApi.remove).toHaveBeenCalledWith(1)
    expect(wrapper.vm.list).toHaveLength(0)
  })
})

async function flushPromises() {
  await nextTick()
  await new Promise((resolve) => setTimeout(resolve, 0))
  await nextTick()
}
