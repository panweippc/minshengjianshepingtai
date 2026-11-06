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

/** 区划树节点（乡镇/村/组） */
export interface VillageTreeVO {
  id: number
  code: string
  name: string
  parentCode: string
  level: number
  regionType: string
  children: VillageTreeVO[]
}

/** 村委会区划记录 */
export interface VillageVO {
  id: number
  code: string
  name: string
  parentCode: string
  level: number
  regionType: string
  secretary: string
  secretaryPhone: string
  director: string
  directorPhone: string
  sort: number
}

/** 村委会区划保存入参（区划编码为业务编码，编辑时用于定位且不可修改） */
export interface VillageSaveDTO {
  code: string
  name: string
  parentCode?: string
  regionType: string
  secretary?: string
  secretaryPhone?: string
  director?: string
  directorPhone?: string
  sort?: number
}

// ==================== 公共基础设施 ====================

/** 设施附件 */
export interface FacilityAttachmentVO {
  id: number
  facilityId: number
  attachmentType: string
  fileName: string
  filePath: string
  fileSize: number
  remark: string
  createTime: string
}

/** 项目（简化版） */
export interface ProjectVO {
  id: number
  projectCode: string
  projectName: string
  projectType: string
  progress: number
  budget: number
  status: string
  startYear: number
  regionCode: string
  createTime: string
  updateTime: string
}

/** 公共基础设施列表记录 */
export interface FacilityVO {
  id: string
  facilityCode: string
  facilityName: string
  facilityType: string
  regionCode: string
  regionName: string
  location: string
  buildYear: number
  quantity: number
  unit: string
  remark: string
  createTime: string
  updateTime: string
}

/** 区划设施数量统计（左侧区划树徽标） */
export interface RegionFacilityCountVO {
  regionCode: string
  count: number
}

/** 公共基础设施详情（含附件和关联项目） */
export interface FacilityDetailVO {
  id: string
  facilityCode: string
  facilityName: string
  facilityType: string
  regionCode: string
  regionName: string
  location: string
  buildYear: number
  quantity: number
  unit: string
  remark: string
  createTime: string
  attachments: FacilityAttachmentVO[]
  projects: ProjectVO[]
}

/** 设施附件保存入参 */
export interface FacilityAttachmentSaveDTO {
  id?: number
  attachmentType?: string
  fileName?: string
  filePath?: string
  fileHash?: string
  fileSize?: number
  remark?: string
}

/** 公共基础设施保存入参 */
export interface FacilitySaveDTO {
  id?: string
  facilityCode: string
  facilityName: string
  facilityType?: string
  regionCode: string
  regionName?: string
  location?: string
  buildYear?: number
  quantity?: number
  unit?: string
  remark?: string
  attachments?: FacilityAttachmentSaveDTO[]
}
