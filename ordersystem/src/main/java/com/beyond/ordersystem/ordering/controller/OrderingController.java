package com.beyond.ordersystem.ordering.controller;

import com.beyond.ordersystem.common.dto.CommonResDto;
import com.beyond.ordersystem.ordering.dto.OrderingSaveReqDto;
import com.beyond.ordersystem.ordering.service.OrderingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class OrderingController {

    private final OrderingService orderingService;
    @Autowired
    public OrderingController(OrderingService orderingService) {
        this.orderingService = orderingService;
    }

    @PostMapping("/order/create")
    public ResponseEntity<?> orderCreate(@RequestBody List<OrderingSaveReqDto> dto){
        CommonResDto commonResDto = new CommonResDto(HttpStatus.CREATED, "주문이 등록되었습니다.", orderingService.orderCreate(dto).getId()); // 엔티티 그대로 리턴하는건 순환참조 빠진다. 그러지 마라.
        return new ResponseEntity<>(commonResDto,HttpStatus.CREATED);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/order/list")
    public ResponseEntity<?> orderList(Pageable pageable){
        CommonResDto commonResDto = new CommonResDto(HttpStatus.OK, "주문목록이 조회되었습니다.", orderingService.orderList(pageable));
        return new ResponseEntity<>(commonResDto,HttpStatus.OK);
    }

//    @PreAuthorize("hasRole('USER')")
    @GetMapping("/order/myorders")
    public ResponseEntity<?> myorderList(Pageable pageable){
        CommonResDto commonResDto = new CommonResDto(HttpStatus.OK, "주문목록이 조회되었습니다.", orderingService.myorderList(pageable));
        return new ResponseEntity<>(commonResDto,HttpStatus.OK);
    }

//    admin 사용자의 주문 취소 /order/{id}/cancel -> orderstatus만 변경
    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/order/{id}/cancel")
    public ResponseEntity<?> orderCancel(@PathVariable Long id){
        CommonResDto commonResDto = new CommonResDto(HttpStatus.CREATED, "주문이 취소되었습니다.", orderingService.orderCancel(id));
        return new ResponseEntity<>(commonResDto,HttpStatus.CREATED);
    }

}
