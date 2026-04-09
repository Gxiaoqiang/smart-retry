package com.smart.retry.web.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

import java.io.IOException;

/**
 * Web MVC配置
 * 用于支持Vue Router的History模式和跨域配置
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    /**
     * 配置跨域
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }

    /**
     * 配置静态资源处理
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/static/")
                .resourceChain(true)
                .addResolver(new PathResourceResolver() {
                    @Override
                    protected Resource getResource(String resourcePath, Resource location) throws IOException {
                        Resource requestedResource = location.createRelative(resourcePath);

                        // 如果资源存在且可读，返回该资源
                        if (requestedResource.exists() && requestedResource.isReadable()) {
                            return requestedResource;
                        }

                        // 对于前端路由，返回 index.html
                        // 排除 API 请求和静态资源文件
                        if (!resourcePath.startsWith("api/") &&
                                !resourcePath.endsWith(".js") &&
                                !resourcePath.endsWith(".css") &&
                                !resourcePath.endsWith(".png") &&
                                !resourcePath.endsWith(".jpg") &&
                                !resourcePath.endsWith(".jpeg") &&
                                !resourcePath.endsWith(".gif") &&
                                !resourcePath.endsWith(".svg") &&
                                !resourcePath.endsWith(".ico") &&
                                !resourcePath.endsWith(".woff") &&
                                !resourcePath.endsWith(".woff2") &&
                                !resourcePath.endsWith(".ttf") &&
                                !resourcePath.endsWith(".otf") &&
                                !resourcePath.endsWith(".otf") &&
                                !resourcePath.endsWith(".svg") &&
                                !resourcePath.endsWith(".eot")) {
                            return new ClassPathResource("/static/index.html");
                        }

                        return null;
                    }
                });
    }

    /**
     * 配置默认视图控制器
     */
    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        // 将根路径映射到 index.html
        registry.addViewController("/").setViewName("forward:/index.html");
    }
}
