package com.beyond.ordersystem.product.controller;

import com.beyond.ordersystem.common.dto.CommonResDto;
import com.beyond.ordersystem.product.domain.Product;
import com.beyond.ordersystem.product.dto.ProductListResDto;
import com.beyond.ordersystem.product.dto.ProductSaveReqDto;
import com.beyond.ordersystem.product.dto.ProductSearchDto;
import com.beyond.ordersystem.product.dto.ProductUpdateStockDto;
import com.beyond.ordersystem.product.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
//import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.Valid;
import java.util.List;

//@RefreshScope // 해당 어노테이션 사용시 아래(나오는) (스프링)빈은 실시간 config 변경사항의 대상이 됨
@RestController
public class ProductController {


    private final ProductService productService;
    @Autowired
    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/product/create")
    public ResponseEntity<Object> memberCreate(@ModelAttribute ProductSaveReqDto dto){ // mutipart form 데이터로 받아용 그래서 그대로 받아용
//        Product product = productService.productCreate(dto);
        Product product = productService.productAwsCreate(dto);
        CommonResDto commonResDto = new CommonResDto(HttpStatus.CREATED, "상품이 등록되었습니다.", product.getId());
        return new ResponseEntity<>(commonResDto,HttpStatus.CREATED);
    }

    @GetMapping("/product/list")
    public ResponseEntity<?> productList(ProductSearchDto dto, Pageable pageable){
        Page<ProductListResDto> dtos = productService.productList(dto, pageable);
        CommonResDto commonResDto = new CommonResDto(HttpStatus.OK, "상품정보리스트가 조회되었습니다.", dtos);
        return new ResponseEntity<>(commonResDto,HttpStatus.OK);
    }

    @GetMapping("/product/{id}")
    public ResponseEntity<?> productDetail(@PathVariable Long id){
        ProductListResDto dto = productService.productDetail(id);
        CommonResDto commonResDto = new CommonResDto(HttpStatus.OK, "상품정보가 조회되었습니다.", dto);
        return new ResponseEntity<>(commonResDto,HttpStatus.OK);
    }

    @PutMapping("/product/updateStock")
    public ResponseEntity<?> productStockUpdate(@RequestBody ProductUpdateStockDto dto){
        Product product = productService.productUpdateStock(dto);
        CommonResDto commonResDto = new CommonResDto(HttpStatus.OK, "재고업데이트성공", product.getId());
        return new ResponseEntity<>(commonResDto, HttpStatus.OK);
    }


}








//    @GetMapping("/product/list")
//    public ResponseEntity<?> productList(Pageable pageable){
////        Page : 1,2,3, 있으면 페이징객체 필요해요
////        스크롤은 상관없...   ? 구냥 페이지 말고 리스트로..?
//        Page<ProductListResDto> dtos = productService.productList(pageable);
//        CommonResDto commonResDto = new CommonResDto(HttpStatus.OK, "상품정보리스트가 조회되었습니다.", dtos);
//        return new ResponseEntity<>(commonResDto,HttpStatus.OK);
//    }
