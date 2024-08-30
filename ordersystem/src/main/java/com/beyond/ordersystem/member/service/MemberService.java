package com.beyond.ordersystem.member.service;


import com.beyond.ordersystem.member.domain.Member;
import com.beyond.ordersystem.member.dto.MemberListResDto;
import com.beyond.ordersystem.member.dto.MemberLoginDto;
import com.beyond.ordersystem.member.dto.MemberResetPasswordReqDto;
import com.beyond.ordersystem.member.dto.MemberSaveReqDto;
import com.beyond.ordersystem.member.repository.MemberRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityNotFoundException;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class MemberService {

    private final MemberRepository memberRepository;
    @Autowired
    public MemberService(MemberRepository memberRepository) {
        this.memberRepository = memberRepository;
    }

    @Autowired
    PasswordEncoder passwordEncoder;

    @Transactional
    public Member memberCreate(MemberSaveReqDto dto){
        if(memberRepository.findByEmail(dto.getEmail()).isPresent()){
            throw new IllegalArgumentException("존재하는 회원의 이메일입니다.");
        }
        if(dto.getPassword().length()<8){
            throw new IllegalArgumentException("비밀번호가 너무 짧습니다.");
        }
        return memberRepository.save(dto.toEntity(passwordEncoder.encode(dto.getPassword())));
    }

    @Transactional
    public Member login(MemberLoginDto dto){
//        email 존재여부
        Member member = memberRepository.findByEmail(dto.getEmail()).orElseThrow(()->new EntityNotFoundException("존재하지 않는 이메일입니다."));
//        password 일치여부
//        if(!member.getPassword().equals(passwordEncoder.encode(dto.getPassword()))){ // 이게 문제? 고쳐봐?
        if(!passwordEncoder.matches(dto.getPassword(), member.getPassword())){
            throw new IllegalArgumentException("비밀번호가 일치하지 않습니다.");
        }
        return member;
    }

    public Page<MemberListResDto> memberList(Pageable pageable){
        Page<Member> members = memberRepository.findAll(pageable);
        return members.map(a->a.listFromEntity());
    }

    public MemberListResDto memberMyInfo(){ // 단일조회
        Member member = memberRepository.findByEmail(SecurityContextHolder.getContext().getAuthentication().getName()).orElseThrow(()->new EntityNotFoundException("회원이 없습니다."));
        return member.listFromEntity();
    }

    @Transactional
    public void memberResetPassword(MemberResetPasswordReqDto dto) { // MemberListResDto
        Member member = memberRepository.findByEmail(dto.getEmail()).orElseThrow(() -> new EntityNotFoundException("회원이 없습니다."));
        if (!passwordEncoder.matches(dto.getAsIsPassword(), member.getPassword())) {
            throw new IllegalArgumentException("비밀번호가 일치하지 않습니다.");
        }
        member.resetPassword(passwordEncoder.encode(dto.getToBePassword()));
//        memberRepository.save(member); // 안해도 된다.
//        return member.listFromEntity(); // 전체적으로 reset 말고 update가 맞았을텐데.. 허허
    }
}










// 리스트 메소드
//        Page<MemberListResDto> memberListResDtos = members.map(a->a.listFromEntity());

//        if(members.isEmpty()){throw new EntityNotFoundException("회원이 없습니다.");}
//        Page<MemberListResDto> memberListResDtos = new
//        for(Member member : members){memberListResDtos.add(member.listFromEntity());}
//        return memberListResDtos; // 내가 한거 고치다가 멈춤
