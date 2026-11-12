package com.msjs.project.config;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonStreamContext;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Jackson 全局配置
 * <p>
 * 1. LocalDateTime / LocalDate 统一格式化为 "yyyy-MM-dd HH:mm:ss" / "yyyy-MM-dd"
 * 2. Long 反序列化兼容（前端传 "字符串" 或 "裸数字" 两种情况都能正确转 Long）
 *    —— 雪花ID 在各实体/VO 的 id 字段上单独标注 @JsonSerialize(using = ToStringSerializer.class)
 *    —— 不再全局 Long→String（会导致 PageVO.total 等数值字段变成字符串，ElPagination 渲染失败）
 */
@Configuration
public class JacksonConfig {

    private static final String DATE_TIME_PATTERN = "yyyy-MM-dd HH:mm:ss";
    private static final String DATE_PATTERN = "yyyy-MM-dd";

    /**
     * 全局定制：日期格式化 + Long↔String
     */
    @Bean
    public Jackson2ObjectMapperBuilderCustomizer jacksonCustomizer() {
        return builder -> {
            DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern(DATE_TIME_PATTERN);
            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern(DATE_PATTERN);
            // —— 日期序列化 ——
            builder.serializerByType(LocalDateTime.class, new LocalDateTimeSerializer(dateTimeFormatter));
            builder.deserializerByType(LocalDateTime.class, new LocalDateTimeDeserializer(dateTimeFormatter));
            builder.serializerByType(LocalDate.class, new LocalDateSerializer(dateFormatter));
            builder.deserializerByType(LocalDate.class, new LocalDateDeserializer(dateFormatter));
            // —— Long 反序列化（兼容 "字符串" 或 "裸数字"）——
            // 雪花ID 19 位，前端 JSON.parse 后再传给后端可能丢失精度，
            // 所以前端传字符串是更安全的方式；这里两种输入都支持
            builder.deserializerByType(Long.class, new SafeLongDeserializer());
            builder.deserializerByType(long.class, new SafeLongDeserializer());
        };
    }

    /**
     * 安全的 Long 反序列化器
     * <p>
     * 优先按字符串读取（前端传 "1234567890123456789" 这种），转 Long；
     * 如果是裸数字（number token，旧前端遗留），让 Jackson 默认 LongDeserializer 处理。
     */
    public static class SafeLongDeserializer extends JsonDeserializer<Long> {
        @Override
        public Long deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            if (p.currentTokenId() == com.fasterxml.jackson.core.JsonTokenId.ID_STRING) {
                String text = p.getText();
                if (text == null || text.isEmpty()) return null;
                try {
                    return Long.parseLong(text.trim());
                } catch (NumberFormatException e) {
                    throw ctxt.weirdStringException(text, Long.class, "Not a valid Long value");
                }
            }
            // 裸 number / null → 直接读值（等价于 Jackson 默认行为）
            if (p.currentTokenId() == com.fasterxml.jackson.core.JsonTokenId.ID_NULL) {
                return null;
            }
            return p.getLongValue();
        }
    }
}
