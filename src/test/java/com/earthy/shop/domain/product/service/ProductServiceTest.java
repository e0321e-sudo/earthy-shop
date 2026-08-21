package com.earthy.shop.domain.product.service;

import com.earthy.shop.common.exception.BusinessException;
import com.earthy.shop.common.exception.ErrorCode;
import com.earthy.shop.common.response.PageResponseDto;
import com.earthy.shop.domain.addon.dto.response.AddonResponseDto;
import com.earthy.shop.domain.addon.enums.AddonType;
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
import com.earthy.shop.support.TestEntityUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private AddonService addonService;

    @Mock
    private CartItemRepository cartItemRepository;

    @InjectMocks
    private ProductService productService;

    @Test
    void 고객용_전체_상품_목록을_조회한다() {
        // given
        Pageable pageable = PageRequest.of(0, 20);
        Product product = product("sunset sea postcard", ProductCategory.POSTCARD, 3500, 100);

        given(productRepository.findByActiveTrueAndDeletedFalse(pageable))
                .willReturn(new PageImpl<>(List.of(product), pageable, 1));

        // when
        PageResponseDto<ProductResponseDto> response = productService.getProducts(null, pageable);

        // then
        assertThat(response.content()).hasSize(1);
        assertThat(response.content().getFirst().name()).isEqualTo("sunset sea postcard");
    }

    @Test
    void 고객용_카테고리별_상품_목록을_조회한다() {
        // given
        Pageable pageable = PageRequest.of(0, 20);
        Product product = product("sunset sea poster", ProductCategory.POSTER, 12000, 100);

        given(productRepository.findByCategoryAndActiveTrueAndDeletedFalse(ProductCategory.POSTER, pageable))
                .willReturn(new PageImpl<>(List.of(product), pageable, 1));

        // when
        PageResponseDto<ProductResponseDto> response = productService.getProducts(ProductCategory.POSTER, pageable);

        // then
        assertThat(response.content()).hasSize(1);
        assertThat(response.content().getFirst().category()).isEqualTo(ProductCategory.POSTER);
    }

    @Test
    void 포스터_상세_조회_시_추가상품을_함께_조회한다() {
        // given
        Product product = product("sunset sea poster", ProductCategory.POSTER, 12000, 100);
        AddonResponseDto addon = new AddonResponseDto(1L, "A3 원목 액자", AddonType.FRAME, "액자", 12000, false);

        given(productRepository.findByIdAndActiveTrueAndDeletedFalse(1L))
                .willReturn(Optional.of(product));
        given(addonService.getAddons()).willReturn(List.of(addon));

        // when
        ProductDetailResponseDto response = productService.getProduct(1L);

        // then
        assertThat(response.addons()).hasSize(1);
        assertThat(response.addons().getFirst().name()).isEqualTo("A3 원목 액자");
    }

    @Test
    void 엽서_상세_조회_시_추가상품을_조회하지_않는다() {
        // given
        Product product = product("sunset sea postcard", ProductCategory.POSTCARD, 3500, 100);

        given(productRepository.findByIdAndActiveTrueAndDeletedFalse(1L))
                .willReturn(Optional.of(product));

        // when
        ProductDetailResponseDto response = productService.getProduct(1L);

        // then
        verify(addonService, never()).getAddons();
        assertThat(response.addons()).isEmpty();
    }

    @Test
    void 상품명을_검색한다() {
        // given
        Pageable pageable = PageRequest.of(0, 20);
        Product product = product("sunset sea postcard", ProductCategory.POSTCARD, 3500, 100);

        given(productRepository.searchByName("sunset", pageable))
                .willReturn(new PageImpl<>(List.of(product), pageable, 1));

        // when
        PageResponseDto<ProductResponseDto> response = productService.searchProducts("sunset", pageable);

        // then
        assertThat(response.content()).hasSize(1);
        assertThat(response.content().getFirst().name()).contains("sunset");
    }

    @Test
    void 상품을_등록한다() {
        // given
        ProductCreateRequestDto requestDto = new ProductCreateRequestDto(
                "sunset sea postcard",
                ProductCategory.POSTCARD,
                3500,
                "/assets/products/sunset-sea.jpeg",
                "/assets/products/detail.jpeg",
                "노을이 담긴 엽서",
                100
        );

        given(productRepository.save(any(Product.class)))
                .willAnswer(invocation -> {
                    Product product = invocation.getArgument(0);
                    TestEntityUtils.setId(product, 1L);
                    return product;
                });

        // when
        AdminProductResponseDto response = productService.createProduct(requestDto);

        // then
        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.name()).isEqualTo("sunset sea postcard");
        assertThat(response.category()).isEqualTo(ProductCategory.POSTCARD);
        assertThat(response.stockQuantity()).isEqualTo(100);
    }

    @Test
    void 상품을_수정한다() {
        // given
        Product product = product("sunset sea postcard", ProductCategory.POSTCARD, 3500, 100);
        ProductUpdateRequestDto requestDto = new ProductUpdateRequestDto(
                "forest poster",
                ProductCategory.POSTER,
                12000,
                "/assets/products/forest.jpeg",
                "/assets/products/forest-detail.jpeg",
                "숲 포스터",
                30
        );

        given(productRepository.findById(1L)).willReturn(Optional.of(product));

        // when
        AdminProductResponseDto response = productService.updateProduct(1L, requestDto);

        // then
        assertThat(response.name()).isEqualTo("forest poster");
        assertThat(response.category()).isEqualTo(ProductCategory.POSTER);
        assertThat(response.price()).isEqualTo(12000);
        assertThat(response.stockQuantity()).isEqualTo(30);
    }

    @Test
    void 상품_재고를_차감한다() {
        // given
        Product product = product("sunset sea postcard", ProductCategory.POSTCARD, 3500, 5);

        given(productRepository.findActiveByIdForUpdate(1L))
                .willReturn(Optional.of(product));

        // when
        productService.decreaseStock(1L, 2);

        // then
        verify(productRepository).findActiveByIdForUpdate(1L);
        assertThat(product.getStockQuantity()).isEqualTo(3);
    }

    @Test
    void 상품_재고가_충분하면_재고_검증을_통과한다() {
        // given
        Product product = product("sunset sea postcard", ProductCategory.POSTCARD, 3500, 5);

        given(productRepository.findByIdAndActiveTrueAndDeletedFalse(1L))
                .willReturn(Optional.of(product));

        // when
        productService.validateStock(1L, 5);

        // then
        assertThat(product.getStockQuantity()).isEqualTo(5);
    }

    @Test
    void 상품_재고를_복구한다() {
        // given
        Product product = product("sunset sea postcard", ProductCategory.POSTCARD, 3500, 5);

        given(productRepository.findById(1L)).willReturn(Optional.of(product));

        // when
        productService.increaseStock(1L, 3);

        // then
        assertThat(product.getStockQuantity()).isEqualTo(8);
    }

    @Test
    void 상품_재고_복구_수량이_1보다_작으면_예외가_발생한다() {
        // given
        Product product = product("sunset sea postcard", ProductCategory.POSTCARD, 3500, 5);

        given(productRepository.findById(1L)).willReturn(Optional.of(product));

        // when & then
        assertThatExceptionOfType(BusinessException.class)
                .isThrownBy(() -> productService.increaseStock(1L, 0))
                .satisfies(exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_QUANTITY)
                );
    }

    @Test
    void 재고가_부족하면_예외가_발생한다() {
        // given
        Product product = product("sunset sea postcard", ProductCategory.POSTCARD, 3500, 1);

        given(productRepository.findActiveByIdForUpdate(1L))
                .willReturn(Optional.of(product));

        // when & then
        assertThatExceptionOfType(BusinessException.class)
                .isThrownBy(() -> productService.decreaseStock(1L, 2))
                .satisfies(exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.OUT_OF_STOCK)
                );
        verify(productRepository).findActiveByIdForUpdate(1L);
    }

    @Test
    void 상품을_판매중지한다() {
        // given
        Product product = product("sunset sea postcard", ProductCategory.POSTCARD, 3500, 5);

        given(productRepository.findById(1L)).willReturn(Optional.of(product));

        // when
        AdminProductResponseDto response = productService.deactivateProduct(1L);

        // then
        assertThat(response.active()).isFalse();
    }

    @Test
    void 상품을_판매재개한다() {
        // given
        Product product = product("sunset sea postcard", ProductCategory.POSTCARD, 3500, 5);
        product.deactivate();

        given(productRepository.findById(1L)).willReturn(Optional.of(product));

        // when
        AdminProductResponseDto response = productService.activateProduct(1L);

        // then
        assertThat(response.active()).isTrue();
    }

    @Test
    void 상품을_삭제하면_장바구니_항목을_정리하고_소프트_삭제한다() {
        // given
        Product product = product("sunset sea postcard", ProductCategory.POSTCARD, 3500, 5);

        given(productRepository.findById(1L)).willReturn(Optional.of(product));

        // when
        productService.deleteProduct(1L);

        // then
        verify(cartItemRepository).deleteByProduct(product);
        assertThat(product.isDeleted()).isTrue();
        assertThat(product.isActive()).isFalse();
    }

    @Test
    void 상품이_없으면_예외가_발생한다() {
        // given
        given(productRepository.findByIdAndActiveTrueAndDeletedFalse(1L))
                .willReturn(Optional.empty());

        // when & then
        assertThatExceptionOfType(BusinessException.class)
                .isThrownBy(() -> productService.getActiveProduct(1L))
                .satisfies(exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.PRODUCT_NOT_FOUND)
                );
    }

    private Product product(String name, ProductCategory category, int price, int stockQuantity) {
        Product product = new Product(
                name,
                category,
                price,
                "/assets/products/image.jpeg",
                "/assets/products/detail.jpeg",
                "상품 설명",
                stockQuantity
        );
        TestEntityUtils.setId(product, 1L);
        return product;
    }
}
