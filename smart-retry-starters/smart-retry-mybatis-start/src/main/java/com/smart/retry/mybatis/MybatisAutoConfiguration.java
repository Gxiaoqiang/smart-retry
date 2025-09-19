package com.smart.retry.mybatis;


import com.smart.retry.common.RetryConfiguration;
import com.smart.retry.common.RetryContainer;
import com.smart.retry.common.RetryTaskAccess;
import com.smart.retry.common.RetryTaskHeart;
import com.smart.retry.core.CommonConfiguration;
import com.smart.retry.core.HeartbeatContainer;
import com.smart.retry.core.SimpleContainer;
import com.smart.retry.core.config.SmartExecutorConfigure;
import com.smart.retry.mybatis.access.MybatisAccess;
import com.smart.retry.mybatis.config.SmartConfigure;
import com.smart.retry.mybatis.dao.RetryShardingDao;
import com.smart.retry.mybatis.dao.RetryTaskDao;
import com.smart.retry.mybatis.heart.MybatisHeart;
import com.smart.retry.mybatis.repo.RetryShardingRepo;
import com.smart.retry.mybatis.repo.RetryTaskRepo;
import com.smart.retry.mybatis.repo.impl.RetryShardingRepoImpl;
import com.smart.retry.mybatis.repo.impl.RetryTaskRepoImpl;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.mapper.MapperScannerConfigurer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;

/**
 * @author gwq
 */
@Configuration
@EnableConfigurationProperties(SmartConfigure.class)
@ConditionalOnProperty(prefix = "spring.smart-retry.mybatis", name = "enabled",  matchIfMissing = true)
@Component
public class MybatisAutoConfiguration extends CommonConfiguration
        implements ApplicationContextAware , EnvironmentAware {

    private static final Logger LOGGER = LoggerFactory.getLogger(MybatisAutoConfiguration.class);

    private ApplicationContext applicationContext;

    private Environment environment;

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @Bean
    public MapperScannerConfigurer mapperScannerConfigurer() {
        MapperScannerConfigurer scannerConfigurer = new MapperScannerConfigurer();
        scannerConfigurer.setSqlSessionFactoryBeanName("smartRetrySqlSessionFactory"); // 设置 SqlSessionFactoryBean 的名称
        scannerConfigurer.setBasePackage("com.smart.retry.mybatis.dao"); // 设置你的 mapper 接口所在的包
        return scannerConfigurer;
    }

    @Bean("smartRetrySqlSessionFactory")
    public SqlSessionFactory smartRetrySqlSessionFactory( SmartConfigure smartConfigure)
            throws Exception {
        //environment.getActiveProfiles("spring.smart-retry.mybatis.dataSource")
        SqlSessionFactoryBean sqlSessionFactoryBean = new SqlSessionFactoryBean();
        //SmartConfigure smartConfigure = applicationContext.getBean(SmartConfigure.class);
        if(smartConfigure == null||smartConfigure.getDatasource() == null) {
            throw new IllegalArgumentException("spring.smart-retry.mybatis.datasource is not configured");
        }

        sqlSessionFactoryBean.setDataSource((DataSource)applicationContext.getBean(smartConfigure.getDatasource()));
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        Resource resource = resolver.getResource("classpath:smart-mybatis-config.xml");
        sqlSessionFactoryBean.setConfigLocation(resource);
        return sqlSessionFactoryBean.getObject();
    }



    @Bean
    public RetryShardingRepo retryShardingRepo(RetryShardingDao retryShardingDao) {
        return new RetryShardingRepoImpl(retryShardingDao);

    }

    @Bean
    public RetryTaskHeart retryTaskHeart(RetryShardingRepo retryShardingRepo) {
        return new MybatisHeart(retryShardingRepo);
    }



    @Bean
    public RetryTaskRepo retryTaskRepo(RetryTaskDao retryTaskDao) {
        return new RetryTaskRepoImpl(retryTaskDao);
    }

    @Bean
    public RetryTaskAccess retryTaskAccess(RetryTaskRepo retryTaskRepo) {
        return new MybatisAccess(retryTaskRepo);
    }
    @Bean(initMethod = "start")
    public HeartbeatContainer heartbeatContainer(RetryTaskHeart retryTaskHeart) {
        LOGGER.warn("[MybatisAutoConfiguration#heartbeatContainer] heartbeatContainer init");
        return new HeartbeatContainer(retryTaskHeart);
    }
    @Bean(initMethod = "start")
    public RetryContainer retryContainer(SmartExecutorConfigure smartExecutorConfigure, HeartbeatContainer heartbeatContainer,
                                         RetryConfiguration configuration) {
        LOGGER.warn("[MybatisAutoConfiguration#retryContainer] retryContainer init");
        return new SimpleContainer(configuration,smartExecutorConfigure);
    }

}
