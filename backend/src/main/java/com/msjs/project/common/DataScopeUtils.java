package com.msjs.project.common;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;

/**
 * 数据权限工具类
 * 业务约束：按当前登录用户的数据权限范围，为查询追加区划过滤条件
 * 安全要求：非全部范围用户未绑定区划时按空条件查询，避免越权读取全量数据
 */
public final class DataScopeUtils {

    private DataScopeUtils() {
    }

    /**
     * 为 Lambda 查询追加区划过滤
     *
     * @param wrapper      查询条件
     * @param regionColumn 区划编码字段（如 Household::getRegionCode）
     * @param <T>          实体类型
     */
    public static <T> void applyRegionScope(LambdaQueryWrapper<T> wrapper, SFunction<T, String> regionColumn) {
        LoginUser user = UserContext.get();
        if (user == null || user.getDataScope() == null || user.getDataScope() == Constants.DATA_SCOPE_ALL) {
            return;
        }
        String regionCode = user.getRegionCode();
        if (regionCode == null || regionCode.isBlank()) {
            wrapper.eq(regionColumn, "");
            return;
        }
        String code = regionCode.trim();
        if (user.getDataScope() == Constants.DATA_SCOPE_REGION_AND_CHILDREN) {
            wrapper.likeRight(regionColumn, code);
        } else {
            wrapper.eq(regionColumn, code);
        }
    }

    /**
     * 为原生列名查询追加区划过滤（用于分组统计等无法使用 Lambda 的场景）
     * 说明：列名由调用方以代码常量传入，不接收前端输入，无注入风险
     *
     * @param wrapper          查询条件
     * @param regionColumnName 数据库列名（如 region_code）
     */
    public static void applyRegionScope(QueryWrapper<?> wrapper, String regionColumnName) {
        LoginUser user = UserContext.get();
        if (user == null || user.getDataScope() == null || user.getDataScope() == Constants.DATA_SCOPE_ALL) {
            return;
        }
        String regionCode = user.getRegionCode();
        if (regionCode == null || regionCode.isBlank()) {
            wrapper.apply(regionColumnName + " = ''");
            return;
        }
        String code = regionCode.trim();
        if (user.getDataScope() == Constants.DATA_SCOPE_REGION_AND_CHILDREN) {
            wrapper.apply(regionColumnName + " LIKE CONCAT({0}, '%')", code);
        } else {
            wrapper.apply(regionColumnName + " = {0}", code);
        }
    }

    /**
     * 判断当前登录用户是否可访问指定区划的数据
     * 业务约束：用于详情、修改、删除等按主键直取的接口，防止通过 ID 越权读取
     *
     * @param regionCode 数据所属区划编码
     * @return true-可访问；false-越权
     */
    public static boolean canAccessRegion(String regionCode) {
        LoginUser user = UserContext.get();
        if (user == null || user.getDataScope() == null || user.getDataScope() == Constants.DATA_SCOPE_ALL) {
            return true;
        }
        String ownCode = user.getRegionCode();
        if (ownCode == null || ownCode.isBlank()) {
            return false;
        }
        String target = regionCode == null ? "" : regionCode.trim();
        if (user.getDataScope() == Constants.DATA_SCOPE_REGION_AND_CHILDREN) {
            return target.startsWith(ownCode.trim());
        }
        return target.equals(ownCode.trim());
    }
}
