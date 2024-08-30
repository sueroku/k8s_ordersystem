//package com.beyond.ordersystem.ordering.service;
//
//import com.beyond.ordersystem.common.config.RabbitMqConfig;
//import com.beyond.ordersystem.ordering.dto.StockDecreaseEvent;
//
//import com.beyond.ordersystem.product.domain.Product;
//import com.beyond.ordersystem.product.repository.ProductRepository;
//import com.fasterxml.jackson.core.JsonProcessingException;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import org.springframework.amqp.core.Message;
//import org.springframework.amqp.rabbit.annotation.RabbitListener;
//import org.springframework.amqp.rabbit.core.RabbitTemplate;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.stereotype.Component;
//import org.springframework.transaction.annotation.Transactional;
//
//import javax.persistence.EntityNotFoundException;
//
//@Component
//public class StockDecreaseEventHandler {
//    @Autowired
//    private RabbitTemplate rabbitTemplate;
//    @Autowired
//    private ProductRepository productRepository;
//
//    public void publish(StockDecreaseEvent event){
//        rabbitTemplate.convertAndSend(RabbitMqConfig.STOCK_DECREASE_QUEUE, event); // rabbitTemplate.convertAndSend(큐이름, xxxx);
//    }
//
//    @Transactional  // 큐에 들어간 요청들이 별도의 트랜잭션으로 처리되기 때문에 // 트랜잭셔널은 component 면(컨트롤러도 된다구) 다 된다.
//    @RabbitListener(queues = RabbitMqConfig.STOCK_DECREASE_QUEUE) // 어떤 큐를 바라보고 있을거냐 @RabbitListener(queues = 큐이름)
//    public void listen(Message message){
//        String messageBody = new String(message.getBody());
////        json 메세지를 ObjectMapper로 직적 parsing
//        ObjectMapper objectMapper = new ObjectMapper();
//        try{
//            StockDecreaseEvent stockDecreaseEvent = objectMapper.readValue(messageBody, StockDecreaseEvent.class);
////            재고 update
//            Product product= productRepository.findById(stockDecreaseEvent.getProductId()).orElseThrow(()-> new EntityNotFoundException("없는 상품입니다."));
//            product.updateStockQuantity(stockDecreaseEvent.getProductCount());
//        }catch (JsonProcessingException e){
//            throw  new RuntimeException(e);
//        }
//    }
//}
