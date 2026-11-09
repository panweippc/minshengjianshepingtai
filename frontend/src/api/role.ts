import { get, post, put, del } from './request'
import type { PageQuery, PageVO, Result } from '@/types/common'
import type { RoleVO, RoleSaveDTO } from '@/types/system'

/** 角色分页查询 */
export function getRolePage(params: PageQuery & { roleName?: string }) {
  return get<Result<PageVO<RoleVO>>>('/api/system/role/page', { params })
}

/** 全部角色列表（下拉用） */
export function getRoleAll() {
  return get<Result<RoleVO[]>>('/api/system/role/all')
}

/** 新增角色 */
export function createRole(data: RoleSaveDTO) {
  return post<Result<void>>('/api/system/role', data)
}

/** 编辑角色 */
export function updateRole(data: RoleSaveDTO) {
  return put<Result<void>>('/api/system/role', data)
}

/** 删除角色 */
export function deleteRole(id: string) {
  return del<Result<void>>(`/api/system/role/${id}`)
}

/** 分配菜单权限 */
export function assignMenus(id: string, menuIds: string[]) {
  return put<Result<void>>(`/api/system/role/menuIds/${id}`, menuIds)
}
