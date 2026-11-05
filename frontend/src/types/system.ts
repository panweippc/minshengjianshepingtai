/** 登录入参 */
export interface LoginDTO {
  username: string
  password: string
}

/** 登录返回 */
export interface LoginVO {
  token: string
}

/** 当前用户信息 */
export interface UserInfoVO {
  id: number
  username: string
  realName: string
  regionCode: string
  regionName: string
  roles: string[]
  perms: string[]
  menus: MenuVO[]
}

/** 菜单树节点 */
export interface MenuVO {
  id: number
  parentId: number
  menuName: string
  menuType: string
  path: string
  perm: string
  icon: string
  sort: number
  status: number
  children?: MenuVO[]
}

/** 用户分页记录 */
export interface UserVO {
  id: number
  username: string
  realName: string
  regionCode: string
  regionName: string
  phone: string
  status: number
  remark: string
  createTime: string
  updateTime: string
}

/** 用户保存入参 */
export interface UserSaveDTO {
  id?: number
  username: string
  password?: string
  realName: string
  regionCode?: string
  phone?: string
  status?: number
  remark?: string
  roleIds?: number[]
}

/** 角色分页记录 */
export interface RoleVO {
  id: number
  roleName: string
  roleKey: string
  dataScope: number
  status: number
  remark: string
  createTime: string
  updateTime: string
}

/** 角色保存入参 */
export interface RoleSaveDTO {
  id?: number
  roleName: string
  roleKey: string
  dataScope: number
  status?: number
  remark?: string
  menuIds?: number[]
}

/** 菜单保存入参 */
export interface MenuSaveDTO {
  id?: number
  parentId: number
  menuName: string
  menuType: string
  path?: string
  perm?: string
  icon?: string
  sort?: number
  status?: number
}

/** 字典记录 */
export interface DictVO {
  id: number
  dictType: string
  description: string
  dictLabel: string
  dictValue: string
  sort: number
  remark: string
  createTime: string
  updateTime: string
}

/** 字典保存入参 */
export interface DictSaveDTO {
  id?: number
  dictType: string
  description?: string
  dictLabel: string
  dictValue: string
  sort?: number
  remark?: string
}

/** 操作日志分页记录 */
export interface OperLogVO {
  id: number
  module: string
  operation: string
  method: string
  url: string
  ip: string
  params: string
  resultCode: number
  errorMsg: string
  operTime: string
  costMs: number
  username: string
}

/** 行政区划 */
export interface RegionVO {
  code: string
  name: string
  parentCode: string
  level: number
}
