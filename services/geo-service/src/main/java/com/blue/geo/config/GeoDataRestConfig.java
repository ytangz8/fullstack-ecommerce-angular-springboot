package com.blue.geo.config;

import com.blue.geo.entity.Country;
import com.blue.geo.entity.State;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.rest.core.config.RepositoryRestConfiguration;
import org.springframework.data.rest.webmvc.config.RepositoryRestConfigurer;
import org.springframework.http.HttpMethod;
import org.springframework.web.servlet.config.annotation.CorsRegistry;

@Configuration
public class GeoDataRestConfig implements RepositoryRestConfigurer {

    @Override
    public void configureRepositoryRestConfiguration(RepositoryRestConfiguration config, CorsRegistry cors) {
        HttpMethod[] unsupported = {HttpMethod.PUT, HttpMethod.POST, HttpMethod.DELETE, HttpMethod.PATCH};
        for (Class<?> type : new Class<?>[]{Country.class, State.class}) {
            config.getExposureConfiguration()
                    .forDomainType(type)
                    .withItemExposure((md, m) -> m.disable(unsupported))
                    .withCollectionExposure((md, m) -> m.disable(unsupported));
        }
        config.exposeIdsFor(Country.class, State.class);
    }
}
