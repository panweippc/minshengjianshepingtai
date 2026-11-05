import { get, post, put, del } from './request'
import type { PageQuery, PageVO, Result } from '@/types/common'

/** 附件子项（与后端 HouseholdSaveDTO.AttachmentItem 对齐） */
export interface HouseholdAttachmentItem {
  /** 已有附件ID（编辑时回传，新增附件时不传） */
  id?: string
  /** 附件类型（字典 fu_jian_lei_xing） */
  attachmentType?: string
  /** 原始文件名 */
  fileName?: string
  /** MinIO 对象存储 key */
  fileKey?: string
  /** 存储桶 */
  bucket?: string
  /** 文件大小（字节） */
  fileSize?: number
  /** 附件备注 */
  remark?: string
}

/** 户基础信息查询参数 */
export interface HouseholdQuery extends PageQuery {
  /** 户主姓名（模糊匹配） */
  headName?: string
  /** 户主身份证号（精确匹配） */
  headIdCard?: string
  /** 树节点区划编码（支持乡镇/村编码） */
  nodeCode?: string
}

/** 户基础信息 VO（后端 Household 实体直出） */
export interface HouseholdVO {
  /** 雪花ID（后端序列化为字符串） */
  id: string
  householdNo: string
  regionCode: string
  regionName: string
  headName: string
  headIdCard?: string
  contactPhone?: string
  familyPopulation?: number
  houseArea?: number
  houseType?: string
  householdType?: string
  propertyNo?: string
  archiveTime?: string
  remark?: string
  createBy?: string
  createTime?: string
  updateBy?: string
  updateTime?: string
}

/** 附件 VO */
export interface HouseholdAttachmentVO {
  /** 雪花ID（后端序列化为字符串） */
  id: string
  householdId: string
  attachmentType?: string
  fileName?: string
  fileKey?: string
  bucket?: string
  fileSize?: number
  remark?: string
  createTime?: string
}

/** 户基础信息保存入参（新增/编辑共用） */
export interface HouseholdSaveDTO {
  id?: string
  /** 户号（新增必填，人工录入，全局唯一；编辑时不可修改） */
  householdNo?: string
  /** 行政村区划编码（必填） */
  regionCode?: string
  headName?: string
  headIdCard?: string
  contactPhone?: string
  familyPopulation?: number
  houseArea?: number
  houseType?: string
  householdType?: string
  propertyNo?: string
  archiveTime?: string
  remark?: string
  attachments?: HouseholdAttachmentItem[]
}

/** 户基础信息分页查询 */
export function getHouseholdPage(params: HouseholdQuery) {
  return get<Result<PageVO<HouseholdVO>>>('/api/base/household/page', { params })
}

/** 户基础信息详情 */
export function getHouseholdDetail(id: string) {
  return get<Result<HouseholdVO>>(`/api/base/household/detail/${id}`)
}

/** 查询附件列表 */
export function getHouseholdAttachments(householdId: string) {
  return get<Result<HouseholdAttachmentVO[]>>(`/api/base/household/attachments/${householdId}`)
}

/** 新增户基础信息 */
export function createHousehold(data: HouseholdSaveDTO) {
  return post<Result<number>>('/api/base/household', data)
}

/** 编辑户基础信息 */
export function updateHousehold(data: HouseholdSaveDTO) {
  return put<Result<void>>('/api/base/household', data)
}

/** 删除户基础信息 */
export function deleteHousehold(id: string) {
  return del<Result<void>>(`/api/base/household/${id}`)
}
