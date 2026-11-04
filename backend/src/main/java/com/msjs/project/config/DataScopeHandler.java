package com.msjs.project.config;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.handler.MultiDataPermissionHandler;
import com.msjs.project.common.LoginUser;
import com.msjs.project.common.UserContext;
import com.msjs.project.common.annotation.DataScope;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.StringValue;
import net.sf.jsqlparser.expression.operators.relational.EqualsTo;
import net.sf.jsqlparser.expression.operators.relational.LikeExpression;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AssignableTypeFilter;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 数据权限处理器（行政区划行级隔离）
 * 对标注 @DataScope 的 Mapper 查询方法，按当前登录用户数据权限范围生成过滤片段：
 *   范围1-全部：不追加；范围2-本区及以下：region_code LIKE '区划前缀%'；范围3-仅本区：region_code = 等值
 * 由 MyBatis-Plus DataPermissionInterceptor 调用，同时作用于分页 COUNT 与主查询
 * 业务约束：未登录/范围为全部时返回 null 不过滤；用户无区划时按等值空串处理（查不到数据）
 */
@Component
public class DataScopeHandler implements MultiDataPermissionHandler, ApplicationListener<ApplicationReadyEvent> {

    private static final Logger LOG = LoggerFactory.getLogger(DataScopeHandler.class);

    /** 需数据过滤的 MappedStatement ID -> 区划列名 */
    private static final Map<String, String> SCOPE_METHODS = new ConcurrentHashMap<>();

    @Override
    public Expression getSqlSegment(Table table, Expression where, String mappedStatementId) {
        String regionColumn = SCOPE_METHODS.get(mappedStatementId);
        if (regionColumn == null) {
            return null;
        }
        LoginUser user = UserContext.get();
        if (user == null || user.getDataScope() == null || user.getDataScope() == 1) {
            return null;
        }
        Column column = new Column(regionColumn);
        if (user.getDataScope() == 2 && user.getRegionCode() != null) {
            // 本区及以下：行政区划代码前缀匹配，如 110000 覆盖 1101xx
            String prefix = stripTrailingZeros(user.getRegionCode());
            LikeExpression like = new LikeExpression();
            like.setLeftExpression(column);
            like.setRightExpression(new StringValue(prefix + "%"));
            LOG.debug("数据权限过滤：{} LIKE '{}%'", regionColumn, prefix);
            return like;
        }
        // 仅本区
        EqualsTo eq = new EqualsTo();
        eq.setLeftExpression(column);
        eq.setRightExpression(new StringValue(user.getRegionCode() == null ? "" : user.getRegionCode()));
        LOG.debug("数据权限过滤：{} = '{}'", regionColumn, user.getRegionCode());
        return eq;
    }

    /** 启动完成后扫描所有 BaseMapper 接口，登记 @DataScope 方法 */
    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        // 默认扫描器排除接口，此处覆盖为接受接口（Mapper 均为接口）
        ClassPathScanningCandidateComponentProvider scanner =
                new ClassPathScanningCandidateComponentProvider(false) {
                    @Override
                    protected boolean isCandidateComponent(
                            org.springframework.beans.factory.annotation.AnnotatedBeanDefinition beanDefinition) {
                        return beanDefinition.getMetadata().isInterface();
                    }
                };
        scanner.addIncludeFilter(new AssignableTypeFilter(BaseMapper.class));
        for (BeanDefinition bd : scanner.findCandidateComponents("com.msjs.project.mapper")) {
            try {
                Class<?> clazz = Class.forName(bd.getBeanClassName());
                for (Method m : clazz.getMethods()) {
                    DataScope ds = m.getAnnotation(DataScope.class);
                    if (ds != null) {
                        SCOPE_METHODS.put(clazz.getName() + "." + m.getName(), ds.regionColumn());
                        LOG.info("登记数据权限方法：{}.{}，区划列={}", clazz.getSimpleName(), m.getName(), ds.regionColumn());
                    }
                }
            } catch (ClassNotFoundException ignored) {
            }
        }
        LOG.info("数据权限登记完成，共 {} 个方法", SCOPE_METHODS.size());
    }

    /** 去除行政区划尾部零，得到层级前缀，如 110000 -> 11，110100 -> 1101 */
    private String stripTrailingZeros(String code) {
        int end = code.length();
        while (end > 0 && code.charAt(end - 1) == '0') {
            end--;
        }
        return code.substring(0, end);
    }
}
