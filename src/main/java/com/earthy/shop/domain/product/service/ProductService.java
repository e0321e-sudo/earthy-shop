package com.earthy.shop.domain.product.service;

import com.earthy.shop.common.exception.BusinessException;
import com.earthy.shop.common.exception.ErrorCode;
import com.earthy.shop.common.response.PageResponseDto;
import com.earthy.shop.domain.addon.dto.response.AddonResponseDto;
import com.earthy.shop.domain.addon.service.AddonService;
import com.earthy.shop.domain.cart.repository.CartItemRepository;
import com.earthy.shop.domain.product.dto.request.ProductCreateRequestDto;
import com.earthy.shop.domain.product.dto.request.ProductUpdateRequestDto;
import com.earthy.shop.domain.product.dto.response.AdminProductResponseDto;
import com.earthy.shop.domain.product.dto.response.ProductDetailResponseDto;
import com.earthy.shop.domain.product.dto.response.ProductResponseDto;
import com.earthy.shop.domain.product.entity.Product;
import com.earthy.shop.domain.product.enums.ProductCategory;
import com.earthy.shop.domain.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductService {

    private final ProductRepository productRepository;
    private final AddonService addonService;
    private final CartItemRepository cartItemRepository;

    // 고객용 상품 목록 조회
    public PageResponseDto<ProductResponseDto> getProducts(ProductCategory category, Pageable pageable){
        // 전체상품조회 선택 시 전체 활성 상품 조회
        if (category == null) {
            return PageResponseDto.from(productRepository.findByActiveTrueAndDeletedFalse(pageable)
                    .map(ProductResponseDto::from));
        }

        // 카테고리 선택 시 해당 카테고리 활성 상품 조회
        return PageResponseDto.from(productRepository.findByCategoryAndActiveTrueAndDeletedFalse(category, pageable)
                .map(ProductResponseDto::from));
    }

    // 고객 상품 상세 조회
    public ProductDetailResponseDto getProduct(Long productId) {
        Product product = productRepository.findByIdAndActiveTrueAndDeletedFalse(productId)
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
    public PageResponseDto<AdminProductResponseDto> getAdminProducts(Pageable pageable) {
        return PageResponseDto.from(productRepository.findByDeletedFalse(pageable)
                .map(AdminProductResponseDto::from));
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
        Product product = findProduct(productId);

        product.deactivate();

        return AdminProductResponseDto.from(product);
    }

    // 관리자용 상품 활성화
    @Transactional
    public AdminProductResponseDto activateProduct(Long productId) {
        Product product = findProduct(productId);

        product.activate();

        return AdminProductResponseDto.from(product);
    }

    // 관리자용 상품 삭제
    @Transactional
    public void deleteProduct(Long productId) {
        Product product = findProduct(productId);

        // 삭제 상품이 담긴 모든 장바구니 항목 정리
        cartItemRepository.deleteByProduct(product);

        // 상품 소프트 삭제
        product.delete();
    }

    // 상품 재고 차감
    @Transactional
    public void decreaseStock(Long productId, int quantity) {
        // 상품 조회
        Product product = getActiveProduct(productId);

        // 상품 재고 차감
        product.decreaseStock(quantity);
    }

    // 상품 재고 검증
    public void validateStock(Long productId, int quantity) {
        Product product = getActiveProduct(productId);

        if (product.getStockQuantity() < quantity) {
            throw new BusinessException(ErrorCode.OUT_OF_STOCK);
        }
    }

    // 상품 재고 복구
    @Transactional
    public void increaseStock(Long productId, int quantity) {
        Product product = findProduct(productId);

        product.increaseStock(quantity);
    }

    // 활성 상품 조회
    public Product getActiveProduct(Long productId) {
        return productRepository.findByIdAndActiveTrueAndDeletedFalse(productId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));
    }

    // 상품 단건 조회
    public Product findProduct(Long productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));
    }
}
