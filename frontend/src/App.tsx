import { Fragment, useEffect, useRef, useState, type FormEvent, type MouseEvent, type ReactNode } from "react";
import {
  findEmail,
  findPassword,
  getKakaoLoginUrl,
  login,
  logout as requestLogout,
  signup,
  type LoginRequest,
  type LoginResponse,
  type EmailFindAccount,
  type MemberResponse,
  type SignupRequest,
} from "./api/auth";
import {
  addCartItem,
  deleteCartItem,
  getCart,
  updateCartItemQuantity,
  type CartItemAddRequest,
  type CartItemResponse,
} from "./api/cart";
import {
  deactivateMyAccount,
  getMyInfo,
  updateMyInfo,
  updateMyPassword,
} from "./api/member";
import {
  cancelMyOrder,
  createOrder,
  getMyOrder,
  getMyOrdersPage,
  type OrderCreateRequest,
  type OrderResponse,
  type PageResponse as OrderPageResponse,
} from "./api/orders";
import { confirmPayment, type PaymentResponse } from "./api/payments";
import { getProduct, getProductsPage, searchProductsPage, type PageResponse as ProductPageResponse } from "./api/products";
import { getNotice, getNoticesPage, type NoticeResponse } from "./api/notices";
import { refreshAuthIfPossible } from "./api/http";
import {
  createBoard,
  getBoardsPage,
  getPrivateBoard,
  getPublicBoard,
  updateBoard,
  type BoardListResponse,
  type BoardResponse,
  type BoardSaveRequest,
  type BoardType,
} from "./api/boards";
import {
  clearStoredPortOnePaymentContext,
  getStoredPortOnePaymentContext,
  requestPortOnePayment,
  type PortOnePayMethod,
} from "./api/portone";
import marketingConsentText from "./terms/marketing-consent.txt?raw";
import privacyCollectionText from "./terms/privacy-collection.txt?raw";
import serviceTermsText from "./terms/service-terms.txt?raw";
import {
  categoryTabs,
  productSortOptions,
  type Addon,
  type Product,
  type ProductCategory,
  type ProductSort,
  type ProductSizeOption,
} from "./data/products";

const DAUM_POSTCODE_SCRIPT_URL = "//t1.daumcdn.net/mapjsapi/bundle/postcode/prod/postcode.v2.js";
const FREE_DELIVERY_MIN_AMOUNT = 30000;
const BASE_DELIVERY_FEE = 2500;
const REMOTE_AREA_DELIVERY_FEE = 2000;
const PHONE_PREFIXES = ["010", "011", "016", "017", "018", "019"];
const EMAIL_FIND_PHONE_PATTERN = /^[0-9]{3}-[0-9]{4}-[0-9]{4}$/;
const DELIVERY_MEMO_OPTIONS = [
  "배송 전에 미리 연락바랍니다.",
  "부재 시 경비실에 맡겨주세요.",
  "부재 시 문 앞에 놓아주세요.",
  "빠른 배송 부탁드립니다.",
  "택배함에 보관해 주세요.",
];
const CUSTOM_DELIVERY_MEMO = "직접 입력";
const CUSTOMER_CANCEL_REASON_OPTIONS = [
  "단순 변심",
  "주문 실수",
  "배송지 변경",
  "결제수단 변경",
  "기타",
];
const BOARD_DEFAULT_CONTENT = `[고객상담 업무시간]
평일 오전 10시 - 오후 5시 (점심시간 오후 12시 - 1시) / 주말, 공휴일 휴무
*문의량이 많을 경우 당일 내 답변되지 못하고 1일 후 답변될 수 있습니다.

`;
const BOARD_PASSWORD_MASK = "****";
const BOARD_TYPES: Array<{ value: BoardType; label: string }> = [
  { value: "PRODUCT", label: "상품문의" },
  { value: "DELIVERY", label: "배송문의" },
  { value: "EXCHANGE_RETURN", label: "교환/반품문의" },
  { value: "CANCEL_CHANGE", label: "배송 전 취소/변경" },
  { value: "PAYMENT", label: "입금확인 문의" },
  { value: "ETC", label: "기타문의" },
];

// 다음 우편번호 검색 응답
interface DaumPostcodeData {
  zonecode: string;
  roadAddress: string;
  jibunAddress: string;
  address: string;
}

interface DaumPostcodeInstance {
  open: () => void;
  embed: (element: HTMLElement) => void;
}

interface DaumPostcodeConstructor {
  new (options: { oncomplete: (data: DaumPostcodeData) => void }): DaumPostcodeInstance;
}

declare global {
  interface Window {
    daum?: {
      Postcode: DaumPostcodeConstructor;
    };
  }
}

// 다음 우편번호 검색 스크립트 로드
function loadDaumPostcodeScript(): Promise<void> {
  if (window.daum?.Postcode) {
    return Promise.resolve();
  }

  const existingScript = document.querySelector<HTMLScriptElement>(
    `script[src="${DAUM_POSTCODE_SCRIPT_URL}"]`
  );

  if (existingScript) {
    return new Promise((resolve, reject) => {
      existingScript.addEventListener("load", () => resolve(), { once: true });
      existingScript.addEventListener("error", () => reject(new Error("우편번호 검색창을 불러오지 못했습니다.")), {
        once: true,
      });
    });
  }

  return new Promise((resolve, reject) => {
    const script = document.createElement("script");
    script.src = DAUM_POSTCODE_SCRIPT_URL;
    script.async = true;
    script.onload = () => resolve();
    script.onerror = () => reject(new Error("우편번호 검색창을 불러오지 못했습니다."));
    document.head.appendChild(script);
  });
}

type Page = "home" | "shop" | "search" | "detail" | "about" | "notice" | "board" | "cart" | "checkout" | "paymentResult" | "auth" | "mypage";
type AuthMode = "login" | "signup" | "findEmail" | "findPassword";
type AuthForm = Pick<SignupRequest, "email" | "password" | "name" | "phone">;
type EmailFindForm = Pick<SignupRequest, "name" | "phone">;
type MyPageView = "home" | "orders" | "orderDetail" | "profile";
type PaymentResultState =
  | { status: "processing"; message: string }
  | { status: "success"; message: string; payment: PaymentResponse }
  | { status: "fail"; message: string; retryOrder?: OrderResponse; showCancelNotice?: boolean };

type CustomerRoute = {
  page: Page;
  category?: ProductCategory;
  productId?: number;
  productPage?: number;
  productSort?: ProductSort;
};

const CATEGORY_ROUTE_SEGMENTS: Record<ProductCategory, string> = {
  ALL: "",
  POSTCARD: "postcard",
  POSTER: "poster",
  ETC: "etc",
};

const ROUTE_SEGMENT_CATEGORIES: Record<string, ProductCategory> = {
  postcard: "POSTCARD",
  poster: "POSTER",
  etc: "ETC",
};

const PRODUCT_SORT_VALUES = productSortOptions.map((option) => option.value);

function readPageParam(search: string) {
  const pageParam = new URLSearchParams(search).get("page");
  const parsedPage = pageParam === null ? 0 : Number(pageParam);

  return Number.isInteger(parsedPage) && parsedPage >= 0 ? parsedPage : 0;
}

function readProductSortParam(search: string): ProductSort {
  const sortParam = new URLSearchParams(search).get("sort");

  return PRODUCT_SORT_VALUES.includes(sortParam as ProductSort) ? (sortParam as ProductSort) : "latest";
}

function readCustomerRouteFromLocation(): CustomerRoute {
  const path = window.location.pathname.replace(/\/+$/, "") || "/";

  if (path === "/products") {
    return {
      page: "shop",
      category: "ALL",
      productPage: readPageParam(window.location.search),
      productSort: readProductSortParam(window.location.search),
    };
  }

  const productPathMatch = path.match(/^\/products\/([^/]+)$/);

  if (productPathMatch) {
    const segment = productPathMatch[1];
    const categoryFromSegment = ROUTE_SEGMENT_CATEGORIES[segment];

    if (categoryFromSegment) {
      return {
        page: "shop",
        category: categoryFromSegment,
        productPage: readPageParam(window.location.search),
        productSort: readProductSortParam(window.location.search),
      };
    }

    const productId = Number(segment);

    if (Number.isInteger(productId) && productId > 0) {
      return { page: "detail", productId };
    }
  }

  if (path === "/search") {
    return { page: "search" };
  }

  if (path === "/about") {
    return { page: "about" };
  }

  if (path === "/notice") {
    return { page: "notice" };
  }

  if (path === "/qna") {
    return { page: "board" };
  }

  if (path === "/cart") {
    return { page: "cart" };
  }

  if (path === "/checkout") {
    return { page: "checkout" };
  }

  if (path === "/payment-result") {
    return { page: "paymentResult" };
  }

  if (path === "/mypage") {
    return { page: "mypage" };
  }

  if (path === "/auth") {
    return { page: "auth" };
  }

  return { page: "home" };
}

function createCustomerRouteUrl(route: CustomerRoute) {
  if (route.page === "shop") {
    const category = route.category ?? "ALL";
    const productSort = route.productSort ?? "latest";
    const segment = CATEGORY_ROUTE_SEGMENTS[category];
    const path = segment ? `/products/${segment}` : "/products";
    const params = new URLSearchParams({ sort: productSort });

    if (route.productPage && route.productPage > 0) {
      params.set("page", String(route.productPage));
    }

    return `${path}?${params.toString()}`;
  }

  if (route.page === "detail" && route.productId) {
    return `/products/${route.productId}`;
  }

  if (route.page === "search") {
    return "/search";
  }

  if (route.page === "about") {
    return "/about";
  }

  if (route.page === "notice") {
    return "/notice";
  }

  if (route.page === "board") {
    return "/qna";
  }

  if (route.page === "cart") {
    return "/cart";
  }

  if (route.page === "checkout") {
    return "/checkout";
  }

  if (route.page === "paymentResult") {
    return "/payment-result";
  }

  if (route.page === "mypage") {
    return "/mypage";
  }

  if (route.page === "auth") {
    return "/auth";
  }

  return "/";
}

interface HeaderProps {
  page: Page;
  category: ProductCategory;
  cartCount: number;
  cartBumped: boolean;
  loggedIn: boolean;
  onHome: () => void;
  onCategory: (category: ProductCategory) => void;
  onAbout: () => void;
  onNotice: () => void;
  onBoard: () => void;
  onSearch: () => void;
  onAuth: () => void;
  onCart: () => void;
}

interface ShopProps {
  category: ProductCategory;
  products: Product[];
  pageInfo: ProductPageResponse<Product>;
  sort: ProductSort;
  loading: boolean;
  loaded: boolean;
  error: string | null;
  onOpenDetail: (productId: number) => void;
  onChangePage: (page: number) => void;
  onChangeSort: (sort: ProductSort) => void;
}

interface SearchPageProps {
  onOpenDetail: (productId: number) => void;
}

interface ProductDetailProps {
  product: Product | null;
  loading: boolean;
  error: string | null;
  onAddToCart: (requestBody: CartItemAddRequest) => Promise<void>;
  onBuyNow: (requestBody: CartItemAddRequest) => Promise<void>;
}

interface QuantityControlProps {
  label: string;
  value: number;
  onChange: (value: number) => void;
  trailingAction?: ReactNode;
  reserveTrailingAction?: boolean;
}

interface CartProps {
  items: CartItemResponse[];
  totalPrice: number;
  cartCount: number;
  loading: boolean;
  error: string | null;
  onOpenDetail: (productId: number) => void;
  onUpdateQuantity: (cartItemId: number, quantity: number) => Promise<void>;
  onRemove: (cartItemId: number) => Promise<void>;
  onCheckout: (cartItemIds?: number[]) => void;
}

interface CheckoutProps {
  totalPrice: number;
  member: MemberResponse | null;
  onMemberLoaded: (member: MemberResponse) => void;
  onCreateOrder: (requestBody: OrderCreateRequest, idempotencyKey: string) => Promise<OrderResponse>;
  onRequestPayment: (order: OrderResponse, payMethod: PortOnePayMethod, customerEmail: string) => Promise<void>;
}

interface PaymentResultProps {
  result: PaymentResultState;
  onRetryPayment: (order: OrderResponse) => Promise<void>;
  onMoveOrders: () => void;
  onMoveCart: () => void;
}

interface MyPageProps {
  member: MemberResponse | null;
  initialView: MyPageView;
  orders: OrderResponse[];
  orderPageInfo: OrderPageResponse<OrderResponse>;
  loading: boolean;
  error: string | null;
  onChangeOrderPage: (page: number) => void;
  onUpdateInfo: (
    name: string,
    phone: string,
    zipCode: string,
    address: string,
    detailAddress: string
  ) => Promise<void>;
  onUpdatePassword: (currentPassword: string, newPassword: string) => Promise<void>;
  onCancelOrder: (orderId: number, cancelReason: string) => Promise<OrderResponse>;
  onDeactivate: () => Promise<void>;
  onLogout: () => void;
}

interface AuthPageProps {
  onLoginSuccess: (loginResponse: LoginResponse) => void;
}

// 금액 표기
const formatWon = (value: number) => `${value.toLocaleString("ko-KR")}원`;

const preventProtectedImageContextMenu = (event: MouseEvent<HTMLImageElement>) => {
  event.preventDefault();
};

const preventProtectedImageAreaContextMenu = (event: MouseEvent<HTMLDivElement>) => {
  event.preventDefault();
};

const protectedImageProps = (className = "") => ({
  className: ["protected-image", className].filter(Boolean).join(" "),
  draggable: false,
  onContextMenu: preventProtectedImageContextMenu,
});

// 상품 상세 사이즈 옵션 표기
const formatSizeOptionLabel = (option: ProductSizeOption) => {
  if (option.additionalPrice === 0) {
    return option.sizeName;
  }

  const pricePrefix = option.additionalPrice > 0 ? "+" : "-";
  const price = Math.abs(option.additionalPrice).toLocaleString("ko-KR");

  return `${option.sizeName} (${pricePrefix}${price}원)`;
};

// 상품 상세 추가상품 옵션 표기
const formatAddonOptionLabel = (addon: Addon) => {
  if (addon.price === 0) {
    return addon.name;
  }

  const pricePrefix = addon.price > 0 ? "+" : "-";
  const price = Math.abs(addon.price).toLocaleString("ko-KR");

  return `${addon.name} (${pricePrefix}${price}원)`;
};

// 장바구니 사이즈 옵션 표기
const formatCartSizeOptionLabel = (item: CartItemResponse) => {
  if (!item.sizeName) {
    return null;
  }

  if (item.sizeAdditionalPrice === 0) {
    return `사이즈: ${item.sizeName}`;
  }

  const pricePrefix = item.sizeAdditionalPrice > 0 ? "+" : "-";
  const price = Math.abs(item.sizeAdditionalPrice).toLocaleString("ko-KR");

  return `사이즈: ${item.sizeName} (${pricePrefix}${price}원)`;
};

// 주문 상세 사이즈 옵션 표기
const formatOrderSizeOptionLabel = (item: {
  sizeName: string | null;
  sizeAdditionalPrice: number;
}) => {
  if (!item.sizeName) {
    return null;
  }

  if (item.sizeAdditionalPrice === 0) {
    return `사이즈: ${item.sizeName}`;
  }

  const pricePrefix = item.sizeAdditionalPrice > 0 ? "+" : "-";
  const price = Math.abs(item.sizeAdditionalPrice).toLocaleString("ko-KR");

  return `사이즈: ${item.sizeName} (${pricePrefix}${price}원)`;
};

// 주문 상세 결제수단 표기
const formatPaymentMethod = (paymentMethod?: string | null) => {
  if (!paymentMethod) {
    return "-";
  }

  const normalizedMethod = paymentMethod.replace(/[\s_-]/g, "").toUpperCase();

  if (
    normalizedMethod === "카드" ||
    normalizedMethod === "카드결제" ||
    normalizedMethod.includes("CARD") ||
    normalizedMethod.includes("EASYPAY")
  ) {
    return "카드결제";
  }

  if (
    normalizedMethod === "휴대폰" ||
    normalizedMethod === "휴대폰결제" ||
    normalizedMethod.includes("MOBILE") ||
    normalizedMethod.includes("PHONE")
  ) {
    return "휴대폰결제";
  }

  return paymentMethod;
};

// 우체국 배송조회 새 창 열기
const openPostOfficeTracking = (trackingNumber?: string | null) => {
  if (!trackingNumber) {
    window.alert("등록된 운송장 번호가 없습니다.");
    return;
  }

  const trackingUrl = `https://service.epost.go.kr/trace.RetrieveDomRigiTraceList.comm?sid1=${encodeURIComponent(
    trackingNumber
  )}`;

  window.open(trackingUrl, "_blank", "noopener,noreferrer");
};

// 일시 표기
const formatDate = (value: string) =>
  new Intl.DateTimeFormat("ko-KR", {
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
  }).format(new Date(value));

// Q&A 목록 일자 표기
const formatDateHyphen = (value: string) => {
  const date = new Date(value);
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, "0");
  const day = String(date.getDate()).padStart(2, "0");

  return `${year}-${month}-${day}`;
};

// 당일 작성 여부
const isCreatedToday = (value: string) => {
  const createdDate = new Date(value);
  const today = new Date();

  return (
    createdDate.getFullYear() === today.getFullYear() &&
    createdDate.getMonth() === today.getMonth() &&
    createdDate.getDate() === today.getDate()
  );
};

// 기본 배송비 계산
const calculateDeliveryFee = (productTotalPrice: number) =>
  productTotalPrice > 0 && productTotalPrice < FREE_DELIVERY_MIN_AMOUNT ? BASE_DELIVERY_FEE : 0;

// 지역별 배송비 계산
const calculateRemoteAreaDeliveryFee = (zipCode: string, address: string) =>
  isRemoteArea(zipCode, address) ? REMOTE_AREA_DELIVERY_FEE : 0;

function createEmptyPage<T>(size = 20): ProductPageResponse<T> {
  return {
    content: [],
    page: 0,
    size,
    totalElements: 0,
    totalPages: 0,
    first: true,
    last: true,
  };
}

// 제주/도서산간 여부 확인
function isRemoteArea(zipCode: string, address: string) {
  return (
    zipCode.startsWith("63") ||
    containsAny(address, [
      "제주특별자치도",
      "제주시",
      "서귀포시",
      "울릉군",
      "백령면",
      "대청면",
      "소청",
      "연평면",
      "흑산면",
      "홍도",
      "비금면",
      "도초면",
      "신의면",
      "하의면",
      "장산면",
      "안좌면",
      "팔금면",
      "암태면",
      "자은면",
      "압해읍",
      "완도군",
      "청산면",
      "노화읍",
      "보길면",
      "금일읍",
      "금당면",
      "생일면",
      "소안면",
      "진도군",
      "조도면",
      "남해군",
      "욕지면",
      "한산면",
      "사량면",
      "강원특별자치도 인제군",
      "강원특별자치도 양양군",
      "강원특별자치도 평창군",
      "강원특별자치도 정선군",
      "강원특별자치도 화천군",
      "강원특별자치도 양구군",
    ])
  );
}

function containsAny(value: string, keywords: string[]) {
  return keywords.some((keyword) => value.includes(keyword));
}

// 토스 결제창 이동 전 저장해둔 주문 정보 조회
function getStoredPaymentOrder(orderNumber: string | null): OrderResponse | undefined {
  if (!orderNumber) {
    return undefined;
  }

  const storedOrder = sessionStorage.getItem(`earthyPaymentOrderData:${orderNumber}`);

  if (!storedOrder) {
    return undefined;
  }

  try {
    return JSON.parse(storedOrder) as OrderResponse;
  } catch {
    return undefined;
  }
}

