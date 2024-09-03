package com.beyond.ordersystem.ordering.service;

import com.beyond.ordersystem.common.dto.CommonResDto;
import com.beyond.ordersystem.common.service.StockInventoryService;
import com.beyond.ordersystem.ordering.controller.SseController;
import com.beyond.ordersystem.ordering.domain.OrderDetail;
import com.beyond.ordersystem.ordering.domain.OrderStatus;
import com.beyond.ordersystem.ordering.domain.Ordering;
import com.beyond.ordersystem.ordering.dto.*;
import com.beyond.ordersystem.ordering.repository.OrderDetailRepository;
import com.beyond.ordersystem.ordering.repository.OrderingRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.*;
//import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import javax.persistence.EntityNotFoundException;
import java.util.ArrayList;
import java.util.List;


@Service
@Transactional(readOnly = true)
public class OrderingService {

    private final OrderingRepository orderingRepository;
    private final StockInventoryService stockInventoryService;
//    private final StockDecreaseEventHandler stockDecreaseEventHandler;

    private final SseController sseController;

    private final RestTemplate restTemplate;

    private final ProductFeign productFeign;

//    private final KafkaTemplate<String, Object> kafkaTemplate;

    private final OrderDetailRepository orderDetailRepository; // 없어도 되는 겁니다. 없다고 생각하세용

    @Autowired
    public OrderingService(OrderingRepository orderingRepository, StockInventoryService stockInventoryService,SseController sseController, RestTemplate restTemplate, ProductFeign productFeign, OrderDetailRepository orderDetailRepository) {
        this.orderingRepository = orderingRepository;
        this.stockInventoryService = stockInventoryService;
        this.sseController = sseController;
        this.restTemplate = restTemplate;
        this.productFeign = productFeign;
        this.orderDetailRepository = orderDetailRepository;
    }

    @Transactional
    public Ordering orderRestTemplateCreate(List<OrderingSaveReqDto> dtos){
        String memberEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        Ordering ordering = new Ordering().builder()
                .memberEmail(memberEmail).build();
        for(OrderingSaveReqDto dto : dtos){
            int quantity = dto.getProductCount();
//            product API에 요청을 통해 product객체를 조회해야함
            String productGetUrl = "http://product-service/product/"+dto.getProductId();
            // 토큰 세팅해줘야해서 헤더 세팅한다.
            HttpHeaders httpHeaders = new HttpHeaders();
            String token = (String)SecurityContextHolder.getContext().getAuthentication().getCredentials();
            httpHeaders.set("Authorization", token);
            HttpEntity<String> entity = new HttpEntity<>(httpHeaders);
            ResponseEntity<CommonResDto> productEntity = restTemplate.exchange(productGetUrl, HttpMethod.GET, entity, CommonResDto.class);
            ObjectMapper objectMapper = new ObjectMapper();
            ProductDto productDto = objectMapper.convertValue(productEntity.getBody().getResult(), ProductDto.class);
            if(productDto.getName().contains("sale")){
//                redis를 통한 재고관리 및 재고잔량 확인
                int newQuantity = stockInventoryService.decreaseStop(dto.getProductId(), dto.getProductCount()).intValue(); // 재고를 integer로 저장했기 때문에 데이터타입전환해줌.
                if(newQuantity<0){
                    throw new IllegalArgumentException(productDto.getId()+" "+productDto.getName()+" "+"재고부족");
                }
//                rdb에 재고 업데이트   -   product.updateStockQuantity(dto.getProductCount()); 혹은 스케줄러...? db 터져 데드락 걸려 실시간 요청이 유실될 수 있어
//                강사님 아이디어 : rabbitmq 를 통해 비동기적으로 이벤트 처리. // 다른 방법들도 있으니 잘 서치해보세요
//                stockDecreaseEventHandler.publish(new StockDecreaseEvent(productDto.getId(), dto.getProductCount()));

            }else{
                if(productDto.getStockQuantity() < dto.getProductCount()){
                    throw new IllegalArgumentException(productDto.getId()+" "+productDto.getName()+" "+"재고부족");
                }
////                restTemplate를 통한 update 요청
//                productDto.updateStockQuantity(dto.getProductCount()); // 변경감지(dirty checking)로 인해 별도의 save 불필요(jpa가 알아서 해준다.)

                String updateUrl = "http://product-service/product/updateStock";
                httpHeaders.setContentType(MediaType.APPLICATION_JSON);
                HttpEntity<ProductUpdateStockDto> updateEntity = new HttpEntity<>(
                        new ProductUpdateStockDto(dto.getProductId(), dto.getProductCount()), httpHeaders);
                restTemplate.exchange(updateUrl, HttpMethod.PUT, updateEntity, Void.class);
            }

            OrderDetail orderDetail = OrderDetail.builder()
                .ordering(ordering)
                .productId(productDto.getId()).quantity(dto.getProductCount()).build();
            ordering.getOrderDetails().add(orderDetail); // 불러다가 넣어
        }

        Ordering savedOrdering = orderingRepository.save(ordering);

        sseController.publishMessage(savedOrdering.listFromEntity(),"admin@test.com"); // 여기에 보낼 사람

        return savedOrdering;

    }

