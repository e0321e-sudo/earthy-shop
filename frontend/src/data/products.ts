export type ProductCategory = "ALL" | "POSTCARD" | "POSTER" | "ETC";
export type AddonType = "FRAME";

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
  description: string;
  soldOut: boolean;
  addons?: Addon[];
}

export interface CategoryTab {
  label: string;
  value: ProductCategory;
}

const imageItems: Array<[string, string, string]> = [
  ["sunset-sea", "sunset sea", "/assets/products/sunset-sea.jpeg"],
  ["field-town", "field town", "/assets/products/field-town.jpeg"],
  ["cloud-field", "cloud field", "/assets/products/cloud-field.jpeg"],
  ["grazing-horse", "grazing horse", "/assets/products/grazing-horse.jpeg"],
  ["green-valley", "green valley", "/assets/products/green-valley.jpeg"],
  ["tidal-beach", "tidal beach", "/assets/products/tidal-beach.jpeg"],
  ["road-field", "road field", "/assets/products/road-field.jpeg"],
  ["forest-walk", "forest walk", "/assets/products/forest-walk.jpeg"],
];

export const addons: Addon[] = [
  {
    id: 1,
    name: "A3 원목 액자",
    type: "FRAME",
    typeDescription: "액자",
    price: 12000,
    soldOut: false,
  },
  {
    id: 2,
    name: "A2 원목 액자",
    type: "FRAME",
    typeDescription: "액자",
    price: 18000,
    soldOut: false,
  },
];

export const products: Product[] = [
  ...imageItems.map(([, name, imageUrl], index) => ({
    id: index + 1,
    name: `${name} postcard`,
    category: "POSTCARD" as const,
    categoryDescription: "엽서",
    price: 3500,
    imageUrl,
    description: "자연의 색과 온도를 작은 종이에 담은 엽서",
    soldOut: false,
  })),
  ...imageItems.map(([, name, imageUrl], index) => ({
    id: imageItems.length + index + 1,
    name: `${name} poster`,
    category: "POSTER" as const,
    categoryDescription: "포스터",
    price: 18000,
    imageUrl,
    description: "방 안에 오래 걸어두기 좋은 자연 사진 포스터",
    soldOut: false,
    addons,
  })),
];

export const categoryTabs: CategoryTab[] = [
  { label: "ALL", value: "ALL" },
  { label: "엽서", value: "POSTCARD" },
  { label: "포스터", value: "POSTER" },
  { label: "ETC", value: "ETC" },
];
