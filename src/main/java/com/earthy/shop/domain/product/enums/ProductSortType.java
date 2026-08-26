package com.earthy.shop.domain.product.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;

@Getter
@RequiredArgsConstructor
public enum ProductSortType {

    LATEST("latest", Sort.by(Sort.Direction.DESC, "createdAt")),
    PRICE_ASC("priceAsc", Sort.by(Sort.Direction.ASC, "price")),
    PRICE_DESC("priceDesc", Sort.by(Sort.Direction.DESC, "price")),
    POPULAR("popular", Sort.unsorted());

    private final String value;
    private final Sort sort;

    public static ProductSortType from(String value) {
        for (ProductSortType sortType : values()) {
            if (sortType.value.equals(value)) {
                return sortType;
            }
        }

        return LATEST;
    }
}