    @Transactional
    public Ordering orderFeignClientCreate(List<OrderingSaveReqDto> dtos){
        String memberEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        Ordering ordering = new Ordering().builder()
                .memberEmail(memberEmail).build();
        for(OrderingSaveReqDto dto : dtos){
            int quantity = dto.getProductCount();
//            ResponseEntity가 기본 응답값이므로 바로 commonResDto로 매핑 (만들어라)
            CommonResDto commonResDto = productFeign.getProductById(dto.getProductId());
            ObjectMapper objectMapper = new ObjectMapper();
            ProductDto productDto = objectMapper.convertValue(commonResDto.getResult(), ProductDto.class);
            if(productDto.getName().contains("sale")){
                int newQuantity = stockInventoryService.decreaseStop(dto.getProductId(), dto.getProductCount()).intValue(); // 재고를 integer로 저장했기 때문에 데이터타입전환해줌.
                if(newQuantity<0){
                    throw new IllegalArgumentException(productDto.getId()+" "+productDto.getName()+" "+"재고부족");
                }
//                stockDecreaseEventHandler.publish(new StockDecreaseEvent(productDto.getId(), dto.getProductCount()));

            }else{
                if(productDto.getStockQuantity() < dto.getProductCount()){
                    throw new IllegalArgumentException(productDto.getId()+" "+productDto.getName()+" "+"재고부족");
                }
                productFeign.updateProductStock(new ProductUpdateStockDto(dto.getProductId(), dto.getProductCount()));
            }

            OrderDetail orderDetail = OrderDetail.builder()
                    .ordering(ordering)
                    .productId(productDto.getId()).quantity(dto.getProductCount()).build();
            ordering.getOrderDetails().add(orderDetail);
        }

        Ordering savedOrdering = orderingRepository.save(ordering);

        sseController.publishMessage(savedOrdering.listFromEntity(),"admin@test.com");

        return savedOrdering;
    }

//    @Transactional
//    public Ordering orderFeignKafkaCreate(List<OrderingSaveReqDto> dtos){
//        String memberEmail = SecurityContextHolder.getContext().getAuthentication().getName();
//        Ordering ordering = new Ordering().builder()
//                .memberEmail(memberEmail).build();
//        for(OrderingSaveReqDto dto : dtos){
//            int quantity = dto.getProductCount();
//            CommonResDto commonResDto = productFeign.getProductById(dto.getProductId());
//            ObjectMapper objectMapper = new ObjectMapper();
//            ProductDto productDto = objectMapper.convertValue(commonResDto.getResult(), ProductDto.class);
//            if(productDto.getName().contains("sale")){
//                int newQuantity = stockInventoryService.decreaseStop(dto.getProductId(), dto.getProductCount()).intValue(); // 재고를 integer로 저장했기 때문에 데이터타입전환해줌.
//                if(newQuantity<0){
//                    throw new IllegalArgumentException(productDto.getId()+" "+productDto.getName()+" "+"재고부족");
//                }
//                stockDecreaseEventHandler.publish(new StockDecreaseEvent(productDto.getId(), dto.getProductCount()));
//
//            }else{
//                if(productDto.getStockQuantity() < dto.getProductCount()){
//                    throw new IllegalArgumentException(productDto.getId()+" "+productDto.getName()+" "+"재고부족");
//                }
//                ProductUpdateStockDto productUpdateStockDto = new ProductUpdateStockDto(dto.getProductId(), dto.getProductCount());
//                kafkaTemplate.send("product-update-topic",productUpdateStockDto);
//            }
//
//            OrderDetail orderDetail = OrderDetail.builder()
//                    .ordering(ordering)
//                    .productId(productDto.getId()).quantity(dto.getProductCount()).build();
//            ordering.getOrderDetails().add(orderDetail);
//        }
//
//        Ordering savedOrdering = orderingRepository.save(ordering);
//
//        sseController.publishMessage(savedOrdering.listFromEntity(),"admin@test.com");
//
//        return savedOrdering;
//    }

