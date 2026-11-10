import { get, post, put, del } from './request'
import type { PageQuery, PageVO, Result } from '@/types/common'
import type {
  MaterialItemVO,
  MaterialItemSaveDTO,
  MaterialWarehouseVO,
  MaterialWarehouseSaveDTO,
  MaterialStockInItemVO,
  MaterialStockInVO,
  MaterialStockInSaveDTO,
  MaterialStockInDetailVO,
  MaterialStockOutItemVO,
  MaterialStockOutVO,
  MaterialStockOutSaveDTO,
  MaterialStockOutDetailVO,
  MaterialReportVO,
  MaterialReportDetailVO,
  MaterialInventorySaveDTO,
  MaterialHandoverSaveDTO,
  MaterialDamageSaveDTO
} from '@/types/system'

/** 物品信息分页查询入参 */
export interface MaterialItemQuery extends PageQuery {
  itemName?: string
  itemCategory?: string
  lowWarning?: boolean
}

/** 物品信息分页查询 */
export function getMaterialItemPage(params: MaterialItemQuery) {
  return get<Result<PageVO<MaterialItemVO>>>('/api/material/item/page', { params })
}

/** 根据ID集合查询物品（导出用） */
export function getMaterialItemListByIds(ids: string[]) {
  return get<Result<MaterialItemVO[]>>('/api/material/item/list', { params: { ids } })
}

/** 新增物品 */
export function createMaterialItem(data: MaterialItemSaveDTO) {
  return post<Result<string>>('/api/material/item', data)
}

/** 编辑物品 */
export function updateMaterialItem(data: MaterialItemSaveDTO) {
  return put<Result<void>>('/api/material/item', data)
}

/** 删除物品 */
export function deleteMaterialItem(id: string) {
  return del<Result<void>>(`/api/material/item/${id}`)
}

/** 库房信息分页查询入参 */
export interface MaterialWarehouseQuery extends PageQuery {
  warehouseName?: string
  location?: string
  manager?: string
  phone?: string
}

/** 库房信息分页查询 */
export function getMaterialWarehousePage(params: MaterialWarehouseQuery) {
  return get<Result<PageVO<MaterialWarehouseVO>>>('/api/material/warehouse/page', { params })
}

/** 新增库房 */
export function createMaterialWarehouse(data: MaterialWarehouseSaveDTO) {
  return post<Result<string>>('/api/material/warehouse', data)
}

/** 编辑库房 */
export function updateMaterialWarehouse(data: MaterialWarehouseSaveDTO) {
  return put<Result<void>>('/api/material/warehouse', data)
}

/** 删除库房 */
export function deleteMaterialWarehouse(id: string) {
  return del<Result<void>>(`/api/material/warehouse/${id}`)
}

/** 入库单明细分页查询入参 */
export interface MaterialStockInItemQuery extends PageQuery {
  itemName?: string
  warehouseId?: string
  itemCategory?: string
  inType?: string
}

/** 入库单分页查询入参 */
export interface MaterialStockInOrderQuery extends PageQuery {
  orderName?: string
  handler?: string
}

/** 入库单明细分页查询 */
export function getMaterialStockInItemPage(params: MaterialStockInItemQuery) {
  return get<Result<PageVO<MaterialStockInItemVO>>>('/api/material/stockin/item/page', { params })
}

/** 根据明细ID集合查询（导出用） */
export function getMaterialStockInItemListByIds(ids: string[]) {
  return get<Result<MaterialStockInItemVO[]>>('/api/material/stockin/item/list', { params: { ids } })
}

/** 入库单分页查询 */
export function getMaterialStockInPage(params: MaterialStockInOrderQuery) {
  return get<Result<PageVO<MaterialStockInVO>>>('/api/material/stockin/page', { params })
}

/** 入库单详情 */
export function getMaterialStockInDetail(id: string) {
  return get<Result<MaterialStockInDetailVO>>(`/api/material/stockin/${id}`)
}

/** 新增入库单 */
export function createMaterialStockIn(data: MaterialStockInSaveDTO) {
  return post<Result<string>>('/api/material/stockin', data)
}

/** 编辑入库单 */
export function updateMaterialStockIn(data: MaterialStockInSaveDTO) {
  return put<Result<void>>('/api/material/stockin', data)
}