// 결제 승인 후 정리할 장바구니 항목 저장
function storePaymentCartItemIds(orderNumber: string, cartItemIds: number[]) {
  sessionStorage.setItem(`earthyPaymentCartItems:${orderNumber}`, JSON.stringify(cartItemIds));
}

// 결제 승인 멱등성 키 저장
function storePaymentConfirmKey(orderNumber: string, idempotencyKey: string) {
  sessionStorage.setItem(`earthyPaymentConfirmKey:${orderNumber}`, idempotencyKey);
}

// 결제 승인 멱등성 키 조회 또는 생성
function getOrCreatePaymentConfirmKey(orderNumber: string) {
  const storedKey = sessionStorage.getItem(`earthyPaymentConfirmKey:${orderNumber}`);

  if (storedKey) {
    return storedKey;
  }

  const nextKey = createIdempotencyKey();
  storePaymentConfirmKey(orderNumber, nextKey);

  return nextKey;
}

// 결제 승인 후 정리할 장바구니 항목 조회
function getStoredPaymentCartItemIds(orderNumber: string | null): number[] {
  if (!orderNumber) {
    return [];
  }

  const storedCartItemIds = sessionStorage.getItem(`earthyPaymentCartItems:${orderNumber}`);

  if (!storedCartItemIds) {
    return [];
  }

  try {
    const parsedCartItemIds = JSON.parse(storedCartItemIds);

    if (!Array.isArray(parsedCartItemIds)) {
      return [];
    }

    return parsedCartItemIds.filter((cartItemId): cartItemId is number => typeof cartItemId === "number");
  } catch {
    return [];
  }
}

function App() {
  const initialRoute = useRef(readCustomerRouteFromLocation()).current;

  // 화면 전환 상태
  const [page, setPage] = useState<Page>(initialRoute.page);
  const [category, setCategory] = useState<ProductCategory>(initialRoute.category ?? "ALL");
  const [selectedProductId, setSelectedProductId] = useState<number | null>(initialRoute.productId ?? null);
  const [noticePageKey, setNoticePageKey] = useState(0);
  const [boardPageKey, setBoardPageKey] = useState(0);

  // 장바구니 상태
  const [cartItems, setCartItems] = useState<CartItemResponse[]>([]);
  const [cartLoading, setCartLoading] = useState(false);
  const [cartError, setCartError] = useState<string | null>(null);

  // 상품 상태
  const [products, setProducts] = useState<Product[]>([]);
  const [productPageInfo, setProductPageInfo] = useState<ProductPageResponse<Product>>(() => createEmptyPage<Product>());
  const [productPage, setProductPage] = useState(initialRoute.productPage ?? 0);
  const [productSort, setProductSort] = useState<ProductSort>(initialRoute.productSort ?? "latest");
  const [productLoading, setProductLoading] = useState(initialRoute.page === "shop");
  const [productLoaded, setProductLoaded] = useState(false);
  const [productError, setProductError] = useState<string | null>(null);
  const [productDetail, setProductDetail] = useState<Product | null>(null);
  const [productDetailLoading, setProductDetailLoading] = useState(false);
  const [productDetailError, setProductDetailError] = useState<string | null>(null);

  // 인증/마이페이지 상태
  const [accessToken, setAccessToken] = useState(() => localStorage.getItem("earthyAccessToken"));
  const [authPageKey, setAuthPageKey] = useState(0);
  const [myPageKey, setMyPageKey] = useState(0);
  const [myPageInitialView, setMyPageInitialView] = useState<MyPageView>("home");
  const [member, setMember] = useState<MemberResponse | null>(null);
  const [orders, setOrders] = useState<OrderResponse[]>([]);
  const [orderPageInfo, setOrderPageInfo] = useState<OrderPageResponse<OrderResponse>>(() => createEmptyPage<OrderResponse>());
  const [orderPage, setOrderPage] = useState(0);
  const [myPageLoading, setMyPageLoading] = useState(false);
  const [myPageError, setMyPageError] = useState<string | null>(null);

  // UI 알림/결제 상태
  const [cartNoticeOpen, setCartNoticeOpen] = useState(false);
  const [cartBumped, setCartBumped] = useState(false);
  const [paymentResult, setPaymentResult] = useState<PaymentResultState | null>(null);

  // 선택상품주문 대상 장바구니 ID
  const [checkoutCartItemIds, setCheckoutCartItemIds] = useState<number[] | undefined>(undefined);

  const cartCount = cartItems.length;
  const cartTotal = cartItems.reduce((sum, item) => sum + item.itemTotalPrice, 0);
  const checkoutItems = checkoutCartItemIds
    ? cartItems.filter((item) => checkoutCartItemIds.includes(item.cartItemId))
    : cartItems;
  const checkoutTotal = checkoutItems.reduce((sum, item) => sum + item.itemTotalPrice, 0);

  const applyCustomerRoute = (route: CustomerRoute) => {
    setPage(route.page);

    if (route.page === "shop") {
      setCategory(route.category ?? "ALL");
      setProductPage(route.productPage ?? 0);
      setProductSort(route.productSort ?? "latest");
      setProducts([]);
      setProductPageInfo(createEmptyPage<Product>());
      setProductLoading(true);
      setProductLoaded(false);
      setProductError(null);
      setSelectedProductId(null);
      setProductDetail(null);
      return;
    }

    if (route.page === "detail" && route.productId) {
      setSelectedProductId(route.productId);
      setProductDetail(null);
      return;
    }

    setSelectedProductId(null);
    setProductDetail(null);
  };

  const navigateCustomerRoute = (route: CustomerRoute, options?: { replace?: boolean; scroll?: boolean }) => {
    applyCustomerRoute(route);

    const nextUrl = createCustomerRouteUrl(route);
    const currentUrl = `${window.location.pathname}${window.location.search}`;

    if (nextUrl !== currentUrl) {
      if (options?.replace) {
        window.history.replaceState({}, "", nextUrl);
      } else {
        window.history.pushState({}, "", nextUrl);
      }
    }

    if (options?.scroll !== false) {
      window.scrollTo({ top: 0, behavior: "smooth" });
    }
  };

  // 고객용 사이트 전체 우클릭 메뉴 차단
  useEffect(() => {
    const preventCustomerContextMenu = (event: Event) => {
      event.preventDefault();
    };

    document.addEventListener("contextmenu", preventCustomerContextMenu);

    return () => {
      document.removeEventListener("contextmenu", preventCustomerContextMenu);
    };
  }, []);

  useEffect(() => {
    const handlePopState = () => {
      applyCustomerRoute(readCustomerRouteFromLocation());
    };

    window.addEventListener("popstate", handlePopState);

    return () => {
      window.removeEventListener("popstate", handlePopState);
    };
  }, []);

  // 소셜 로그인 완료 토큰 저장
  useEffect(() => {
    const params = new URLSearchParams(window.location.search);
    const oauthProvider = params.get("oauth");
    const oauthAccessToken = params.get("accessToken");
    const oauthRefreshToken = params.get("refreshToken");

    if (oauthProvider !== "kakao" || !oauthAccessToken || !oauthRefreshToken) {
      return;
    }

    localStorage.setItem("earthyAccessToken", oauthAccessToken);
    localStorage.setItem("earthyRefreshToken", oauthRefreshToken);
    setAccessToken(oauthAccessToken);
    setPage("home");

    window.history.replaceState({}, "", "/");
  }, []);

  // 리프레시 토큰 재발급 실패 시 로그인 상태 정리
  useEffect(() => {
    const clearAuthState = () => {
      setAccessToken(null);
      setMember(null);
      setOrders([]);
      setCartItems([]);
      setCheckoutCartItemIds(undefined);
    };

    window.addEventListener("earthy-auth-cleared", clearAuthState);

    return () => {
      window.removeEventListener("earthy-auth-cleared", clearAuthState);
    };
  }, []);

  // 상품 목록 조회
  useEffect(() => {
    let ignore = false;

    async function fetchProducts() {
      setProductLoading(true);
      setProductLoaded(false);
      setProductError(null);
      setProducts([]);
      setProductPageInfo(createEmptyPage<Product>());

      try {
        const data = await getProductsPage(category, productPage, 20, productSort);

        if (!ignore) {
          setProducts(data.content);
          setProductPageInfo(data);
          setProductLoaded(true);
        }
      } catch (error) {
        if (!ignore) {
          setProducts([]);
          setProductPageInfo(createEmptyPage<Product>());
          setProductLoaded(true);
          setProductError(error instanceof Error ? error.message : "상품 목록 조회 실패");
        }
      } finally {
        if (!ignore) {
          setProductLoading(false);
        }
      }
    }

    if (page === "shop") {
      void fetchProducts();
    }

    return () => {
      ignore = true;
    };
  }, [category, page, productPage, productSort]);

  // 상품 상세 조회
  useEffect(() => {
    let ignore = false;

    async function fetchProductDetail(productId: number) {
      setProductDetailLoading(true);
      setProductDetailError(null);

      try {
        const data = await getProduct(productId);

        if (!ignore) {
          setProductDetail(data);
        }
      } catch (error) {
        if (!ignore) {
          setProductDetail(null);
          setProductDetailError(error instanceof Error ? error.message : "상품 상세 조회 실패");
        }
      } finally {
        if (!ignore) {
          setProductDetailLoading(false);
        }
      }
    }

    if (page === "detail" && selectedProductId !== null) {
      void fetchProductDetail(selectedProductId);
    }

    return () => {
      ignore = true;
    };
  }, [page, selectedProductId]);

  // 장바구니 조회
  const loadCart = async () => {
    if (!accessToken) {
      setCartItems([]);
      return;
    }

    setCartLoading(true);
    setCartError(null);

    try {
      const cart = await getCart();
      setCartItems(cart.items);
    } catch (error) {
      setCartError(error instanceof Error ? error.message : "장바구니 조회 실패");
    } finally {
      setCartLoading(false);
    }
  };

  // 마이페이지 정보 조회
  const loadMyPage = async () => {
    if (!accessToken) {
      setMember(null);
      setOrders([]);
      setOrderPageInfo(createEmptyPage<OrderResponse>());
      return;
    }

    setMyPageLoading(true);
    setMyPageError(null);

    try {
      const [memberData, orderData] = await Promise.all([getMyInfo(), getMyOrdersPage(orderPage)]);
      setMember(memberData);
      setOrders(orderData.content);
      setOrderPageInfo(orderData);
    } catch (error) {
      setMyPageError(error instanceof Error ? error.message : "마이페이지 조회 실패");
    } finally {
      setMyPageLoading(false);
    }
  };

  // 결제 성공 후 주문한 장바구니 항목 정리
  const clearPaidCartItems = async (orderNumber: string) => {
    const cartItemIds = getStoredPaymentCartItemIds(orderNumber);

    if (cartItemIds.length === 0) {
      return;
    }

    await Promise.all(
      cartItemIds.map(async (cartItemId) => {
        try {
          await deleteCartItem(cartItemId);
        } catch {
          // 이미 삭제된 항목은 결제 완료 흐름을 막지 않음
        }
      })
    );

    sessionStorage.removeItem(`earthyPaymentCartItems:${orderNumber}`);
    await loadCart();
  };

  // PortOne 결제창 요청 후 서버 승인 처리
  const requestAndConfirmPortOnePayment = async (
    order: OrderResponse,
    payMethod: PortOnePayMethod = "CARD",
    customerEmail = member?.email ?? ""
  ) => {
    setPaymentResult({ status: "processing", message: "결제창을 여는 중입니다." });
    setPage("paymentResult");

    try {
      const portOnePaymentId = await requestPortOnePayment(order, payMethod, customerEmail);
      setPaymentResult({ status: "processing", message: "결제 승인 중입니다." });

      const payment = await confirmPayment(
        {
          orderId: order.orderId,
          paymentId: portOnePaymentId,
          amount: order.totalPrice,
        },
        getOrCreatePaymentConfirmKey(order.orderNumber)
      );

      await clearPaidCartItems(order.orderNumber);
      sessionStorage.removeItem(`earthyPaymentOrder:${order.orderNumber}`);
      sessionStorage.removeItem(`earthyPaymentOrderData:${order.orderNumber}`);
      sessionStorage.removeItem(`earthyPaymentConfirmKey:${order.orderNumber}`);
      clearStoredPortOnePaymentContext(portOnePaymentId);
      await loadMyPage();

      setPaymentResult({
        status: "success",
        message: "결제가 완료되었습니다.",
        payment,
      });
    } catch (paymentError) {
      setPaymentResult({
        status: "fail",
        message: paymentError instanceof Error ? paymentError.message : "결제 요청 실패",
        retryOrder: order,
        showCancelNotice: true,
      });
    }
  };

  // 결제 결과 콜백 처리
  useEffect(() => {
    const params = new URLSearchParams(window.location.search);
    const result = params.get("paymentResult");

    if (result !== "success" && result !== "fail" && result !== "portone") {
      return;
    }

    const cleanPaymentUrl = () => {
      window.history.replaceState({}, "", window.location.pathname);
    };

    if (result === "portone") {
      const portOnePaymentId = params.get("paymentId") ?? params.get("payment_id");
      const portOneErrorCode = params.get("code");
      const portOneErrorMessage = params.get("message");
      const storedContext = portOnePaymentId ? getStoredPortOnePaymentContext(portOnePaymentId) : undefined;
      const storedRetryOrder = getStoredPaymentOrder(storedContext?.orderNumber ?? null);

      if (portOneErrorCode) {
        setPaymentResult({
          status: "fail",
          message: portOneErrorMessage ?? "결제가 취소되었거나 실패했습니다.",
          retryOrder: storedRetryOrder,
          showCancelNotice: true,
        });
        setPage("paymentResult");
        cleanPaymentUrl();
        return;
      }

      if (!portOnePaymentId || !storedContext) {
        setPaymentResult({
          status: "fail",
          message: "결제 승인에 필요한 정보가 없습니다. 주문 내역을 확인해주세요.",
          retryOrder: storedRetryOrder,
          showCancelNotice: Boolean(storedRetryOrder),
        });
        setPage("paymentResult");
        cleanPaymentUrl();
        return;
      }

      const confirmedPortOnePaymentId = portOnePaymentId;
      const confirmedPortOneContext = storedContext;

      setPaymentResult({ status: "processing", message: "결제 승인 중입니다." });
      setPage("paymentResult");
      cleanPaymentUrl();

      // PortOne 모바일 redirect 결제 승인
      async function approvePortOnePayment() {
        try {
          const payment = await confirmPayment(
            {
              orderId: confirmedPortOneContext.orderId,
              paymentId: confirmedPortOnePaymentId,
              amount: confirmedPortOneContext.amount,
            },
            getOrCreatePaymentConfirmKey(confirmedPortOneContext.orderNumber)
          );

          await clearPaidCartItems(confirmedPortOneContext.orderNumber);
          sessionStorage.removeItem(`earthyPaymentOrder:${confirmedPortOneContext.orderNumber}`);
          sessionStorage.removeItem(`earthyPaymentOrderData:${confirmedPortOneContext.orderNumber}`);
          sessionStorage.removeItem(`earthyPaymentConfirmKey:${confirmedPortOneContext.orderNumber}`);
          clearStoredPortOnePaymentContext(confirmedPortOnePaymentId);
          await loadMyPage();
          setPaymentResult({
            status: "success",
            message: "결제가 완료되었습니다.",
            payment,
          });
        } catch (paymentError) {
          let retryOrder: OrderResponse | undefined = storedRetryOrder;

          if (!retryOrder) {
            try {
              retryOrder = await getMyOrder(confirmedPortOneContext.orderId);
            } catch {
              retryOrder = undefined;
            }
          }

          setPaymentResult({
            status: "fail",
            message: paymentError instanceof Error ? paymentError.message : "결제 승인 실패",
            retryOrder,
            showCancelNotice: false,
          });
        }
      }

      void approvePortOnePayment();
      return;
    }

    const paymentKey = params.get("paymentKey");
    const legacyOrderNumber = params.get("orderId");
    const amount = params.get("amount");
    const storedOrderId = legacyOrderNumber ? sessionStorage.getItem(`earthyPaymentOrder:${legacyOrderNumber}`) : null;
    const storedRetryOrder = getStoredPaymentOrder(legacyOrderNumber);

    if (result === "fail") {
      setPaymentResult({
        status: "fail",
        message: params.get("message") ?? "결제가 취소되었거나 실패했습니다.",
        retryOrder: storedRetryOrder,
        showCancelNotice: true,
      });
      setPage("paymentResult");
      cleanPaymentUrl();
      return;
    }

    if (!paymentKey || !legacyOrderNumber || !amount || !storedOrderId) {
      setPaymentResult({
        status: "fail",
        message: "결제 승인에 필요한 정보가 없습니다. 주문 내역을 확인해주세요.",
        showCancelNotice: false,
      });
      setPage("paymentResult");
      cleanPaymentUrl();
      return;
    }

    const confirmedPaymentKey = paymentKey;
    const confirmedAmount = Number(amount);
    const confirmedOrderId = Number(storedOrderId);
    const confirmedLegacyOrderNumber = legacyOrderNumber;

    setPaymentResult({ status: "processing", message: "결제 승인 중입니다." });
    setPage("paymentResult");
    cleanPaymentUrl();

    // 외부 결제 승인
    async function approvePayment() {
      try {
        const payment = await confirmPayment(
          {
            orderId: confirmedOrderId,
            paymentId: confirmedPaymentKey,
            paymentKey: confirmedPaymentKey,
            amount: confirmedAmount,
          },
          getOrCreatePaymentConfirmKey(confirmedLegacyOrderNumber)
        );

        await clearPaidCartItems(confirmedLegacyOrderNumber);
        sessionStorage.removeItem(`earthyPaymentOrder:${confirmedLegacyOrderNumber}`);
        sessionStorage.removeItem(`earthyPaymentOrderData:${confirmedLegacyOrderNumber}`);
        sessionStorage.removeItem(`earthyPaymentConfirmKey:${confirmedLegacyOrderNumber}`);
        await loadMyPage();
        setPaymentResult({
          status: "success",
          message: "결제가 완료되었습니다.",
          payment,
        });
      } catch (paymentError) {
        let retryOrder: OrderResponse | undefined;

        try {
          retryOrder = await getMyOrder(confirmedOrderId);
        } catch {
          retryOrder = undefined;
        }

        setPaymentResult({
          status: "fail",
          message: paymentError instanceof Error ? paymentError.message : "결제 승인 실패",
          retryOrder,
          showCancelNotice: false,
        });
      }
    }

    void approvePayment();
  }, []);

  // 로그인 상태 변경 시 장바구니 동기화
  useEffect(() => {
    if (!accessToken) {
      setCartItems([]);
      return;
    }

    void loadCart();
  }, [accessToken]);

  // 마이페이지 진입 시 회원/주문 정보 조회
  useEffect(() => {
    if (page === "mypage") {
      void loadMyPage();
    }
  }, [page, accessToken, orderPage]);

  // 홈 이동
  const goHome = () => {
    navigateCustomerRoute({ page: "home" });
  };

  // 카테고리 이동
  const openCategory = (nextCategory: ProductCategory) => {
    navigateCustomerRoute({ page: "shop", category: nextCategory, productPage: 0, productSort });
  };

  // 상품 상세 이동
  const openDetail = (productId: number) => {
    navigateCustomerRoute({ page: "detail", productId });
  };

  // 공지사항 화면 이동
  const openNotice = () => {
    setNoticePageKey((key) => key + 1);
    navigateCustomerRoute({ page: "notice" });
  };

  // 게시판 화면 이동
  const openBoard = () => {
    setBoardPageKey((key) => key + 1);
    navigateCustomerRoute({ page: "board" });
  };

  // 상품 검색 화면 이동
  const openSearch = () => {
    navigateCustomerRoute({ page: "search" });
  };

  // 상품 목록 페이지 이동
  const changeProductPage = (nextProductPage: number) => {
    navigateCustomerRoute({ page: "shop", category, productPage: nextProductPage, productSort });
  };

  // 상품 목록 정렬 변경
  const changeProductSort = (nextProductSort: ProductSort) => {
    navigateCustomerRoute({ page: "shop", category, productPage: 0, productSort: nextProductSort });
  };

  // 로그인 필요 화면 진입 검증
  const requireLogin = () => {
    if (accessToken) {
      return true;
    }

    setAuthPageKey((key) => key + 1);
    navigateCustomerRoute({ page: "auth" });
    return false;
  };

  // 장바구니 담기
  const addToCart = async (requestBody: CartItemAddRequest) => {
    if (!requireLogin()) {
      return;
    }

    const cart = await addCartItem(requestBody, createIdempotencyKey());
    setCartItems(cart.items);
    setCartNoticeOpen(true);
    setCartBumped(true);
    window.setTimeout(() => setCartBumped(false), 260);
  };

  // 바로 구매
  const buyNow = async (requestBody: CartItemAddRequest) => {
    if (!requireLogin()) {
      return;
    }

    const cart = await addCartItem(requestBody, createIdempotencyKey());
    setCartItems(cart.items);

    if (accessToken) {
      setCheckoutCartItemIds(undefined);
      navigateCustomerRoute({ page: "checkout" });
    }
  };

  // 장바구니 수량 변경
  const updateCartQuantity = async (cartItemId: number, quantity: number) => {
    const cart = await updateCartItemQuantity(cartItemId, { quantity, addonQuantity: null });
    setCartItems(cart.items);
  };

  // 장바구니 상품 삭제
  const removeCartItem = async (cartItemId: number) => {
    const cart = await deleteCartItem(cartItemId);
    setCartItems(cart.items);
  };

  // 주문 생성
  const submitOrder = async (requestBody: OrderCreateRequest, idempotencyKey: string) => {
    const order = await createOrder({
      ...requestBody,
      cartItemIds: checkoutCartItemIds,
    }, idempotencyKey);

    const orderedCartItemIds = checkoutCartItemIds ?? cartItems.map((item) => item.cartItemId);
    sessionStorage.setItem(`earthyPaymentOrder:${order.orderNumber}`, String(order.orderId));
    sessionStorage.setItem(`earthyPaymentOrderData:${order.orderNumber}`, JSON.stringify(order));
    storePaymentCartItemIds(order.orderNumber, orderedCartItemIds);
    storePaymentConfirmKey(order.orderNumber, createIdempotencyKey());

    return order;
  };

  // 결제 재시도
  const retryPayment = async (order: OrderResponse) => {
    setPaymentResult({ status: "processing", message: "결제창을 다시 여는 중입니다." });
    setPage("paymentResult");

    try {
      sessionStorage.setItem(`earthyPaymentOrder:${order.orderNumber}`, String(order.orderId));
      sessionStorage.setItem(`earthyPaymentOrderData:${order.orderNumber}`, JSON.stringify(order));
      storePaymentConfirmKey(order.orderNumber, createIdempotencyKey());
      await requestAndConfirmPortOnePayment(order, "CARD", member?.email ?? "");
    } catch (retryError) {
      setPaymentResult({
        status: "fail",
        message: retryError instanceof Error ? retryError.message : "결제 재요청 실패",
        retryOrder: order,
        showCancelNotice: false,
      });
    }
  };

  // 계정 아이콘 이동
  const openAccount = () => {
    if (accessToken) {
      setMyPageInitialView("home");
      setMyPageKey((key) => key + 1);
      navigateCustomerRoute({ page: "mypage" });
      return;
    }

    setAuthPageKey((key) => key + 1);
    navigateCustomerRoute({ page: "auth" });
  };

  // 장바구니 화면 이동
  const openCart = () => {
    setCartNoticeOpen(false);
    navigateCustomerRoute({ page: "cart" });
  };

  // 회원 정보 수정
  const saveMyInfo = async (
    name: string,
    phone: string,
    zipCode: string,
    address: string,
    detailAddress: string
  ) => {
    const updatedMember = await updateMyInfo({ name, phone, zipCode, address, detailAddress });
    setMember(updatedMember);
  };

  // 회원 비밀번호 변경
  const saveMyPassword = async (currentPassword: string, newPassword: string) => {
    await updateMyPassword({ currentPassword, newPassword });
  };

  // 주문 취소
  const cancelOrder = async (orderId: number, cancelReason: string) => {
    const updatedOrder = await cancelMyOrder(orderId, cancelReason, createIdempotencyKey());
    setOrders((prevOrders) =>
      prevOrders.map((order) => (order.orderId === orderId ? updatedOrder : order))
    );
    return updatedOrder;
  };

  // 회원 탈퇴
  const deactivateAccount = async () => {
    await deactivateMyAccount();
    localStorage.removeItem("earthyAccessToken");
    localStorage.removeItem("earthyRefreshToken");
    setAccessToken(null);
    setMember(null);
    setOrders([]);
    setCartItems([]);
    navigateCustomerRoute({ page: "home" });
  };

  // 로그아웃
  const logout = async () => {
    const refreshToken = localStorage.getItem("earthyRefreshToken");

    try {
      if (refreshToken) {
        await requestLogout(refreshToken);
      }
    } finally {
      localStorage.removeItem("earthyAccessToken");
      localStorage.removeItem("earthyRefreshToken");
      setAccessToken(null);
      setCartItems([]);
      navigateCustomerRoute({ page: "home" });
    }
  };

  return (
    <>
      <Header
        page={page}
        category={category}
        cartCount={cartCount}
        cartBumped={cartBumped}
        loggedIn={Boolean(accessToken)}
        onHome={goHome}
        onCategory={openCategory}
        onAbout={() => navigateCustomerRoute({ page: "about" })}
        onNotice={openNotice}
        onBoard={openBoard}
        onSearch={openSearch}
        onAuth={openAccount}
        onCart={openCart}
      />

      <main>
        {page === "home" && <Home onHome={goHome} />}
        {page === "shop" && (
          <Shop
            category={category}
            products={products}
            pageInfo={productPageInfo}
            sort={productSort}
            loading={productLoading}
            loaded={productLoaded}
            error={productError}
            onOpenDetail={openDetail}
            onChangePage={changeProductPage}
            onChangeSort={changeProductSort}
          />
        )}
        {page === "search" && <SearchPage onOpenDetail={openDetail} />}
        {page === "detail" && (
          <ProductDetail
            product={productDetail}
            loading={productDetailLoading}
            error={productDetailError}
            onAddToCart={addToCart}
            onBuyNow={buyNow}
          />
        )}
        {page === "about" && <About />}
        {page === "notice" && <NoticePage key={noticePageKey} />}
        {page === "board" && (
          <BoardPage
            key={boardPageKey}
            loggedIn={Boolean(accessToken)}
            onRequireLogin={requireLogin}
          />
        )}
        {page === "cart" && (
          <Cart
            items={cartItems}
            totalPrice={cartTotal}
            cartCount={cartCount}
            loading={cartLoading}
            error={cartError}
            onOpenDetail={openDetail}
            onUpdateQuantity={updateCartQuantity}
            onRemove={removeCartItem}
            onCheckout={(cartItemIds) => {
              setCheckoutCartItemIds(cartItemIds);
              navigateCustomerRoute({ page: "checkout" });
            }}
          />
        )}
        {page === "checkout" && (
          <Checkout
            totalPrice={checkoutTotal}
            member={member}
            onMemberLoaded={setMember}
            onCreateOrder={submitOrder}
            onRequestPayment={requestAndConfirmPortOnePayment}
          />
        )}
        {page === "paymentResult" && paymentResult && (
          <PaymentResult
            result={paymentResult}
            onRetryPayment={retryPayment}
            onMoveOrders={() => {
              setMyPageInitialView("orders");
              setMyPageKey((key) => key + 1);
              navigateCustomerRoute({ page: "mypage" });
            }}
            onMoveCart={() => {
              navigateCustomerRoute({ page: "cart" });
            }}
          />
        )}
        {page === "mypage" && (
          <MyPage
            key={myPageKey}
            member={member}
            initialView={myPageInitialView}
            orders={orders}
            orderPageInfo={orderPageInfo}
            loading={myPageLoading}
            error={myPageError}
            onChangeOrderPage={setOrderPage}
            onUpdateInfo={saveMyInfo}
            onUpdatePassword={saveMyPassword}
            onCancelOrder={cancelOrder}
            onDeactivate={deactivateAccount}
            onLogout={logout}
          />
        )}
        {page === "auth" && (
          <AuthPage
            key={authPageKey}
            onLoginSuccess={(loginResponse) => {
              localStorage.setItem("earthyAccessToken", loginResponse.accessToken);
              localStorage.setItem("earthyRefreshToken", loginResponse.refreshToken);
              setAccessToken(loginResponse.accessToken);
              navigateCustomerRoute({ page: "home" });
            }}
          />
        )}
      </main>

      {cartNoticeOpen && (
        <CartNotice
          onClose={() => setCartNoticeOpen(false)}
          onOpenCart={openCart}
        />
      )}

      <BusinessFooter />
    </>
  );
}

