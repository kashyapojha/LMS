package com.geeknito.LMS_backend;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
    "spring.datasource.url=${SPRING_DATASOURCE_URL:jdbc:postgresql://localhost:5432/lms_test}",
    "spring.datasource.username=${SPRING_DATASOURCE_USERNAME:postgres}",
    "spring.datasource.password=${SPRING_DATASOURCE_PASSWORD:password}",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.data.redis.host=${REDIS_HOST:localhost}",
    "spring.data.redis.port=${REDIS_PORT:6379}",
    "cloudinary.cloud-name=test",
    "cloudinary.api-key=test",
    "cloudinary.api-secret=test"
})
class LMSBackendApplicationTests {

    @Test
    void contextLoads() {
    }
}