    public Page<OrderListResDto> orderList(Pageable pageable){
        Page<Ordering> orderings = orderingRepository.findAll(pageable);
        return orderings.map(a->a.listFromEntity()); // List로 만든다면 for문으로 만들어죵.. 어 근데 걔도 맵가능하지 않나..
    }

    public Page<OrderListResDto> myorderList(Pageable pageable){
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        Page<Ordering> orderings = orderingRepository.findByMemberEmail(email,pageable); // Page<Ordering> orderings = orderingRepository.findByMember(member,pageable);
        return orderings.map(a->a.listFromEntity()); // List로 만든다면 for문으로 만들어죵.. 어 근데 걔도 맵가능하지 않나..
    }

    @Transactional
    public OrderListResDto orderCancel(Long id){
        Ordering ordering = orderingRepository.findById(id).orElseThrow(()->new EntityNotFoundException("주문이 없습니다."));
        ordering.updateOrderStatus(OrderStatus.CANCELD);
//        orderingRepository.save(ordering);
        return ordering.listFromEntity();
    }
}










// 크리에이트
////        방법1.쉬운방식
////        Ordering생성 : member_id, status
//        Member member = memberRepository.findById(dto.getMemberId()).orElseThrow(()->new EntityNotFoundException("없음"));
//        Ordering ordering = orderingRepository.save(dto.toEntity(member));
//
////        OrderDetail생성 : order_id, product_id, quantity
//        for(OrderingSaveReqDto.OrderDetailDto orderDto : dto.getOrderDetailList()){
//            Product product = productRepository.findById(orderDto.getProductId()).orElse(null);
//            int quantity = orderDto.getProductCount();
//            OrderDetail orderDetail =  OrderDetail.builder()
//                    .product(product)
//                    .quantity(quantity)
//                    .ordering(ordering)
//                    .build();
//            orderDetailRepository.save(orderDetail);
//        }
//
//        return ordering;



////        // 방법2. JPA에 최적화한 방식
//        Member member = memberRepository.findById(dto.getMemberId()).orElseThrow(()->new EntityNotFoundException("회원이 없습니다."));
//Ordering ordering = new Ordering().builder()
//        .member(member).build();
//
////        List<OrderDetail> orderDetailList = new ArrayList<>(); // 안만들고
//        for(OrderingSaveReqDto.OrderDetailDto odd : dto.getOrderDetailList()){
//Product product = productRepository.findById(odd.getProductId()).orElseThrow(()->new EntityNotFoundException("상품이 없습니다"));
//            if(product.getStockQuantity() < odd.getProductCount()){
//        throw new IllegalArgumentException(product.getId()+" "+product.getName()+" "+"재고부족");
//        }
//        product.updateStockQuantity(odd.getProductCount()); // 변경감지(dirty checking)로 인해 별도의 save 불필요(jpa가 알아서 해준다.)
//OrderDetail orderDetail = OrderDetail.builder()
//        .ordering(ordering)
//        .product(product).quantity(odd.getProductCount()).build();
//            ordering.getOrderDetails().add(orderDetail); // 불러다가 넣어
//        }
//
//Ordering savedOrdering = orderingRepository.save(ordering);
//        return savedOrdering;