/*
 * Copyright (c) 2020. Tolgee
 */

package io.tolgee.configuration

import com.fasterxml.jackson.databind.ObjectMapper
import io.tolgee.configuration.tolgee.TolgeeProperties
import org.springframework.boot.web.servlet.MultipartConfigFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.CacheControl
import org.springframework.util.unit.DataSize
import org.springframework.web.client.RestTemplate
import org.springframework.web.servlet.config.annotation.CorsRegistry
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import java.security.SecureRandom
import java.util.concurrent.TimeUnit
import javax.servlet.MultipartConfigElement


@Configuration
open class WebConfiguration(
        private val tolgeeProperties: TolgeeProperties
) : WebMvcConfigurer {
    override fun addViewControllers(registry: ViewControllerRegistry) {
        registry.run {
            val forwardTo = "forward:/"
            addViewController("/{spring:[\\w-_]+}")
                    .setViewName(forwardTo)
            addViewController("/**/{spring:[\\w-_]+}")
                    .setViewName(forwardTo)
            addViewController("/{spring:\\w+}/**{spring:?!(\\.js|\\.css||\\.woff2)$}")
                    .setViewName(forwardTo)
        }
    }

    override fun addResourceHandlers(registry: ResourceHandlerRegistry) {
        registry.addResourceHandler("/*.js", "/**/*.woff2", "/*.css", "/**/*.svg")
                .addResourceLocations("classpath:/static/")
                .setCacheControl(CacheControl.maxAge(365, TimeUnit.DAYS))
    }

    override fun addCorsMappings(registry: CorsRegistry) {
        registry.addMapping("/**").allowedMethods("GET", "POST", "PUT", "DELETE")
    }

    @Bean
    open fun restTemplate(): RestTemplate {
        return RestTemplate()
    }

    @Bean
    open fun secureRandom(): SecureRandom {
        return SecureRandom()
    }

    @Bean
    open fun objectMapper(): ObjectMapper {
        return ObjectMapper()
    }

    @Bean
    open fun multipartConfigElement(): MultipartConfigElement {
        val factory = MultipartConfigFactory()
        factory.setMaxFileSize(DataSize.ofKilobytes(tolgeeProperties.maxUploadFileSize.toLong()))
        factory.setMaxRequestSize(DataSize.ofKilobytes(tolgeeProperties.maxUploadFileSize.toLong()))
        return factory.createMultipartConfig()
    }
}
