import { request } from "./http";
import type { Product, ProductCategory, ProductSort } from "../data/products";

export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
}

export function getProducts(category: ProductCategory): Promise<Product[]> {
    return getProductsPage(category).then((page) => page.content);
}

export function getProductsPage(
  category: ProductCategory,
  page = 0,
  size = 20,
  sort: ProductSort = "latest"
): Promise<PageResponse<Product>> {
  const params = new URLSearchParams({
    page: String(page),
    size: String(size),
    sort,
  });

  if (category === "ALL") {
    return request<PageResponse<Product>>(`/api/products?${params.toString()}`);
  }

  params.set("category", category);
  return request<PageResponse<Product>>(`/api/products?${params.toString()}`);
}

export function searchProductsPage(keyword: string, page = 0, size = 20): Promise<PageResponse<Product>> {
  const params = new URLSearchParams({
    keyword: keyword.trim(),
    page: String(page),
    size: String(size),
  });

  return request<PageResponse<Product>>(`/api/products/search?${params.toString()}`);
}

export function getProduct(productId: number): Promise<Product> {
  return request<Product>(`/api/products/${productId}`);
}
