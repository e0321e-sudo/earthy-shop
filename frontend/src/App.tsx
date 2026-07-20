import { useEffect, useState, type FormEvent } from "react";
import {
  login,
  logout as requestLogout,
  signup,
  type LoginRequest,
  type LoginResponse,
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
import { getProduct, getProductsPage, type PageResponse as ProductPageResponse } from "./api/products";
import { requestTossPayment } from "./api/toss";
import {
  categoryTabs,
  products as fallbackProducts,
  type Addon,
  type Product,
  type ProductCategory,
} from "./data/products";

const DAUM_POSTCODE_SCRIPT_URL = "//t1.daumcdn.net/mapjsapi/bundle/postcode/prod/postcode.v2.js";
const FREE_DELIVERY_MIN_AMOUNT = 30000;
const BASE_DELIVERY_FEE = 2500;
const REMOTE_AREA_DELIVERY_FEE = 500;
const PHONE_PREFIXES = ["010", "011", "016", "017", "018", "019"];
const DELIVERY_MEMO_OPTIONS = [
  "배송 전에 미리 연락바랍니다.",
  "부재 시 경비실에 맡겨주세요.",
  "부재 시 문 앞에 놓아주세요.",
  "빠른 배송 부탁드립니다.",
  "택배함에 보관해 주세요.",
];
const CUSTOM_DELIVERY_MEMO = "직접 입력";

// 다음 우편번호 검색 응답
interface DaumPostcodeData {
  zonecode: string;
  roadAddress: string;
  jibunAddress: string;
  address: string;
}

interface DaumPostcodeInstance {
  open: () => void;
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

type Page = "home" | "shop" | "detail" | "about" | "cart" | "checkout" | "paymentResult" | "auth" | "mypage";
type AuthMode = "login" | "signup";
type MyPageView = "home" | "orders" | "orderDetail" | "profile";
type PaymentResultState =
  | { status: "processing"; message: string }
  | { status: "success"; message: string; payment: PaymentResponse }
  | { status: "fail"; message: string; retryOrder?: OrderResponse; showCancelNotice?: boolean };

interface HeaderProps {
  page: Page;
  category: ProductCategory;
  cartCount: number;
  cartBumped: boolean;
  loggedIn: boolean;
  onHome: () => void;
  onCategory: (category: ProductCategory) => void;
  onAbout: () => void;
  onAuth: () => void;
  onCart: () => void;
}

interface ShopProps {
  category: ProductCategory;
  products: Product[];
  pageInfo: ProductPageResponse<Product>;
  loading: boolean;
  error: string | null;
  onOpenDetail: (productId: number) => void;
  onChangePage: (page: number) => void;
}

interface ProductDetailProps {
  product: Product | null;
  loading: boolean;
  error: string | null;
  onBack: () => void;
  onAddToCart: (requestBody: CartItemAddRequest) => Promise<void>;
  onBuyNow: (requestBody: CartItemAddRequest) => Promise<void>;
}

interface QuantityControlProps {
  label: string;
  value: number;
  onChange: (value: number) => void;
}

interface CartProps {
  items: CartItemResponse[];
  totalPrice: number;
  cartCount: number;
  loading: boolean;
  error: string | null;
  onOpenDetail: (productId: number) => void;
  onUpdateQuantity: (cartItemId: number, quantity: number, addonQuantity: number | null) => void;
  onRemove: (cartItemId: number) => void;
  onCheckout: (cartItemIds?: number[]) => void;
}

interface CheckoutProps {
  totalPrice: number;
  member: MemberResponse | null;
  onMemberLoaded: (member: MemberResponse) => void;
  onCreateOrder: (requestBody: OrderCreateRequest) => Promise<OrderResponse>;
}

interface PaymentResultProps {
  result: PaymentResultState;
  onRetryPayment: (order: OrderResponse) => Promise<void>;
  onMoveOrders: () => void;
  onMoveCart: () => void;
}

interface MyPageProps {
  member: MemberResponse | null;
  orders: OrderResponse[];
  orderPageInfo: OrderPageResponse<OrderResponse>;
  loading: boolean;
  error: string | null;
  onChangeOrderPage: (page: number) => void;
  onUpdateInfo: (name: string, phone: string) => Promise<void>;
  onUpdatePassword: (currentPassword: string, newPassword: string) => Promise<void>;
  onCancelOrder: (orderId: number, cancelReason: string) => Promise<void>;
  onDeactivate: () => Promise<void>;
  onLogout: () => void;
}

interface AuthPageProps {
  onLoginSuccess: (loginResponse: LoginResponse) => void;
}

// 금액 표기
const formatWon = (value: number) => `${value.toLocaleString("ko-KR")}원`;

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

function App() {
  // 화면 전환 상태
  const [page, setPage] = useState<Page>("home");
  const [category, setCategory] = useState<ProductCategory>("ALL");
  const [selectedProductId, setSelectedProductId] = useState<number | null>(null);

  // 장바구니 상태
  const [cartItems, setCartItems] = useState<CartItemResponse[]>([]);
  const [cartLoading, setCartLoading] = useState(false);
  const [cartError, setCartError] = useState<string | null>(null);

  // 상품 상태
  const [products, setProducts] = useState<Product[]>(fallbackProducts);
  const [productPageInfo, setProductPageInfo] = useState<ProductPageResponse<Product>>(() => createEmptyPage<Product>());
  const [productPage, setProductPage] = useState(0);
  const [productLoading, setProductLoading] = useState(false);
  const [productError, setProductError] = useState<string | null>(null);
  const [productDetail, setProductDetail] = useState<Product | null>(null);
  const [productDetailLoading, setProductDetailLoading] = useState(false);
  const [productDetailError, setProductDetailError] = useState<string | null>(null);

  // 인증/마이페이지 상태
  const [accessToken, setAccessToken] = useState(() => localStorage.getItem("earthyAccessToken"));
  const [authPageKey, setAuthPageKey] = useState(0);
  const [myPageKey, setMyPageKey] = useState(0);
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

  useEffect(() => {
    setProductPage(0);
  }, [category]);

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
      setProductError(null);

      try {
        const data = await getProductsPage(category, productPage);

        if (!ignore) {
          setProducts(data.content);
          setProductPageInfo(data);
        }
      } catch (error) {
        if (!ignore) {
          setProducts(
            category === "ALL"
              ? fallbackProducts
              : fallbackProducts.filter((product) => product.category === category)
          );
          setProductPageInfo(createEmptyPage<Product>());
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
  }, [category, page, productPage]);

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
          const fallbackProduct = [...products, ...fallbackProducts].find(
            (product) => product.id === productId
          );

          setProductDetail(fallbackProduct ?? null);
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
  }, [page, selectedProductId, products]);

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

  // 토스 결제 결과 콜백 처리
  useEffect(() => {
    const params = new URLSearchParams(window.location.search);
    const result = params.get("paymentResult");

    if (result !== "success" && result !== "fail") {
      return;
    }

    const cleanPaymentUrl = () => {
      window.history.replaceState({}, "", window.location.pathname);
    };

    const paymentKey = params.get("paymentKey");
    const tossOrderNumber = params.get("orderId");
    const amount = params.get("amount");
    const storedOrderId = tossOrderNumber ? sessionStorage.getItem(`earthyPaymentOrder:${tossOrderNumber}`) : null;
    const storedRetryOrder = getStoredPaymentOrder(tossOrderNumber);

    if (result === "fail") {
      const failedOrderId = storedOrderId ? Number(storedOrderId) : null;

      setPaymentResult({
        status: "fail",
        message: params.get("message") ?? "결제가 취소되었거나 실패했습니다.",
        retryOrder: storedRetryOrder,
        showCancelNotice: true,
      });
      setPage("paymentResult");
      cleanPaymentUrl();

      if (failedOrderId) {
        void getMyOrder(failedOrderId)
          .then((retryOrder) => {
            setPaymentResult({
              status: "fail",
              message: params.get("message") ?? "결제가 취소되었거나 실패했습니다.",
              retryOrder,
              showCancelNotice: true,
            });
          })
          .catch(() => {
            setPaymentResult({
              status: "fail",
              message: storedRetryOrder
                ? params.get("message") ?? "결제가 취소되었거나 실패했습니다."
                : "결제가 취소되었습니다. 주문 내역에서 다시 확인해주세요.",
              retryOrder: storedRetryOrder,
              showCancelNotice: true,
            });
          });
      }

      return;
    }

    if (!paymentKey || !tossOrderNumber || !amount || !storedOrderId) {
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
    const confirmedTossOrderNumber = tossOrderNumber;

    setPaymentResult({ status: "processing", message: "결제 승인 중입니다." });
    setPage("paymentResult");
    cleanPaymentUrl();

    // 토스 결제 승인
    async function approvePayment() {
      try {
        const payment = await confirmPayment({
          orderId: confirmedOrderId,
          paymentKey: confirmedPaymentKey,
          amount: confirmedAmount,
        });

        sessionStorage.removeItem(`earthyPaymentOrder:${confirmedTossOrderNumber}`);
        sessionStorage.removeItem(`earthyPaymentOrderData:${confirmedTossOrderNumber}`);
        setCartItems([]);
        await Promise.all([loadCart(), loadMyPage()]);
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
    setPage("home");
    setSelectedProductId(null);
    setProductDetail(null);
    window.scrollTo({ top: 0, behavior: "smooth" });
  };

  // 카테고리 이동
  const openCategory = (nextCategory: ProductCategory) => {
    setCategory(nextCategory);
    setPage("shop");
    setSelectedProductId(null);
    setProductDetail(null);
    window.scrollTo({ top: 0, behavior: "smooth" });
  };

  // 상품 상세 이동
  const openDetail = (productId: number) => {
    setSelectedProductId(productId);
    setProductDetail(null);
    setPage("detail");
    window.scrollTo({ top: 0, behavior: "smooth" });
  };

  // 로그인 필요 화면 진입 검증
  const requireLogin = () => {
    if (accessToken) {
      return true;
    }

    setAuthPageKey((key) => key + 1);
    setPage("auth");
    window.scrollTo({ top: 0, behavior: "smooth" });
    return false;
  };

  // 장바구니 담기
  const addToCart = async (requestBody: CartItemAddRequest) => {
    if (!requireLogin()) {
      return;
    }

    const cart = await addCartItem(requestBody);
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

    const cart = await addCartItem(requestBody);
    setCartItems(cart.items);

    if (accessToken) {
      setCheckoutCartItemIds(undefined);
      setPage("checkout");
      window.scrollTo({ top: 0, behavior: "smooth" });
    }
  };

  // 장바구니 수량 변경
  const updateCartQuantity = async (
    cartItemId: number,
    quantity: number,
    addonQuantity: number | null
  ) => {
    const cart = await updateCartItemQuantity(cartItemId, { quantity, addonQuantity });
    setCartItems(cart.items);
  };

  // 장바구니 상품 삭제
  const removeCartItem = async (cartItemId: number) => {
    const cart = await deleteCartItem(cartItemId);
    setCartItems(cart.items);
  };

  // 주문 생성
  const submitOrder = async (requestBody: OrderCreateRequest) => {
    const order = await createOrder({
      ...requestBody,
      cartItemIds: checkoutCartItemIds,
    });
    return order;
  };

  // 결제 재시도
  const retryPayment = async (order: OrderResponse) => {
    setPaymentResult({ status: "processing", message: "결제창을 다시 여는 중입니다." });

    try {
      await requestTossPayment(order);
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
      setMyPageKey((key) => key + 1);
      setPage("mypage");
      window.scrollTo({ top: 0, behavior: "smooth" });
      return;
    }

    setAuthPageKey((key) => key + 1);
    setPage("auth");
  };

  // 장바구니 화면 이동
  const openCart = () => {
    setCartNoticeOpen(false);
    setPage("cart");
    window.scrollTo({ top: 0, behavior: "smooth" });
  };

  // 회원 정보 수정
  const saveMyInfo = async (name: string, phone: string) => {
    const updatedMember = await updateMyInfo({ name, phone });
    setMember(updatedMember);
  };

  // 회원 비밀번호 변경
  const saveMyPassword = async (currentPassword: string, newPassword: string) => {
    await updateMyPassword({ currentPassword, newPassword });
  };

  // 주문 취소
  const cancelOrder = async (orderId: number, cancelReason: string) => {
    const updatedOrder = await cancelMyOrder(orderId, cancelReason);
    setOrders((prevOrders) =>
      prevOrders.map((order) => (order.orderId === orderId ? updatedOrder : order))
    );
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
    setPage("home");
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
      setPage("home");
      window.scrollTo({ top: 0, behavior: "smooth" });
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
        onAbout={() => setPage("about")}
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
            loading={productLoading}
            error={productError}
            onOpenDetail={openDetail}
            onChangePage={setProductPage}
          />
        )}
        {page === "detail" && (
          <ProductDetail
            product={productDetail}
            loading={productDetailLoading}
            error={productDetailError}
            onBack={() => openCategory(category)}
            onAddToCart={addToCart}
            onBuyNow={buyNow}
          />
        )}
        {page === "about" && <About />}
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
              setPage("checkout");
              window.scrollTo({ top: 0, behavior: "smooth" });
            }}
          />
        )}
        {page === "checkout" && (
          <Checkout
            totalPrice={checkoutTotal}
            member={member}
            onMemberLoaded={setMember}
            onCreateOrder={submitOrder}
          />
        )}
        {page === "paymentResult" && paymentResult && (
          <PaymentResult
            result={paymentResult}
            onRetryPayment={retryPayment}
            onMoveOrders={() => {
              setMyPageKey((key) => key + 1);
              setPage("mypage");
              window.scrollTo({ top: 0, behavior: "smooth" });
            }}
            onMoveCart={() => {
              setPage("cart");
              window.scrollTo({ top: 0, behavior: "smooth" });
            }}
          />
        )}
        {page === "mypage" && (
          <MyPage
            key={myPageKey}
            member={member}
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
              setPage("home");
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

      <nav className={`main-nav ${menuOpen ? "is-open" : ""}`} aria-label="주요 메뉴">
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
            ABOUT US
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
          <button type="button">NOTICE</button>
          <button type="button">BOARD</button>
        </div>
      </nav>

      <button className="brand-button" type="button" onClick={handleHome} aria-label="EARTHY home">
        <img src="/assets/earthy-logo-transparent.png" alt="EARTHY" />
      </button>

      <div className="header-actions">
        <button className="icon-button" type="button" aria-label="검색" onClick={() => setMenuOpen(false)}>
          <svg viewBox="0 0 24 24" aria-hidden="true">
            <circle cx="11" cy="11" r="7" />
            <path d="m16.5 16.5 4 4" />
          </svg>
        </button>
        <button
          className={`icon-button ${page === "auth" || page === "mypage" || loggedIn ? "is-active" : ""}`}
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
    <div className="cart-notice-backdrop" role="presentation" onClick={onClose}>
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
        <img src="/assets/field-postcard.jpeg" alt="풀 언덕 풍경 엽서" />
      </button>
    </section>
  );
}

