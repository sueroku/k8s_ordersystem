package com.beyond.ordersystem.ordering.domain;

import com.beyond.ordersystem.common.domain.BaseEntity;
import com.beyond.ordersystem.member.domain.Member;
import com.beyond.ordersystem.ordering.dto.OrderListResDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Getter
@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Ordering extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
//    @ColumnDefault("'ORDERED'")
    @Builder.Default
    private OrderStatus orderStatus = OrderStatus.ORDERED;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    @OneToMany(mappedBy = "ordering", cascade = CascadeType.PERSIST)
    @Builder.Default // 빌터패턴에서도 ArrayList로 초기화 되도록 하는 설정 //
    private List<OrderDetail> orderDetails = new ArrayList<>(); // 빌더패턴은 빌더에서 만들어놓은걸 가져다 쓰는건데, 빌더에는 리스트가 초기화(new)되어 있지 않음.
//    private List<OrderDetail> orderDetails;



    public OrderListResDto listFromEntity(){
        List<OrderListResDto.OrderDetailShort> odsList = new ArrayList<>();
        for(OrderDetail od : this.getOrderDetails()){
            odsList.add(OrderListResDto.OrderDetailShort.builder()
                    .id(od.getId())
                    .productName(od.getProduct().getName())
                    .count(od.getQuantity())
                    .build());
        }
        return OrderListResDto.builder()
                .id(this.id)
                .memberEmail(this.member.getEmail())
                .orderStatus(this.orderStatus)
                .orderDetailShortList(odsList)
                .build();
    }

    public void updateOrderStatus(OrderStatus orderStatus){
        this.orderStatus = orderStatus;
    }

}
