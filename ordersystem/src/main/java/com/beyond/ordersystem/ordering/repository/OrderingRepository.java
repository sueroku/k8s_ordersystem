package com.beyond.ordersystem.ordering.repository;

import com.beyond.ordersystem.member.domain.Member;
import com.beyond.ordersystem.ordering.domain.Ordering;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OrderingRepository extends JpaRepository<Ordering, Long> {
    Page<Ordering> findByMemberId(Long id, Pageable pageable);
    Page<Ordering> findByMember(Member member, Pageable pageable);
}
