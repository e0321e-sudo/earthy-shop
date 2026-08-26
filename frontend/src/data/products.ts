export type ProductCategory = "ALL" | "POSTCARD" | "POSTER" | "ETC";
export type AddonType = "FRAME" | "PREMIUM_FRAME" | "BASIC_FRAME";
export type ProductSort = "latest" | "priceAsc" | "priceDesc" | "popular";

export interface Addon {
  id: number;
  name: string;
  type: AddonType;
  typeDescription: string;
  price: number;
  soldOut: boolean;
}

export interface Product {
  id: number;
  name: string;
  category: Exclude<ProductCategory, "ALL">;
  categoryDescription: string;
  price: number;
  imageUrl: string;
  detailImageUrl?: string | null;
  description: string;
  soldOut: boolean;
  addons?: Addon[];
  sizeOptions?: ProductSizeOption[];
}

export interface ProductSizeOption {
  id: number;
  sizeName: string;
  additionalPrice: number;
  stockQuantity: number;
  active: boolean;
  soldOut: boolean;
}

export interface CategoryTab {
  label: string;
  value: ProductCategory;
}

export const categoryTabs: CategoryTab[] = [
  { label: "ALL", value: "ALL" },
  { label: "엽서", value: "POSTCARD" },
  { label: "포스터", value: "POSTER" },
  { label: "ETC", value: "ETC" },
];

export const productSortOptions: Array<{ label: string; value: ProductSort }> = [
  { label: "신상품순", value: "latest" },
  { label: "낮은 가격순", value: "priceAsc" },
  { label: "높은 가격순", value: "priceDesc" },
  { label: "인기순", value: "popular" },
];