function Header({
  page,
  category,
  cartCount,
  cartBumped,
  loggedIn,
  onHome,
  onCategory,
  onAbout,
  onNotice,
  onBoard,
  onSearch,
  onAuth,
  onCart,
}: HeaderProps) {
  const [menuOpen, setMenuOpen] = useState(false);

  const handleHome = () => {
    onHome();
    setMenuOpen(false);
  };

  const handleCategory = (nextCategory: ProductCategory) => {
    onCategory(nextCategory);
    setMenuOpen(false);
  };

  const handleAbout = () => {
    onAbout();
    setMenuOpen(false);
  };

  const handleNotice = () => {
    onNotice();
    setMenuOpen(false);
  };

  const handleBoard = () => {
    onBoard();
    setMenuOpen(false);
  };

  const handleSearch = () => {
    onSearch();
    setMenuOpen(false);
  };

  const handleAuth = () => {
    onAuth();
    setMenuOpen(false);
  };

  const handleCart = () => {
    onCart();
    setMenuOpen(false);
  };

  return (
    <header className="site-header">
      <button
        className="menu-button"
        type="button"
        aria-label="카테고리 메뉴"
        aria-expanded={menuOpen}
        onClick={() => setMenuOpen((open) => !open)}
      >
        <span />
        <span />
        <span />
      </button>
      {menuOpen && (
        <button
          className="mobile-menu-backdrop"
          type="button"
          aria-label="메뉴 닫기"
          onClick={() => setMenuOpen(false)}
        />
      )}

      <nav className={`main-nav ${menuOpen ? "is-open" : ""}`} aria-label="주요 메뉴">
        <button className="mobile-menu-close" type="button" aria-label="메뉴 닫기" onClick={() => setMenuOpen(false)}>
          ×
        </button>
        <div className="nav-row">
          <button className={page === "home" ? "is-active" : ""} type="button" onClick={handleHome}>
            HOME
          </button>
          {categoryTabs
            .filter((tab) => tab.value === "ALL")
            .map((tab) => (
              <button
                key={tab.value}
                className={page === "shop" && category === tab.value ? "is-active" : ""}
                type="button"
                onClick={() => handleCategory(tab.value)}
              >
                {tab.label}
              </button>
            ))}
          <button className={page === "about" ? "is-active" : ""} type="button" onClick={handleAbout}>
            ABOUT
          </button>
          {categoryTabs
            .filter((tab) => tab.value === "POSTCARD" || tab.value === "POSTER")
            .map((tab) => (
              <button
                key={tab.value}
                className={page === "shop" && category === tab.value ? "is-active" : ""}
                type="button"
                onClick={() => handleCategory(tab.value)}
              >
                {tab.label}
              </button>
            ))}
        </div>

        <div className="nav-row">
          {categoryTabs
            .filter((tab) => tab.value === "ETC")
            .map((tab) => (
            <button
              key={tab.value}
              className={page === "shop" && category === tab.value ? "is-active" : ""}
              type="button"
              onClick={() => handleCategory(tab.value)}
            >
              {tab.label}
            </button>
          ))}
          <button className={page === "notice" ? "is-active" : ""} type="button" onClick={handleNotice}>
            NOTICE
          </button>
          <button className={page === "board" ? "is-active" : ""} type="button" onClick={handleBoard}>
            Q&A
          </button>
        </div>
      </nav>

      <button className="brand-button" type="button" onClick={handleHome} aria-label="EARTHY home">
        <img src="/assets/earthy-logo-transparent.png" alt="EARTHY" />
      </button>

      <div className="header-actions">
        <button className={`icon-button ${page === "search" ? "is-active" : ""}`} type="button" aria-label="검색" onClick={handleSearch}>
          <svg viewBox="0 0 24 24" aria-hidden="true">
            <circle cx="11" cy="11" r="7" />
            <path d="m16.5 16.5 4 4" />
          </svg>
        </button>
        <button
          className={`icon-button account-button ${page === "auth" || page === "mypage" || loggedIn ? "is-active" : ""} ${
            page === "mypage" ? "is-mypage-active" : ""
          }`}
          type="button"
          aria-label={loggedIn ? "마이페이지" : "로그인"}
          onClick={handleAuth}
        >
          <svg viewBox="0 0 24 24" aria-hidden="true">
            <circle cx="12" cy="8" r="4" />
            <path d="M4 21c1.5-4 4.2-6 8-6s6.5 2 8 6" />
          </svg>
        </button>
        <button
          className={`icon-button cart-button ${cartBumped ? "is-bumped" : ""}`}
          type="button"
          onClick={handleCart}
          aria-label="장바구니"
        >
          <svg viewBox="0 0 24 24" aria-hidden="true">
            <path d="M6 6h15l-2 8H8L6 3H3" />
            <circle cx="9" cy="20" r="1.5" />
            <circle cx="18" cy="20" r="1.5" />
          </svg>
          {cartCount > 0 && <em>{cartCount}</em>}
        </button>
      </div>
    </header>
  );
}

function CartNotice({
  onClose,
  onOpenCart,
}: {
  onClose: () => void;
  onOpenCart: () => void;
}) {
  return (
    <div className="cart-notice-backdrop" role="presentation">
      <section className="cart-notice" role="dialog" aria-modal="true" onClick={(event) => event.stopPropagation()}>
        <p>장바구니에 담았습니다.</p>
        <div>
          <button type="button" onClick={onOpenCart}>
            장바구니 보기
          </button>
          <button type="button" onClick={onClose}>
            계속 쇼핑하기
          </button>
        </div>
      </section>
    </div>
  );
}

function Home({ onHome }: { onHome: () => void }) {
  return (
    <section className="home-view">
      <button className="home-photo" type="button" onClick={onHome} aria-label="홈으로 이동">
        <img {...protectedImageProps()} src="/assets/field-postcard.jpeg" alt="풀 언덕 풍경 엽서" />
      </button>
    </section>
  );
}

function NoticePage() {
  const [keyword, setKeyword] = useState("");
  const [appliedKeyword, setAppliedKeyword] = useState("");
  const [notices, setNotices] = useState<NoticeResponse[]>([]);
  const [pageInfo, setPageInfo] = useState<ProductPageResponse<NoticeResponse>>(() => createEmptyPage<NoticeResponse>());
  const [currentPage, setCurrentPage] = useState(0);
  const [selectedNotice, setSelectedNotice] = useState<NoticeResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  async function loadNotices(page = currentPage) {
    setLoading(true);
    setError("");

    try {
      const data = await getNoticesPage(appliedKeyword, page);
      setNotices(data.content);
      setPageInfo(data);
      setCurrentPage(data.page);
    } catch (loadError) {
      setError(loadError instanceof Error ? loadError.message : "공지사항을 불러오지 못했습니다.");
    } finally {
      setLoading(false);
    }
  }

  async function openNoticeDetail(noticeId: number) {
    setError("");

    try {
      setSelectedNotice(await getNotice(noticeId));
      window.scrollTo({ top: 0, behavior: "smooth" });
    } catch (loadError) {
      setError(loadError instanceof Error ? loadError.message : "공지사항 상세를 불러오지 못했습니다.");
    }
  }

  useEffect(() => {
    void loadNotices();
  }, [appliedKeyword, currentPage]);

  if (selectedNotice) {
    return (
      <section className="page-view board-view">
        <div className="subpage-title-row board-list-title-row">
          <p>NOTICE</p>
        </div>
        <article className="community-detail">
          <header>
            <h2>{selectedNotice.title}</h2>
            <time>{formatDate(selectedNotice.createdAt)}</time>
          </header>
          <p>{selectedNotice.content}</p>
        </article>
      </section>
    );
  }

  return (
    <section className="page-view board-view">
      <div className="subpage-title-row board-list-title-row">
        <p>NOTICE</p>
      </div>
      {error && <p className="form-error">{error}</p>}
      <div className="qa-board-list notice-board-list">
        <div className="qa-board-header notice-board-header" aria-hidden="true">
          <span>NO</span>
          <span>TITLE</span>
          <span>DATE</span>
        </div>
        {loading ? (
          <p className="empty-message">공지사항을 불러오는 중입니다.</p>
        ) : notices.length === 0 ? (
          <p className="empty-message">해당하는 공지사항이 없습니다.</p>
        ) : (
          notices.map((notice, index) => (
            <div className="qa-board-row notice-board-row" key={notice.id}>
              <span>{pageInfo.page * pageInfo.size + index + 1}</span>
              <strong>
                <button className="qa-title-button" type="button" onClick={() => void openNoticeDetail(notice.id)}>
                  <span>{notice.title}</span>
                </button>
              </strong>
              <time>{formatDateHyphen(notice.createdAt)}</time>
            </div>
          ))
        )}
      </div>
      <Pagination pageInfo={pageInfo} onChangePage={setCurrentPage} />
      <form
        className="qa-board-search"
        onSubmit={(event) => {
          event.preventDefault();
          setCurrentPage(0);
          setAppliedKeyword(keyword);
        }}
      >
        <select aria-label="검색 조건" defaultValue="all">
          <option value="all">전체</option>
          <option value="title">제목</option>
          <option value="content">내용</option>
        </select>
        <input
          value={keyword}
          onChange={(event) => setKeyword(event.target.value)}
          onKeyDown={(event) => {
            if (event.key === "Enter" && !event.nativeEvent.isComposing) {
              event.preventDefault();
              setCurrentPage(0);
              setAppliedKeyword(keyword);
            }
          }}
          aria-label="검색어"
        />
        <button type="submit">SEARCH</button>
      </form>
    </section>
  );
}

