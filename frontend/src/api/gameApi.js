const BASE = '/api'

async function request(url, options = {}) {
  const res = await fetch(BASE + url, {
    headers: { 'Content-Type': 'application/json' },
    ...options,
  })
  return res.json()
}

export const api = {
  /** 获取所有存档列表 */
  getSaves: () => request('/saves'),

  /** 保存到指定槽位 */
  save: (saveId) =>
    request('/save', {
      method: 'POST',
      body: JSON.stringify({ saveId }),
    }),

  /** 从指定槽位加载存档 */
  load: (saveId) =>
    request('/load', {
      method: 'POST',
      body: JSON.stringify({ saveId }),
    }),

  /** 删除指定槽位的存档 */
  deleteSave: (saveId) =>
    request('/deleteSave', {
      method: 'POST',
      body: JSON.stringify({ saveId }),
    }),

  /** 重置游戏（新游戏） */
  reset: () => {
    return fetch(BASE + '/reset', { method: 'POST' })
  },

  /** 获取当前游戏状态 */
  getGameState: () => request('/game'),
}
