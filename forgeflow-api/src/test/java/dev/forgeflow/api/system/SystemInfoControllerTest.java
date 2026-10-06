package dev.forgeflow.api.system;

import dev.forgeflow.api.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Import;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(AbstractIntegrationTest.class)
class SystemInfoControllerTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void returnsVersionAndUptime() {
        ResponseEntity<SystemInfoResponse> response = restTemplate.getForEntity("/api/v1/system/info", SystemInfoResponse.class);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().version()).isNotBlank();
        assertThat(response.getBody().uptimeSeconds()).isGreaterThanOrEqualTo(0);
    }
}
