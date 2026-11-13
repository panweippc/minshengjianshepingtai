import { get, post, put } from './request'
import type { ChangePasswordDTO, LoginDTO, LoginVO, ProfileUpdateDTO, UserInfoVO } from '@/types/system'
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

/** 修改个人资料 */
export function updateProfile(data: ProfileUpdateDTO) {
  return put<Result<void>>('/api/auth/profile', data)
}

/** 修改登录密码 */
export function changePassword(data: ChangePasswordDTO) {
  return put<Result<void>>('/api/auth/password', data)
}
