package com.vida.apirest.tenant;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Evita que TenantFilter se registre dos veces (servlet + SecurityFilterChain).
 */
@Configuration
public class TenantFilterRegistration {

    @Bean
    public FilterRegistrationBean<TenantFilter> disableTenantFilterServletRegistration(TenantFilter filter) {
        FilterRegistrationBean<TenantFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }
}
