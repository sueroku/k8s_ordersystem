package com.beyond.ordersystem.member.repository;

import com.beyond.ordersystem.member.domain.Member;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long> {
    Optional<Member> findByEmail(String email);

    Page<Member> findAll(Pageable pageable); // 오 지워도 알아서 해준데, 근데 응? 뭐 어쩌라고임... 왜 셋이 얘기해... slice는 뭐야...힝
}