function BoardPage({
  loggedIn,
  onRequireLogin,
}: {
  loggedIn: boolean;
  onRequireLogin: () => boolean;
}) {
  const [keyword, setKeyword] = useState("");
  const [appliedKeyword, setAppliedKeyword] = useState("");
  const [boards, setBoards] = useState<BoardListResponse[]>([]);
  const [pageInfo, setPageInfo] = useState<ProductPageResponse<BoardListResponse>>(() => createEmptyPage<BoardListResponse>());
  const [currentPage, setCurrentPage] = useState(0);
  const [selectedBoard, setSelectedBoard] = useState<BoardResponse | null>(null);
  const [privateTarget, setPrivateTarget] = useState<BoardListResponse | null>(null);
  const [privatePassword, setPrivatePassword] = useState("");
  const [privateError, setPrivateError] = useState("");
  const [mode, setMode] = useState<"list" | "detail" | "form">("list");
  const [editingBoard, setEditingBoard] = useState<BoardResponse | null>(null);
  const [form, setForm] = useState<BoardSaveRequest>({
    type: "PRODUCT",
    title: "문의합니다.",
    content: BOARD_DEFAULT_CONTENT,
    visibility: "PUBLIC",
    postPassword: "",
  });
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [notice, setNotice] = useState("");

  const canEditSelectedBoard = Boolean(selectedBoard?.mine && !selectedBoard.answer);

  async function loadBoards(page = currentPage) {
    setLoading(true);
    setError("");

    try {
      const data = await getBoardsPage(appliedKeyword, page);
      setBoards(data.content);
      setPageInfo(data);
      setCurrentPage(data.page);
    } catch (loadError) {
      setError(loadError instanceof Error ? loadError.message : "게시글을 불러오지 못했습니다.");
    } finally {
      setLoading(false);
    }
  }

  async function openBoardDetail(board: BoardListResponse) {
    setError("");
    setNotice("");

    if (board.visibility === "PRIVATE") {
      setPrivateTarget(board);
      setPrivatePassword("");
      setPrivateError("");
      return;
    }

    try {
      if (loggedIn) {
        await refreshAuthIfPossible();
      }

      setSelectedBoard(await getPublicBoard(board.id));
      setMode("detail");
      window.scrollTo({ top: 0, behavior: "smooth" });
    } catch (loadError) {
      setError(loadError instanceof Error ? loadError.message : "게시글 상세를 불러오지 못했습니다.");
    }
  }

  async function submitPrivatePassword(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();

    if (!privateTarget) {
      return;
    }

    setPrivateError("");

    if (!privatePassword.trim()) {
      setPrivateError("비밀번호를 입력해 주세요.");
      return;
    }

    try {
      if (loggedIn) {
        await refreshAuthIfPossible();
      }

      setSelectedBoard(await getPrivateBoard(privateTarget.id, privatePassword));
      setPrivateTarget(null);
      setMode("detail");
      window.scrollTo({ top: 0, behavior: "smooth" });
    } catch (loadError) {
      setPrivateError(loadError instanceof Error ? loadError.message : "비밀번호를 확인해주세요.");
    }
  }

  function openCreateForm() {
    if (!onRequireLogin()) {
      return;
    }

    setEditingBoard(null);
    setForm({ type: "PRODUCT", title: "문의합니다.", content: BOARD_DEFAULT_CONTENT, visibility: "PUBLIC", postPassword: "" });
    setMode("form");
    setNotice("");
    setError("");
    window.scrollTo({ top: 0, behavior: "smooth" });
  }

  function openEditForm() {
    if (!selectedBoard) {
      return;
    }

    setEditingBoard(selectedBoard);
    setForm({
      type: selectedBoard.type,
      title: selectedBoard.title,
      content: selectedBoard.content,
      visibility: selectedBoard.visibility,
      postPassword: selectedBoard.visibility === "PRIVATE" ? BOARD_PASSWORD_MASK : "",
    });
    setMode("form");
    setNotice("");
    setError("");
  }

  async function submitBoardForm(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setError("");
    setNotice("");

    try {
      const postPassword =
        form.visibility === "PRIVATE" && form.postPassword !== BOARD_PASSWORD_MASK
          ? form.postPassword
          : undefined;
      const requestBody = {
        ...form,
        postPassword,
      };

      const savedBoard = editingBoard
        ? await updateBoard(editingBoard.id, requestBody)
        : await createBoard(requestBody);

      setEditingBoard(null);
      await loadBoards(currentPage);

      if (editingBoard) {
        setSelectedBoard(null);
        setMode("list");
        setNotice("");
        return;
      }

      setSelectedBoard(savedBoard);
      setMode("detail");
    } catch (submitError) {
      setError(submitError instanceof Error ? submitError.message : "게시글 저장에 실패했습니다.");
    }
  }

  useEffect(() => {
    void loadBoards();
  }, [appliedKeyword, currentPage]);

  if (mode === "form") {
    return (
      <section className="page-view board-view">
        <div className="subpage-title-row board-list-title-row">
          <p>Q&A</p>
        </div>
        <form className="community-form" onSubmit={submitBoardForm}>
          <label>
            문의종류
            <select value={form.type} onChange={(event) => setForm((prev) => ({ ...prev, type: event.target.value as BoardType }))}>
              {BOARD_TYPES.map((type) => (
                <option key={type.value} value={type.value}>
                  {type.label}
                </option>
              ))}
            </select>
          </label>
          <label>
            제목
            <input value={form.title} onChange={(event) => setForm((prev) => ({ ...prev, title: event.target.value }))} required />
          </label>
          <label>
            내용
            <textarea value={form.content} onChange={(event) => setForm((prev) => ({ ...prev, content: event.target.value }))} required rows={8} />
          </label>
          <div className="board-visibility-options">
            <label>
              <input
                type="radio"
                name="boardVisibility"
                value="PUBLIC"
                checked={form.visibility === "PUBLIC"}
                onChange={() => setForm((prev) => ({ ...prev, visibility: "PUBLIC" }))}
              />
              공개글
            </label>
            <label>
              <input
                type="radio"
                name="boardVisibility"
                value="PRIVATE"
                checked={form.visibility === "PRIVATE"}
                onChange={() => setForm((prev) => ({ ...prev, visibility: "PRIVATE" }))}
              />
              비밀글
            </label>
          </div>
          {form.visibility === "PRIVATE" && (
            <label>
              비밀번호
              <input
                type="password"
                minLength={4}
                maxLength={20}
                value={form.postPassword ?? ""}
                onFocus={() => {
                  if (form.postPassword === BOARD_PASSWORD_MASK) {
                    setForm((prev) => ({ ...prev, postPassword: "" }));
                  }
                }}
                onChange={(event) => setForm((prev) => ({ ...prev, postPassword: event.target.value }))}
                placeholder="4자 이상 20자 이하"
                required={!editingBoard || editingBoard.visibility === "PUBLIC"}
              />
            </label>
          )}
          {error && <p className="form-error">{error}</p>}
          <div className="community-form-actions">
            <button className="is-primary" type="submit">{editingBoard ? "수정" : "등록"}</button>
            <button type="button" onClick={() => setMode(editingBoard ? "detail" : "list")}>
              취소
            </button>
          </div>
        </form>
      </section>
    );
  }

  if (mode === "detail" && selectedBoard) {
    return (
      <section className="page-view board-view">
        <div className="subpage-title-row board-list-title-row">
          <p>Q&A</p>
        </div>
        {notice && <p className="form-message">{notice}</p>}
        <article className="community-detail">
          <header>
            <p>{selectedBoard.typeDescription}</p>
            <h2>{selectedBoard.title}</h2>
            <time>{selectedBoard.writerName} / {formatDate(selectedBoard.createdAt)}</time>
          </header>
          <p>{selectedBoard.content}</p>
          {selectedBoard.answer && (
            <div className="board-answer">
              <strong>EARTHY 답변</strong>
              <time>{selectedBoard.answeredAt ? formatDate(selectedBoard.answeredAt) : ""}</time>
              <p>{selectedBoard.answer}</p>
            </div>
          )}
          {loggedIn && canEditSelectedBoard && (
            <div className="community-actions">
              <button type="button" onClick={openEditForm}>
                수정하기
              </button>
            </div>
          )}
        </article>
      </section>
    );
  }

  let nextQaDisplayNo = pageInfo.page * pageInfo.size + 1;
  const qaRows = boards.flatMap((board) => {
    const rows = [
      {
        board,
        displayNo: nextQaDisplayNo,
        answer: false,
      },
    ];
    nextQaDisplayNo += 1;

    if (board.answeredAt) {
      rows.push({
        board,
        displayNo: nextQaDisplayNo,
        answer: true,
      });
      nextQaDisplayNo += 1;
    }

    return rows;
  });

  return (
    <section className="page-view board-view">
      <div className="subpage-title-row board-list-title-row">
        <p>Q&A</p>
      </div>
      {error && <p className="form-error">{error}</p>}
      <div className="qa-board-list">
        <div className="qa-board-header" aria-hidden="true">
          <span>NO</span>
          <span>CATE</span>
          <span>TITLE</span>
          <span>NAME</span>
          <span>DATE</span>
        </div>
        {loading ? (
          <p className="empty-message">게시글을 불러오는 중입니다.</p>
        ) : boards.length === 0 ? (
          <p className="empty-message">해당하는 문의가 없습니다.</p>
        ) : (
          qaRows.map(({ board, displayNo, answer }) => (
            <Fragment key={`${board.id}-${answer ? "answer" : "question"}`}>
              <div className={`qa-board-row${answer ? " qa-answer-row" : ""}`}>
                <span>{displayNo}</span>
                <span>{answer ? "" : board.typeDescription}</span>
                <strong>
                  {answer && <em>↳ RE</em>}
                  <button className="qa-title-button" type="button" onClick={() => void openBoardDetail(board)}>
                    <span>{board.title}</span>
                  </button>
                  {board.visibility === "PRIVATE" && <i className="qa-lock-icon" aria-label="비공개" />}
                  {((!answer && isCreatedToday(board.createdAt)) ||
                    (answer && board.answeredAt && isCreatedToday(board.answeredAt))) && (
                    <b className="qa-new-icon" aria-label="새 글">N</b>
                  )}
                </strong>
                <span>{answer ? "" : board.writerName}</span>
                <time>{formatDateHyphen(answer && board.answeredAt ? board.answeredAt : board.createdAt)}</time>
              </div>
            </Fragment>
          ))
        )}
      </div>
      <Pagination pageInfo={pageInfo} onChangePage={setCurrentPage} />
      <div className="community-toolbar qa-board-toolbar">
        {loggedIn && (
          <button type="button" onClick={openCreateForm}>
            WRITE
          </button>
        )}
      </div>
      <form
        className="qa-board-search"
        onSubmit={(event) => {
          event.preventDefault();
          setCurrentPage(0);
          setAppliedKeyword(keyword);
        }}
      >
        <select aria-label="검색 조건" defaultValue="all">
          <option value="all">전체</option>
          <option value="title">제목</option>
          <option value="content">내용</option>
          <option value="writer">작성자</option>
        </select>
        <input
          value={keyword}
          onChange={(event) => setKeyword(event.target.value)}
          onKeyDown={(event) => {
            if (event.key === "Enter" && !event.nativeEvent.isComposing) {
              event.preventDefault();
              setCurrentPage(0);
              setAppliedKeyword(keyword);
            }
          }}
          aria-label="검색어"
        />
        <button type="submit">SEARCH</button>
      </form>

      {privateTarget && (
        <div className="cart-notice-backdrop" role="presentation" onClick={() => setPrivateTarget(null)}>
          <form className="cart-notice private-board-modal" onSubmit={submitPrivatePassword} onClick={(event) => event.stopPropagation()}>
            <p>비공개 게시글입니다.</p>
            <input
              type="password"
              value={privatePassword}
              onChange={(event) => setPrivatePassword(event.target.value)}
              placeholder="게시글 비밀번호"
              minLength={4}
              autoFocus
            />
            {privateError && <span className="private-password-help">{privateError}</span>}
            <div>
              <button type="button" onClick={() => setPrivateTarget(null)}>
                닫기
              </button>
              <button type="submit">확인</button>
            </div>
          </form>
        </div>
      )}
    </section>
  );
}

function Shop({
  category,
  products,
  pageInfo,
  sort,
  loading,
  loaded,
  error,
  onOpenDetail,
  onChangePage,
  onChangeSort,
}: ShopProps) {
  const title = categoryTabs.find((tab) => tab.value === category)?.label ?? "ALL";

  return (
    <section className="page-view shop-view">
      <div className="page-title">
        <span>SHOP</span>
        <h1>{title}</h1>
      </div>

      <div className="product-list-toolbar">
        <select
          value={sort}
          onChange={(event) => onChangeSort(event.target.value as ProductSort)}
          aria-label="상품 정렬"
        >
          {productSortOptions.map((option) => (
            <option key={option.value} value={option.value}>
              {option.label}
            </option>
          ))}
        </select>
      </div>

      {error && <p className="state-text">상품 목록을 불러오지 못했습니다.</p>}
      {!loading && loaded && !error && products.length === 0 && (
        <p className="state-text">상품을 준비중입니다.</p>
      )}

      {!loading && !error && products.length > 0 && (
        <div className="product-grid">
          {products.map((product) => (
            <button
              className={`product-item ${product.category === "POSTER" ? "is-poster" : ""} ${product.category === "POSTCARD" ? "is-postcard" : ""} ${product.soldOut ? "is-sold-out" : ""}`}
              type="button"
              key={product.id}
              onClick={() => onOpenDetail(product.id)}
            >
              <img {...protectedImageProps()} src={product.imageUrl} alt={product.name} />
              <strong>{product.name}</strong>
              <small>{product.soldOut ? "SOLD OUT" : formatWon(product.price)}</small>
            </button>
          ))}
        </div>
      )}
      <Pagination pageInfo={pageInfo} onChangePage={onChangePage} />
    </section>
  );
}

function SearchPage({ onOpenDetail }: SearchPageProps) {
  const [keyword, setKeyword] = useState("");
  const [appliedKeyword, setAppliedKeyword] = useState("");
  const [products, setProducts] = useState<Product[]>([]);
  const [pageInfo, setPageInfo] = useState<ProductPageResponse<Product>>(() => createEmptyPage<Product>());
  const [page, setPage] = useState(0);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let ignore = false;
    const trimmedKeyword = appliedKeyword.trim();

    if (!trimmedKeyword) {
      setProducts([]);
      setPageInfo(createEmptyPage<Product>());
      setError(null);
      return;
    }

    async function fetchSearchProducts() {
      setLoading(true);
      setError(null);

      try {
        const data = await searchProductsPage(trimmedKeyword, page);

        if (!ignore) {
          setProducts(data.content);
          setPageInfo(data);
        }
      } catch (searchError) {
        if (!ignore) {
          setProducts([]);
          setPageInfo(createEmptyPage<Product>());
          setError(searchError instanceof Error ? searchError.message : "상품 검색 실패");
        }
      } finally {
        if (!ignore) {
          setLoading(false);
        }
      }
    }

    void fetchSearchProducts();

    return () => {
      ignore = true;
    };
  }, [appliedKeyword, page]);

  const submitSearch = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setPage(0);
    setAppliedKeyword(keyword.trim());
  };

  const hasSearched = appliedKeyword.trim().length > 0;

  return (
    <section className="page-view shop-view search-view">
      <div className="page-title">
        <span>SHOP</span>
        <h1>SEARCH</h1>
      </div>

      <form className="product-search-form" onSubmit={submitSearch}>
        <input
          type="search"
          value={keyword}
          onChange={(event) => setKeyword(event.target.value)}
          placeholder="상품명을 입력하세요"
          aria-label="상품명 검색"
        />
        <button type="submit">SEARCH</button>
      </form>

      {loading && <p className="state-text">상품을 검색하는 중입니다.</p>}
      {error && <p className="state-text">상품 검색 결과를 불러오지 못했습니다.</p>}
      {!loading && !error && hasSearched && products.length === 0 && (
        <p className="state-text">검색된 상품이 없습니다.</p>
      )}

      {products.length > 0 && (
        <>
          <div className="product-grid">
            {products.map((product) => (
              <button
                className={`product-item ${product.category === "POSTER" ? "is-poster" : ""} ${product.category === "POSTCARD" ? "is-postcard" : ""} ${product.soldOut ? "is-sold-out" : ""}`}
                type="button"
                key={product.id}
                onClick={() => onOpenDetail(product.id)}
              >
                <img {...protectedImageProps()} src={product.imageUrl} alt={product.name} />
                <strong>{product.name}</strong>
                <small>{product.soldOut ? "SOLD OUT" : formatWon(product.price)}</small>
              </button>
            ))}
          </div>
          <Pagination pageInfo={pageInfo} onChangePage={setPage} />
        </>
      )}
    </section>
  );
}

function Pagination<T>({
  pageInfo,
  onChangePage,
}: {
  pageInfo: ProductPageResponse<T>;
  onChangePage: (page: number) => void;
}) {
  if (pageInfo.totalPages <= 1) {
    return null;
  }

  const visiblePageCount = Math.min(pageInfo.totalPages, 5);
  const firstVisiblePage = Math.min(
    Math.max(pageInfo.page - Math.floor(visiblePageCount / 2), 0),
    pageInfo.totalPages - visiblePageCount
  );
  const pages = Array.from({ length: visiblePageCount }, (_, index) => firstVisiblePage + index);

  return (
    <nav className="pagination" aria-label="페이지 이동">
      <button
        className="pagination-arrow is-prev"
        type="button"
        aria-label="이전 페이지"
        disabled={pageInfo.first}
        onClick={() => onChangePage(pageInfo.page - 1)}
      >
        <span aria-hidden="true" />
      </button>
      {pages.map((page) => (
        <button
          className={`pagination-page${pageInfo.page === page ? " active" : ""}`}
          type="button"
          key={page}
          aria-current={pageInfo.page === page ? "page" : undefined}
          onClick={() => onChangePage(page)}
        >
          {page + 1}
        </button>
      ))}
      <button
        className="pagination-arrow is-next"
        type="button"
        aria-label="다음 페이지"
        disabled={pageInfo.last}
        onClick={() => onChangePage(pageInfo.page + 1)}
      >
        <span aria-hidden="true" />
      </button>
    </nav>
  );
}

