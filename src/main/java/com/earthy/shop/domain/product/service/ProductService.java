package com.earthy.shop.domain.product.service;

import com.earthy.shop.common.exception.BusinessException;
import com.earthy.shop.common.exception.ErrorCode;
import com.earthy.shop.domain.addon.dto.response.AddonResponseDto;
import com.earthy.shop.domain.addon.service.AddonService;
import com.earthy.shop.domain.product.dto.request.ProductCreateRequestDto;
import com.earthy.shop.domain.product.dto.request.ProductUpdateRequestDto;
import com.earthy.shop.domain.product.dto.response.AdminProductResponseDto;
import com.earthy.shop.domain.product.dto.response.ProductDetailResponseDto;
import com.earthy.shop.domain.product.dto.response.ProductResponseDto;
import com.earthy.shop.domain.product.entity.Product;
import com.earthy.shop.domain.product.enums.ProductCategory;
import com.earthy.shop.domain.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductService {

    private final ProductRepository productRepository;
    private final AddonService addonService;

    // 고객용 상품 목록 조회
    public List<ProductResponseDto> getProducts(ProductCategory category){
        // 전체상품조회 선택 시 전체 활성 상품 조회
        if (category == null) {
            return productRepository.findByActiveTrue()
                    .stream()
                    .map(ProductResponseDto::from)
                    .toList();
        }

        // 카테고리 선택 시 해당 카테고리 활성 상품 조회
        return productRepository.findByCategoryAndActiveTrue(category)
                .stream()
                .map(ProductResponseDto::from)
                .toList();
    }

    // 고객 상품 상세 조회
    public ProductDetailResponseDto getProduct(Long productId) {
        Product product = productRepository.findByIdAndActiveTrue(productId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));

        List<AddonResponseDto> addons = List.of();

        // 포스터 상품 추가상품 조회
        if (product.getCategory() == ProductCategory.POSTER) {
            addons = addonService.getAddons();
        }

        return ProductDetailResponseDto.of(product, addons);
    }

    // 관리자용 상품 등록
    @Transactional
    public AdminProductResponseDto createProduct(ProductCreateRequestDto requestDto) {
        Product product = new Product(
                requestDto.getName(),
                requestDto.getCategory(),
                requestDto.getPrice(),
                requestDto.getImageUrl(),
                requestDto.getDescription(),
                requestDto.getStockQuantity()
        );

        Product savedProduct = productRepository.save(product);

        return AdminProductResponseDto.from(savedProduct);
    }

    // 관리자용 전체 상품 목록 조회
    public List<AdminProductResponseDto> getAdminProducts() {
        return productRepository.findAll()
                .stream()
                .map(AdminProductResponseDto::from)
                .toList();
    }

    // 관리자용 상품 수정
    @Transactional
    public AdminProductResponseDto updateProduct(Long productId, ProductUpdateRequestDto requestDto) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));

        product.update(
                requestDto.getName(),
                requestDto.getCategory(),
                requestDto.getPrice(),
                requestDto.getImageUrl(),
                requestDto.getDescription(),
                requestDto.getStockQuantity()
        );

        return AdminProductResponseDto.from(product);
    }

    // 관리자용 상품 비활성화
    @Transactional
    public AdminProductResponseDto deactivateProduct(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));

        product.deactivate();

        return AdminProductResponseDto.from(product);
    }

    // 활성 상품 엔티티 조회
    @Transactional(readOnly = true)
    public Product getActiveProductEntity(Long productId) {
        return productRepository.findByIdAndActiveTrue(productId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));
    }
}
