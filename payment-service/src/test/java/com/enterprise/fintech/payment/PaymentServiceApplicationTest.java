package com.enterprise.fintech.payment;

import jakarta.jms.ConnectionFactory;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
        "grpc.server.port=-1"
})
class PaymentServiceApplicationTest {

    @MockBean
    private ConnectionFactory connectionFactory;

    @MockBean
    private JmsTemplate jmsTemplate;

    @Test
    void contextLoads() {
    }
}
