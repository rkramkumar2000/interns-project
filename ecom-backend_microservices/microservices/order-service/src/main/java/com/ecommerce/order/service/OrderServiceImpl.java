package com.ecommerce.order.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ecommerce.order.client.CartServiceClient;
import com.ecommerce.order.client.ProductServiceClient;
import com.ecommerce.order.dto.AddressDTO;
import com.ecommerce.order.dto.CartDTO;
import com.ecommerce.order.dto.CartItemDTO;
import com.ecommerce.order.dto.CreateOrderRequest;
import com.ecommerce.order.dto.OrderDTO;
import com.ecommerce.order.dto.OrderItemDTO;
import com.ecommerce.order.dto.OrderStatisticsDTO;
import com.ecommerce.order.dto.ProductDTO;
import com.ecommerce.order.dto.UpdateOrderStatusRequest;
import com.ecommerce.order.dto.UserOrderStatisticsDTO;
import com.ecommerce.order.exception.OrderException;
import com.ecommerce.order.exception.OrderNotFoundException;
import com.ecommerce.order.model.Order;
import com.ecommerce.order.model.OrderItem;
import com.ecommerce.order.model.OrderStatus;
import com.ecommerce.order.repository.OrderRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class OrderServiceImpl implements OrderService {
    
    private final OrderRepository orderRepository;
    private final CartServiceClient cartServiceClient;
    private final ProductServiceClient productServiceClient;
    private final ModelMapper modelMapper;
    private final ObjectMapper objectMapper;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    
    private static final String ORDER_TOPIC = "order-events";
    
    @Override
    public OrderDTO createOrder(Long userId, CreateOrderRequest request) {
        try {
            // Get cart details
            CartDTO cart = cartServiceClient.getCartById(request.getCartId());
            
            if (cart == null || cart.getItems().isEmpty()) {
                throw new OrderException("Cart is empty or not found");
            }
            
            // Verify cart belongs to user
            if (!userId.equals(cart.getUserId())) {
                throw new OrderException("Cart does not belong to the user");
            }
            
            // Create order
            Order order = new Order();
            order.setUserId(userId);
            order.setUserEmail(cart.getUserId() + "@temp.com"); // Should get from auth service
            order.setOrderStatus(OrderStatus.PENDING);
            order.setPaymentMethod(request.getPaymentMethod());
            order.setPaymentStatus("PENDING");
            order.setCouponCode(cart.getCouponCode());
            
            // Set addresses
            order.setDeliveryAddress(convertAddressToJson(request.getDeliveryAddress()));
            order.setBillingAddress(convertAddressToJson(
                request.getBillingAddress() != null ? request.getBillingAddress() : request.getDeliveryAddress()
            ));
            
            // Set order notes
            order.setOrderNotes(request.getOrderNotes());
            
            // Create order items from cart items
            List<OrderItem> orderItems = createOrderItems(cart.getItems(), order);
            order.setOrderItems(orderItems);
            
            // Calculate pricing
            order.setSubtotal(cart.getSubtotal());
            order.setDiscount(cart.getDiscount() != null ? cart.getDiscount() : BigDecimal.ZERO);
            order.setShippingCost(request.getShippingCost());
            order.setTax(calculateTax(cart.getSubtotal()));
            order.calculateTotalAmount();
            
            // Save order
            Order savedOrder = orderRepository.save(order);
            
            // Update product stock
            updateProductStock(orderItems, false);
            
            // Clear cart
            cartServiceClient.clearCart(userId);
            
            // Publish order created event
            publishOrderEvent("ORDER_CREATED", savedOrder);
            
            return convertToDTO(savedOrder);
            
        } catch (Exception e) {
            log.error("Error creating order: {}", e.getMessage());
            throw new OrderException("Failed to create order: " + e.getMessage());
        }
    }
    
    private List<OrderItem> createOrderItems(List<CartItemDTO> cartItems, Order order) {
        return cartItems.stream().map(cartItem -> {
            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(order);
            orderItem.setProductId(cartItem.getProductId());
            orderItem.setProductName(cartItem.getProductName());
            orderItem.setProductImage(cartItem.getProductImage());
            orderItem.setUnitPrice(cartItem.getPrice());
            orderItem.setQuantity(cartItem.getQuantity());
            orderItem.setDiscount(cartItem.getDiscount() != null ? cartItem.getDiscount() : BigDecimal.ZERO);
            orderItem.setTotalPrice(cartItem.getTotalPrice());
            
            // Get additional product details
            try {
                ProductDTO product = productServiceClient.getProductById(cartItem.getProductId());
                if (product != null) {
                    orderItem.setProductSku(product.getSku());
                    orderItem.setProductDescription(product.getDescription());
                }
            } catch (Exception e) {
                log.warn("Could not fetch product details for product {}", cartItem.getProductId());
            }
            
            return orderItem;
        }).collect(Collectors.toList());
    }
    
    private void updateProductStock(List<OrderItem> orderItems, boolean increase) {
        for (OrderItem item : orderItems) {
            try {
                Boolean success = productServiceClient.updateStock(
                    item.getProductId(), 
                    item.getQuantity(), 
                    increase
                );
                if (!success) {
                    log.warn("Failed to update stock for product {}", item.getProductId());
                }
            } catch (Exception e) {
                log.error("Error updating stock for product {}: {}", item.getProductId(), e.getMessage());
            }
        }
    }
    
    private String convertAddressToJson(AddressDTO address) {
        try {
            return objectMapper.writeValueAsString(address);
        } catch (Exception e) {
            log.error("Error converting address to JSON", e);
            return "{}";
        }
    }
    
    private AddressDTO convertJsonToAddress(String json) {
        try {
            return objectMapper.readValue(json, AddressDTO.class);
        } catch (Exception e) {
            log.error("Error converting JSON to address", e);
            return new AddressDTO();
        }
    }
    
    private BigDecimal calculateTax(BigDecimal subtotal) {
        // Simple 10% tax calculation
        return subtotal.multiply(BigDecimal.valueOf(0.10));
    }
    
    private void publishOrderEvent(String eventType, Order order) {
        try {
            Map<String, Object> event = new HashMap<>();
            event.put("eventType", eventType);
            event.put("orderId", order.getOrderId());
            event.put("userId", order.getUserId());
            event.put("orderStatus", order.getOrderStatus().toString());
            event.put("totalAmount", order.getTotalAmount());
            event.put("timestamp", LocalDateTime.now());
            
            kafkaTemplate.send(ORDER_TOPIC, event);
            log.info("Published {} event for order {}", eventType, order.getOrderId());
        } catch (Exception e) {
            log.error("Error publishing order event", e);
        }
    }
    
    private OrderDTO convertToDTO(Order order) {
        OrderDTO orderDTO = modelMapper.map(order, OrderDTO.class);
        
        // Convert JSON addresses back to DTOs
        orderDTO.setDeliveryAddress(convertJsonToAddress(order.getDeliveryAddress()));
        orderDTO.setBillingAddress(convertJsonToAddress(order.getBillingAddress()));
        
        // Map order items
        List<OrderItemDTO> itemDTOs = order.getOrderItems().stream()
            .map(item -> modelMapper.map(item, OrderItemDTO.class))
            .collect(Collectors.toList());
        orderDTO.setOrderItems(itemDTOs);
        
        return orderDTO;
    }
    
    @Override
    public OrderDTO getOrderById(Long orderId) {
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new OrderNotFoundException(orderId));
        return convertToDTO(order);
    }
    
    @Override
    public Page<OrderDTO> getOrdersByUserId(Long userId, Pageable pageable) {
        Page<Order> orders = orderRepository.findByUserIdOrderByOrderDateDesc(userId, pageable);
        return orders.map(this::convertToDTO);
    }
    
    @Override
    public List<OrderDTO> getAllOrdersByUserId(Long userId) {
        List<Order> orders = orderRepository.findByUserId(userId);
        return orders.stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());
    }
    
    @Override
    public Page<OrderDTO> getOrdersByStatus(OrderStatus status, Pageable pageable) {
        Page<Order> orders = orderRepository.findByOrderStatus(status, pageable);
        return orders.map(this::convertToDTO);
    }
    
    @Override
    public OrderDTO updateOrderStatus(Long orderId, UpdateOrderStatusRequest request) {
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new OrderNotFoundException(orderId));
        
        // Validate status transition
        if (!isValidStatusTransition(order.getOrderStatus(), request.getOrderStatus())) {
            throw new OrderException("Invalid status transition from " + 
                order.getOrderStatus() + " to " + request.getOrderStatus());
        }
        
        order.setOrderStatus(request.getOrderStatus());
        
        if (request.getTrackingNumber() != null) {
            order.setTrackingNumber(request.getTrackingNumber());
        }
        
        if (request.getOrderStatus() == OrderStatus.DELIVERED) {
            order.setDeliveryDate(LocalDateTime.now());
        }
        
        Order updatedOrder = orderRepository.save(order);
        
        // Publish order status update event
        publishOrderEvent("ORDER_STATUS_UPDATED", updatedOrder);
        
        return convertToDTO(updatedOrder);
    }
    
    @Override
    public OrderDTO cancelOrder(Long orderId, String reason) {
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new OrderNotFoundException(orderId));
        
        // Check if order can be cancelled
        if (!canBeCancelled(order.getOrderStatus())) {
            throw new OrderException("Order cannot be cancelled in current status: " + order.getOrderStatus());
        }
        
        order.setOrderStatus(OrderStatus.CANCELLED);
        order.setOrderNotes(order.getOrderNotes() + "\nCancellation reason: " + reason);
        
        Order cancelledOrder = orderRepository.save(order);
        
        // Restore product stock
        updateProductStock(order.getOrderItems(), true);
        
        // Publish order cancelled event
        publishOrderEvent("ORDER_CANCELLED", cancelledOrder);
        
        return convertToDTO(cancelledOrder);
    }
    
    @Override
    public OrderDTO getOrderByTrackingNumber(String trackingNumber) {
        Order order = orderRepository.findByTrackingNumber(trackingNumber)
            .orElseThrow(() -> new OrderException("Order not found with tracking number: " + trackingNumber));
        return convertToDTO(order);
    }
    
    @Override
    public Page<OrderDTO> getAllOrders(Pageable pageable) {
        Page<Order> orders = orderRepository.findAll(pageable);
        return orders.map(this::convertToDTO);
    }
    
    @Override
    public Page<OrderDTO> getOrdersBetweenDates(LocalDateTime startDate, LocalDateTime endDate, Pageable pageable) {
        Page<Order> orders = orderRepository.findOrdersBetweenDates(startDate, endDate, pageable);
        return orders.map(this::convertToDTO);
    }
    
    @Override
    public OrderStatisticsDTO getOrderStatistics() {
        return OrderStatisticsDTO.builder()
            .totalOrders(orderRepository.count())
            .pendingOrders(orderRepository.countByOrderStatus(OrderStatus.PENDING))
            .processingOrders(orderRepository.countByOrderStatus(OrderStatus.PROCESSING))
            .shippedOrders(orderRepository.countByOrderStatus(OrderStatus.SHIPPED))
            .deliveredOrders(orderRepository.countByOrderStatus(OrderStatus.DELIVERED))
            .cancelledOrders(orderRepository.countByOrderStatus(OrderStatus.CANCELLED))
            .refundedOrders(orderRepository.countByOrderStatus(OrderStatus.REFUNDED))
            .build();
    }
    
    @Override
    public UserOrderStatisticsDTO getUserOrderStatistics(Long userId) {
        List<Order> userOrders = orderRepository.findByUserId(userId);
        
        Long totalOrders = (long) userOrders.size();
        Long deliveredOrders = userOrders.stream()
            .filter(o -> o.getOrderStatus() == OrderStatus.DELIVERED)
            .count();
        Long cancelledOrders = userOrders.stream()
            .filter(o -> o.getOrderStatus() == OrderStatus.CANCELLED)
            .count();
        Long activeOrders = userOrders.stream()
            .filter(o -> Arrays.asList(OrderStatus.PENDING, OrderStatus.PROCESSING, OrderStatus.SHIPPED)
                .contains(o.getOrderStatus()))
            .count();
        
        BigDecimal totalSpent = userOrders.stream()
            .filter(o -> o.getOrderStatus() == OrderStatus.DELIVERED)
            .map(Order::getTotalAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal averageOrderValue = totalOrders > 0 ? 
            totalSpent.divide(BigDecimal.valueOf(deliveredOrders > 0 ? deliveredOrders : 1), 2, RoundingMode.HALF_UP) : 
            BigDecimal.ZERO;
        
        return UserOrderStatisticsDTO.builder()
            .userId(userId)
            .totalOrders(totalOrders)
            .deliveredOrders(deliveredOrders)
            .cancelledOrders(cancelledOrders)
            .activeOrders(activeOrders)
            .totalSpent(totalSpent)
            .averageOrderValue(averageOrderValue)
            .build();
    }
    
    @Override
    public OrderDTO updatePaymentStatus(Long orderId, String paymentId, String paymentStatus) {
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new OrderNotFoundException(orderId));
        
        order.setPaymentId(Long.parseLong(paymentId));
        order.setPaymentStatus(paymentStatus);
        
        if ("SUCCESS".equals(paymentStatus) || "COMPLETED".equals(paymentStatus)) {
            order.setOrderStatus(OrderStatus.CONFIRMED);
        } else if ("FAILED".equals(paymentStatus)) {
            order.setOrderStatus(OrderStatus.FAILED);
        }
        
        Order updatedOrder = orderRepository.save(order);
        
        // Publish payment status update event
        publishOrderEvent("PAYMENT_STATUS_UPDATED", updatedOrder);
        
        return convertToDTO(updatedOrder);
    }
    
    @Override
    public boolean hasUserOrderedProduct(Long userId, Long productId) {
        List<Order> userOrders = orderRepository.findByUserId(userId);
        
        return userOrders.stream()
            .filter(o -> o.getOrderStatus() == OrderStatus.DELIVERED)
            .flatMap(o -> o.getOrderItems().stream())
            .anyMatch(item -> item.getProductId().equals(productId));
    }
    
    @Override
    public List<OrderDTO> getRecentOrdersByUserId(Long userId, int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        List<Order> recentOrders = orderRepository.findRecentOrdersByUserId(userId, pageable);
        return recentOrders.stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());
    }
    
    private boolean isValidStatusTransition(OrderStatus currentStatus, OrderStatus newStatus) {
        Map<OrderStatus, List<OrderStatus>> validTransitions = new HashMap<>();
        validTransitions.put(OrderStatus.PENDING, Arrays.asList(OrderStatus.CONFIRMED, OrderStatus.CANCELLED));
        validTransitions.put(OrderStatus.CONFIRMED, Arrays.asList(OrderStatus.PROCESSING, OrderStatus.CANCELLED));
        validTransitions.put(OrderStatus.PROCESSING, Arrays.asList(OrderStatus.SHIPPED, OrderStatus.CANCELLED));
        validTransitions.put(OrderStatus.SHIPPED, Arrays.asList(OrderStatus.DELIVERED, OrderStatus.CANCELLED));
        validTransitions.put(OrderStatus.DELIVERED, Arrays.asList(OrderStatus.REFUNDED));
        validTransitions.put(OrderStatus.CANCELLED, Arrays.asList(OrderStatus.REFUNDED));
        
        return validTransitions.getOrDefault(currentStatus, Collections.emptyList()).contains(newStatus);
    }
    
    private boolean canBeCancelled(OrderStatus status) {
        return Arrays.asList(OrderStatus.PENDING, OrderStatus.CONFIRMED, OrderStatus.PROCESSING, OrderStatus.SHIPPED)
            .contains(status);
    }
} 