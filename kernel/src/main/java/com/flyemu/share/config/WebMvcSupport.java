package com.flyemu.share.config;

import cn.dev33.satoken.dao.SaTokenDaoRedisJackson;
import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.date.LocalDateTimeUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.StrUtil;
import com.blazebit.persistence.ConfigurationProperties;
import com.blazebit.persistence.Criteria;
import com.blazebit.persistence.CriteriaBuilderFactory;
import com.blazebit.persistence.spi.CriteriaBuilderConfiguration;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalTimeSerializer;
import com.flyemu.share.converter.DateConverter;
import com.flyemu.share.converter.LocalDateConverter;
import com.flyemu.share.resolver.SaHandlerMethodArgumentResolver;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.annotation.Resource;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceUnit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Scope;
import org.springframework.format.FormatterRegistry;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurationSupport;

import java.io.IOException;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @功能描述: WebMvcSupport
 * @创建时间: 2023年08月08日
 * @公司官网: www.fenxi365.com
 * @公司信息: 纷析云（杭州）科技有限公司
 * @公司介绍: 专注于财务相关软件开发, 企业会计自动化解决方案
 */
@Slf4j
@Configuration
public class WebMvcSupport extends WebMvcConfigurationSupport implements InitializingBean {

    @Resource
    private ObjectMapper objectMapper;

    @Resource
    private SaTokenDaoRedisJackson saTokenDaoRedisJackson;

    @Resource
    private SaHandlerMethodArgumentResolver saHandlerMethodArgumentResolver;

    @PersistenceUnit
    private EntityManagerFactory entityManagerFactory;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        InterceptorRegistration interceptorRegistration = registry.addInterceptor(new SaInterceptor());
        List<String> list = new ArrayList<>();
        list.add("/wallet/**");
        list.add("/product/**");
        list.add("/category/**");
        interceptorRegistration.addPathPatterns("/**").excludePathPatterns(list);
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(saHandlerMethodArgumentResolver);
    }

    @Bean
    @Autowired
    public JPAQueryFactory jpaQuery(EntityManager entityManager) {
        return new JPAQueryFactory(entityManager);
    }

    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_SINGLETON)
    @Lazy(false)
    public CriteriaBuilderFactory createCriteriaBuilderFactory() {
        CriteriaBuilderConfiguration config = Criteria.getDefault();
        config.setProperty(ConfigurationProperties.INLINE_COUNT_QUERY, "false");
        config.setProperty(ConfigurationProperties.IMPLICIT_GROUP_BY_FROM_SELECT, "false");
        config.setProperty(ConfigurationProperties.IMPLICIT_GROUP_BY_FROM_HAVING, "false");
        config.setProperty(ConfigurationProperties.IMPLICIT_GROUP_BY_FROM_ORDER_BY, "false");
        return config.createCriteriaBuilderFactory(entityManagerFactory);
    }


    //    @Bean
//    public HibernatePropertiesCustomizer jsonFormatMapperCustomizer() {
//        return (properties) -> properties.put(AvailableSettings.JSON_FORMAT_MAPPER,
//                new JacksonJsonFormatMapper(objectMapper));
//    }
    @Override
    protected void addFormatters(FormatterRegistry registry) {
        registry.addConverter(new DateConverter());
        registry.addConverter(new LocalDateConverter());
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        SimpleModule simpleModule = new SimpleModule();
        simpleModule.addSerializer(LocalDate.class, new LocalDateSerializer(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
        simpleModule.addSerializer(LocalDateTime.class, new LocalDateTimeSerializer(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
        simpleModule.addSerializer(LocalTime.class, new LocalTimeSerializer(DateTimeFormatter.ofPattern("HH:mm:ss")));
        simpleModule.addDeserializer(Date.class, new JsonDeserializer<Date>() {
            @Override
            public Date deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
                return StrUtil.isNotEmpty(p.getText()) ? DateUtil.parse(p.getText(), "yyyy-MM-dd", "yyyy-MM", "yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd HH:mm") : null;
            }
        });
        simpleModule.addDeserializer(LocalDateTime.class, new JsonDeserializer<LocalDateTime>() {
            @Override
            public LocalDateTime deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
                if (StrUtil.isNotEmpty(p.getText())) {
                    DateTime dateTime = DateUtil.parse(p.getText(), "yyyy-MM-dd", "yyyy-MM", "yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd HH:mm");
                    return dateTime.toTimestamp().toLocalDateTime();
                }
                return null;
            }
        });
        simpleModule.addSerializer(Double.class, new JsonSerializer<Double>() {
            @Override
            public void serialize(Double value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
                if (value != null) {
                    gen.writeNumber(NumberUtil.roundStr(value, 2));
                }
            }
        });
        simpleModule.addSerializer(BigDecimal.class, new JsonSerializer<BigDecimal>() {
            @Override
            public void serialize(BigDecimal value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
                if (value != null) {
                    gen.writeNumber(NumberUtil.roundStr(value.doubleValue(), 5));
                }
            }
        });
        simpleModule.addDeserializer(LocalDate.class, new JsonDeserializer<LocalDate>() {
            @Override
            public LocalDate deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
                if (StrUtil.isNotEmpty(p.getText())) {
                    return LocalDateTimeUtil.of(DateUtil.parse(p.getText(), "yyyy-MM", "yyyy-MM-dd")).toLocalDate();
                }
                return null;
            }
        });

        objectMapper.registerModule(simpleModule);
        objectMapper.setDateFormat(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"));
        // 禁止将时间类型数据转换成时间戳
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
        saTokenDaoRedisJackson.objectMapper.registerModule(new JavaTimeModule());
        saTokenDaoRedisJackson.objectMapper.registerModule(new Jdk8Module());
        // 禁止将时间类型数据转换成时间戳
        saTokenDaoRedisJackson.objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        saTokenDaoRedisJackson.objectMapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
        saTokenDaoRedisJackson.objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        getMessageConverters().forEach(cv -> {
            if (cv instanceof MappingJackson2HttpMessageConverter) {
                ((MappingJackson2HttpMessageConverter) cv).setObjectMapper(objectMapper);
            }
        });
    }
}
