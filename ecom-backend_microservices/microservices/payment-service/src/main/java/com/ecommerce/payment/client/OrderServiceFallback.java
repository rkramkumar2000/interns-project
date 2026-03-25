package com.ecommerce.payment.client;

import com.ecommerce.payment.dto.OrderDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class OrderServiceFallback implements OrderServiceClient {
    
    @Override
    public OrderDTO getOrderById(Long orderId) {
        log.error("Fallback: Unable to get order with id {}", orderId);
        OrderDTO order = new OrderDTO();
        order.setOrderId(orderId);
        order.setAvailable(false);
        return order;
    }
    
    @Override
    public OrderDTO updatePaymentStatus(Long orderId, String paymentId, String paymentStatus) {
        log.error("Fallback: Unable to update payment status for order {}", orderId);
        // In a real scenario, might want to queue this operation for retry
        OrderDTO order = new OrderDTO();
        order.setOrderId(orderId);
        order.setAvailable(false);
        return order;
    }
} 