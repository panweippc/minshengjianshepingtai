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
  id: string
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
  id: string
  parentId: string
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
  id: string
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
  id?: string
  username: string
  password?: string
  realName: string
  regionCode?: string
  phone?: string
  status?: number
  remark?: string
  roleIds?: string[]
}

/** 角色分页记录 */
export interface RoleVO {
  id: string
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
  id?: string
  roleName: string
  roleKey: string
  dataScope: number
  status?: number
  remark?: string
  menuIds?: string[]
}

/** 菜单保存入参 */
export interface MenuSaveDTO {
  id?: string
  parentId: string
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
  id: string
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
  id?: string
  dictType: string
  description?: string
  dictLabel: string
  dictValue: string
  sort?: number
  remark?: string
}

/** 操作日志分页记录 */
export interface OperLogVO {
  id: string
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
  id: string
  code: string
  name: string
  parentCode: string
  level: number
  regionType: string
  secretary?: string
  secretaryPhone?: string
  director?: string
  directorPhone?: string
  sort?: number
  children: VillageTreeVO[]
}

/** 村委会区划记录 */
export interface VillageVO {
  id: string
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
  id: string
  facilityId: string
  attachmentType: string
  fileName: string
  filePath: string
  fileSize: number
  remark: string
  createTime: string
}

