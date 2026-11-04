import { get, post } from './request'
import type { LoginDTO, LoginVO, UserInfoVO } from '@/types/system'
import type { Result } from '@/types/common'

/** 登录 */
export function login(data: LoginDTO) {
  return post<Result<LoginVO>>('/api/auth/login', data)
}

/** 登出 */
export function logout() {
  return post<Result<void>>('/api/auth/logout')
}

/** 获取当前用户信息（含菜单树） */
export function getUserInfo() {
  return get<Result<UserInfoVO>>('/api/auth/info')
}
