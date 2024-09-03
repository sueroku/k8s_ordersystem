package com.beyond.ordersystem.product.domain;

import com.beyond.ordersystem.common.domain.BaseEntity;
import com.beyond.ordersystem.product.dto.ProductListResDto;
import lombok.*;

import javax.persistence.*;

@Getter
@Builder
@Entity
@AllArgsConstructor
@NoArgsConstructor
public class Product extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    private String category;
    private Integer price;
    private Integer stockQuantity;
    private String imagePath; // 지금은 일단 실제파일 경로

    public ProductListResDto listFromEntity(){
//        String address = this.city + " " + this.street + " " + this.zipcode;
        return new ProductListResDto().builder()
                .id(this.id).name(this.name).category(this.category).price(this.price)
                .stockQuantity(this.stockQuantity).imagePath(this.imagePath).build();
    }

    public void updateImagePath(String imagePath){
        this.imagePath = imagePath;
    }

    public void updateStockQuantity(Integer stockQuantity){
        this.stockQuantity -= stockQuantity;
    }
}
