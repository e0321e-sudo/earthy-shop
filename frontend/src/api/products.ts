import { request } from "./http";
import type { Product, ProductCategory } from "../data/products";

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

export function getProductsPage(category: ProductCategory, page = 0, size = 20): Promise<PageResponse<Product>> {
  if (category === "ALL") {
    return request<PageResponse<Product>>(`/api/products?page=${page}&size=${size}`);
  }

  return request<PageResponse<Product>>(`/api/products?category=${category}&page=${page}&size=${size}`);
}

export function getProduct(productId: number): Promise<Product> {
  return request<Product>(`/api/products/${productId}`);
}
