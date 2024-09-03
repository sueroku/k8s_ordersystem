package com.beyond.ordersystem.ordering.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// 암튼간에 디티오도 아니고 뭣도 아닌 클래스입니다. 편히 쓰려고 만든
@Data
@AllArgsConstructor
@NoArgsConstructor
public class StockDecreaseEvent {
    private Long productId;
    private Integer productCount;
}