/** 删除入库单 */
export function deleteMaterialStockIn(id: string) {
  return del<Result<void>>(`/api/material/stockin/${id}`)
}

// ==================== 出库单 ====================

/** 出库单明细分页查询入参 */
export interface MaterialStockOutItemQuery extends PageQuery {
  itemName?: string
  warehouseId?: string
  itemCategory?: string
  outType?: string
}

/** 出库单分页查询入参 */
export interface MaterialStockOutOrderQuery extends PageQuery {
  orderName?: string
  handler?: string
}

/** 出库单明细分页查询 */
export function getMaterialStockOutItemPage(params: MaterialStockOutItemQuery) {
  return get<Result<PageVO<MaterialStockOutItemVO>>>('/api/material/stockout/item/page', { params })
}

/** 根据明细ID集合查询（导出用） */
export function getMaterialStockOutItemListByIds(ids: string[]) {
  return get<Result<MaterialStockOutItemVO[]>>('/api/material/stockout/item/list', { params: { ids } })
}

/** 出库单分页查询 */
export function getMaterialStockOutPage(params: MaterialStockOutOrderQuery) {
  return get<Result<PageVO<MaterialStockOutVO>>>('/api/material/stockout/page', { params })
}

/** 出库单详情 */
export function getMaterialStockOutDetail(id: string) {
  return get<Result<MaterialStockOutDetailVO>>(`/api/material/stockout/${id}`)
}

/** 新增出库单 */
export function createMaterialStockOut(data: MaterialStockOutSaveDTO) {
  return post<Result<string>>('/api/material/stockout', data)
}

/** 编辑出库单 */
export function updateMaterialStockOut(data: MaterialStockOutSaveDTO) {
  return put<Result<void>>('/api/material/stockout', data)
}

/** 删除出库单 */
export function deleteMaterialStockOut(id: string) {
  return del<Result<void>>(`/api/material/stockout/${id}`)
}

// ==================== 统计表（盘点表/交接表/损毁表） ====================

/** 报表分页查询入参 */
export interface MaterialReportQuery extends PageQuery {
  reportName?: string
  reportType?: string
  startTime?: string
  endTime?: string
}

/** 报表分页查询 */
export function getMaterialReportPage(params: MaterialReportQuery) {
  return get<Result<PageVO<MaterialReportVO>>>('/api/material/report/page', { params })
}

/** 报表详情 */
export function getMaterialReportDetail(id: string) {
  return get<Result<MaterialReportDetailVO>>(`/api/material/report/${id}`)
}

/** 新增盘点表 */
export function createMaterialInventory(data: MaterialInventorySaveDTO) {
  return post<Result<string>>('/api/material/report/inventory', data)
}

/** 编辑盘点表 */
export function updateMaterialInventory(data: MaterialInventorySaveDTO) {
  return put<Result<void>>('/api/material/report/inventory', data)
}

/** 新增交接表 */
export function createMaterialHandover(data: MaterialHandoverSaveDTO) {
  return post<Result<string>>('/api/material/report/handover', data)
}

/** 编辑交接表 */
export function updateMaterialHandover(data: MaterialHandoverSaveDTO) {
  return put<Result<void>>('/api/material/report/handover', data)
}

/** 新增损毁表 */
export function createMaterialDamage(data: MaterialDamageSaveDTO) {
  return post<Result<string>>('/api/material/report/damage', data)
}

/** 编辑损毁表 */
export function updateMaterialDamage(data: MaterialDamageSaveDTO) {
  return put<Result<void>>('/api/material/report/damage', data)
}

/** 物品库房库存（盘点带出） */
export interface MaterialStockVO {
  warehouseName: string
  itemName: string
  unit?: string
  bookQty: number
}

/** 删除报表 */
export function deleteMaterialReport(id: string) {
  return del<Result<void>>(`/api/material/report/${id}`)
}

/** 盘点物品库房库存（自动带出物品所在库房与账面数量） */
export function getMaterialStockList(params: {
  itemName?: string
  warehouseName?: string
  itemCategory?: string
}) {
  return get<Result<MaterialStockVO[]>>('/api/material/report/stock-list', { params })
}
