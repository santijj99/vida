package com.vida.apirest.tenant;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

import javax.sql.DataSource;

/**
 * Enruta cada request a la DB del tenant según {@link TenantContext}.
 */
public class TenantRoutingDataSource extends AbstractRoutingDataSource {

    private final ObjectProvider<TenantDataSourceManager> managerProvider;
    private DataSource defaultDataSource;

    public TenantRoutingDataSource(ObjectProvider<TenantDataSourceManager> managerProvider) {
        this.managerProvider = managerProvider;
    }

    public void setDefaultDataSource(DataSource defaultDataSource) {
        this.defaultDataSource = defaultDataSource;
        setDefaultTargetDataSource(defaultDataSource);
    }

    @Override
    protected Object determineCurrentLookupKey() {
        return TenantContext.getCodigoLicencia();
    }

    @Override
    protected DataSource determineTargetDataSource() {
        TenantDataSourceManager manager = managerProvider.getIfAvailable();
        if (manager == null || !manager.isMultiTenantEnabled()) {
            return defaultDataSource;
        }
        String codigo = TenantContext.getCodigoLicencia();
        if (codigo == null || codigo.isBlank()) {
            return defaultDataSource;
        }
        return manager.resolve(codigo);
    }
}
