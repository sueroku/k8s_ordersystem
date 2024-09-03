package com.beyond.ordersystem.ordering.controller;

import com.beyond.ordersystem.ordering.dto.OrderListResDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@RestController // 엄밀히 말하면 컨트롤러 코드라 보기 어렵다.
public class SseController implements MessageListener {
//    SseEmitter 는 연결된 사용자 정보를 의미 (연결목록)
//    ConcurrentHashMap은 Thread-safe한 map(동시성 이슈 발생x)
    private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();

//    여러번 구독을 방지하기 위한 ConcurrentHashSet 변수 생성. //
    private Set<String> subscribeList = ConcurrentHashMap.newKeySet();

    @Qualifier("4")
    private final RedisTemplate<String, Object> sseRedisTemplate;

    private final RedisMessageListenerContainer redisMessageListenerContainer;

    public SseController(@Qualifier("4") RedisTemplate<String, Object> sseRedisTemplate, RedisMessageListenerContainer redisMessageListenerContainer) {
        this.sseRedisTemplate = sseRedisTemplate;
        this.redisMessageListenerContainer = redisMessageListenerContainer;
    }

//    email에 해당되는 메세지를 listen하는 리스너를 추가한 것.
    public void subscribeChannel(String email){
//        이미 구독한 email일 경우에는 더이상 구독하지 않는 분기처리
        if(!subscribeList.contains(email)){
            MessageListenerAdapter listenerAdapter = new MessageListenerAdapter(this);
            redisMessageListenerContainer.addMessageListener(listenerAdapter, new PatternTopic(email));
            subscribeList.add(email);
        }
    }

    public MessageListenerAdapter createListenerAdapter(SseController sseController){
        return new MessageListenerAdapter(sseController, "onMessage");
    }

    @GetMapping("/subscribe")
    public SseEmitter subscribe() {
        SseEmitter emitter = new SseEmitter(14400 * 60 * 1000L); // 30분 정도로 emitter 유효시간 설정
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        emitters.put(email, emitter);
        emitter.onCompletion(() -> emitters.remove((email)));
        emitter.onTimeout(() -> emitters.remove(email));

        try {
            emitter.send(SseEmitter.event().name("connect").data("connected!")); // 앞단에서 이벤트 네임으로 찾는당
        } catch (IOException e) {
            e.printStackTrace();
        }
        subscribeChannel(email);
        return emitter;
    }

    public void publishMessage(OrderListResDto dto, String email){ //   보낼 대상이 여러명이면 email을 리스트로
        SseEmitter emitter = emitters.get(email);
//        if(emitter!=null){
//            try{
//                emitter.send(SseEmitter.event().name("ordered").data(dto));
//            }catch (IOException e){
//                throw new RuntimeException(e);
//            } // 이중화 서버에서는 주석 풀어야한다. 필요해
//        }else { // 주석처리한 이유 : 단일 서버에서 pub/sub 확인하기 위해
            sseRedisTemplate.convertAndSend(email, dto);
//        }
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
//        message내용 parsing
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            System.out.println("listening");
            OrderListResDto dto = objectMapper.readValue(message.getBody(), OrderListResDto.class);
            String email = new String(pattern, StandardCharsets.UTF_8);
            SseEmitter emitter = emitters.get(email);
            if(emitter!=null){
                emitter.send(SseEmitter.event().name("ordered").data(dto));
            }
//            System.out.println(dto);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