/** 项目（简化版） */
export interface ProjectVO {
  id: string
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

/** 文件上传结果 */
export interface FileUploadVO {
  fileName: string
  filePath: string
  fileHash: string
  fileSize: number
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

/** 物品信息记录 */
export interface MaterialItemVO {
  id: string
  itemName: string
  itemCategory?: string
  warningQty: number
  stockQty: number
  unit?: string
  remark?: string
  createTime: string
  updateTime: string
}

/** 物品信息保存入参 */
export interface MaterialItemSaveDTO {
  id?: string
  itemName: string
  itemCategory?: string
  warningQty: number
  unit?: string
  remark?: string
}

/** 库房信息记录 */
export interface MaterialWarehouseVO {
  id: string
  warehouseName: string
  location?: string
  manager?: string
  phone?: string
  createTime: string
  updateTime: string
}

/** 库房信息保存入参 */
export interface MaterialWarehouseSaveDTO {
  id?: string
  warehouseName: string
  location?: string
  manager?: string
  phone?: string
}

/** 入库单明细记录 */
export interface MaterialStockInItemVO {
  id: string
  orderId?: string
  orderName?: string
  warehouseId?: string
  warehouseName?: string
  itemId?: string
  itemName: string
  itemCategory?: string
  quantity: number
  inType?: string
  inTime?: string
  handler?: string
  remark?: string
}

/** 入库单记录 */
export interface MaterialStockInVO {
  id: string
  orderName: string
  inDate?: string
  handler?: string
  remark?: string
  createTime?: string
}

/** 入库单明细保存入参 */
export interface MaterialStockInItemSaveDTO {
  warehouseId?: string
  itemId?: string
  itemName: string
  itemCategory?: string
  quantity: number
  inType: string
  inTime: string
  remark?: string
}

/** 入库单保存入参 */
export interface MaterialStockInSaveDTO {
  id?: string
  orderName: string
  remark?: string
  items: MaterialStockInItemSaveDTO[]
}

/** 入库单详情 */
export interface MaterialStockInDetailVO {
  id: string
  orderName: string
  remark?: string
  inDate?: string
  handler?: string
  createTime?: string
  items: MaterialStockInItemVO[]
}

// ==================== 出库单 ====================

/** 出库单明细记录 */
export interface MaterialStockOutItemVO {
  id: string
  orderId?: string
  orderName?: string
  warehouseId?: string
  warehouseName?: string
  itemId?: string
  itemName: string
  itemCategory?: string
  quantity: number
  outType?: string
  outTime?: string
  handler?: string
  remark?: string
}

/** 出库单记录 */
export interface MaterialStockOutVO {
  id: string
  orderName: string
  outDate?: string
  handler?: string
  remark?: string
  createTime?: string
}

/** 出库单明细保存入参 */
export interface MaterialStockOutItemSaveDTO {
  warehouseId?: string
  itemId?: string
  itemName: string
  itemCategory?: string
  quantity: number
  outType: string
  outTime: string
  remark?: string
}

/** 出库单保存入参 */
export interface MaterialStockOutSaveDTO {
  id?: string
  orderName: string
  remark?: string
  items: MaterialStockOutItemSaveDTO[]
}

/** 出库单详情 */
export interface MaterialStockOutDetailVO {
  id: string
  orderName: string
  remark?: string
  outDate?: string
  handler?: string
  createTime?: string
  items: MaterialStockOutItemVO[]
}

// ==================== 统计表（盘点表/交接表/损毁表） ====================

/** 报表类型枚举 */
export type MaterialReportType = 'INVENTORY' | 'HANDOVER' | 'DAMAGE'

/** 报表主记录 */
export interface MaterialReportVO {
  id: string
  reportName: string
  reportType: MaterialReportType
  reportDate?: string
  handler?: string
  auditor?: string
  createTime: string
}

/** 盘点表明细 */
export interface MaterialInventoryItemVO {
  id: string
  reportId?: string
  warehouseName?: string
  itemName: string
  unit?: string
  bookQty: number
  actualQty: number
  profitQty: number
  remark?: string
}

/** 交接表明细 */
export interface MaterialHandoverItemVO {
  id: string
  reportId?: string
  warehouseName?: string
  itemName: string
  unit?: string
  bookQty: number
  actualQty: number
  profitQty: number
  remark?: string
}

/** 损毁表明细 */
export interface MaterialDamageItemVO {
  id: string
  reportId?: string
  warehouseName?: string
  itemName: string
  unit?: string
  bookQty: number
  handler?: string
  remark?: string
}

/** 盘点表保存入参 */
export interface MaterialInventorySaveDTO {
  id?: string
  reportName: string
  reportDate?: string
  handler?: string
  auditor?: string
  department?: string
  /** 报表备注（表格纸底部备注行） */
  remark?: string
  items: MaterialInventoryItemSaveDTO[]
}

/** 盘点表明细保存入参 */
export interface MaterialInventoryItemSaveDTO {
  warehouseName?: string
  itemName: string
  unit?: string
  bookQty: number
  actualQty: number
  remark?: string
}

/** 交接表保存入参 */
export interface MaterialHandoverSaveDTO {
  id?: string
  reportName: string
  reportDate?: string
  handler?: string
  handoverPerson?: string
  auditor?: string
  department?: string
  remark?: string
  items: MaterialHandoverItemSaveDTO[]
}

/** 交接表明细保存入参 */
export interface MaterialHandoverItemSaveDTO {
  warehouseName?: string
  itemName: string
  unit?: string
  bookQty: number
  actualQty: number
  remark?: string
}

/** 损毁表保存入参 */
export interface MaterialDamageSaveDTO {
  id?: string
  reportName: string
  reportDate?: string
  handler?: string
  auditor?: string
  department?: string
  remark?: string
  items: MaterialDamageItemSaveDTO[]
}

/** 损毁表明细保存入参 */
export interface MaterialDamageItemSaveDTO {
  warehouseName?: string
  itemName: string
  unit?: string
  bookQty: number
  handler?: string
  remark?: string
}

/** 报表明细（盘点/交接/损毁通用结构） */
export interface MaterialReportDetailItemVO {
  id: string
  reportId?: string
  warehouseName?: string
  itemName: string
  unit?: string
  bookQty: number
  actualQty?: number
  profitQty?: number
  handler?: string
  remark?: string
}

/** 报表详情 */
export interface MaterialReportDetailVO {
  id: string
  reportName: string
  reportType: MaterialReportType
  reportDate?: string
  handler?: string
  auditor?: string
  handoverPerson?: string
  department?: string
  /** 报表备注 */
  remark?: string
  createTime: string
  /** 后端统一返回的明细集合 */
  items?: MaterialReportDetailItemVO[]
  inventoryItems?: MaterialInventoryItemVO[]
  handoverItems?: MaterialHandoverItemVO[]
  damageItems?: MaterialDamageItemVO[]
}
