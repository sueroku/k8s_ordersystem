package com.beyond.ordersystem.product.service;

import com.beyond.ordersystem.common.service.StockInventoryService;
import com.beyond.ordersystem.product.domain.Product;
import com.beyond.ordersystem.product.dto.ProductListResDto;
import com.beyond.ordersystem.product.dto.ProductSaveReqDto;
import com.beyond.ordersystem.product.dto.ProductSearchDto;
import com.beyond.ordersystem.product.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class ProductService {

    private final ProductRepository productRepository;
    private final StockInventoryService stockInventoryService;

    @Autowired
    public ProductService(ProductRepository productRepository, StockInventoryService stockInventoryService, S3Client s3Client) {
        this.productRepository = productRepository;
        this.stockInventoryService = stockInventoryService;
        this.s3Client = s3Client;
    }

    @Transactional
    public Product productCreate(ProductSaveReqDto dto){
        MultipartFile image = dto.getProductImage();
        Product product;
        try{
            product = productRepository.save(dto.toEntity());
            byte[] bytes = image.getBytes();
            Path path = Paths.get("C:/Users/Playdata/Desktop/tmp",
                    product.getId()+ "_" + image.getOriginalFilename());
            Files.write(path, bytes, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
            product.updateImagePath(path.toString());
            if(dto.getName().contains("sale")){
                stockInventoryService.increaseStop(product.getId(), dto.getStockQuantity());
            }
        }catch (IOException e){ // 트라이-캐치 때문에 트랜잭션 처리때문에
            throw new RuntimeException("이미지 저장 실패"); // 여기서 예외를 던져용
        }
        return product;
    }

    @Value("${cloud.aws.s3.bucket}")
    private String bucket;
    private final S3Client s3Client;

    @Transactional
    public Product productAwsCreate(ProductSaveReqDto dto){
        MultipartFile image = dto.getProductImage();
        Product product;
        try{
            product = productRepository.save(dto.toEntity());
            byte[] bytes = image.getBytes();
            String fileName = product.getId()+ "_" + image.getOriginalFilename();
//            Path path = Paths.get("C:/Users/Playdata/Desktop/tmp", fileName);
            Path path = Paths.get("/tmp/", fileName);
            Files.write(path, bytes, StandardOpenOption.CREATE, StandardOpenOption.WRITE); // local pc에 임시 저장
//            aws에 pc에 저장된 파일을 업로드
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(fileName)
                    .build();
            PutObjectResponse putObjectResponse = s3Client.putObject(putObjectRequest, RequestBody.fromFile(path));
            String s3Path = s3Client.utilities().getUrl(a->a.bucket(bucket).key(fileName)).toExternalForm();
            product.updateImagePath(s3Path);
        }catch (IOException e){ // 트라이-캐치 때문에 트랜잭션 처리때문에
            throw new RuntimeException("이미지 저장 실패"); // 여기서 예외를 던져용
        }
        return product;
    }



    public Page<ProductListResDto> productList(ProductSearchDto dto, Pageable pageable){
//        검색을 위해 Specification 객체 사용
//         Specification 객체 는 복잡한 쿼리를 명세를 이용하여 정의하는 방식으로, 쿼리를 쉽게 생성
        Specification<Product> specification = new Specification<Product>() {
            @Override
            public Predicate toPredicate(Root<Product> root, CriteriaQuery<?> query, CriteriaBuilder criteriaBuilder) {
                List<Predicate> predicates = new ArrayList<>();
                if(dto.getName() != null){
                    // root는 엔티티의 속성을 접근하기 위한 객체, CriteriaBuilder 는 쿼리를 생성하기 위한 객체
                    predicates.add(criteriaBuilder.like(root.get("name"),"%"+dto.getName()+"%"));
                }
                if(dto.getCategory() != null){
                    predicates.add(criteriaBuilder.like(root.get("category"),"%"+dto.getCategory()+"%"));
                }

                Predicate[] predicatesArr = new Predicate[predicates.size()];
                for(int i=0; i<predicatesArr.length; i++){
                    predicatesArr[i] = predicates.get(i);
                    // 위 2개의 쿼리 조건문을 and 조건으로 연결.
                    // 우리는 지금 name 검색 / category 검색 나눠놔서 독립적이지만, and 로 엮여도 상관 없다.
                    // 추후 and 가 필요할 수 있으니 미리 작성하였음.
                }
                Predicate predicate = criteriaBuilder.and(predicatesArr);

                return predicate;
            }
        };
        Page<Product> products = productRepository.findAll(specification, pageable);
        return products.map(a->a.listFromEntity());
    }



}





//
//@Transactional
//public Product productCreate(ProductSaveReqDto dto){
//    MultipartFile image = dto.getProductImage();
//    Product product;
//    try{
//        byte[] bytes = image.getBytes();
//        Path path = Paths.get("C:/Users/Playdata/Desktop/tmp",
//                UUID.randomUUID()+ "_" + image.getOriginalFilename()); // 같은이름의 파일은 어째.. 그래서 유효값 붙이기 UUID말고 상품 id 붙이려 하면..?
//        Files.write(path, bytes, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
//        product = productRepository.save(dto.toEntity(path.toString()));
//    }catch (IOException e){ // 트라이-캐치 때문에 트랜잭션 처리때문에
//        throw new RuntimeException("이미지 저장 실패"); // 여기서 예외를 던져용
//    }
//    String imagePath = dto.getProductImage().getName(); // 내가한게 남았네
//    return product;
//}
