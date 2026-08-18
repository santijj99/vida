package com.vida.apirest.tenant;

import com.vida.apirest.config.AuthRateLimitFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Evita que filtros de seguridad se registren dos veces (servlet + SecurityFilterChain).
 */
@Configuration
public class TenantFilterRegistration {

    @Bean
    public FilterRegistrationBean<TenantFilter> disableTenantFilterServletRegistration(TenantFilter filter) {
        FilterRegistrationBean<TenantFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }

    @Bean
    public FilterRegistrationBean<AuthRateLimitFilter> disableAuthRateLimitServletRegistration(
            AuthRateLimitFilter filter) {
        FilterRegistrationBean<AuthRateLimitFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }
}
