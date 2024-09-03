package com.beyond.ordersystem.common.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatus;

@Data
//@AllArgsConstructor
@NoArgsConstructor
public class CommonResDto {
    private int status_code;
    private String status_message;
    private Object result;
//    private Object result = new ArrayList<>(); // 리스트도 가능 //  이렇게 안써도 가능하넹 신기하다

    public CommonResDto(HttpStatus httpStatus, String message, Object result){
        this.status_code = httpStatus.value();
        this.status_message = message;
        this.result = result;
    }
}
