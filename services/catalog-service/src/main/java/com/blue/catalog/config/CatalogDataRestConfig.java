package com.blue.catalog.config;

import com.blue.catalog.entity.Product;
import com.blue.catalog.entity.ProductCategory;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.rest.core.config.RepositoryRestConfiguration;
import org.springframework.data.rest.webmvc.config.RepositoryRestConfigurer;
import org.springframework.http.HttpMethod;
import org.springframework.web.servlet.config.annotation.CorsRegistry;

@Configuration
public class CatalogDataRestConfig implements RepositoryRestConfigurer {

    @Override
    public void configureRepositoryRestConfiguration(RepositoryRestConfiguration config, CorsRegistry cors) {
        HttpMethod[] unsupported = {HttpMethod.PUT, HttpMethod.POST, HttpMethod.DELETE, HttpMethod.PATCH};
        for (Class<?> type : new Class<?>[]{Product.class, ProductCategory.class}) {
            config.getExposureConfiguration()
                    .forDomainType(type)
                    .withItemExposure((md, m) -> m.disable(unsupported))
                    .withCollectionExposure((md, m) -> m.disable(unsupported));
        }
        config.exposeIdsFor(Product.class, ProductCategory.class);
    }
}
