import { get, post, put, del } from './request'
import type { Result } from '@/types/common'
import type { MenuVO, MenuSaveDTO } from '@/types/system'

/** 菜单树查询 */
export function getMenuTree() {
  return get<Result<MenuVO[]>>('/api/system/menu/tree')
}

/** 新增菜单 */
export function createMenu(data: MenuSaveDTO) {
  return post<Result<void>>('/api/system/menu', data)
}

/** 编辑菜单 */
export function updateMenu(data: MenuSaveDTO) {
  return put<Result<void>>('/api/system/menu', data)
}

/** 删除菜单 */
export function deleteMenu(id: string) {
  return del<Result<void>>(`/api/system/menu/${id}`)
}