function ProductDetail({
  product,
  loading,
  error,
  onAddToCart,
  onBuyNow,
}: ProductDetailProps) {
  const [quantity, setQuantity] = useState(1);
  const [sizeOptionId, setSizeOptionId] = useState("");
  const [sizeOptionsOpen, setSizeOptionsOpen] = useState(false);
  const [selectedAddons, setSelectedAddons] = useState<Record<number, number>>({});
  const [openAddonGroups, setOpenAddonGroups] = useState<Record<string, boolean>>({});
  const [submitError, setSubmitError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const submittingRef = useRef(false);

  useEffect(() => {
    setQuantity(1);
    setSizeOptionId("");
    setSizeOptionsOpen(false);
    setSelectedAddons({});
    setOpenAddonGroups({});
  }, [product?.id]);

  if (loading) {
    return (
      <section className="page-view detail-view">
        <p className="state-text">상품 상세 정보를 불러오는 중입니다.</p>
      </section>
    );
  }

  if (!product) {
    return (
      <section className="page-view detail-view">
        <p className="state-text">{error ?? "상품을 찾을 수 없습니다."}</p>
      </section>
    );
  }

  const productAddons: Addon[] = product.addons ?? [];
  const sizeOptions = product.sizeOptions ?? [];
  const selectedSizeOption = sizeOptions.find((option) => option.id === Number(sizeOptionId));
  const selectedProductAddons = productAddons.filter((addon) => selectedAddons[addon.id]);
  const groupedAddons = productAddons.reduce<Record<string, Addon[]>>((groups, addon) => {
    const groupName = addon.typeDescription || addon.type;
    groups[groupName] = [...(groups[groupName] ?? []), addon];
    return groups;
  }, {});
  const orderDisabled = submitting || product.soldOut;
  const unitPrice = product.category === "POSTER" && selectedSizeOption
    ? product.price + selectedSizeOption.additionalPrice
    : product.price;
  const addonTotal = selectedProductAddons.reduce(
    (total, addon) => total + addon.price * selectedAddons[addon.id],
    0
  );
  const productTotal = unitPrice * quantity;
  const totalPrice = productTotal + addonTotal;

  const createCartRequest = (): CartItemAddRequest => ({
      productId: product.id,
      productSizeOptionId: selectedSizeOption?.id ?? null,
      addonId: null,
      addonQuantity: null,
      addons: selectedProductAddons.map((addon) => ({
        addonId: addon.id,
        quantity: selectedAddons[addon.id],
      })),
      quantity,
  });

  const selectAddon = (addonId: number, groupName: string) => {
    setSelectedAddons((prevAddons) => {
      if (prevAddons[addonId]) {
        return prevAddons;
      }

      return {
        ...prevAddons,
        [addonId]: 1,
      };
    });
    setOpenAddonGroups((prevGroups) => ({
      ...prevGroups,
      [groupName]: false,
    }));
  };

  const removeSelectedAddon = (addonId: number) => {
    setSelectedAddons((prevAddons) => {
      const nextAddons = { ...prevAddons };
      delete nextAddons[addonId];
      return nextAddons;
    });
  };

  const toggleAddonGroup = (groupName: string) => {
    setOpenAddonGroups((prevGroups) => ({
      ...prevGroups,
      [groupName]: !prevGroups[groupName],
    }));
  };

  const updateSelectedAddonQuantity = (addonId: number, nextQuantity: number) => {
    setSelectedAddons((prevAddons) => ({
      ...prevAddons,
      [addonId]: Math.max(1, nextQuantity),
    }));
  };

  const handleCartAction = async (action: "cart" | "buy") => {
    if (submittingRef.current) {
      return;
    }

    submittingRef.current = true;
    setSubmitting(true);
    setSubmitError(null);

    if (product.soldOut) {
      submittingRef.current = false;
      setSubmitting(false);
      return;
    }

    if (product.category === "POSTER" && !selectedSizeOption) {
      setSubmitError("사이즈를 선택해주세요.");
      submittingRef.current = false;
      setSubmitting(false);
      return;
    }

    try {
      if (action === "cart") {
        await onAddToCart(createCartRequest());
        return;
      }

      await onBuyNow(createCartRequest());
    } catch (cartError) {
      setSubmitError(cartError instanceof Error ? cartError.message : "장바구니 처리 실패");
    } finally {
      submittingRef.current = false;
      setSubmitting(false);
    }
  };

  return (
    <section className="page-view detail-view">
      <div className="detail-layout">
        <img {...protectedImageProps("detail-image")} src={product.imageUrl} alt={product.name} />

        <article className="detail-panel">
          <h1>{product.name}</h1>
          <p>{formatWon(product.price)}</p>
          <small>{product.description}</small>

          <dl className="delivery-list">
            <div>
              <dt>배송비</dt>
              <dd>2,500원 (30,000원 이상 구매 시 무료)</dd>
            </div>
            <div>
              <dt>도서산간</dt>
              <dd>제주 및 도서산간 2,000원 추가</dd>
            </div>
          </dl>

          {(product.category === "POSTER" || productAddons.length > 0) && (
            <div className="addon-box">
              {product.category === "POSTER" && (
                <div className="option-choice-group size-choice-group">
                  <span className="option-section-title">사이즈</span>
                  <div className={`option-control addon-group-header ${sizeOptionsOpen ? "is-open" : ""}`}>
                    <button
                      className="addon-group-toggle"
                      type="button"
                      onClick={() => setSizeOptionsOpen((isOpen) => !isOpen)}
                      aria-expanded={sizeOptionsOpen}
                    >
                      <span>{selectedSizeOption ? formatSizeOptionLabel(selectedSizeOption) : "사이즈 선택"}</span>
                    </button>
                    <div className="addon-group-actions">
                      <span aria-hidden="true">{sizeOptionsOpen ? "⌃" : "⌄"}</span>
                    </div>
                  </div>
                  {sizeOptionsOpen && (
                    <div className="addon-option-list">
                      {sizeOptions.map((option) => {
                        const disabled = !option.active || option.soldOut;

                        return (
                          <div
                            className={`addon-option-row ${sizeOptionId === String(option.id) ? "is-selected" : ""} ${disabled ? "is-disabled" : ""}`}
                            key={option.id}
                          >
                            <button
                              className="addon-option-button"
                              type="button"
                              disabled={disabled}
                              onClick={() => {
                                setSizeOptionId(String(option.id));
                                setSizeOptionsOpen(false);
                              }}
                            >
                              <span>{formatSizeOptionLabel(option)}</span>
                              {option.soldOut && <small>SOLD OUT</small>}
                            </button>
                          </div>
                        );
                      })}
                    </div>
                  )}
                </div>
              )}
              {productAddons.length > 0 && (
                <div className="addon-choice-list">
                  <span className="addon-choice-heading">추가상품</span>
                  {Object.entries(groupedAddons).map(([groupName, addons]) => {
                    const isOpen = Boolean(openAddonGroups[groupName]);

                    return (
                      <div className="addon-choice-group" key={groupName}>
                        <div className={`option-control addon-group-header ${isOpen ? "is-open" : ""}`}>
                          <button
                            className="addon-group-toggle"
                            type="button"
                            onClick={() => toggleAddonGroup(groupName)}
                            aria-expanded={isOpen}
                          >
                            <span>{groupName}</span>
                          </button>
                          <div className="addon-group-actions">
                            <span aria-hidden="true">{isOpen ? "⌃" : "⌄"}</span>
                          </div>
                        </div>
                        {isOpen && (
                          <div className="addon-option-list">
                            {addons.map((addon) => (
                              <div
                                className={`addon-option-row ${selectedAddons[addon.id] ? "is-selected" : ""} ${addon.soldOut ? "is-disabled" : ""}`}
                                key={addon.id}
                              >
                                <button
                                  className="addon-option-button"
                                  type="button"
                                  disabled={addon.soldOut}
                                  onClick={() => selectAddon(addon.id, groupName)}
                                >
                                  <span>{formatAddonOptionLabel(addon)}</span>
                                  {addon.soldOut && <small>SOLD OUT</small>}
                                </button>
                              </div>
                            ))}
                          </div>
                        )}
                      </div>
                    );
                  })}
                  {selectedProductAddons.length > 0 && (
                    <div className="selected-addon-quantity-box">
                      <span>추가상품 수량</span>
                      <div className="selected-addon-quantity-list">
                        {selectedProductAddons.map((addon) => (
                          <QuantityControl
                            key={addon.id}
                            label={addon.name}
                            value={selectedAddons[addon.id]}
                            onChange={(nextQuantity) => updateSelectedAddonQuantity(addon.id, nextQuantity)}
                            trailingAction={
                              <button
                                className="addon-quantity-clear"
                                aria-label={`${addon.name} 선택 해제`}
                                type="button"
                                onClick={() => removeSelectedAddon(addon.id)}
                              >
                                ×
                              </button>
                            }
                          />
                        ))}
                      </div>
                    </div>
                  )}
                </div>
              )}
            </div>
          )}

          {selectedProductAddons.length > 0 && <div className="detail-section-divider" />}

          <QuantityControl
            label="상품 수량"
            value={quantity}
            onChange={setQuantity}
            reserveTrailingAction={selectedProductAddons.length > 0}
          />

          <div className="detail-section-divider" />

          <div className="summary-box">
            <p>
              <span>주문 수량</span>
              <strong>{quantity}개</strong>
            </p>
            {selectedProductAddons.length > 0 && (
              <p>
                <span>추가상품 금액</span>
                <strong>{formatWon(addonTotal)}</strong>
              </p>
            )}
            <p>
              <span>총 상품 금액</span>
              <strong>{formatWon(totalPrice)}</strong>
            </p>
          </div>

          {submitError && <p className="form-error">{submitError}</p>}

          <div className="detail-actions">
            <button type="button" disabled={orderDisabled} onClick={() => void handleCartAction("buy")}>
              {product.soldOut ? "SOLD OUT" : submitting ? "처리 중" : "구매하기"}
            </button>
            <button type="button" disabled={orderDisabled} onClick={() => void handleCartAction("cart")}>
              {submitting ? "처리 중" : "장바구니에 담기"}
            </button>
          </div>
        </article>
      </div>
      {product.detailImageUrl && (
        <div className="product-detail-content">
          <img {...protectedImageProps()} src={product.detailImageUrl} alt={`${product.name} 상세 이미지`} />
        </div>
      )}
    </section>
  );
}

function QuantityControl({
  label,
  value,
  onChange,
  trailingAction,
  reserveTrailingAction = false,
}: QuantityControlProps) {
  const hasTrailingAction = Boolean(trailingAction) || reserveTrailingAction;

  return (
    <div className={`quantity-row ${hasTrailingAction ? "has-trailing-action" : ""}`}>
      <span>{label}</span>
      <div>
        <button type="button" onClick={() => onChange(Math.max(1, value - 1))} aria-label={`${label} 줄이기`}>
          −
        </button>
        <output>{value}</output>
        <button type="button" onClick={() => onChange(value + 1)} aria-label={`${label} 늘리기`}>
          +
        </button>
      </div>
      {hasTrailingAction && <span className="quantity-row-action">{trailingAction}</span>}
    </div>
  );
}

function Cart({
  items,
  totalPrice,
  cartCount,
  loading,
  error,
  onOpenDetail,
  onUpdateQuantity,
  onRemove,
  onCheckout,
}: CartProps) {
  // 선택상품 주문/삭제 대상
  const [selectedItemIds, setSelectedItemIds] = useState<number[]>([]);
  const [actionError, setActionError] = useState<string | null>(null);

  // 장바구니 결제예정금액
  const deliveryFee = calculateDeliveryFee(totalPrice);
  const paymentTotal = totalPrice + deliveryFee;
  const selectedItems = items.filter((item) => selectedItemIds.includes(item.cartItemId));
  const allSelected = items.length > 0 && selectedItemIds.length === items.length;

  // 삭제된 장바구니 항목 선택 상태 제거
  useEffect(() => {
    const itemIds = items.map((item) => item.cartItemId);
    setSelectedItemIds((prevIds) => prevIds.filter((id) => itemIds.includes(id)));
  }, [items]);

  const toggleAll = () => {
    setSelectedItemIds(allSelected ? [] : items.map((item) => item.cartItemId));
  };

  const toggleItem = (cartItemId: number) => {
    setSelectedItemIds((prevIds) =>
      prevIds.includes(cartItemId)
        ? prevIds.filter((id) => id !== cartItemId)
        : [...prevIds, cartItemId]
    );
  };

  // 장바구니 수량 변경
  const handleUpdateQuantity = async (
    cartItemId: number,
    quantity: number
  ) => {
    setActionError(null);

    try {
      await onUpdateQuantity(cartItemId, quantity);
    } catch (updateError) {
      setActionError(updateError instanceof Error ? updateError.message : "수량 변경 실패");
    }
  };

  // 선택상품 삭제
  const handleRemoveSelected = async () => {
    setActionError(null);

    try {
      for (const item of selectedItems) {
        await onRemove(item.cartItemId);
      }
      setSelectedItemIds([]);
    } catch (removeError) {
      setActionError(removeError instanceof Error ? removeError.message : "선택 상품 삭제 실패");
    }
  };

  // 선택상품 주문
  const handleSelectedCheckout = () => {
    if (selectedItemIds.length === 0) {
      setActionError("선택된 상품이 없습니다.");
      return;
    }

    setActionError(null);
    onCheckout(selectedItemIds);
  };

  return (
    <section className="page-view cart-view">
      <div className="cart-list-title">일반상품 ({cartCount})</div>

      {loading && <p className="state-text">장바구니를 불러오는 중입니다.</p>}
      {error && <p className="form-error">{error}</p>}

      {items.length === 0 ? (
        <p className="empty-text">장바구니가 비어 있습니다.</p>
      ) : (
        <>
          <div className="cart-list">
            {items.map((item) => (
              <article className="cart-item" key={item.cartItemId}>
                <button
                  className={`cart-product-image ${item.sizeName ? "" : "is-square-thumbnail"}`}
                  type="button"
                  onClick={() => onOpenDetail(item.productId)}
                  aria-label={`${item.productName} 상세보기`}
                >
                  <img {...protectedImageProps()} src={item.productImageUrl} alt={item.productName} />
                </button>
                <div className="cart-item-info">
                  <button
                    className="cart-product-name"
                    type="button"
                    onClick={() => onOpenDetail(item.productId)}
                  >
                    {item.productName}
                  </button>
                  <p>{formatWon(item.sizeName ? item.productPrice : item.itemTotalPrice)}</p>
                  {item.sizeName && <span>{formatCartSizeOptionLabel(item)}</span>}
                  {(item.addons ?? []).map((addon) => (
                    <span key={addon.cartItemAddonId}>
                      추가상품: {addon.addonName} (+{formatWon(addon.addonPrice)}) {addon.quantity}개
                    </span>
                  ))}
                  {(item.addons ?? []).length === 0 && item.addonName && (
                    <span>추가상품: {item.addonName} (+{formatWon(item.addonPrice)}) {item.addonQuantity}개</span>
                  )}
                  <div className="cart-quantity-row">
                    <span>상품</span>
                    <div className="cart-quantity">
                      <output>{item.quantity}</output>
                      <button
                        type="button"
                        onClick={() => void handleUpdateQuantity(item.cartItemId, item.quantity + 1)}
                        aria-label={`${item.productName} 수량 늘리기`}
                      >
                        +
                      </button>
                      <button
                        type="button"
                        onClick={() => void handleUpdateQuantity(item.cartItemId, Math.max(1, item.quantity - 1))}
                        aria-label={`${item.productName} 수량 줄이기`}
                      >
                        −
                      </button>
                    </div>
                  </div>
                </div>
                <label className="cart-item-check">
                  <input
                    type="checkbox"
                    checked={selectedItemIds.includes(item.cartItemId)}
                    onChange={() => toggleItem(item.cartItemId)}
                    aria-label={`${item.productName} 선택`}
                  />
                </label>
              </article>
            ))}
          </div>

          <div className="cart-delivery-row">
            <strong>[기본배송]</strong>
            <p>
              상품구매금액 <strong>{formatWon(totalPrice)}</strong> + 배송비{" "}
              <strong>{formatWon(deliveryFee)}</strong> = 합계 :{" "}
              <strong>{formatWon(paymentTotal)}</strong>
            </p>
          </div>

          <div className="cart-secondary-actions">
            <button type="button" onClick={toggleAll}>
              전체선택
            </button>
            <button type="button" onClick={() => void handleRemoveSelected()}>
              삭제하기
            </button>
          </div>

          <div className="cart-summary">
            <p>
              <span>총 상품금액</span>
              <strong>{formatWon(totalPrice)}</strong>
            </p>
            <p>
              <span>총 배송비</span>
              <strong>{formatWon(deliveryFee)}</strong>
            </p>
            <p>
              <span>결제예정금액</span>
              <strong>{formatWon(paymentTotal)}</strong>
            </p>
          </div>

          {actionError && <p className="form-error">{actionError}</p>}

          <div className="cart-order-actions">
            <button type="button" onClick={handleSelectedCheckout}>
              선택상품주문
            </button>
            <button type="button" onClick={() => onCheckout()}>
              전체상품주문
            </button>
          </div>
        </>
      )}
    </section>
  );
}

function Checkout({ totalPrice, member, onMemberLoaded, onCreateOrder, onRequestPayment }: CheckoutProps) {
  // 주문서 결제예정금액
  const deliveryFee = calculateDeliveryFee(totalPrice);
  const idempotencyKeyRef = useRef<string | null>(null);
  const [form, setForm] = useState<OrderCreateRequest>({
    receiverName: "",
    receiverPhone: "",
    zipCode: "",
    address: "",
    detailAddress: "",
    deliveryMemo: "",
  });
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [orderTermsAgreed, setOrderTermsAgreed] = useState(false);
  const [paymentTermsAgreed, setPaymentTermsAgreed] = useState(false);
  const [termsOpen, setTermsOpen] = useState(false);
  const [termsText, setTermsText] = useState("");
  const [phonePrefix, setPhonePrefix] = useState("010");
  const [phoneMiddle, setPhoneMiddle] = useState("");
  const [phoneLast, setPhoneLast] = useState("");
  const [deliveryMemoType, setDeliveryMemoType] = useState("");
  const [postcodeOpen, setPostcodeOpen] = useState(false);
  const [paymentMethod, setPaymentMethod] = useState<PortOnePayMethod>("CARD");
  const [customerEmail, setCustomerEmail] = useState(member?.email ?? "");

  const remoteAreaDeliveryFee = calculateRemoteAreaDeliveryFee(form.zipCode, form.address);
  const paymentTotal = totalPrice + deliveryFee + remoteAreaDeliveryFee;
  const allTermsAgreed = orderTermsAgreed && paymentTermsAgreed;

  // 회원 정보로 수령인 기본값 채움
  const applyMemberContact = (nextMember: MemberResponse) => {
    setCustomerEmail(nextMember.email);

    setForm((prevForm) => ({
      ...prevForm,
      receiverName: prevForm.receiverName || nextMember.name,
      zipCode: prevForm.zipCode || nextMember.zipCode || "",
      address: prevForm.address || nextMember.address || "",
      detailAddress: prevForm.detailAddress || nextMember.detailAddress || "",
    }));

    const phoneParts = nextMember.phone.split("-");

    if (
      !form.receiverPhone &&
      phoneParts.length === 3 &&
      PHONE_PREFIXES.includes(phoneParts[0]) &&
      phoneParts[1].length === 4 &&
      phoneParts[2].length === 4
    ) {
      updatePhone(phoneParts[0], phoneParts[1], phoneParts[2]);
    }
  };

  const updateField = (field: keyof OrderCreateRequest, value: string) => {
    setForm((prevForm) => ({
      ...prevForm,
      [field]: value,
    }));
  };

  // 연락처 입력값 조합
  const updatePhone = (nextPrefix: string, nextMiddle: string, nextLast: string) => {
    const normalizedMiddle = nextMiddle.replace(/\D/g, "").slice(0, 4);
    const normalizedLast = nextLast.replace(/\D/g, "").slice(0, 4);

    setPhonePrefix(nextPrefix);
    setPhoneMiddle(normalizedMiddle);
    setPhoneLast(normalizedLast);

    setForm((prevForm) => ({
      ...prevForm,
      receiverPhone:
        normalizedMiddle.length === 4 && normalizedLast.length === 4
          ? `${nextPrefix}-${normalizedMiddle}-${normalizedLast}`
          : "",
    }));
  };

  // 약관 전체 동의
  const updateAllTerms = (checked: boolean) => {
    setOrderTermsAgreed(checked);
    setPaymentTermsAgreed(checked);
  };

  // 배송 메모 선택
  const updateDeliveryMemoType = (value: string) => {
    setDeliveryMemoType(value);
    updateField("deliveryMemo", value === CUSTOM_DELIVERY_MEMO ? "" : value);
  };

  // 주문서 진입 시 회원 기본 정보 채움
  useEffect(() => {
    if (member) {
      applyMemberContact(member);
    }

    void getMyInfo()
      .then((memberData) => {
        onMemberLoaded(memberData);
        applyMemberContact(memberData);
      })
      .catch(() => undefined);
  }, []);

  useEffect(() => {
    void loadDaumPostcodeScript().catch(() => undefined);
  }, []);

  // 우편번호 검색 모달 열기
  const openAddressSearch = async () => {
    setError(null);

    try {
      await loadDaumPostcodeScript();

      if (!window.daum?.Postcode) {
        throw new Error("우편번호 검색창을 사용할 수 없습니다.");
      }

      setPostcodeOpen(true);
    } catch (addressError) {
      setError(addressError instanceof Error ? addressError.message : "우편번호 검색 실패");
    }
  };

  // 결제 약관 내용 로드
  useEffect(() => {
    if (!termsOpen || termsText) {
      return;
    }

    fetch("/terms/electronic-finance.txt")
      .then((response) => {
        if (!response.ok) {
          throw new Error("약관을 불러오지 못했습니다.");
        }

        return response.text();
      })
      .then(setTermsText)
      .catch(() => setTermsText("약관을 불러오지 못했습니다."));
  }, [termsOpen, termsText]);

  // 모달 오픈 시 배경 스크롤 잠금
  useEffect(() => {
    if (!termsOpen && !postcodeOpen) {
      return;
    }

    const previousBodyOverflow = document.body.style.overflow;
    const previousHtmlOverflow = document.documentElement.style.overflow;

    document.body.style.overflow = "hidden";
    document.documentElement.style.overflow = "hidden";

    return () => {
      document.body.style.overflow = previousBodyOverflow;
      document.documentElement.style.overflow = previousHtmlOverflow;
    };
  }, [termsOpen, postcodeOpen]);

  // 주문 생성 후 PortOne 결제창 요청
  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    if (!allTermsAgreed) {
      setError("필수 약관에 동의해주세요.");
      return;
    }

    setSubmitting(true);
    setError(null);

    try {
      idempotencyKeyRef.current ??= createIdempotencyKey();

      const normalizedCustomerEmail = customerEmail.trim();

      if (!normalizedCustomerEmail) {
        setError("회원 이메일을 확인할 수 없어 결제를 진행할 수 없습니다.");
        return;
      }

      const order = await onCreateOrder(form, idempotencyKeyRef.current);
      await onRequestPayment(order, paymentMethod, normalizedCustomerEmail);
    } catch (submitError) {
      setError(submitError instanceof Error ? submitError.message : "결제 요청 실패");
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <section className="page-view checkout-view">
      <div className="page-title">
        <span>ORDER</span>
        <h1>주문서</h1>
      </div>

      <form className="checkout-form" onSubmit={handleSubmit}>
        <div className="checkout-grid">
          <label>
            <span>
              받는 분 <b className="required-mark">*</b>
            </span>
            <input
              value={form.receiverName}
              onChange={(event) => updateField("receiverName", event.target.value)}
              required
            />
          </label>
          <label>
            <span>
              연락처 <b className="required-mark">*</b>
            </span>
            <div className="checkout-phone-row">
              <select
                aria-label="연락처 앞자리"
                value={phonePrefix}
                onChange={(event) => updatePhone(event.target.value, phoneMiddle, phoneLast)}
                required
              >
                {PHONE_PREFIXES.map((prefix) => (
                  <option key={prefix} value={prefix}>
                    {prefix}
                  </option>
                ))}
              </select>
              <span>-</span>
              <input
                aria-label="연락처 가운데자리"
                type="tel"
                value={phoneMiddle}
                inputMode="numeric"
                minLength={4}
                maxLength={4}
                onChange={(event) => updatePhone(phonePrefix, event.target.value, phoneLast)}
                required
              />
              <span>-</span>
              <input
                aria-label="연락처 끝자리"
                type="tel"
                value={phoneLast}
                inputMode="numeric"
                minLength={4}
                maxLength={4}
                onChange={(event) => updatePhone(phonePrefix, phoneMiddle, event.target.value)}
                required
              />
            </div>
          </label>
          <div className="checkout-address-field wide-field">
            <span>
              주소 <b className="required-mark">*</b>
            </span>
            <div className="checkout-address-inputs">
              <div className="checkout-address-search-row">
                <input
                  value={form.zipCode}
                  placeholder="우편번호"
                  readOnly
                  required
                />
                <button type="button" onClick={() => void openAddressSearch()}>
                  주소검색
                </button>
              </div>
              <input
                value={form.address}
                placeholder="기본주소"
                readOnly
                required
              />
              <input
                id="checkout-detail-address"
                value={form.detailAddress}
                placeholder="나머지 주소"
                onChange={(event) => updateField("detailAddress", event.target.value)}
              />
            </div>
          </div>
          <label className="wide-field">
            배송 메모
            <select
              className={`checkout-delivery-memo-select${deliveryMemoType ? "" : " is-placeholder"}`}
              value={deliveryMemoType}
              onChange={(event) => updateDeliveryMemoType(event.target.value)}
            >
              <option value="">-- 메시지 선택 (선택사항) --</option>
              {DELIVERY_MEMO_OPTIONS.map((memo) => (
                <option key={memo} value={memo}>
                  {memo}
                </option>
              ))}
              <option value={CUSTOM_DELIVERY_MEMO}>{CUSTOM_DELIVERY_MEMO}</option>
            </select>
            {deliveryMemoType === CUSTOM_DELIVERY_MEMO && (
              <input
                value={form.deliveryMemo}
                placeholder="배송 메모를 입력해주세요"
                onChange={(event) => updateField("deliveryMemo", event.target.value)}
              />
            )}
          </label>
        </div>

        <section className="checkout-payment-panel">
          <div className="payment-summary-head">
            <h2>총 결제 금액</h2>
            <strong>{formatWon(paymentTotal)}</strong>
          </div>

          <div className="payment-summary-box">
            <p>
              <span>주문상품</span>
              <strong>{formatWon(totalPrice)}</strong>
            </p>
            <p>
              <span>배송비</span>
              <strong>{formatWon(deliveryFee)}</strong>
            </p>
            <p>
              <span>지역별 배송비</span>
              <strong>{formatWon(remoteAreaDeliveryFee)}</strong>
            </p>
          </div>

          <div className="payment-methods">
            <h2>결제수단</h2>
            <div className="payment-method-options" role="radiogroup" aria-label="결제수단 선택">
              <button
                type="button"
                className={paymentMethod === "CARD" ? "is-active" : ""}
                role="radio"
                aria-checked={paymentMethod === "CARD"}
                onClick={() => setPaymentMethod("CARD")}
              >
                <span className="payment-method-radio" aria-hidden="true" />
                <span className="payment-method-text">
                  <strong>카드</strong>
                  <small>신용/체크카드</small>
                </span>
              </button>
              <button
                type="button"
                className={paymentMethod === "MOBILE" ? "is-active" : ""}
                role="radio"
                aria-checked={paymentMethod === "MOBILE"}
                onClick={() => setPaymentMethod("MOBILE")}
              >
                <span className="payment-method-radio" aria-hidden="true" />
                <span className="payment-method-text">
                  <strong>휴대폰</strong>
                  <small>통신사 결제</small>
                </span>
              </button>
            </div>
          </div>

          <div className="payment-terms">
            <h2>이용약관동의</h2>
            <div className="payment-terms-box">
              <div className="payment-terms-row payment-terms-main">
                <input
                  aria-label="전체 약관 동의"
                  checked={allTermsAgreed}
                  type="checkbox"
                  onChange={(event) => updateAllTerms(event.target.checked)}
                />
                <span>위 주문 내용을 확인하였으며, 아래 모든 약관에 동의합니다.</span>
              </div>
              <div className="payment-terms-row">
                <input
                  aria-label="전자금융거래 이용약관 동의"
                  checked={paymentTermsAgreed}
                  type="checkbox"
                  onChange={(event) => setPaymentTermsAgreed(event.target.checked)}
                />
                <span>[필수] 전자금융거래 이용약관 동의</span>
                <button type="button" onClick={() => setTermsOpen(true)}>
                  자세히
                </button>
              </div>
            </div>
          </div>
        </section>

        {error && <p className="form-error">{error}</p>}

        <div className="checkout-total">
          <button type="submit" disabled={submitting || totalPrice <= 0 || !allTermsAgreed}>
            {submitting ? "결제 준비 중" : `${formatWon(paymentTotal)} 결제하기`}
          </button>
        </div>
      </form>

      {termsOpen && (
        <PaymentTermsModal
          termsText={termsText}
          onClose={() => setTermsOpen(false)}
        />
      )}

      {postcodeOpen && (
        <PostcodeModal
          onComplete={(data) => {
            setForm((prevForm) => ({
              ...prevForm,
              zipCode: data.zonecode,
              address: data.roadAddress || data.jibunAddress || data.address,
            }));
            setPostcodeOpen(false);

            window.setTimeout(() => {
              document.getElementById("checkout-detail-address")?.focus();
            }, 0);
          }}
          onClose={() => setPostcodeOpen(false)}
        />
      )}
    </section>
  );
}

// 멱등성 키 생성
function createIdempotencyKey() {
  if (window.crypto?.randomUUID) {
    return window.crypto.randomUUID();
  }

  return `order-${Date.now()}-${Math.random().toString(16).slice(2)}`;
}

function PostcodeModal({
  onComplete,
  onClose,
}: {
  onComplete: (data: DaumPostcodeData) => void;
  onClose: () => void;
}) {
  useEffect(() => {
    const postcodeLayer = document.getElementById("checkout-postcode-layer");

    if (!postcodeLayer || !window.daum?.Postcode) {
      return;
    }

    postcodeLayer.innerHTML = "";

    new window.daum.Postcode({
      oncomplete: onComplete,
    }).embed(postcodeLayer);

    return () => {
      postcodeLayer.innerHTML = "";
    };
  }, [onComplete]);

  return (
    <div className="postcode-modal-backdrop" role="dialog" aria-modal="true" aria-labelledby="postcode-modal-title">
      <div className="postcode-modal">
        <div className="postcode-modal-header">
          <strong id="postcode-modal-title">우편번호 검색</strong>
          <button type="button" onClick={onClose}>
            닫기
          </button>
        </div>
        <div id="checkout-postcode-layer" className="postcode-modal-content" />
      </div>
    </div>
  );
}

function PaymentTermsModal({
  title = "전자금융거래 이용약관",
  termsText,
  onClose,
}: {
  title?: string;
  termsText: string;
  onClose: () => void;
}) {
  return (
    <div className="terms-modal-backdrop" role="dialog" aria-modal="true" aria-labelledby="payment-terms-title">
      <div className="terms-modal">
        <h1 id="payment-terms-title">{title}</h1>
        <div className="terms-modal-content">
          <pre>{termsText || "약관을 불러오는 중입니다."}</pre>
        </div>
        <button type="button" onClick={onClose}>
          확인
        </button>
      </div>
    </div>
  );
}

function PaymentResult({ result, onRetryPayment, onMoveOrders, onMoveCart }: PaymentResultProps) {
  const isSuccess = result.status === "success";
  const isProcessing = result.status === "processing";
  const retryOrder = result.status === "fail" ? result.retryOrder : undefined;
  const shouldShowCancelNotice = result.status === "fail" && Boolean(result.showCancelNotice);
  const [cancelNoticeOpen, setCancelNoticeOpen] = useState(shouldShowCancelNotice);

  useEffect(() => {
    if (result.status === "fail") {
      setCancelNoticeOpen(Boolean(result.showCancelNotice));
    }
  }, [result]);

  useEffect(() => {
    if (!cancelNoticeOpen) {
      return;
    }

    const previousBodyOverflow = document.body.style.overflow;
    const previousHtmlOverflow = document.documentElement.style.overflow;

    document.body.style.overflow = "hidden";
    document.documentElement.style.overflow = "hidden";

    return () => {
      document.body.style.overflow = previousBodyOverflow;
      document.documentElement.style.overflow = previousHtmlOverflow;
    };
  }, [cancelNoticeOpen]);

  return (
    <section className="page-view checkout-view">
      <div className="order-complete">
        <h1>
          {isProcessing
            ? "결제 승인 중입니다."
            : isSuccess
              ? "결제가 완료되었습니다."
              : "결제를 완료하지 못했습니다."}
        </h1>
        {!isSuccess && <p>{result.message}</p>}
        {isSuccess && <strong>{formatWon(result.payment.amount)}</strong>}
        {isSuccess ? (
          <button type="button" onClick={onMoveOrders}>
            주문 내역으로 이동
          </button>
        ) : retryOrder ? (
          <div className="payment-result-actions">
            <button type="button" onClick={() => void onRetryPayment(retryOrder)}>
              다시 결제하기
            </button>
            <button type="button" onClick={onMoveOrders}>
              주문 내역으로 이동
            </button>
          </div>
        ) : (
          <button type="button" onClick={onMoveCart} disabled={isProcessing}>
            장바구니로 이동
          </button>
        )}
      </div>

      {cancelNoticeOpen && shouldShowCancelNotice && (
        <div className="payment-cancel-modal-backdrop" role="dialog" aria-modal="true">
          <div className="payment-cancel-modal">
            <p>결제를 취소하였습니다.</p>
            <button type="button" onClick={() => setCancelNoticeOpen(false)}>
              확인
            </button>
          </div>
        </div>
      )}
    </section>
  );
}

function MyPage({
  member,
  initialView,
  orders,
  orderPageInfo,
  loading,
  error,
  onChangeOrderPage,
  onUpdateInfo,
  onUpdatePassword,
  onCancelOrder,
  onDeactivate,
  onLogout,
}: MyPageProps) {
  const [infoForm, setInfoForm] = useState({
    name: "",
    phone: "",
    zipCode: "",
    address: "",
    detailAddress: "",
  });
  const [passwordForm, setPasswordForm] = useState({
    currentPassword: "",
    newPassword: "",
    newPasswordConfirm: "",
  });
  const [cancelReasonSelections, setCancelReasonSelections] = useState<Record<number, string>>({});
  const [cancelCustomReasons, setCancelCustomReasons] = useState<Record<number, string>>({});
  const [cancelReasonError, setCancelReasonError] = useState("");
  const [cancelReasonOrderId, setCancelReasonOrderId] = useState<number | null>(null);
  const [cancelOrderSubmitting, setCancelOrderSubmitting] = useState(false);
  const [view, setView] = useState<MyPageView>(initialView);
  const [selectedOrder, setSelectedOrder] = useState<OrderResponse | null>(null);
  const [message, setMessage] = useState<string | null>(null);
  const [toastMessage, setToastMessage] = useState("");
  const [actionError, setActionError] = useState<string | null>(null);
  const [passwordChangeStep, setPasswordChangeStep] = useState<"newPassword" | "currentPassword" | null>(null);
  const [passwordModalError, setPasswordModalError] = useState<string | null>(null);
  const [deactivateConfirmOpen, setDeactivateConfirmOpen] = useState(false);
  const [profilePostcodeOpen, setProfilePostcodeOpen] = useState(false);
  const isSocialOnlyMember =
    (member?.provider === "KAKAO" || member?.provider === "NAVER") &&
    member.email.endsWith("@earthy.local");
  const passwordChangeDisabled = isSocialOnlyMember;

  useEffect(() => {
    if (member) {
      setInfoForm({
        name: member.name,
        phone: member.phone,
        zipCode: member.zipCode || "",
        address: member.address || "",
        detailAddress: member.detailAddress || "",
      });

      if (member.provider === "KAKAO") {
        setPasswordForm({ currentPassword: "", newPassword: "", newPasswordConfirm: "" });
      }
    }
  }, [member]);

  const updatePhone = (value: string) => {
    const numbers = value.replace(/\D/g, "").slice(0, 11);
    const formattedPhone =
      numbers.length > 7
        ? `${numbers.slice(0, 3)}-${numbers.slice(3, 7)}-${numbers.slice(7)}`
        : numbers.length > 3
          ? `${numbers.slice(0, 3)}-${numbers.slice(3)}`
          : numbers;

    setInfoForm((prevForm) => ({ ...prevForm, phone: formattedPhone }));
  };

  const updateInfoField = (field: keyof typeof infoForm, value: string) => {
    setInfoForm((prevForm) => ({
      ...prevForm,
      [field]: value,
    }));
  };

  // 마이페이지 우편번호 검색 모달 열기
  const openProfileAddressSearch = async () => {
    setActionError(null);

    try {
      await loadDaumPostcodeScript();

      if (!window.daum?.Postcode) {
        throw new Error("우편번호 검색창을 사용할 수 없습니다.");
      }

      setProfilePostcodeOpen(true);
    } catch (addressError) {
      setActionError(addressError instanceof Error ? addressError.message : "우편번호 검색 실패");
    }
  };

  const orderCounts = orders.reduce(
    (counts, order) => {
      if (order.status === "PENDING") {
        counts.paymentPending += 1;
      }

      if (order.status === "PAID" || order.status === "PREPARING") {
        counts.preparing += 1;
      }

      if (order.status === "SHIPPED") {
        counts.shipped += 1;
      }

      if (order.status === "DELIVERED") {
        counts.delivered += 1;
      }

      if (order.status === "CANCELED") {
        counts.canceled += 1;
      }

      return counts;
    },
    {
      paymentPending: 0,
      preparing: 0,
      shipped: 0,
      delivered: 0,
      canceled: 0,
    }
  );

  const openOrderDetail = (order: OrderResponse) => {
    setSelectedOrder(order);
    setView("orderDetail");
    setCancelReasonOrderId(null);
    setMessage(null);
    setActionError(null);
  };

  const handleUpdateProfile = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setMessage(null);
    setActionError(null);

    try {
      await onUpdateInfo(
        infoForm.name,
        infoForm.phone,
        infoForm.zipCode,
        infoForm.address,
        infoForm.detailAddress
      );
      setToastMessage("회원 정보가 수정되었습니다.");
    } catch (updateError) {
      setActionError(updateError instanceof Error ? updateError.message : "회원 정보 수정 실패");
    }
  };

  const openPasswordChange = () => {
    if (passwordChangeDisabled) {
      return;
    }

    setPasswordForm({ currentPassword: "", newPassword: "", newPasswordConfirm: "" });
    setPasswordModalError(null);
    setPasswordChangeStep("newPassword");
  };

  const closePasswordChange = () => {
    setPasswordChangeStep(null);
    setPasswordModalError(null);
    setPasswordForm({ currentPassword: "", newPassword: "", newPasswordConfirm: "" });
  };

  const handlePasswordNext = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setPasswordModalError(null);

    if (!passwordForm.newPassword || !passwordForm.newPasswordConfirm) {
      setPasswordModalError("새 비밀번호를 입력해주세요.");
      return;
    }

    if (passwordForm.newPassword !== passwordForm.newPasswordConfirm) {
      setPasswordModalError("새 비밀번호가 일치하지 않습니다.");
      return;
    }

    setPasswordChangeStep("currentPassword");
  };

  const handleUpdatePassword = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setPasswordModalError(null);

    if (!passwordForm.currentPassword) {
      setPasswordModalError("현재 비밀번호를 입력해주세요.");
      return;
    }

    try {
      await onUpdatePassword(passwordForm.currentPassword, passwordForm.newPassword);
      closePasswordChange();
      setToastMessage("비밀번호가 변경되었습니다.");
    } catch (updateError) {
      setPasswordModalError(updateError instanceof Error ? updateError.message : "비밀번호 변경 실패");
    }
  };

  const handleCancelOrder = async (orderId: number) => {
    if (cancelOrderSubmitting) {
      return;
    }

    setMessage(null);
    setActionError(null);
    setCancelReasonError("");

    const selectedReason = cancelReasonSelections[orderId] ?? "";
    const customReason = cancelCustomReasons[orderId]?.trim() ?? "";

    if (!selectedReason) {
      setCancelReasonError("취소 사유를 선택해주세요.");
      return;
    }

    if (selectedReason === "기타" && !customReason) {
      setCancelReasonError("취소 사유를 입력해주세요.");
      return;
    }

    const cancelReason = selectedReason === "기타" ? customReason : selectedReason;
    setCancelOrderSubmitting(true);

    try {
      const canceledOrder = await onCancelOrder(orderId, cancelReason);

      if (selectedOrder?.orderId === orderId) {
        setSelectedOrder(canceledOrder);
      }

      setCancelReasonOrderId(null);
      setToastMessage("주문이 취소되었습니다.");
    } catch (cancelError) {
      setActionError(cancelError instanceof Error ? cancelError.message : "주문 취소 실패");
    } finally {
      setCancelOrderSubmitting(false);
    }
  };

  const handleCancelOrderClick = (orderId: number) => {
    setCancelReasonOrderId(orderId);
    setMessage(null);
    setActionError(null);
    setCancelReasonError("");
  };

  const closeCancelReasonModal = () => {
    if (cancelOrderSubmitting) {
      return;
    }

    setCancelReasonOrderId(null);
    setCancelReasonError("");
  };

  const handleDeactivate = async () => {
    setMessage(null);
    setActionError(null);

    try {
      await onDeactivate();
      setDeactivateConfirmOpen(false);
    } catch (deactivateError) {
      setActionError(deactivateError instanceof Error ? deactivateError.message : "회원 탈퇴 실패");
    }
  };

  const myPageTitle =
    view === "orders"
      ? "주문내역 조회"
      : view === "orderDetail"
        ? "주문 상세내역"
        : view === "profile"
          ? "회원정보 수정"
          : "MYPAGE";

  return (
    <section className="page-view mypage-view">
      {view === "home" && (
        <div className="page-title">
          <span>ACCOUNT</span>
          <h1>MYPAGE</h1>
        </div>
      )}

      {view !== "home" && (
        <div className="mypage-sub-title">
          <h1>{myPageTitle}</h1>
          {view === "orderDetail" && (
            <button type="button" onClick={() => setView("orders")}>
              목록으로 돌아가기
              <span aria-hidden="true" />
            </button>
          )}
        </div>
      )}

      {view === "home" && (
        <section className="order-status-summary">
          <div className="order-status-heading">
            <h2>나의 주문처리 현황</h2>
            <button type="button" onClick={onLogout}>
              로그아웃
            </button>
          </div>
          <div className="order-status-grid">
            <span>입금전 <strong>{orderCounts.paymentPending}</strong></span>
            <span>배송준비중 <strong>{orderCounts.preparing}</strong></span>
            <span>배송중 <strong>{orderCounts.shipped}</strong></span>
            <span>배송완료 <strong>{orderCounts.delivered}</strong></span>
            <span>취소 : <strong>{orderCounts.canceled}</strong></span>
            <span>교환 : <strong>0</strong></span>
            <span>반품 : <strong>0</strong></span>
          </div>
        </section>
      )}

      {loading && <p className="state-text">마이페이지 정보를 불러오는 중입니다.</p>}
      {error && <p className="form-error">{error}</p>}
      {view !== "profile" && actionError && <p className="form-error">{actionError}</p>}
      <CustomerToast message={toastMessage} onClose={() => setToastMessage("")} />

      {view === "home" && (
        <div className="mypage-menu-grid">
          <button type="button" onClick={() => setView("orders")}>
            <strong>주문내역 조회</strong>
            <span>주문 목록과 상세 내역 확인</span>
          </button>
          <button type="button" onClick={() => setView("profile")}>
            <strong>회원정보 수정</strong>
            <span>이름, 비밀번호, 연락처 변경</span>
          </button>
        </div>
      )}

      {view === "orders" && (
        <section className="mypage-section order-section">
          {orders.length === 0 ? (
            <p className="empty-text">주문 내역이 없습니다.</p>
          ) : (
            <div className="order-list">
              {orders.map((order) => {
                const firstItem = order.items[0];
                const extraItemCount = Math.max(order.items.length - 1, 0);

                return (
                  <article className="order-list-card" key={order.orderId}>
                    <div className="order-list-head">
                      <strong>
                        {new Date(order.createdAt).toISOString().slice(0, 10)}
                        <span>({order.orderNumber})</span>
                      </strong>
                    </div>

                    <div className="order-list-main">
                      {firstItem && (
                        <div className="order-list-body">
                          <img
                            {...protectedImageProps(firstItem.sizeName ? "" : "is-square-thumbnail")}
                            src={firstItem.productImageUrl}
                            alt={firstItem.productName}
                          />
                          <div className="order-list-product">
                            <strong>
                              {firstItem.productName}
                              {extraItemCount > 0 && ` 외 ${extraItemCount}개`}
                            </strong>
                            <span>{formatWon(order.totalPrice)}</span>
                            {firstItem.addonName && (
                              <small>
                                [추가상품: {firstItem.addonName} {firstItem.addonQuantity}개]
                              </small>
                            )}
                          </div>
                        </div>
                      )}

                      <div className="order-list-actions">
                        <button type="button" onClick={() => openOrderDetail(order)}>
                          상세보기
                        </button>
                        <button type="button" onClick={() => openPostOfficeTracking(order.trackingNumber)}>
                          배송조회
                        </button>
                      </div>
                    </div>

                    <div className="order-list-foot">
                      <span>{order.statusDescription}</span>
                    </div>
                  </article>
                );
              })}
            </div>
          )}
          <Pagination pageInfo={orderPageInfo} onChangePage={onChangeOrderPage} />
        </section>
      )}

      {view === "orderDetail" && selectedOrder && (
        <section className="mypage-section order-section">
          <article className="order-detail-sheet">
            <section className="order-detail-block">
              <h2>주문정보</h2>
              <dl className="order-detail-table">
                <div>
                  <dt>주문번호</dt>
                  <dd>{selectedOrder.orderNumber}</dd>
                </div>
                <div>
                  <dt>주문일자</dt>
                  <dd>{new Date(selectedOrder.createdAt).toLocaleString("ko-KR")}</dd>
                </div>
                <div>
                  <dt>주문자</dt>
                  <dd>{selectedOrder.receiverName}</dd>
                </div>
                <div>
                  <dt>주문처리상태</dt>
                  <dd>{selectedOrder.statusDescription}</dd>
                </div>
              </dl>
            </section>

            <section className="order-detail-block">
              <h2>주문상품</h2>
              <ul className="order-detail-products">
                {selectedOrder.items.map((item) => (
                  <li key={item.orderItemId}>
                    <img
                      {...protectedImageProps(item.sizeName ? "" : "is-square-thumbnail")}
                      src={item.productImageUrl}
                      alt={item.productName}
                    />
                    <div>
                      <strong>{item.productName}</strong>
                      <span>
                        {formatWon(item.productPrice)} / {item.quantity}개
                      </span>
                      {formatOrderSizeOptionLabel(item) && (
                        <span>{formatOrderSizeOptionLabel(item)}</span>
                      )}
                      {(item.addons ?? []).map((addon) => (
                        <span key={addon.orderItemAddonId}>
                          추가상품: {addon.addonName} (+{formatWon(addon.addonPrice)}) / {addon.quantity}개
                        </span>
                      ))}
                      {(item.addons ?? []).length === 0 && item.addonName && (
                        <span>
                          추가상품: {item.addonName} (+{formatWon(item.addonPrice)}) / {item.addonQuantity}개
                        </span>
                      )}
                    </div>
                  </li>
                ))}
              </ul>
            </section>

            <section className="order-detail-block">
              <h2>결제 정보</h2>
              <dl className="order-detail-table">
                <div>
                  <dt>결제방법</dt>
                  <dd>{formatPaymentMethod(selectedOrder.paymentMethod)}</dd>
                </div>
                <div>
                  <dt>총 결제금액</dt>
                  <dd>{formatWon(selectedOrder.totalPrice)}</dd>
                </div>
                <div>
                  <dt>상품금액</dt>
                  <dd>{formatWon(selectedOrder.productTotalPrice)}</dd>
                </div>
                <div>
                  <dt>기본 배송비</dt>
                  <dd>{formatWon(selectedOrder.deliveryFee)}</dd>
                </div>
                <div>
                  <dt>지역별 배송비</dt>
                  <dd>{formatWon(selectedOrder.remoteAreaDeliveryFee)}</dd>
                </div>
              </dl>
            </section>

            <section className="order-detail-block">
              <h2>배송지정보</h2>
              <dl className="order-detail-table">
                <div>
                  <dt>받으시는 분</dt>
                  <dd>{selectedOrder.receiverName}</dd>
                </div>
                <div>
                  <dt>우편번호</dt>
                  <dd>{selectedOrder.zipCode}</dd>
                </div>
                <div>
                  <dt>주소</dt>
                  <dd>
                    {selectedOrder.address}
                    {selectedOrder.detailAddress && `, ${selectedOrder.detailAddress}`}
                  </dd>
                </div>
                <div>
                  <dt>일반전화</dt>
                  <dd>-</dd>
                </div>
                <div>
                  <dt>휴대전화</dt>
                  <dd>{selectedOrder.receiverPhone}</dd>
                </div>
                <div>
                  <dt>배송메시지</dt>
                  <dd>{selectedOrder.deliveryMemo || "-"}</dd>
                </div>
                {selectedOrder.trackingNumber && (
                  <div>
                    <dt>운송장</dt>
                    <dd>{selectedOrder.carrier} {selectedOrder.trackingNumber}</dd>
                  </div>
                )}
              </dl>
            </section>

            <div className="order-actions">
              <span className="order-actions-spacer" aria-hidden="true" />
              <button
                type="button"
                disabled={
                  selectedOrder.status === "PENDING" ||
                  selectedOrder.status === "CANCELED" ||
                  selectedOrder.status === "SHIPPED" ||
                  selectedOrder.status === "DELIVERED"
                }
                onClick={() => handleCancelOrderClick(selectedOrder.orderId)}
              >
                주문 취소
              </button>
            </div>
          </article>
        </section>
      )}

      {cancelReasonOrderId !== null && (
        <div className="cart-notice-backdrop" role="presentation" onClick={closeCancelReasonModal}>
          <form
            className="cart-notice order-cancel-notice"
            role="dialog"
            aria-modal="true"
            aria-label="주문 취소 사유 입력"
            onSubmit={(event) => {
              event.preventDefault();
              void handleCancelOrder(cancelReasonOrderId);
            }}
            onClick={(event) => event.stopPropagation()}
          >
            <p>주문 취소 사유를 입력해주세요.</p>
            <select
              value={cancelReasonSelections[cancelReasonOrderId] ?? ""}
              disabled={cancelOrderSubmitting}
              onChange={(event) => {
                setCancelReasonSelections((prevReasons) => ({
                  ...prevReasons,
                  [cancelReasonOrderId]: event.target.value,
                }));
                setCancelReasonError("");
              }}
              autoFocus
            >
              <option value="">취소 사유를 선택해주세요.</option>
              {CUSTOMER_CANCEL_REASON_OPTIONS.map((reason) => (
                <option key={reason} value={reason}>
                  {reason}
                </option>
              ))}
            </select>
            {cancelReasonSelections[cancelReasonOrderId] === "기타" && (
              <textarea
                value={cancelCustomReasons[cancelReasonOrderId] ?? ""}
                placeholder="취소 사유를 입력해주세요."
                disabled={cancelOrderSubmitting}
                onChange={(event) => {
                  setCancelCustomReasons((prevReasons) => ({
                    ...prevReasons,
                    [cancelReasonOrderId]: event.target.value,
                  }));
                  setCancelReasonError("");
                }}
              />
            )}
            {cancelReasonError && <span className="order-cancel-error">{cancelReasonError}</span>}
            <small>취소 후에는 주문이 더 이상 진행되지 않습니다.</small>
            <div>
              <button type="submit" disabled={cancelOrderSubmitting}>
                {cancelOrderSubmitting ? "취소 처리 중..." : "취소하기"}
              </button>
              <button type="button" onClick={closeCancelReasonModal} disabled={cancelOrderSubmitting}>
                닫기
              </button>
            </div>
          </form>
        </div>
      )}

      {view === "profile" && (
        <section className="mypage-section profile-section">
          <form className="mypage-form profile-form" onSubmit={handleUpdateProfile}>
            <label>
              이메일
              <input value={member?.email ?? ""} readOnly />
            </label>
            <label>
              이름
              <input
                value={infoForm.name}
                onChange={(event) => updateInfoField("name", event.target.value)}
                required
              />
            </label>
            <label>
              연락처
              <input
                type="tel"
                value={infoForm.phone}
                inputMode="numeric"
                maxLength={13}
                onChange={(event) => updatePhone(event.target.value)}
                required
              />
            </label>
            <div className="profile-address-field">
              <span>주소</span>
              <div className="checkout-address-inputs">
                <div className="checkout-address-search-row">
                  <input
                    value={infoForm.zipCode}
                    placeholder="우편번호"
                    readOnly
                  />
                  <button type="button" onClick={() => void openProfileAddressSearch()}>
                    주소검색
                  </button>
                </div>
                <input
                  value={infoForm.address}
                  placeholder="기본주소"
                  readOnly
                />
                <input
                  id="profile-detail-address"
                  value={infoForm.detailAddress}
                  placeholder="나머지 주소"
                  onChange={(event) => updateInfoField("detailAddress", event.target.value)}
                />
              </div>
            </div>
            {message && <p className="form-message profile-form-message">{message}</p>}
            {actionError && <p className="form-error profile-form-message">{actionError}</p>}
            <button className="profile-submit-button" type="submit">수정하기</button>
          </form>

          <div className="account-actions">
            <button type="button" disabled={passwordChangeDisabled} onClick={openPasswordChange}>
              비밀번호 변경
            </button>
            <button type="button" onClick={() => setDeactivateConfirmOpen(true)}>
              회원탈퇴
            </button>
          </div>

          {deactivateConfirmOpen && (
            <div className="cart-notice-backdrop" role="presentation" onClick={() => setDeactivateConfirmOpen(false)}>
              <section
                className="cart-notice deactivate-confirm-modal"
                role="dialog"
                aria-modal="true"
                aria-label="회원탈퇴 확인"
                onClick={(event) => event.stopPropagation()}
              >
                <p>회원탈퇴를 진행하시겠습니까?</p>
                <small>탈퇴 후에는 계정 이용이 제한됩니다.</small>
                <div>
                  <button type="button" onClick={() => setDeactivateConfirmOpen(false)}>
                    취소
                  </button>
                  <button type="button" onClick={() => void handleDeactivate()}>
                    탈퇴하기
                  </button>
                </div>
              </section>
            </div>
          )}

          {passwordChangeStep === "newPassword" && (
            <div className="cart-notice-backdrop" role="presentation" onClick={closePasswordChange}>
              <form
                className="cart-notice password-change-modal"
                role="dialog"
                aria-modal="true"
                aria-label="새 비밀번호 입력"
                onClick={(event) => event.stopPropagation()}
                onSubmit={handlePasswordNext}
              >
                <p>새 비밀번호를 입력해주세요.</p>
                <input
                  type="password"
                  value={passwordForm.newPassword}
                  placeholder="새 비밀번호"
                  onChange={(event) =>
                    setPasswordForm((prevForm) => ({
                      ...prevForm,
                      newPassword: event.target.value,
                    }))
                  }
                  autoFocus
                />
                <input
                  type="password"
                  value={passwordForm.newPasswordConfirm}
                  placeholder="새 비밀번호 확인"
                  onChange={(event) =>
                    setPasswordForm((prevForm) => ({
                      ...prevForm,
                      newPasswordConfirm: event.target.value,
                    }))
                  }
                />
                {passwordModalError && <span className="private-password-help">{passwordModalError}</span>}
                <div>
                  <button type="submit">변경하기</button>
                  <button type="button" onClick={closePasswordChange}>
                    닫기
                  </button>
                </div>
              </form>
            </div>
          )}

          {passwordChangeStep === "currentPassword" && (
            <div className="cart-notice-backdrop" role="presentation" onClick={closePasswordChange}>
              <form
                className="cart-notice password-change-modal"
                role="dialog"
                aria-modal="true"
                aria-label="현재 비밀번호 입력"
                onClick={(event) => event.stopPropagation()}
                onSubmit={(event) => void handleUpdatePassword(event)}
              >
                <p>현재 비밀번호를 입력해주세요.</p>
                <input
                  type="password"
                  value={passwordForm.currentPassword}
                  placeholder="현재 비밀번호"
                  onChange={(event) =>
                    setPasswordForm((prevForm) => ({
                      ...prevForm,
                      currentPassword: event.target.value,
                    }))
                  }
                  autoFocus
                />
                {passwordModalError && <span className="private-password-help">{passwordModalError}</span>}
                <div>
                  <button type="submit">변경하기</button>
                  <button type="button" onClick={closePasswordChange}>
                    닫기
                  </button>
                </div>
              </form>
            </div>
          )}

          {profilePostcodeOpen && (
            <PostcodeModal
              onComplete={(data) => {
                setInfoForm((prevForm) => ({
                  ...prevForm,
                  zipCode: data.zonecode,
                  address: data.roadAddress || data.jibunAddress || data.address,
                }));
                setProfilePostcodeOpen(false);

                window.setTimeout(() => {
                  document.getElementById("profile-detail-address")?.focus();
                }, 0);
              }}
              onClose={() => setProfilePostcodeOpen(false)}
            />
          )}
        </section>
      )}
    </section>
  );
}