function Shop({ category, products, pageInfo, loading, error, onOpenDetail, onChangePage }: ShopProps) {
  const title = categoryTabs.find((tab) => tab.value === category)?.label ?? "ALL";

  return (
    <section className="page-view shop-view">
      <div className="page-title">
        <span>SHOP</span>
        <h1>{title}</h1>
      </div>

      {loading && <p className="state-text">상품을 불러오는 중입니다.</p>}
      {error && <p className="state-text">백엔드 연결 전이라 임시 상품을 보여줍니다.</p>}

      <div className="product-grid">
        {products.map((product) => (
          <button
            className={`product-item ${product.soldOut ? "is-sold-out" : ""}`}
            type="button"
            key={product.id}
            onClick={() => onOpenDetail(product.id)}
          >
            <img src={product.imageUrl} alt={product.name} />
            <span>{product.categoryDescription}</span>
            <strong>{product.name}</strong>
            <small>{product.soldOut ? "SOLD OUT" : formatWon(product.price)}</small>
          </button>
        ))}
      </div>
      <Pagination pageInfo={pageInfo} onChangePage={onChangePage} />
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

  return (
    <nav className="pagination" aria-label="페이지 이동">
      <button type="button" disabled={pageInfo.first} onClick={() => onChangePage(pageInfo.page - 1)}>
        이전
      </button>
      <span>
        {pageInfo.page + 1} / {pageInfo.totalPages}
      </span>
      <button type="button" disabled={pageInfo.last} onClick={() => onChangePage(pageInfo.page + 1)}>
        다음
      </button>
    </nav>
  );
}

function ProductDetail({
  product,
  loading,
  error,
  onBack,
  onAddToCart,
  onBuyNow,
}: ProductDetailProps) {
  const [quantity, setQuantity] = useState(1);
  const [addonId, setAddonId] = useState("");
  const [addonQuantity, setAddonQuantity] = useState(1);
  const [submitError, setSubmitError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    setQuantity(1);
    setAddonId("");
    setAddonQuantity(1);
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
        <button className="text-button" type="button" onClick={onBack}>
          목록
        </button>
        <p className="state-text">{error ?? "상품을 찾을 수 없습니다."}</p>
      </section>
    );
  }

  const productAddons: Addon[] = product.addons ?? [];
  const selectedAddon = productAddons.find((addon) => addon.id === Number(addonId));
  const orderDisabled = submitting || product.soldOut || Boolean(selectedAddon?.soldOut);
  const addonTotal = selectedAddon ? selectedAddon.price * addonQuantity : 0;
  const productTotal = product.price * quantity;
  const totalPrice = productTotal + addonTotal;

  const createCartRequest = (): CartItemAddRequest => ({
      productId: product.id,
      addonId: selectedAddon?.id ?? null,
      addonQuantity: selectedAddon ? addonQuantity : null,
      quantity,
  });

  const handleCartAction = async (action: "cart" | "buy") => {
    setSubmitting(true);
    setSubmitError(null);

    if (product.soldOut) {
      setSubmitting(false);
      return;
    }

    if (selectedAddon?.soldOut) {
      setSubmitError("품절된 추가상품입니다.");
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
      setSubmitting(false);
    }
  };

  return (
    <section className="page-view detail-view">
      <button className="text-button" type="button" onClick={onBack}>
        목록
      </button>

      <div className="detail-layout">
        <img className="detail-image" src={product.imageUrl} alt={product.name} />

        <article className="detail-panel">
          <span>{product.categoryDescription}</span>
          <h1>{product.name}</h1>
          <p>{formatWon(product.price)}</p>
          <small>{product.description}</small>

          <dl className="delivery-list">
            <div>
              <dt>배송비</dt>
              <dd>3,000원 (30,000원 이상 구매 시 무료)</dd>
            </div>
            <div>
              <dt>도서산간</dt>
              <dd>제주 및 도서 산간 2,000원 추가</dd>
            </div>
          </dl>

          {product.category === "POSTER" && (
            <div className="addon-box">
              <label>
                추가상품
                <select value={addonId} onChange={(event) => setAddonId(event.target.value)}>
                  <option value="">선택 안 함</option>
                  {productAddons.map((addon) => (
                    <option value={addon.id} key={addon.id} disabled={addon.soldOut}>
                      {addon.name} +{formatWon(addon.price)}{addon.soldOut ? " SOLD OUT" : ""}
                    </option>
                  ))}
                </select>
              </label>

              {selectedAddon && (
                <QuantityControl label="추가상품 수량" value={addonQuantity} onChange={setAddonQuantity} />
              )}
            </div>
          )}

          <QuantityControl label="수량" value={quantity} onChange={setQuantity} />

          <div className="summary-box">
            <p>
              <span>주문 수량</span>
              <strong>{quantity}개</strong>
            </p>
            {selectedAddon && (
              <p>
                <span>추가상품 수량</span>
                <strong>{addonQuantity}개</strong>
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
    </section>
  );
}

function QuantityControl({ label, value, onChange }: QuantityControlProps) {
  return (
    <div className="quantity-row">
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
    quantity: number,
    addonQuantity: number | null
  ) => {
    setActionError(null);

    try {
      await onUpdateQuantity(cartItemId, quantity, addonQuantity);
    } catch (updateError) {
      setActionError(updateError instanceof Error ? updateError.message : "수량 변경 실패");
    }
  };

  // 선택상품 삭제
  const handleRemoveSelected = async () => {
    setActionError(null);

    try {
      await Promise.all(selectedItems.map((item) => onRemove(item.cartItemId)));
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
                  className="cart-product-image"
                  type="button"
                  onClick={() => onOpenDetail(item.productId)}
                  aria-label={`${item.productName} 상세보기`}
                >
                  <img src={item.productImageUrl} alt={item.productName} />
                </button>
                <div className="cart-item-info">
                  <button
                    className="cart-product-name"
                    type="button"
                    onClick={() => onOpenDetail(item.productId)}
                  >
                    {item.productName}
                  </button>
                  {item.addonName && <span>[추가상품: {item.addonName} {item.addonQuantity}개]</span>}
                  <p>{formatWon(item.itemTotalPrice)}</p>
                  {item.addonName && <small>{item.addonName} {formatWon(item.addonPrice)}</small>}
                  <div className="cart-quantity-row">
                    <span>상품</span>
                    <div className="cart-quantity">
                      <output>{item.quantity}</output>
                      <button
                        type="button"
                        onClick={() =>
                          void handleUpdateQuantity(
                            item.cartItemId,
                            item.quantity + 1,
                            item.addonId ? item.addonQuantity : null
                          )
                        }
                        aria-label={`${item.productName} 수량 늘리기`}
                      >
                        +
                      </button>
                      <button
                        type="button"
                        onClick={() =>
                          void handleUpdateQuantity(
                            item.cartItemId,
                            Math.max(1, item.quantity - 1),
                            item.addonId ? item.addonQuantity : null
                          )
                        }
                        aria-label={`${item.productName} 수량 줄이기`}
                      >
                        −
                      </button>
                    </div>
                  </div>
                  {item.addonName && (
                    <div className="cart-quantity-row">
                      <span>추가상품</span>
                      <div className="cart-quantity">
                        <output>{item.addonQuantity}</output>
                        <button
                          type="button"
                          onClick={() =>
                            void handleUpdateQuantity(
                              item.cartItemId,
                              item.quantity,
                              item.addonQuantity + 1
                            )
                          }
                          aria-label={`${item.addonName} 수량 늘리기`}
                        >
                          +
                        </button>
                        <button
                          type="button"
                          onClick={() =>
                            void handleUpdateQuantity(
                              item.cartItemId,
                              item.quantity,
                              Math.max(1, item.addonQuantity - 1)
                            )
                          }
                          aria-label={`${item.addonName} 수량 줄이기`}
                        >
                          −
                        </button>
                      </div>
                    </div>
                  )}
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

function Checkout({ totalPrice, member, onMemberLoaded, onCreateOrder }: CheckoutProps) {
  // 주문서 결제예정금액
  const deliveryFee = calculateDeliveryFee(totalPrice);
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

  const remoteAreaDeliveryFee = calculateRemoteAreaDeliveryFee(form.zipCode, form.address);
  const paymentTotal = totalPrice + deliveryFee + remoteAreaDeliveryFee;
  const allTermsAgreed = orderTermsAgreed && paymentTermsAgreed;

  // 회원 정보로 수령인 기본값 채움
  const applyMemberContact = (nextMember: MemberResponse) => {
    setForm((prevForm) => ({
      ...prevForm,
      receiverName: prevForm.receiverName || nextMember.name,
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
      return;
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

  // 우편번호 검색창 열기
  const openAddressSearch = async () => {
    setError(null);

    try {
      await loadDaumPostcodeScript();

      if (!window.daum?.Postcode) {
        throw new Error("우편번호 검색창을 사용할 수 없습니다.");
      }

      new window.daum.Postcode({
        oncomplete: (data) => {
          setForm((prevForm) => ({
            ...prevForm,
            zipCode: data.zonecode,
            address: data.roadAddress || data.jibunAddress || data.address,
          }));

          window.setTimeout(() => {
            document.getElementById("checkout-detail-address")?.focus();
          }, 0);
        },
      }).open();
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

  // 약관 모달 오픈 시 배경 스크롤 잠금
  useEffect(() => {
    if (!termsOpen) {
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
  }, [termsOpen]);

  // 주문 생성 후 토스 결제창 이동
  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    if (!allTermsAgreed) {
      setError("필수 약관에 동의해주세요.");
      return;
    }

    setSubmitting(true);
    setError(null);

    try {
      const order = await onCreateOrder(form);
      await requestTossPayment(order);
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
                  aria-label="결제대행서비스 약관 동의"
                  checked={paymentTermsAgreed}
                  type="checkbox"
                  onChange={(event) => setPaymentTermsAgreed(event.target.checked)}
                />
                <span>[필수] 결제대행서비스 약관 동의</span>
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
    </section>
  );
}

function PaymentTermsModal({
  termsText,
  onClose,
}: {
  termsText: string;
  onClose: () => void;
}) {
  return (
    <div className="terms-modal-backdrop" role="dialog" aria-modal="true" aria-labelledby="payment-terms-title">
      <div className="terms-modal">
        <h1 id="payment-terms-title">전자금융거래 이용약관</h1>
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
  const [infoForm, setInfoForm] = useState({ name: "", phone: "" });
  const [passwordForm, setPasswordForm] = useState({
    currentPassword: "",
    newPassword: "",
    newPasswordConfirm: "",
  });
  const [cancelReasons, setCancelReasons] = useState<Record<number, string>>({});
  const [cancelReasonOrderId, setCancelReasonOrderId] = useState<number | null>(null);
  const [view, setView] = useState<MyPageView>("home");
  const [selectedOrder, setSelectedOrder] = useState<OrderResponse | null>(null);
  const [message, setMessage] = useState<string | null>(null);
  const [actionError, setActionError] = useState<string | null>(null);

  useEffect(() => {
    if (member) {
      setInfoForm({ name: member.name, phone: member.phone });
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
      await onUpdateInfo(infoForm.name, infoForm.phone);

      const hasPasswordInput =
        passwordForm.currentPassword || passwordForm.newPassword || passwordForm.newPasswordConfirm;

      if (hasPasswordInput) {
        if (!passwordForm.currentPassword || !passwordForm.newPassword || !passwordForm.newPasswordConfirm) {
          setActionError("비밀번호를 변경하려면 현재 비밀번호와 새 비밀번호를 모두 입력해주세요.");
          return;
        }

        if (passwordForm.newPassword !== passwordForm.newPasswordConfirm) {
          setActionError("새 비밀번호가 일치하지 않습니다.");
          return;
        }

        await onUpdatePassword(passwordForm.currentPassword, passwordForm.newPassword);
        setPasswordForm({ currentPassword: "", newPassword: "", newPasswordConfirm: "" });
      }

      setMessage("회원 정보가 수정되었습니다.");
    } catch (updateError) {
      setActionError(updateError instanceof Error ? updateError.message : "회원 정보 수정 실패");
    }
  };

  const handleCancelOrder = async (orderId: number) => {
    setMessage(null);
    setActionError(null);

    try {
      await onCancelOrder(orderId, cancelReasons[orderId] ?? "");
      const canceledOrder = orders.find((order) => order.orderId === orderId);

      if (selectedOrder?.orderId === orderId && canceledOrder) {
        setSelectedOrder({ ...canceledOrder, status: "CANCELED", statusDescription: "주문 취소" });
      }

      setCancelReasonOrderId(null);
      setMessage("주문이 취소되었습니다.");
    } catch (cancelError) {
      setActionError(cancelError instanceof Error ? cancelError.message : "주문 취소 실패");
    }
  };

  const handleCancelOrderClick = (orderId: number) => {
    setCancelReasonOrderId(orderId);
    setMessage(null);
    setActionError(null);
  };

  const closeCancelReasonModal = () => {
    setCancelReasonOrderId(null);
  };

  const handleDeactivate = async () => {
    setMessage(null);
    setActionError(null);

    try {
      await onDeactivate();
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
      {view !== "profile" && message && <p className="form-message">{message}</p>}
      {view !== "profile" && actionError && <p className="form-error">{actionError}</p>}

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
                          <img src={firstItem.productImageUrl} alt={firstItem.productName} />
                          <div className="order-list-product">
                            <strong>
                              {firstItem.productName}
                              {extraItemCount > 0 && ` 외 ${extraItemCount}개`}
                            </strong>
                            <span>
                              {formatWon(firstItem.itemTotalPrice)}
                              {firstItem.quantity > 1 && ` (${firstItem.quantity}개)`}
                            </span>
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
                        <button type="button">배송조회</button>
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
              <h2>
                주문상품
                <span>
                  총 {selectedOrder.items.length}개 / {formatWon(selectedOrder.totalPrice)}
                </span>
              </h2>
              <ul className="order-detail-products">
                {selectedOrder.items.map((item) => (
                  <li key={item.orderItemId}>
                    <img src={item.productImageUrl} alt={item.productName} />
                    <div>
                      <strong>{item.productName}</strong>
                      <span>
                        {formatWon(item.productPrice)} ({item.quantity}개)
                      </span>
                      {item.addonName && (
                        <span>
                          [추가상품: {item.addonName} {item.addonQuantity}개]
                        </span>
                      )}
                      <span>상품구매금액 : {formatWon(item.itemTotalPrice)}</span>
                    </div>
                    <em>{selectedOrder.statusDescription}</em>
                  </li>
                ))}
              </ul>
            </section>

            <section className="order-detail-block">
              <h2>결제 정보</h2>
              <dl className="order-detail-table">
                <div>
                  <dt>결제방법</dt>
                  <dd>{selectedOrder.paymentMethod || "-"}</dd>
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
          <section
            className="cart-notice order-cancel-notice"
            role="dialog"
            aria-modal="true"
            aria-label="주문 취소 사유 입력"
            onClick={(event) => event.stopPropagation()}
          >
            <p>주문 취소 사유를 입력해주세요.</p>
            <textarea
              value={cancelReasons[cancelReasonOrderId] ?? ""}
              placeholder="취소 사유"
              onChange={(event) =>
                setCancelReasons((prevReasons) => ({
                  ...prevReasons,
                  [cancelReasonOrderId]: event.target.value,
                }))
              }
              autoFocus
            />
            <div>
              <button type="button" onClick={() => void handleCancelOrder(cancelReasonOrderId)}>
                취소하기
              </button>
              <button type="button" onClick={closeCancelReasonModal}>
                닫기
              </button>
            </div>
          </section>
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
              현재 비밀번호
              <input
                type="password"
                value={passwordForm.currentPassword}
                onChange={(event) =>
                  setPasswordForm((prevForm) => ({
                    ...prevForm,
                    currentPassword: event.target.value,
                  }))
                }
              />
            </label>
            <label>
              새 비밀번호
              <input
                type="password"
                value={passwordForm.newPassword}
                onChange={(event) =>
                  setPasswordForm((prevForm) => ({
                    ...prevForm,
                    newPassword: event.target.value,
                  }))
                }
              />
            </label>
            <label>
              새 비밀번호 확인
              <input
                type="password"
                value={passwordForm.newPasswordConfirm}
                onChange={(event) =>
                  setPasswordForm((prevForm) => ({
                    ...prevForm,
                    newPasswordConfirm: event.target.value,
                  }))
                }
              />
            </label>
            <label>
              이름
              <input
                value={infoForm.name}
                onChange={(event) => setInfoForm((prevForm) => ({ ...prevForm, name: event.target.value }))}
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
            {message && <p className="form-message profile-form-message">{message}</p>}
            {actionError && <p className="form-error profile-form-message">{actionError}</p>}
            <button type="submit">수정하기</button>
          </form>

          <div className="account-actions">
            <button type="button" onClick={() => void handleDeactivate()}>
              회원탈퇴
            </button>
          </div>
        </section>
      )}
    </section>
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
  const [form, setForm] = useState<SignupRequest>({
    email: "",
    password: "",
    name: "",
    phone: "",
  });
  const [passwordConfirm, setPasswordConfirm] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const [message, setMessage] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [agreements, setAgreements] = useState({
    terms: false,
    privacy: false,
    marketing: false,
  });

  const allAgreed = agreements.terms && agreements.privacy && agreements.marketing;

  const updateField = (field: keyof SignupRequest, value: string) => {
    setForm((prevForm) => ({
      ...prevForm,
      [field]: value,
    }));
  };

  const updatePhone = (value: string) => {
    const numbers = value.replace(/\D/g, "").slice(0, 11);
    const formattedPhone =
      numbers.length > 7
        ? `${numbers.slice(0, 3)}-${numbers.slice(3, 7)}-${numbers.slice(7)}`
        : numbers.length > 3
          ? `${numbers.slice(0, 3)}-${numbers.slice(3)}`
          : numbers;

    updateField("phone", formattedPhone);
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

        await signup(form);
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

  const moveToLogin = () => {
    setSignupCompleted(false);
    setCompletedMemberName("");
    setMode("login");
    setMessage(null);
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

              <div className="agreement-row">
                <input
                  type="checkbox"
                  checked={agreements.terms}
                  onChange={(event) => updateAgreement("terms", event.target.checked)}
                  required
                />
                <span>
                  <strong>이용약관</strong> 동의 (필수)
                </span>
              </div>

              <div className="agreement-row">
                <input
                  type="checkbox"
                  checked={agreements.privacy}
                  onChange={(event) => updateAgreement("privacy", event.target.checked)}
                  required
                />
                <span>
                  <strong>개인정보 수집 및 이용</strong> 동의 (필수)
                </span>
              </div>

              <div className="agreement-row">
                <input
                  type="checkbox"
                  checked={agreements.marketing}
                  onChange={(event) => updateAgreement("marketing", event.target.checked)}
                />
                <span>
                  마케팅 정보 수신 동의 (선택)
                </span>
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
          <button className="auth-switch-button" type="button" onClick={() => setMode("signup")}>
            회원가입
          </button>
        )}
      </div>
    </section>
  );
}

function About() {
  return (
    <section className="page-view about-view">
      <div>
        <span>ABOUT US</span>
        <h1>
          Nature, remembered.
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
    </section>
  );
}

function BusinessFooter() {
  return (
    <footer className="business-footer">
      <p>
        상호명: 얼씨 대표자: 한훈석, 박수지 사업장주소: 경남 창원시 소답동 148-3, 711호 연락처:
        070-0000-0000 사업자등록번호: 000-00-00000
      </p>
      <p>통신판매업신고번호: 제2026-경남창원-0000호 대표자 이메일: earthy@gmail.com</p>
    </footer>
  );
}

export default App;
