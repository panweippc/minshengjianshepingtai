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

/** 重置密码（与后端 PUT /password 对齐，入参走 JSON body） */
export function resetPassword(id: string, password: string) {
  return put<Result<void>>('/api/system/user/password', { userId: id, newPassword: password })
}

/** 查询用户角色ID集合（编辑回显用） */
export function getUserRoleIds(id: string) {
  return get<Result<string[]>>(`/api/system/user/${id}/roleIds`)
}
