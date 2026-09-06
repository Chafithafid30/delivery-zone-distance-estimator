package com.example.delivery.geocoding;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
            "INSTANCE_ID=backend-test-1",
            "spring.datasource.url=jdbc:h2:mem:swarm-backend;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1"
        })
@ActiveProfiles({"test", "swarm"})
class SwarmProfileTest {
    @Autowired private ApplicationContext context;
    @Autowired private TestRestTemplate http;

    @Test
    void replicatedBackendUsesRemoteResolverAndExposesItsInstanceIdentity() {
        assertThat(context.getBean(AddressResolver.class))
                .isInstanceOf(RemoteAddressResolver.class);
        assertThat(context.getBeansOfType(GeocodingProvider.class)).isEmpty();
        assertThat(context.getBeansOfType(GeocodingService.class)).isEmpty();
        var response = http.getForEntity("/api/instance", String.class);
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).contains("backend-test-1");
        assertThat(response.getHeaders().getCacheControl()).isEqualTo("no-store");
        assertThat(http.getForEntity("/api/deliveries", String.class).getStatusCode().value())
                .isEqualTo(200);
        assertThat(
                        http.postForEntity("/internal/geocoding", "{}", String.class)
                                .getStatusCode()
                                .value())
                .isEqualTo(404);
    }
}
