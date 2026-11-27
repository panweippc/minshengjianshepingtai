/**
 * 日期展示工具
 * <p>
 * 后端 LocalDateTime 序列化为 "yyyy-MM-dd HH:mm:ss"，LocalDate 序列化为 "yyyy-MM-dd"，
 * 业务页面统一只展示年月日，因此截取前 10 位即可得到 "YYYY-MM-DD"。
 */

/**
 * 将时间值格式化为仅展示年月日（YYYY-MM-DD）
 * @param value 原始时间值，可为字符串/Date/空
 * @param fallback 空值时的占位展示，默认 '-'
 */
export function formatDate(value: unknown, fallback = '-'): string {
  if (value === null || value === undefined || value === '') {
    return fallback
  }
  const text = String(value)
  // "yyyy-MM-dd HH:mm:ss" / "yyyy-MM-dd" / ISO 均以前 10 位作为日期部分
  return text.length >= 10 ? text.slice(0, 10) : text
}

/**
 * 适配 el-table-column :formatter 的单元格格式化函数
 * 签名：(row, column, cellValue, index) => string
 */
export function formatDateCell(
  _row: unknown,
  _column: unknown,
  cellValue: unknown,
): string {
  return formatDate(cellValue)
}