function CustomerToast({ message, onClose }: { message: string; onClose: () => void }) {
  useEffect(() => {
    if (!message) {
      return;
    }

    const timerId = window.setTimeout(onClose, 2200);

    return () => {
      window.clearTimeout(timerId);
    };
  }, [message, onClose]);

  if (!message) {
    return null;
  }

  return (
    <div className="customer-toast" role="status" aria-live="polite">
      {message}
    </div>
  );
}

function AuthPage({ onLoginSuccess }: AuthPageProps) {
  const [mode, setMode] = useState<AuthMode>("login");
  const [signupCompleted, setSignupCompleted] = useState(
    () =>
      import.meta.env.DEV &&
      new URLSearchParams(window.location.search).get("authPreview") === "complete"
  );
  const [completedMemberName, setCompletedMemberName] = useState(() =>
    import.meta.env.DEV &&
    new URLSearchParams(window.location.search).get("authPreview") === "complete"
      ? "EARTHY"
      : ""
  );
  const [form, setForm] = useState<AuthForm>({
    email: "",
    password: "",
    name: "",
    phone: "",
  });
  const [emailFindForm, setEmailFindForm] = useState<EmailFindForm>({
    name: "",
    phone: "",
  });
  const [passwordFindEmail, setPasswordFindEmail] = useState("");
  const [passwordConfirm, setPasswordConfirm] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const [message, setMessage] = useState<string | null>(null);
  const [emailFindResults, setEmailFindResults] = useState<EmailFindAccount[]>([]);
  const [socialPasswordFindOpen, setSocialPasswordFindOpen] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [serviceTermsOpen, setServiceTermsOpen] = useState(false);
  const [privacyTermsOpen, setPrivacyTermsOpen] = useState(false);
  const [marketingTermsOpen, setMarketingTermsOpen] = useState(false);
  const [agreements, setAgreements] = useState({
    terms: false,
    privacy: false,
    marketing: false,
  });

  const allAgreed = agreements.terms && agreements.privacy && agreements.marketing;

  useEffect(() => {
    if (!serviceTermsOpen && !privacyTermsOpen && !marketingTermsOpen) {
      return;
    }

    const previousBodyOverflow = document.body.style.overflow;
    const previousHtmlOverflow = document.documentElement.style.overflow;

    document.body.style.overflow = "hidden";
    document.documentElement.style.overflow = "hidden";

    return () => {
      document.body.style.overflow = previousBodyOverflow;
      document.documentElement.style.overflow = previousHtmlOverflow;
    };
  }, [serviceTermsOpen, privacyTermsOpen, marketingTermsOpen]);

  const updateField = (field: keyof AuthForm, value: string) => {
    setForm((prevForm) => ({
      ...prevForm,
      [field]: value,
    }));
  };

  const formatPhone = (value: string) => {
    const numbers = value.replace(/\D/g, "").slice(0, 11);

    return numbers.length > 7
      ? `${numbers.slice(0, 3)}-${numbers.slice(3, 7)}-${numbers.slice(7)}`
      : numbers.length > 3
        ? `${numbers.slice(0, 3)}-${numbers.slice(3)}`
        : numbers;
  };

  const updatePhone = (value: string) => {
    updateField("phone", formatPhone(value));
  };

  const updateEmailFindPhone = (value: string) => {
    setEmailFindForm((prevForm) => ({
      ...prevForm,
      phone: formatPhone(value),
    }));
  };

  const moveAuthMode = (nextMode: AuthMode) => {
    setMode(nextMode);
    setMessage(null);
    setEmailFindResults([]);
    setSocialPasswordFindOpen(false);
    setError(null);
  };

  const updateAllAgreements = (checked: boolean) => {
    setAgreements({
      terms: checked,
      privacy: checked,
      marketing: checked,
    });
  };

  const updateAgreement = (field: keyof typeof agreements, checked: boolean) => {
    setAgreements((prevAgreements) => ({
      ...prevAgreements,
      [field]: checked,
    }));
  };

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setSubmitting(true);
    setMessage(null);
    setEmailFindResults([]);
    setSocialPasswordFindOpen(false);
    setError(null);

    try {
      if (!form.email.trim()) {
        setError("이메일을 입력해주세요.");
        return;
      }

      if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.email)) {
        setError("이메일 형식으로 입력해주세요.");
        return;
      }

      if (!form.password) {
        setError("비밀번호를 입력해주세요.");
        return;
      }

      if (mode === "signup") {
        if (form.password !== passwordConfirm) {
          setError("비밀번호가 일치하지 않습니다.");
          return;
        }

        if (!form.name.trim()) {
          setError("이름을 입력해주세요.");
          return;
        }

        if (!/^010-[0-9]{4}-[0-9]{4}$/.test(form.phone)) {
          setError("연락처는 010-0000-0000 형식으로 입력해주세요.");
          return;
        }

        if (!agreements.terms || !agreements.privacy) {
          setError("필수 약관에 동의해주세요.");
          return;
        }

        await signup({
          ...form,
          termsAgreed: agreements.terms,
          privacyAgreed: agreements.privacy,
          marketingAgreed: agreements.marketing,
        });
        setCompletedMemberName(form.name.trim());
        setSignupCompleted(true);
        setMessage(null);
        setForm({
          email: "",
          password: "",
          name: "",
          phone: "",
        });
        setPasswordConfirm("");
        setAgreements({
          terms: false,
          privacy: false,
          marketing: false,
        });
        return;
      }

      const loginRequest: LoginRequest = {
        email: form.email,
        password: form.password,
      };
      const response = await login(loginRequest);
      onLoginSuccess(response);
    } catch (submitError) {
      console.error("[AUTH SUBMIT ERROR]", submitError);
      const errorMessage = submitError instanceof Error ? submitError.message : "요청 실패";
      setError(
        errorMessage.includes("expected pattern")
          ? "서버 연결을 확인해주세요. 백엔드와 프론트 실행 주소가 맞아야 합니다."
          : errorMessage
      );
    } finally {
      setSubmitting(false);
    }
  };

  const handleFindEmailSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setSubmitting(true);
    setMessage(null);
    setEmailFindResults([]);
    setError(null);

    try {
      if (!emailFindForm.name.trim()) {
        setError("이름을 입력해주세요.");
        return;
      }

      if (!EMAIL_FIND_PHONE_PATTERN.test(emailFindForm.phone)) {
        setError("연락처 형식을 확인해주세요.");
        return;
      }

      const response = await findEmail(emailFindForm);
      setEmailFindResults(
        response.accounts && response.accounts.length > 0
          ? response.accounts
          : [
              {
                email: response.email,
                provider: response.provider,
                providerDescription: response.providerDescription,
              },
            ]
      );
    } catch (submitError) {
      const errorMessage = submitError instanceof Error ? submitError.message : "요청 실패";
      setError(errorMessage);
    } finally {
      setSubmitting(false);
    }
  };

  const handleFindPasswordSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setSubmitting(true);
    setMessage(null);
    setEmailFindResults([]);
    setError(null);

    try {
      if (!passwordFindEmail.trim()) {
        setError("이메일을 입력해주세요.");
        return;
      }

      if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(passwordFindEmail)) {
        setError("이메일 형식으로 입력해주세요.");
        return;
      }

      await findPassword({ email: passwordFindEmail });
      setMessage("임시비밀번호를 이메일로 보내드렸습니다.\n로그인 후 반드시 비밀번호를 변경해주세요.");
      setPasswordFindEmail("");
    } catch (submitError) {
      const errorMessage = submitError instanceof Error ? submitError.message : "요청 실패";
      if (errorMessage.includes("소셜 로그인 회원은 비밀번호를 변경할 수 없습니다")) {
        setError(null);
        setSocialPasswordFindOpen(true);
        return;
      }

      setError(errorMessage);
    } finally {
      setSubmitting(false);
    }
  };

  const moveToLogin = () => {
    setSignupCompleted(false);
    setCompletedMemberName("");
    setMode("login");
    setMessage(null);
    setEmailFindResults([]);
    setSocialPasswordFindOpen(false);
    setError(null);
  };

  if (signupCompleted) {
    return (
      <section className="page-view auth-view auth-complete-view">
        <div className="auth-panel auth-complete-panel">
          <h1>{completedMemberName}님!</h1>
          <p>EARTHY 회원이 되신 걸 환영합니다!</p>
          <button className="auth-switch-button" type="button" onClick={moveToLogin}>
            로그인 화면으로 이동
          </button>
        </div>
      </section>
    );
  }

  if (mode === "findEmail") {
    return (
      <section className="page-view auth-view">
        <div className="auth-panel">
          <h1 className="auth-title">이메일 찾기</h1>

          <form className="auth-form" onSubmit={handleFindEmailSubmit} noValidate>
            <label>
              이름
              <input
                value={emailFindForm.name}
                onChange={(event) =>
                  setEmailFindForm((prevForm) => ({
                    ...prevForm,
                    name: event.target.value,
                  }))
                }
                required
              />
            </label>

            <label>
              연락처
              <input
                type="tel"
                value={emailFindForm.phone}
                placeholder="000-0000-0000"
                inputMode="numeric"
                maxLength={13}
                onChange={(event) => updateEmailFindPhone(event.target.value)}
                required
              />
            </label>

            {error && <p className="form-error">{error}</p>}

            <button type="submit" disabled={submitting}>
              {submitting ? "처리 중" : "이메일 찾기"}
            </button>
          </form>

          <button className="auth-switch-button" type="button" onClick={() => moveAuthMode("login")}>
            로그인 화면으로 이동
          </button>
        </div>

        {emailFindResults.length > 0 && (
          <div className="email-find-modal-backdrop" role="presentation">
            <section className="email-find-modal" role="dialog" aria-modal="true">
              <p>가입된 이메일</p>
              <ul className="email-find-result-list">
                {emailFindResults.map((account) => (
                  <li key={`${account.provider}-${account.email}`}>
                    <strong>{account.email}</strong>
                    <span>{account.providerDescription}</span>
                  </li>
                ))}
              </ul>
              <div>
                <button type="button" onClick={moveToLogin}>
                  확인
                </button>
              </div>
            </section>
          </div>
        )}
      </section>
    );
  }

  if (mode === "findPassword") {
    return (
      <section className="page-view auth-view">
        <div className="auth-panel">
          <h1 className="auth-title">비밀번호 찾기</h1>

          <form className="auth-form" onSubmit={handleFindPasswordSubmit} noValidate>
            <label>
              이메일
              <input
                type="email"
                value={passwordFindEmail}
                onChange={(event) => setPasswordFindEmail(event.target.value)}
                required
              />
            </label>

            {error && <p className="form-error">{error}</p>}

            <button type="submit" disabled={submitting}>
              {submitting ? "처리 중" : "비밀번호 찾기"}
            </button>
          </form>

          <button className="auth-switch-button" type="button" onClick={() => moveAuthMode("login")}>
            로그인 화면으로 이동
          </button>
        </div>

        {message && (
          <div className="email-find-modal-backdrop" role="presentation">
            <section className="email-find-modal" role="dialog" aria-modal="true">
              <p>임시비밀번호 발급 완료</p>
              <strong>{message}</strong>
              <div>
                <button type="button" onClick={moveToLogin}>
                  확인
                </button>
              </div>
            </section>
          </div>
        )}

        {socialPasswordFindOpen && (
          <div className="email-find-modal-backdrop" role="presentation">
            <section className="email-find-modal" role="dialog" aria-modal="true">
              <strong>{"카카오 로그인으로 가입된 계정입니다.\n카카오 로그인을 이용해 주세요."}</strong>
              <div>
                <button type="button" onClick={() => setSocialPasswordFindOpen(false)}>
                  확인
                </button>
              </div>
            </section>
          </div>
        )}
      </section>
    );
  }

  return (
    <section className="page-view auth-view">
      <div className="auth-panel">
        <h1 className="auth-title">{mode === "login" ? "LOGIN" : "회원가입"}</h1>

        <form className="auth-form" onSubmit={handleSubmit} noValidate>
          <label>
            이메일
            <input
              type="email"
              value={form.email}
              onChange={(event) => updateField("email", event.target.value)}
              required
            />
            {mode === "signup" && <small>이메일 형식으로 입력해주세요.</small>}
          </label>

          <label>
            비밀번호
            <input
              type="password"
              value={form.password}
              onChange={(event) => updateField("password", event.target.value)}
              required
            />
            {mode === "signup" && (
              <small>영문 대소문자/숫자/특수문자 중 2가지 이상 조합, 10~16자</small>
            )}
          </label>

          {mode === "signup" && (
            <>
              <label>
                비밀번호 확인
                <input
                  type="password"
                  value={passwordConfirm}
                  onChange={(event) => setPasswordConfirm(event.target.value)}
                  required
                />
              </label>
              <label>
                이름
                <input
                  value={form.name}
                  onChange={(event) => updateField("name", event.target.value)}
                  required
                />
              </label>
              <label>
                연락처
                <input
                  type="tel"
                  value={form.phone}
                  placeholder="010-0000-0000"
                  inputMode="numeric"
                  maxLength={13}
                  onChange={(event) => updatePhone(event.target.value)}
                  required
                />
              </label>
            </>
          )}

          {mode === "signup" && (
            <div className="agreement-box">
              <div className="agreement-row">
                <input
                  type="checkbox"
                  checked={allAgreed}
                  onChange={(event) => updateAllAgreements(event.target.checked)}
                />
                <span>전체동의</span>
              </div>

              <div className="agreement-row agreement-row-with-detail">
                <input
                  type="checkbox"
                  checked={agreements.terms}
                  onChange={(event) => updateAgreement("terms", event.target.checked)}
                  required
                />
                <span>
                  <strong>이용약관</strong> 동의 (필수)
                </span>
                <button
                  className="agreement-detail-button"
                  type="button"
                  onClick={() => setServiceTermsOpen(true)}
                >
                  자세히
                </button>
              </div>

              <div className="agreement-row agreement-row-with-detail">
                <input
                  type="checkbox"
                  checked={agreements.privacy}
                  onChange={(event) => updateAgreement("privacy", event.target.checked)}
                  required
                />
                <span>
                  <strong>개인정보 수집 및 이용</strong> 동의 (필수)
                </span>
                <button
                  className="agreement-detail-button"
                  type="button"
                  onClick={() => setPrivacyTermsOpen(true)}
                >
                  자세히
                </button>
              </div>

              <div className="agreement-row agreement-row-with-detail">
                <input
                  type="checkbox"
                  checked={agreements.marketing}
                  onChange={(event) => updateAgreement("marketing", event.target.checked)}
                />
                <span>
                  마케팅 정보 수신 동의 (선택)
                </span>
                <button
                  className="agreement-detail-button"
                  type="button"
                  onClick={() => setMarketingTermsOpen(true)}
                >
                  자세히
                </button>
              </div>
            </div>
          )}

          {message && <p className="form-message">{message}</p>}
          {error && <p className="form-error">{error}</p>}

          <button type="submit" disabled={submitting}>
            {submitting ? "처리 중" : mode === "login" ? "로그인" : "가입하기"}
          </button>
        </form>

        {mode === "login" && (
          <>
            <button className="auth-switch-button" type="button" onClick={() => moveAuthMode("signup")}>
              회원가입
            </button>
            <button className="kakao-login-button" type="button" onClick={() => window.location.href = getKakaoLoginUrl()}>
              카카오 1초 로그인/회원가입
            </button>
            <div className="auth-help-links">
              <button type="button" onClick={() => moveAuthMode("findEmail")}>
                이메일 찾기
              </button>
              <span>/</span>
              <button type="button" onClick={() => moveAuthMode("findPassword")}>
                비밀번호 찾기
              </button>
            </div>
          </>
        )}
      </div>

      {serviceTermsOpen && (
        <PaymentTermsModal
          title="이용약관"
          termsText={serviceTermsText}
          onClose={() => setServiceTermsOpen(false)}
        />
      )}
      {privacyTermsOpen && (
        <PaymentTermsModal
          title="개인정보 수집 및 이용 동의"
          termsText={privacyCollectionText}
          onClose={() => setPrivacyTermsOpen(false)}
        />
      )}
      {marketingTermsOpen && (
        <PaymentTermsModal
          title="마케팅 정보 수신 동의"
          termsText={marketingConsentText}
          onClose={() => setMarketingTermsOpen(false)}
        />
      )}
    </section>
  );
}

