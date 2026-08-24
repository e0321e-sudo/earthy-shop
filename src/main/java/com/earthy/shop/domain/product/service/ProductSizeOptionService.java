package com.earthy.shop.domain.product.service;

import com.earthy.shop.common.exception.BusinessException;
import com.earthy.shop.common.exception.ErrorCode;
import com.earthy.shop.domain.product.dto.request.ProductSizeOptionRequestDto;
import com.earthy.shop.domain.product.entity.Product;
import com.earthy.shop.domain.product.entity.ProductSizeOption;
import com.earthy.shop.domain.product.enums.ProductCategory;
import com.earthy.shop.domain.product.repository.ProductSizeOptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ProductSizeOptionService {

    private final ProductSizeOptionRepository productSizeOptionRepository;

    // 관리자용 상품 사이즈 옵션 조회
    public List<ProductSizeOption> getOptions(Product product) {
        return productSizeOptionRepository.findByProductAndDeletedFalseOrderByIdAsc(product);
    }

    // 고객용 활성 상품 사이즈 옵션 조회
    public List<ProductSizeOption> getActiveOptions(Product product) {
        return productSizeOptionRepository.findByProductAndActiveTrueAndDeletedFalseOrderByIdAsc(product);
    }

    // 고객용 활성 상품 사이즈 옵션 단건 조회
    public ProductSizeOption getActiveOption(Product product, Long optionId) {
        return productSizeOptionRepository.findByIdAndProductAndActiveTrueAndDeletedFalse(optionId, product)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_SIZE_OPTION_NOT_FOUND));
    }

    // 사이즈 옵션 재고 차감
    @Transactional
    public void decreaseStock(Long optionId, int quantity) {
        ProductSizeOption option = productSizeOptionRepository.findActiveByIdForUpdate(optionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_SIZE_OPTION_NOT_FOUND));

        option.decreaseStock(quantity);

        log.info("[PRODUCT SIZE STOCK DECREASED] sizeOptionId={} | quantity={}",
                optionId,
                quantity);
    }

    // 사이즈 옵션 재고 복구
    @Transactional
    public void increaseStock(Long optionId, int quantity) {
        ProductSizeOption option = productSizeOptionRepository.findById(optionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_SIZE_OPTION_NOT_FOUND));

        option.increaseStock(quantity);

        log.info("[PRODUCT SIZE STOCK RESTORED] sizeOptionId={} | quantity={}",
                optionId,
                quantity);
    }

    // 사이즈 옵션 재고 검증
    public void validateStock(Long optionId, int quantity) {
        ProductSizeOption option = productSizeOptionRepository.findById(optionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_SIZE_OPTION_NOT_FOUND));

        if (!option.isActive() || option.isDeleted() || option.getStockQuantity() < quantity) {
            throw new BusinessException(ErrorCode.OUT_OF_STOCK);
        }
    }

    // 상품 카테고리에 맞게 사이즈 옵션 동기화
    @Transactional
    public void syncOptions(Product product, List<ProductSizeOptionRequestDto> requestDtos) {
        // 포스터가 아닌 상품은 사이즈 옵션을 사용하지 않음
        if (product.getCategory() != ProductCategory.POSTER) {
            deleteAllOptions(product);
            return;
        }

        if (requestDtos == null || requestDtos.isEmpty()) {
            throw new BusinessException(ErrorCode.PRODUCT_SIZE_OPTION_REQUIRED);
        }

        List<ProductSizeOption> savedOptions = getOptions(product);
        Map<Long, ProductSizeOption> savedOptionMap = savedOptions.stream()
                .collect(Collectors.toMap(ProductSizeOption::getId, Function.identity()));
        Set<Long> requestedIds = new HashSet<>();

        for (ProductSizeOptionRequestDto requestDto : requestDtos) {
            validateOption(requestDto);

            if (requestDto.getId() == null) {
                productSizeOptionRepository.save(new ProductSizeOption(
                        product,
                        requestDto.getSizeName().trim(),
                        requestDto.getAdditionalPrice(),
                        requestDto.getStockQuantity(),
                        requestDto.getActive()
                ));
                continue;
            }

            ProductSizeOption savedOption = savedOptionMap.get(requestDto.getId());

            if (savedOption == null) {
                throw new BusinessException(ErrorCode.PRODUCT_SIZE_OPTION_NOT_FOUND);
            }

            requestedIds.add(requestDto.getId());
            savedOption.update(
                    requestDto.getSizeName().trim(),
                    requestDto.getAdditionalPrice(),
                    requestDto.getStockQuantity(),
                    requestDto.getActive()
            );
        }

        // 요청에서 빠진 기존 옵션은 소프트 삭제
        for (ProductSizeOption savedOption : savedOptions) {
            if (!requestedIds.contains(savedOption.getId())) {
                savedOption.delete();
            }
        }
    }

    // 상품의 모든 사이즈 옵션 삭제
    private void deleteAllOptions(Product product) {
        getOptions(product).forEach(ProductSizeOption::delete);
    }

    // 사이즈 옵션 입력값 검증
    private void validateOption(ProductSizeOptionRequestDto requestDto) {
        if (requestDto.getSizeName() == null || requestDto.getSizeName().isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_PRODUCT_SIZE_OPTION);
        }

        if (requestDto.getAdditionalPrice() < 0 || requestDto.getStockQuantity() < 0) {
            throw new BusinessException(ErrorCode.INVALID_PRODUCT_SIZE_OPTION);
        }

        if (requestDto.getActive() == null) {
            throw new BusinessException(ErrorCode.INVALID_PRODUCT_SIZE_OPTION);
        }
    }
}
