import { defineStore } from 'pinia'
import { login as loginApi, logout as logoutApi, getUserInfo } from '@/api/auth'
import type { LoginDTO, UserInfoVO, MenuVO } from '@/types/system'

/** 用户状态：token、用户信息、菜单树、权限集合 */
export const useUserStore = defineStore('user', {
  state: () => ({
    token: localStorage.getItem('token') || '',
    userInfo: null as UserInfoVO | null,
    menus: [] as MenuVO[],
    perms: [] as string[]
  }),
  getters: {
    /** 是否已登录 */
    isLogin: (state) => !!state.token,
    /** 动态路由用菜单树（过滤目录与菜单，仅启用状态） */
    routeMenus: (state) => state.menus.filter((m) => m.menuType !== 'F' && m.status === 1)
  },
  actions: {
    /** 登录并保存 token */
    async login(dto: LoginDTO) {
      const res = await loginApi(dto)
      this.token = res.data.token
      localStorage.setItem('token', res.data.token)
    },
    /** 拉取当前用户信息、菜单树、权限集合 */
    async fetchUserInfo() {
      const res = await getUserInfo()
      this.userInfo = res.data
      this.menus = res.data.menus || []
      this.perms = res.data.perms || []
    },
    /** 登出并清理状态 */
    async logout() {
      try {
        await logoutApi()
      } finally {
        this.token = ''
        this.userInfo = null
        this.menus = []
        this.perms = []
        localStorage.removeItem('token')
      }
    },
    /** 校验按钮/接口权限 */
    hasPerm(perm: string) {
      return this.perms.includes('*') || this.perms.includes(perm)
    }
  }
})