const ABOUT_ARCHIVE_IMAGES = [
  {
    src: "/assets/about-panorama/panorama-01.jpeg",
    size: "is-medium",
    ratio: "is-ratio-wide",
    offset: "is-offset-high",
    date: "AUGUST, 2026",
    caption: "A MOMENT WE KEPT",
  },
  {
    src: "/assets/about-panorama/panorama-02.jpeg",
    size: "is-wide",
    ratio: "is-ratio-cinema",
    offset: "is-offset-low",
    date: "SUMMER LIGHT",
    caption: "SLOW ARCHIVE",
  },
  {
    src: "/assets/about-panorama/panorama-04.jpeg",
    size: "is-medium",
    ratio: "is-ratio-wide",
    offset: "is-offset-low",
    date: "FLOWER FIELD",
    caption: "COLOR KEPT",
  },
  {
    src: "/assets/about-panorama/panorama-05.jpeg",
    size: "is-wide",
    ratio: "is-ratio-cinema",
    offset: "is-offset-high",
    date: "QUIET SHADE",
    caption: "FOREST FRAME",
  },
  {
    src: "/assets/about-panorama/panorama-08.jpeg",
    size: "is-medium",
    ratio: "is-ratio-wide",
    offset: "is-offset-low",
    date: "COAST MEMORY",
    caption: "SEASON RECORD",
  },
  {
    src: "/assets/about-panorama/panorama-09.jpeg",
    size: "is-wide",
    ratio: "is-ratio-cinema",
    offset: "is-offset-high",
    date: "WIND HILL",
    caption: "EARTHY ARCHIVE",
  },
  {
    src: "/assets/about-panorama/panorama-10.jpeg",
    size: "is-narrow",
    ratio: "is-ratio-classic",
    offset: "is-offset-low",
    date: "GREENHOUSE",
    caption: "TENDER DETAIL",
  },
  {
    src: "/assets/about-panorama/panorama-11.jpeg",
    size: "is-wide",
    ratio: "is-ratio-wide",
    offset: "is-offset-high",
    date: "TREE LINE",
    caption: "STILL AFTERNOON",
  },
];

