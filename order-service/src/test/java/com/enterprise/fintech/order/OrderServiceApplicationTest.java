package com.enterprise.fintech.order;

import com.enterprise.fintech.order.service.MinioStorageService;
import com.enterprise.fintech.order.service.PaymentGrpcClient;
import jakarta.jms.ConnectionFactory;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.jms.core.JmsTemplate;

import org.springframework.test.context.TestPropertySource;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.data.redis.RedisReactiveAutoConfiguration"
})
class OrderServiceApplicationTest {

    @MockBean
    private ConnectionFactory connectionFactory;

    @MockBean
    private JmsTemplate jmsTemplate;

    @MockBean
    private MinioStorageService minioStorageService;

    @MockBean
    private PaymentGrpcClient paymentGrpcClient;

    @Test
    void contextLoads() {
    }
}
