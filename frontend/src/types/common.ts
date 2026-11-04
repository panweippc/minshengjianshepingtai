/** 统一返回结构，与后端 Result<T> 对齐 */
export interface Result<T = unknown> {
  code: number
  msg: string
  data: T
  success: boolean
}

/** 分页返回结构，与后端 PageVO 对齐 */
export interface PageVO<T = unknown> {
  records: T[]
  total: number
  pageNum: number
  pageSize: number
}

/** 分页查询入参，与后端对齐 */
export interface PageQuery {
  pageNum: number
  pageSize: number
}
