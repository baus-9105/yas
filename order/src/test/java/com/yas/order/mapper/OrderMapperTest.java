package com.yas.order.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.yas.order.model.csv.OrderItemCsv;
import com.yas.order.viewmodel.order.OrderBriefVm;
import com.yas.order.viewmodel.orderaddress.OrderAddressVm;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

class OrderMapperTest {

    private OrderMapper orderMapper;

    @BeforeEach
    void setUp() {
        orderMapper = Mappers.getMapper(OrderMapper.class);
    }

    @Test
    @DisplayName("Should map OrderBriefVm to OrderItemCsv correctly")
    void toCsv_shouldMapCorrectly() {
        OrderAddressVm billingAddress = OrderAddressVm.builder()
                .phone("1234567890")
                .build();
                
        OrderBriefVm briefVm = OrderBriefVm.builder()
                .id(10L)
                .email("test@example.com")
                .billingAddressVm(billingAddress)
                .build();
        
        OrderItemCsv csv = orderMapper.toCsv(briefVm);
        
        assertThat(csv).isNotNull();
        assertThat(csv.getId()).isEqualTo(10L);
        assertThat(csv.getPhone()).isEqualTo("1234567890");
        assertThat(csv.getEmail()).isEqualTo("test@example.com");
    }
}
