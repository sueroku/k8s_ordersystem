package com.beyond.ordersystem.common.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.servlet.*;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Component
@Slf4j
public class JwtAuthFilter extends GenericFilter {

    @Value("${jwt.secretKey}")
    private String secretKey;

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
       String bearerToken = ((HttpServletRequest)servletRequest).getHeader("Authorization");
       try{
           if(bearerToken!=null){
//           token 관례적으로(이유있긴하다) Bearer로 시작하는 문구를 넣어서 (서버에) 요청 // 포스트맨은 선택하면 알아서 붙여주는데, 실제로는 직접 붙여줘야함.
//               if(!bearerToken.substring(0,7).equals("Bearer ")){
               if(!bearerToken.startsWith("Bearer ")){
                   throw new AuthenticationServiceException("Bearer 형식이 아닙니다.");
               }
               String token = bearerToken.substring(7);
//           token 검증 및 claims(사용자 정보-페이로드) 추출
//           token 생성시에 사용한 secret 키값을 넣어 토큰 검증에 사용
               Claims claims = Jwts.parser().setSigningKey(secretKey).parseClaimsJws(token).getBody(); // 이게 검증하고 필요한 꺼내는 코드야!
//           Authentication 객체(인증객체-사용자정보 담겨있는거) 생성 (UserDetails객체도 필요)
               List<GrantedAuthority> authorities = new ArrayList<>();
               authorities.add(new SimpleGrantedAuthority("ROLE_"+claims.get("role"))); // 역할 넣어주고
               UserDetails userDetails = new User(claims.getSubject(), "", authorities); // subject :  email (아까 넣어줬잖앙)
               Authentication authentication = new UsernamePasswordAuthenticationToken(userDetails, "", userDetails.getAuthorities());
               SecurityContextHolder.getContext().setAuthentication(authentication);
           }
//           else {
//               throw new AuthenticationServiceException("token이 없습니다."); //  찜찜.. 알아서 필터로 넘어가면서 해줄거 같은데...
//           } // 이거 때문에 안되네..? ㄱㄴ데 왜?
//           filterchain에서 그 다음 filtering으로 넘어가도록 하는 메서드
           filterChain.doFilter(servletRequest, servletResponse);
       }catch (Exception e){
           log.error(e.getMessage());
           HttpServletResponse httpServletResponse = (HttpServletResponse)servletResponse;
           httpServletResponse.setStatus(HttpStatus.UNAUTHORIZED.value());
           httpServletResponse.setContentType("application/json");
           httpServletResponse.getWriter().write("token에러");
       }

    }
}
