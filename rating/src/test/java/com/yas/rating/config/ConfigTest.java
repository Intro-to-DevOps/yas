package com.yas.rating.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ConfigTest {

    @Test
    void testServiceUrlConfig() {
        ServiceUrlConfig config = new ServiceUrlConfig("prod", "cust", "ord");
        assertThat(config.product()).isEqualTo("prod");
        assertThat(config.customer()).isEqualTo("cust");
        assertThat(config.order()).isEqualTo("ord");
    }

    @Test
    void testDatabaseAutoConfig() {
        DatabaseAutoConfig config = new DatabaseAutoConfig();
        assertThat(config).isNotNull();
    }

    @Test
    void testRestClientConfig() {
        RestClientConfig config = new RestClientConfig();
        RestClient.Builder builder = mock(RestClient.Builder.class);
        RestClient restClient = mock(RestClient.class);
        when(builder.defaultHeader(any(), any())).thenReturn(builder);
        when(builder.build()).thenReturn(restClient);
        
        RestClient result = config.getRestClient(builder);
        assertThat(result).isEqualTo(restClient);
    }
    
    @Test
    void testSwaggerConfig() {
        SwaggerConfig config = new SwaggerConfig();
        assertThat(config).isNotNull();
    }
}
