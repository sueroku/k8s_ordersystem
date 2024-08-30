package com.beyond.ordersystem.ordering.service;

import com.beyond.ordersystem.common.service.StockInventoryService;
import com.beyond.ordersystem.member.domain.Member;
import com.beyond.ordersystem.member.repository.MemberRepository;
import com.beyond.ordersystem.ordering.controller.SseController;
import com.beyond.ordersystem.ordering.domain.OrderDetail;
import com.beyond.ordersystem.ordering.domain.OrderStatus;
import com.beyond.ordersystem.ordering.domain.Ordering;
import com.beyond.ordersystem.ordering.dto.OrderListResDto;
import com.beyond.ordersystem.ordering.dto.OrderingSaveReqDto;
import com.beyond.ordersystem.ordering.dto.StockDecreaseEvent;
import com.beyond.ordersystem.ordering.repository.OrderDetailRepository;
import com.beyond.ordersystem.ordering.repository.OrderingRepository;
import com.beyond.ordersystem.product.domain.Product;
import com.beyond.ordersystem.product.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityNotFoundException;
import java.util.ArrayList;
import java.util.List;


@Service
@Transactional(readOnly = true)
public class OrderingService {

    private final OrderingRepository orderingRepository;
    private final MemberRepository memberRepository;
    private final ProductRepository productRepository;
    private final StockInventoryService stockInventoryService;
//    private final StockDecreaseEventHandler stockDecreaseEventHandler;

    private final SseController sseController;

    private final OrderDetailRepository orderDetailRepository; // 없어도 되는 겁니다. 없다고 생각하세용

    @Autowired
    public OrderingService(OrderingRepository orderingRepository, MemberRepository memberRepository, ProductRepository productRepository, StockInventoryService stockInventoryService,SseController sseController, OrderDetailRepository orderDetailRepository) {
        this.orderingRepository = orderingRepository;
        this.memberRepository = memberRepository;
        this.productRepository = productRepository;
        this.stockInventoryService = stockInventoryService;
        this.sseController = sseController;
        this.orderDetailRepository = orderDetailRepository;
    }

    @Transactional
    public Ordering orderCreate(List<OrderingSaveReqDto> dtos){
//  public synchronized Ordering orderCreate(List<OrderingSaveReqDto> dtos){// syncronized 를 설정한다 하더라도, 재고 감소가 db에 반영되는 시점은 트랜잭션이 커밋되고 종료되는 시점
//        // 방법2. JPA에 최적화한 방식
//        Member member = memberRepository.findById(dto.getMemberId()).orElseThrow(()->new EntityNotFoundException("회원이 없습니다.")); // 로그인 적용 전. 이제는 필요없어용
        String memberEmail = SecurityContextHolder.getContext().getAuthentication().getName();
//        String memberRole = SecurityContextHolder.getContext().getAuthentication().getAuthorities(); // 딱히 꺼낼일 없다. 지워라. 근데 안지우고 주석처리~
        Member member = memberRepository.findByEmail(memberEmail).orElseThrow(()->new EntityNotFoundException("회원이 없습니다."));
        Ordering ordering = new Ordering().builder()
                .member(member).build();
        for(OrderingSaveReqDto dto : dtos){
            Product product = productRepository.findById(dto.getProductId()).orElseThrow(()->new EntityNotFoundException("상품이 없습니다"));
            if(product.getName().contains("sale")){
//                redis를 통한 재고관리 및 재고잔량 확인
                int newQuantity = stockInventoryService.decreaseStop(dto.getProductId(), dto.getProductCount()).intValue(); // 재고를 integer로 저장했기 때문에 데이터타입전환해줌.
                if(newQuantity<0){
                    throw new IllegalArgumentException(product.getId()+" "+product.getName()+" "+"재고부족");
                }
//                rdb에 재고 업데이트   -   product.updateStockQuantity(dto.getProductCount()); 혹은 스케줄러...? db 터져 데드락 걸려 실시간 요청이 유실될 수 있어
//                강사님 아이디어 : rabbitmq 를 통해 비동기적으로 이벤트 처리. // 다른 방법들도 있으니 잘 서치해보세요
//                stockDecreaseEventHandler.publish(new StockDecreaseEvent(product.getId(), dto.getProductCount()));

            }else{
                if(product.getStockQuantity() < dto.getProductCount()){
                    throw new IllegalArgumentException(product.getId()+" "+product.getName()+" "+"재고부족");
                }
                product.updateStockQuantity(dto.getProductCount()); // 변경감지(dirty checking)로 인해 별도의 save 불필요(jpa가 알아서 해준다.)
            }

            OrderDetail orderDetail = OrderDetail.builder()
                .ordering(ordering)
                .product(product).quantity(dto.getProductCount()).build();
            ordering.getOrderDetails().add(orderDetail); // 불러다가 넣어
        }

        Ordering savedOrdering = orderingRepository.save(ordering);

        sseController.publishMessage(savedOrdering.listFromEntity(),"admin@test.com"); // 여기에 보낼 사람

        return savedOrdering;

    }

    public Page<OrderListResDto> orderList(Pageable pageable){
        Page<Ordering> orderings = orderingRepository.findAll(pageable);
        return orderings.map(a->a.listFromEntity()); // List로 만든다면 for문으로 만들어죵.. 어 근데 걔도 맵가능하지 않나..
    }

    public Page<OrderListResDto> myorderList(Pageable pageable){
        Member member = memberRepository.findByEmail(SecurityContextHolder.getContext().getAuthentication().getName()).orElseThrow(()-> new EntityNotFoundException("회원이 없습니다."));
        Page<Ordering> orderings = orderingRepository.findByMemberId(member.getId(),pageable); // Page<Ordering> orderings = orderingRepository.findByMember(member,pageable);
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