package com.beyond.ordersystem.member.controller;

import com.beyond.ordersystem.common.auth.JwtTokenProvider;
import com.beyond.ordersystem.common.dto.CommonErrorDto;
import com.beyond.ordersystem.common.dto.CommonResDto;
import com.beyond.ordersystem.member.domain.Member;
import com.beyond.ordersystem.member.dto.*;
import com.beyond.ordersystem.member.service.MemberService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

@RestController
public class MemberController {

    private final MemberService memberService;
    private final JwtTokenProvider jwtTokenProvider;

    @Qualifier("2")
    private final RedisTemplate<String,Object> redisTemplate;

    @Autowired
    public MemberController(MemberService memberService, JwtTokenProvider jwtTokenProvider, @Qualifier("2") RedisTemplate<String, Object> template) {
        this.memberService = memberService;
        this.jwtTokenProvider = jwtTokenProvider;
        this.redisTemplate = template;
    }

    @PostMapping("/member/create")
    public ResponseEntity<Object> memberCreate(@Valid @RequestBody MemberSaveReqDto dto){
        CommonResDto commonResDto = new CommonResDto(HttpStatus.CREATED, "회원이 등록되었습니다.", memberService.memberCreate(dto)); // null 말고 다른 데이터 들어가기두...
        return new ResponseEntity<>(commonResDto,HttpStatus.CREATED);
    }

    @PostMapping("/doLogin") //patch? post? -- front axios.patch   axios.post
    public ResponseEntity<?> doLogin(@RequestBody MemberLoginDto dto){
        Member member = memberService.login(dto); // email password 일치하는지 검증
//        일치할 경우 accessToken 생성
        String jwtToken = jwtTokenProvider.createToken(member.getEmail(), member.getRole().toString());
        String refreshToken = jwtTokenProvider.createRefreshToken(member.getEmail(), member.getRole().toString());

//        REDIS에 EMAIL과 RT를 KET:VALUE로 하여 저장 // 멀티서버에서 해당 서버 메모리에만 저장하면 접근하기 어렵거나, 탈취시 레디스에서 삭제하여 범죄를 방지한다.
        redisTemplate.opsForValue().set(member.getEmail(), refreshToken, 240, TimeUnit.HOURS); //240시간
        //        생성된 토큰을 commonResDto에 담아 사용자에게 return
        Map<String, Object> loginInfo = new HashMap<>();
        loginInfo.put("id", member.getId());
        loginInfo.put("token",jwtToken);
        loginInfo.put("refreshToken",refreshToken);
        CommonResDto commonResDto = new CommonResDto(HttpStatus.OK, "로그인 성공!", loginInfo);
        return new ResponseEntity<>(commonResDto, HttpStatus.OK);
    }

//    admin만 회원 목록 전체 조회 가능
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/member/list")
    public ResponseEntity<?> memberList(Pageable pageable){
//        Page : 1,2,3, 있으면 페이징객체 필요해요
//        스크롤은 상관없...   ? 구냥 페이지 말고 리스트로..?
        Page<MemberListResDto> dtos = memberService.memberList(pageable);
        CommonResDto commonResDto = new CommonResDto(HttpStatus.OK, "회원정보리스트가 조회되었습니다.", dtos);
        return new ResponseEntity<>(commonResDto,HttpStatus.OK);
    }

//    회원은 자신의 회원정보만 조회가능
    @GetMapping("/member/myinfo")
    public ResponseEntity<?> memberMyInfo(){
        CommonResDto commonResDto = new CommonResDto(HttpStatus.OK, "회원정보가 조회되었습니다.", memberService.memberMyInfo());
        return new ResponseEntity<>(commonResDto,HttpStatus.OK);
    }


    @Value("${jwt.secretKeyRt}")
    private String secretKeyRt;

    @PostMapping("/refresh-token") // patch? post?
    public ResponseEntity<?> generateNewAccessToken(@RequestBody MemberRefreshDto dto){
        String rt = dto.getRefreshToken();
        Claims claims = null;
        try {
//            코드를 통해 rt 검증
            claims = Jwts.parser().setSigningKey(secretKeyRt).parseClaimsJws(rt).getBody();
        }catch (Exception e){
            return new ResponseEntity<>(new CommonErrorDto(HttpStatus.BAD_REQUEST, "invalid refresh token"), HttpStatus.BAD_REQUEST);
        }
        String email = claims.getSubject();
        String role = claims.get("role").toString();

//        redis를 조회하여 rt 추가 검증
        Object obj = redisTemplate.opsForValue().get(email);
//        String storedRedisRt = redisTemplate.opsForValue().get(email).toString(); //  인텔리제이제안      String storedRedisRt = Objects.requireNonNull(redisTemplate.opsForValue().get(email)).toString();
//        if(storedRedisRt == null || !storedRedisRt.equals(rt)){
        if(obj == null || !obj.toString().equals(rt)){
            return new ResponseEntity<>(new CommonErrorDto(HttpStatus.BAD_REQUEST, "invalid refresh token"), HttpStatus.BAD_REQUEST);
        }

        String newAt = jwtTokenProvider.createToken(email, role);

        Map<String, Object> info = new HashMap<>();
        info.put("token",newAt);
        CommonResDto commonResDto = new CommonResDto(HttpStatus.OK, "At is renewed", info);
        return new ResponseEntity<>(commonResDto, HttpStatus.OK);
    }

    @PatchMapping("/member/reset-password")
    public ResponseEntity<?> memberResetPassword(@RequestBody MemberResetPasswordReqDto dto){
        memberService.memberResetPassword(dto);
        CommonResDto commonResDto = new CommonResDto(HttpStatus.OK, "reset password", "ok"); // memberService.memberResetPassword(dto)
        return new ResponseEntity<>(commonResDto, HttpStatus.OK);
    }

}
