package com.routive.healthhabit.health;

import static org.assertj.core.api.Assertions.assertThat;

import com.routive.healthhabit.common.api.ApiResponse;
import java.util.Map;
import org.junit.jupiter.api.Test;

class HealthControllerTest {

    @Test
    void returnsUpStatus() {
        ApiResponse<Map<String, Object>> response = new HealthController().health();

        assertThat(response.data()).containsEntry("status", "UP");
        assertThat(response.data()).containsEntry("service", "health-habit-api");
    }
}
