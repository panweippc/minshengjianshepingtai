import { get, post, put, del } from './request'
import type { PageQuery, PageVO, Result } from '@/types/common'
import type { UserVO, UserSaveDTO } from '@/types/system'

/** 用户分页查询 */
export function getUserPage(params: PageQuery & { username?: string; realName?: string }) {
  return get<Result<PageVO<UserVO>>>('/api/system/user/page', { params })
}

/** 新增用户 */
export function createUser(data: UserSaveDTO) {
  return post<Result<void>>('/api/system/user', data)
}

/** 编辑用户 */
export function updateUser(data: UserSaveDTO) {
  return put<Result<void>>('/api/system/user', data)
}

/** 删除用户 */
export function deleteUser(id: string) {
  return del<Result<void>>(`/api/system/user/${id}`)
}

/** 重置密码 */
export function resetPassword(id: string, password: string) {
  return put<Result<void>>(`/api/system/user/password/${id}`, { password })
}

/** 分配角色 */
export function assignRoles(id: string, roleIds: string[]) {
  return put<Result<void>>(`/api/system/user/roleIds/${id}`, roleIds)
}
