package com.earthy.shop.domain.product.service;

import com.earthy.shop.common.exception.BusinessException;
import com.earthy.shop.common.exception.ErrorCode;
import com.earthy.shop.common.response.PageResponseDto;
import com.earthy.shop.common.storage.service.S3ImageService;
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
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ProductService {

    private final ProductRepository productRepository;
    private final AddonService addonService;
    private final CartItemRepository cartItemRepository;
    private final ProductSizeOptionService productSizeOptionService;
    private final S3ImageService s3ImageService;

    // 고객용 상품 목록 조회
    public PageResponseDto<ProductResponseDto> getProducts(ProductCategory category, Pageable pageable){
        // 전체상품조회 선택 시 전체 활성 상품 조회
        if (category == null) {
            return PageResponseDto.from(productRepository.findByActiveTrueAndDeletedFalse(pageable)
                    .map(this::toProductResponse));
        }

        // 카테고리 선택 시 해당 카테고리 활성 상품 조회
        return PageResponseDto.from(productRepository.findByCategoryAndActiveTrueAndDeletedFalse(category, pageable)
                .map(this::toProductResponse));
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

        return ProductDetailResponseDto.of(
                product,
                addons,
                product.getCategory() == ProductCategory.POSTER
                        ? productSizeOptionService.getActiveOptions(product)
                        : List.of()
        );
    }

    // 고객용 상품명 검색
    public PageResponseDto<ProductResponseDto> searchProducts(String keyword, Pageable pageable) {
        return PageResponseDto.from(productRepository.searchByName(keyword, pageable)
                .map(this::toProductResponse));
    }

    // 관리자용 상품 등록
    @Transactional
    public AdminProductResponseDto createProduct(ProductCreateRequestDto requestDto) {
        Product product = new Product(
                requestDto.getName(),
                requestDto.getCategory(),
                requestDto.getPrice(),
                requestDto.getImageUrl(),
                normalizeOptionalText(requestDto.getDetailImageUrl()),
                requestDto.getDescription(),
                resolveStockQuantity(requestDto.getCategory(), requestDto.getStockQuantity())
        );

        Product savedProduct = productRepository.save(product);

        productSizeOptionService.syncOptions(savedProduct, requestDto.getSizeOptions());

        return AdminProductResponseDto.from(savedProduct, productSizeOptionService.getOptions(savedProduct));
    }

    // 관리자용 전체 상품 목록 조회
    public PageResponseDto<AdminProductResponseDto> getAdminProducts(Pageable pageable) {
        return PageResponseDto.from(productRepository.findByDeletedFalse(pageable)
                .map(product -> AdminProductResponseDto.from(product, productSizeOptionService.getOptions(product))));
    }

    // 관리자용 상품 수정
    @Transactional
    public AdminProductResponseDto updateProduct(Long productId, ProductUpdateRequestDto requestDto) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));
        String previousImageUrl = product.getImageUrl();
        String previousDetailImageUrl = product.getDetailImageUrl();
        String nextDetailImageUrl = normalizeOptionalText(requestDto.getDetailImageUrl());

        product.update(
                requestDto.getName(),
                requestDto.getCategory(),
                requestDto.getPrice(),
                requestDto.getImageUrl(),
                nextDetailImageUrl,
                requestDto.getDescription(),
                resolveStockQuantity(requestDto.getCategory(), requestDto.getStockQuantity())
        );

        productSizeOptionService.syncOptions(product, requestDto.getSizeOptions());
        deleteReplacedImageAfterCommit(previousImageUrl, requestDto.getImageUrl());
        deleteReplacedImageAfterCommit(previousDetailImageUrl, nextDetailImageUrl);

        return AdminProductResponseDto.from(product, productSizeOptionService.getOptions(product));
    }

    // 관리자용 상품 비활성화
    @Transactional
    public AdminProductResponseDto deactivateProduct(Long productId) {
        Product product = findProduct(productId);

        product.deactivate();

        return AdminProductResponseDto.from(product, productSizeOptionService.getOptions(product));
    }

    // 관리자용 상품 활성화
    @Transactional
    public AdminProductResponseDto activateProduct(Long productId) {
        Product product = findProduct(productId);

        product.activate();

        return AdminProductResponseDto.from(product, productSizeOptionService.getOptions(product));
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
        // 상품 잠금 조회
        Product product = getActiveProductForUpdate(productId);

        // 상품 재고 차감
        product.decreaseStock(quantity);

        log.info("[PRODUCT STOCK DECREASED] productId={} | quantity={}",
                productId,
                quantity);
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

        log.info("[PRODUCT STOCK RESTORED] productId={} | quantity={}",
                productId,
                quantity);
    }

    // 활성 상품 조회
    public Product getActiveProduct(Long productId) {
        return productRepository.findByIdAndActiveTrueAndDeletedFalse(productId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));
    }

    // 활성 상품 잠금 조회
    private Product getActiveProductForUpdate(Long productId) {
        return productRepository.findActiveByIdForUpdate(productId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));
    }

    // 상품 단건 조회
    public Product findProduct(Long productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));
    }

    // 선택 입력값은 비어 있으면 DB에 null로 저장
    private String normalizeOptionalText(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim();
    }

    // 포스터 상품은 사이즈별 재고를 사용하므로 기존 단일 재고는 0으로 유지
    private int resolveStockQuantity(ProductCategory category, int stockQuantity) {
        if (category == ProductCategory.POSTER) {
            return 0;
        }

        return stockQuantity;
    }

    // 상품 수정 트랜잭션 커밋 후 교체된 기존 S3 이미지만 정리
    private void deleteReplacedImageAfterCommit(String previousImageUrl, String nextImageUrl) {
        if (previousImageUrl == null || previousImageUrl.isBlank() || Objects.equals(previousImageUrl, nextImageUrl)) {
            return;
        }

        Runnable deleteTask = () -> s3ImageService.deleteImageIfOwned(previousImageUrl);

        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            deleteTask.run();
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                deleteTask.run();
            }
        });
    }

    // 포스터 상품은 사이즈 옵션 재고 기준으로 품절 여부를 계산
    private ProductResponseDto toProductResponse(Product product) {
        if (product.getCategory() != ProductCategory.POSTER) {
            return ProductResponseDto.from(product);
        }

        return ProductResponseDto.from(product, productSizeOptionService.getActiveOptions(product));
    }
}
