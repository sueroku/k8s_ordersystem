package com.beyond.ordersystem.member.dto;

import com.beyond.ordersystem.common.domain.Address;
import com.beyond.ordersystem.member.domain.Member;
import com.beyond.ordersystem.member.domain.Role;
import lombok.*;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Size;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MemberSaveReqDto {
    @NotEmpty(message="name is essential")
    private String name;
    @NotEmpty(message="email is essential")
    private String email;
    @NotEmpty(message="password is essential")
    @Size(min=8, message="password 최소 길이는 8")
    private String password;
//    private String city;
//    private String street;
//    private String zipcode;
    private Address address;
    private Role role = Role.USER;

    public Member toEntity(String password){
        return new Member().builder()
                .name(this.name).email(this.email).password(password)
                .address(this.address)
                .role(this.role)
                .build();
    }
}
