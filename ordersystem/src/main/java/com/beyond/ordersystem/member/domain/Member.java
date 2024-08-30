package com.beyond.ordersystem.member.domain;

import com.beyond.ordersystem.common.domain.Address;
import com.beyond.ordersystem.common.domain.BaseEntity;
import com.beyond.ordersystem.member.dto.MemberListResDto;
import com.beyond.ordersystem.ordering.domain.Ordering;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;

import javax.persistence.*;
import java.util.List;

@Getter
@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Member extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    @Column(nullable = false, unique = true)//길이는 알아서
    private String email;
    private String password;
    @Embedded
    private Address address;
    @Enumerated(EnumType.STRING)
//    @ColumnDefault("'USER'") // 이건 왜 안되나용
    @Builder.Default // 따로 입력하면 덮어쓰기 됩니다.
    private Role role = Role.USER;

    @OneToMany(mappedBy = "member" ,fetch = FetchType.LAZY)
    private List<Ordering> orderingList;


    public MemberListResDto listFromEntity(){
//        String address = this.city + " " + this.street + " " + this.zipcode;
        return new MemberListResDto().builder()
                .id(this.id).name(this.name).email(this.email).address(this.address).orderCount(this.orderingList.size()).build();
    }
    public void resetPassword(String toBePassword){
        this.password = toBePassword;
    }

}
