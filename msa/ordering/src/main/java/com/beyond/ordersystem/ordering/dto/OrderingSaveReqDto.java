package com.beyond.ordersystem.ordering.dto;

import com.beyond.ordersystem.ordering.domain.OrderDetail;
import com.beyond.ordersystem.ordering.domain.OrderStatus;
import com.beyond.ordersystem.ordering.domain.Ordering;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OrderingSaveReqDto {
    private Long productId;
    private Integer productCount; // 이렇게 살리면 ~ 컨트롤러에서 OrderingSaveReqDto말고 List<OrderingSaveReqDto> 받으면 돼징


}





// 내가 하던거..?
//    public Ordering toOEntity(Member member){
//        List<OrderDetail> orderDetails = new ArrayList<>();
//        for(OrderDetailDto dto : this.orderDetailList)
//
//        Ordering ordering = Ordering.builder()
//                .orderStatus(OrderStatus.ORDERED).member(member)
//                .orderDetailList(this.orderDetailList).build();
//    }

// 강사님
//@Data
//@NoArgsConstructor
//@AllArgsConstructor
//@Builder
//public class OrderSaveReqDto {
//    private Long memberId;
//    private List<OrderDto> orderDtos;
//
//    @Data
//    @NoArgsConstructor
//    @AllArgsConstructor
//    @Builder
//    public static class OrderDto{
//        private Long productId;
//        private Integer productCount;
//    }
//
//    public Ordering toEntity(Member member){
//        return Ordering.builder()
//                .member(member)
//                .orderStatus(OrderStatus.ORDERED)
//                .build();
//    }
//}