function About() {
  const archiveFlowImages = [...ABOUT_ARCHIVE_IMAGES, ...ABOUT_ARCHIVE_IMAGES];

  return (
    <section className="page-view about-page">
      <div className="about-view">
        <div>
          <span>01 / ABOUT EARTHY</span>
          <h1>
            Nature,
            <br />
            remembered.
            <br />
            자연을 오래 간직하는 방법.
          </h1>
        </div>
        <p>
          계절이 지나면 다시 만날 수 없는 풍경, 바람의 결, 빛의 온도, 숲의 숨결을 사진으로 담아
          엽서와 포스터, 포토북으로 전합니다.
          <br />
          <br />
          자연을 오래 곁에 두는 가장 작은 방법 EARTHY.
        </p>
      </div>

      <div
        className="about-archive"
        aria-label="EARTHY 풍경 사진 아카이브"
        onContextMenu={preventProtectedImageAreaContextMenu}
      >
        <div className="about-archive-window">
          <div className="about-archive-track photo-track">
            {archiveFlowImages.map((image, index) => (
              <figure
                aria-hidden={index >= ABOUT_ARCHIVE_IMAGES.length}
                className={`about-archive-item ${image.size} ${image.ratio} ${image.offset}`}
                key={`${image.src}-${index}`}
              >
                <div className="about-archive-image-frame">
                  <img
                    {...protectedImageProps()}
                    src={image.src}
                    alt={index >= ABOUT_ARCHIVE_IMAGES.length ? "" : `EARTHY 아카이브 사진 ${index + 1}`}
                  />
                </div>
                <figcaption>
                  <span>{String((index % ABOUT_ARCHIVE_IMAGES.length) + 1).padStart(2, "0")} / EARTHY ARCHIVE</span>
                  <span>{image.date}</span>
                </figcaption>
              </figure>
            ))}
          </div>
        </div>
      </div>
    </section>
  );
}

function BusinessFooter() {
  return (
    <footer className="business-footer">
      <div className="business-footer-links" aria-label="소셜 및 문의 링크">
        <a
          href="https://www.instagram.com/earthy_official_?igsi=MWVocndobmN0ajQxMA=="
          target="_blank"
          rel="noreferrer"
          aria-label="EARTHY 인스타그램 새 탭으로 열기"
        >
          <svg viewBox="0 0 24 24" aria-hidden="true">
            <rect x="4" y="4" width="16" height="16" rx="5" />
            <circle cx="12" cy="12" r="3.4" />
            <circle cx="16.5" cy="7.5" r="0.8" />
          </svg>
        </a>
        <a href="mailto:earthy9194@gmail.com" aria-label="EARTHY 이메일 보내기">
          <svg viewBox="0 0 24 24" aria-hidden="true">
            <rect x="4" y="6" width="16" height="12" rx="1.5" />
            <path d="m5 7 7 6 7-6" />
          </svg>
        </a>
      </div>
      <p>
        <span>
          <span className="business-footer-label">COMPANY</span> EARTHY STUDIO
        </span>
        <span>
          <span className="business-footer-label">ADDRESS</span> 부산광역시 남구 수영로 312, 21센츄리오피스텔 611호
        </span>
      </p>
      <p>
        <span>
          <span className="business-footer-label">BUSINESS NO.</span> 877-05-02984
        </span>
        <span>
          <span className="business-footer-label">E-MAIL</span> earthy9194@gmail.com
        </span>
      </p>
    </footer>
  );
}

export default App;
