import { ChangeEvent, DragEvent, FormEvent, ReactNode, useEffect, useMemo, useRef, useState } from "react";
import {
  AddonSaveRequest,
  AdminAddon,
  AdminBoard,
  AdminMember,
  AdminOrder,
  AdminProduct,
  Notice,
  AddonType,
  MemberStatusFilter,
  NoticeSaveRequest,
  NoticeVisibilityFilter,
  ProductCategory,
  ProductSaveRequest,
  ProductSizeOptionSaveRequest,
  OrderStatus,
  activateAdminAddon,
  activateAdminProduct,
  cancelAdminOrder,
  answerAdminBoard,
  createAdminAddon,
  createAdminNotice,
  createAdminProduct,
  deleteAdminImage,
  deactivateAdminAddon,
  deactivateAdminProduct,
  deleteAdminAddon,
  deleteAdminProduct,
  getAdminAddons,
  getAdminAddonsPage,
  getAdminBoard,
  getAdminBoardsPage,
  getAdminMembersPage,
  getAdminMember,
  getAdminNotice,
  getAdminNoticesPage,
  getAdminOrder,
  getAdminOrders,
  getAdminOrderStatusCounts,
  getAdminOrdersPage,
  getAdminProducts,
  getAdminProductsPage,
  PageResponse,
  getStoredAdminAccessToken,
  loginAdmin,
  logoutAdmin,
  onAdminAuthCleared,
  hideAdminNotice,
  showAdminNotice,
  updateAdminAddon,
  updateAdminNotice,
  updateAdminOrderStatus,
  updateAdminPassword,
  updateAdminProduct,
  uploadAdminProductDetailImage,
  uploadAdminProductImage,
} from "./api";
import "./admin.css";

type AdminTab = "dashboard" | "products" | "addons" | "orders" | "customers" | "notices" | "boards" | "password";

const ADMIN_TAB_PATHS: Record<AdminTab, string> = {
  dashboard: "/admin",
  products: "/admin/products",
  addons: "/admin/addons",
  orders: "/admin/orders",
  customers: "/admin/customers",
  notices: "/admin/notices",
  boards: "/admin/inquiries",
  password: "/admin/password",
};

function normalizeAdminPath(pathname = window.location.pathname) {
  const normalized = pathname.replace(/\/+$/, "");
  return normalized || "/admin";
}

function getAdminTabFromPath(pathname = window.location.pathname): AdminTab {
  const path = normalizeAdminPath(pathname);

  if (path.startsWith("/admin/products")) {
    return "products";
  }
  if (path.startsWith("/admin/addons")) {
    return "addons";
  }
  if (path.startsWith("/admin/orders")) {
    return "orders";
  }
  if (path.startsWith("/admin/customers") || path.startsWith("/admin/members")) {
    return "customers";
  }
  if (path.startsWith("/admin/notices")) {
    return "notices";
  }
  if (path.startsWith("/admin/inquiries") || path.startsWith("/admin/boards")) {
    return "boards";
  }
  if (path.startsWith("/admin/password")) {
    return "password";
  }

  return "dashboard";
}

function getAdminRouteId(prefix: string) {
  const path = normalizeAdminPath();
  const match = path.match(new RegExp(`^${prefix}/(\\d+)(?:/edit)?$`));
  return match ? Number(match[1]) : null;
}

function setAdminHistory(path: string, replace = false) {
  const current = `${window.location.pathname}${window.location.search}`;

  if (current === path) {
    return;
  }

  if (replace) {
    window.history.replaceState({}, "", path);
    return;
  }

  window.history.pushState({}, "", path);
}

// 멱등성 키 생성
function createIdempotencyKey() {
  if (window.crypto?.randomUUID) {
    return window.crypto.randomUUID();
  }

  return `admin-${Date.now()}-${Math.random().toString(16).slice(2)}`;
}

const PRODUCT_CATEGORIES: Array<{ value: ProductCategory; label: string }> = [
  { value: "POSTCARD", label: "엽서" },
  { value: "POSTER", label: "포스터" },
  { value: "ETC", label: "기타" },
];

const ADDON_TYPES: Array<{ value: AddonType; label: string }> = [
  { value: "PREMIUM_FRAME", label: "프리미엄 액자" },
  { value: "BASIC_FRAME", label: "베이직 액자" },
];
const LEGACY_ADDON_TYPE: { value: AddonType; label: string } = { value: "FRAME", label: "액자" };

const ADMIN_CANCEL_REASON_OPTIONS = [
  "재고 부족",
  "상품 품절",
  "배송 불가 지역",
  "고객 요청",
  "결제 오류",
  "상품 문제",
  "기타",
];

const LOW_STOCK_THRESHOLD = 0;
const ADMIN_IMAGE_MAX_SIZE = 5 * 1024 * 1024;
const ADMIN_IMAGE_ALLOWED_TYPES = new Set(["image/jpeg", "image/png", "image/webp"]);
const ADMIN_IMAGE_ALLOWED_EXTENSIONS = new Set([".jpg", ".jpeg", ".png", ".webp"]);
type ProductImageField = "imageUrl" | "detailImageUrl";
const emptyProductOriginalImageUrls: Record<ProductImageField, string> = {
  imageUrl: "",
  detailImageUrl: "",
};

const ORDER_STATUSES: Array<{ value: OrderStatus; label: string }> = [
  { value: "PAID", label: "결제 완료" },
  { value: "PREPARING", label: "상품 준비중" },
  { value: "SHIPPED", label: "배송중" },
  { value: "DELIVERED", label: "배송 완료" },
  { value: "CANCELED", label: "주문 취소" },
];

const ORDER_UPDATE_STATUSES: Array<{ value: OrderStatus; label: string }> = [
  { value: "PREPARING", label: "상품 준비중" },
  { value: "SHIPPED", label: "배송중" },
  { value: "DELIVERED", label: "배송 완료" },
];

const MEMBER_STATUS_FILTERS: Array<{ value: MemberStatusFilter; label: string }> = [
  { value: "ALL", label: "전체" },
  { value: "ACTIVE", label: "활성" },
  { value: "INACTIVE", label: "탈퇴" },
];

const NOTICE_VISIBILITY_FILTERS: Array<{ value: NoticeVisibilityFilter; label: string }> = [
  { value: "ALL", label: "전체" },
  { value: "PUBLIC", label: "공개" },
  { value: "PRIVATE", label: "비공개" },
];

const emptyNoticeForm: NoticeSaveRequest = {
  title: "",
  content: "",
};

const emptyProductForm: ProductSaveRequest = {
  name: "",
  category: "POSTCARD",
  price: 0,
  imageUrl: "",
  detailImageUrl: "",
  description: "",
  stockQuantity: 0,
  sizeOptions: [],
};

const createEmptySizeOption = (): ProductSizeOptionSaveRequest => ({
  sizeName: "",
  additionalPrice: 0,
  stockQuantity: 0,
  active: true,
});

const ensurePosterSizeOptions = (sizeOptions: ProductSizeOptionSaveRequest[]) =>
  sizeOptions.length > 0 ? sizeOptions : [createEmptySizeOption()];

const emptyAddonForm: AddonSaveRequest = {
  name: "",
  type: "PREMIUM_FRAME",
  price: 0,
  stockQuantity: 0,
};

function createEmptyPage<T>(size = 20): PageResponse<T> {
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

function formatWon(value: number) {
  return `${value.toLocaleString("ko-KR")}원`;
}

function formatOrderSizeOptionLabel(item: {
  sizeName: string | null;
  sizeAdditionalPrice: number;
}) {
  if (!item.sizeName) {
    return null;
  }

  if (item.sizeAdditionalPrice === 0) {
    return `사이즈: ${item.sizeName}`;
  }

  const pricePrefix = item.sizeAdditionalPrice > 0 ? "+" : "-";
  const price = Math.abs(item.sizeAdditionalPrice).toLocaleString("ko-KR");

  return `사이즈: ${item.sizeName} (${pricePrefix}${price}원)`;
}

function formatNumber(value: number) {
  return value.toLocaleString("ko-KR");
}

// 관리자 주문 상세 결제수단 표기
function formatPaymentMethod(paymentMethod?: string | null) {
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
}

function formatDate(value: string) {
  return new Intl.DateTimeFormat("ko-KR", {
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
  }).format(new Date(value));
}

function formatDateOnly(value: string) {
  return new Intl.DateTimeFormat("ko-KR", {
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
  }).format(new Date(value));
}

function isCreatedToday(value: string) {
  const target = new Date(value);
  const today = new Date();

  return (
    target.getFullYear() === today.getFullYear() &&
    target.getMonth() === today.getMonth() &&
    target.getDate() === today.getDate()
  );
}

function getNextOrderStatus(status: OrderStatus): OrderStatus | "" {
  if (status === "PAID") {
    return "PREPARING";
  }

  if (status === "PREPARING") {
    return "SHIPPED";
  }

  if (status === "SHIPPED") {
    return "DELIVERED";
  }

  return "";
}

export default function AdminApp() {
  const [isAuthed, setIsAuthed] = useState(() => Boolean(getStoredAdminAccessToken()));
  const [activeTab, setActiveTab] = useState<AdminTab>(() => getAdminTabFromPath());
  const [loginError, setLoginError] = useState("");
  const [isMobileMenuOpen, setIsMobileMenuOpen] = useState(false);
  const [productsMenuKey, setProductsMenuKey] = useState(0);
  const [addonsMenuKey, setAddonsMenuKey] = useState(0);
  const [ordersMenuKey, setOrdersMenuKey] = useState(0);
  const [noticesMenuKey, setNoticesMenuKey] = useState(0);
  const [boardsMenuKey, setBoardsMenuKey] = useState(0);
  const [dashboardOrderTargetId, setDashboardOrderTargetId] = useState<number | null>(null);
  const [dashboardProductTarget, setDashboardProductTarget] = useState<AdminProduct | null>(null);
  const [dashboardAddonTarget, setDashboardAddonTarget] = useState<AdminAddon | null>(null);

  useEffect(() => onAdminAuthCleared(() => setIsAuthed(false)), []);

  useEffect(() => {
    const handlePopState = () => {
      setActiveTab(getAdminTabFromPath());
      setIsMobileMenuOpen(false);
    };

    window.addEventListener("popstate", handlePopState);
    return () => window.removeEventListener("popstate", handlePopState);
  }, []);

  function changeTab(tab: AdminTab) {
    setAdminHistory(ADMIN_TAB_PATHS[tab]);
    setActiveTab(tab);
    if (tab === "products") {
      setProductsMenuKey((current) => current + 1);
    }
    if (tab === "addons") {
      setAddonsMenuKey((current) => current + 1);
    }
    if (tab === "orders") {
      setOrdersMenuKey((current) => current + 1);
    }
    if (tab === "notices") {
      setNoticesMenuKey((current) => current + 1);
    }
    if (tab === "boards") {
      setBoardsMenuKey((current) => current + 1);
    }
    setIsMobileMenuOpen(false);
  }

  function openDashboardProduct(product: AdminProduct) {
    setDashboardProductTarget(product);
    setAdminHistory(`${ADMIN_TAB_PATHS.products}/${product.id}/edit`);
    setActiveTab("products");
  }

  function openDashboardOrder(order: AdminOrder) {
    setDashboardOrderTargetId(order.orderId);
    setAdminHistory(`${ADMIN_TAB_PATHS.orders}/${order.orderId}`);
    setActiveTab("orders");
  }

  function openDashboardAddon(addon: AdminAddon) {
    setDashboardAddonTarget(addon);
    setAdminHistory(`${ADMIN_TAB_PATHS.addons}/${addon.id}/edit`);
    setActiveTab("addons");
  }

  async function handleLogin(email: string, password: string) {
    setLoginError("");

    try {
      await loginAdmin(email, password);
      setIsAuthed(true);
    } catch (error) {
      setLoginError(error instanceof Error ? error.message : "관리자 로그인에 실패했습니다.");
    }
  }

  async function handleLogout() {
    await logoutAdmin();
    setIsAuthed(false);
  }

  if (!isAuthed) {
    return <AdminLoginPage error={loginError} onLogin={handleLogin} />;
  }

  return (
    <div className={`admin-shell ${isMobileMenuOpen ? "is-menu-open" : ""}`}>
      <header className="admin-mobile-header">
        <button
          className="admin-menu-button"
          type="button"
          aria-label="관리자 메뉴 열기"
          aria-expanded={isMobileMenuOpen}
          onClick={() => setIsMobileMenuOpen((current) => !current)}
        >
          <span />
          <span />
          <span />
        </button>
        <button className="admin-mobile-logo" type="button" onClick={() => changeTab("dashboard")}>
          EARTHY ADMIN
        </button>
      </header>

      <aside className="admin-sidebar" onClick={(event) => event.stopPropagation()}>
        <button className="admin-logo" type="button" onClick={() => changeTab("dashboard")}>
          EARTHY
        </button>
        <nav className="admin-nav" aria-label="관리자 메뉴">
          <button className={activeTab === "dashboard" ? "is-active" : ""} type="button" onClick={() => changeTab("dashboard")}>
            대시보드
          </button>
          <button className={activeTab === "products" ? "is-active" : ""} type="button" onClick={() => changeTab("products")}>
            상품관리
          </button>
          <button className={`admin-sub-nav-button ${activeTab === "addons" ? "is-active" : ""}`} type="button" onClick={() => changeTab("addons")}>
            추가상품관리
          </button>
          <button className={activeTab === "orders" ? "is-active" : ""} type="button" onClick={() => changeTab("orders")}>
            주문관리
          </button>
          <button className={activeTab === "customers" ? "is-active" : ""} type="button" onClick={() => changeTab("customers")}>
            고객관리
          </button>
          <button className={activeTab === "notices" ? "is-active" : ""} type="button" onClick={() => changeTab("notices")}>
            공지관리
          </button>
          <button className={activeTab === "boards" ? "is-active" : ""} type="button" onClick={() => changeTab("boards")}>
            문의관리
          </button>
          <button className={activeTab === "password" ? "is-active" : ""} type="button" onClick={() => changeTab("password")}>
            비밀번호변경
          </button>
        </nav>
        <button className="admin-logout" type="button" onClick={handleLogout}>
          로그아웃
        </button>
      </aside>

      <main
        className="admin-main"
        onClick={() => {
          if (isMobileMenuOpen) {
            setIsMobileMenuOpen(false);
          }
        }}
      >
        {activeTab === "dashboard" && (
          <DashboardPanel
            onMoveTab={changeTab}
            onOpenOrder={openDashboardOrder}
            onOpenProduct={openDashboardProduct}
            onOpenAddon={openDashboardAddon}
          />
        )}
        {activeTab === "orders" && (
          <OrdersPanel
            menuKey={ordersMenuKey}
            initialOrderId={dashboardOrderTargetId}
            onInitialOrderOpened={() => setDashboardOrderTargetId(null)}
          />
        )}
        {activeTab === "products" && (
          <ProductsPanel
            menuKey={productsMenuKey}
            initialProduct={dashboardProductTarget}
            onInitialProductOpened={() => setDashboardProductTarget(null)}
          />
        )}
        {activeTab === "addons" && (
          <AddonsPanel
            menuKey={addonsMenuKey}
            initialAddon={dashboardAddonTarget}
            onInitialAddonOpened={() => setDashboardAddonTarget(null)}
          />
        )}
        {activeTab === "customers" && <CustomersPanel />}
        {activeTab === "notices" && <NoticesPanel key={noticesMenuKey} />}
        {activeTab === "boards" && <BoardsPanel key={boardsMenuKey} />}
        {activeTab === "password" && <PasswordPanel />}
      </main>
    </div>
  );
}

function AdminLoginPage({ error, onLogin }: { error: string; onLogin: (email: string, password: string) => Promise<void> }) {
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [submitting, setSubmitting] = useState(false);

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setSubmitting(true);

    try {
      await onLogin(email, password);
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <main className="admin-login-page">
      <form className="admin-login-card" onSubmit={handleSubmit}>
        <h1>EARTHY ADMIN</h1>
        <label>
          이메일
          <input value={email} onChange={(event) => setEmail(event.target.value)} autoComplete="username" />
        </label>
        <label>
          비밀번호
          <input
            type="password"
            value={password}
            onChange={(event) => setPassword(event.target.value)}
            autoComplete="current-password"
          />
        </label>
        <AdminErrorMessage message={error} />
        <button className="admin-primary-button" type="submit" disabled={submitting}>
          {submitting ? "로그인 중" : "로그인"}
        </button>
      </form>
    </main>
  );
}

function DashboardPanel({
  onMoveTab,
  onOpenOrder,
  onOpenProduct,
  onOpenAddon,
}: {
  onMoveTab: (tab: AdminTab) => void;
  onOpenOrder: (order: AdminOrder) => void;
  onOpenProduct: (product: AdminProduct) => void;
  onOpenAddon: (addon: AdminAddon) => void;
}) {
  const [orders, setOrders] = useState<AdminOrder[]>([]);
  const [products, setProducts] = useState<AdminProduct[]>([]);
  const [addons, setAddons] = useState<AdminAddon[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  const paidOrders = orders.filter((order) => order.status === "PAID");
  const preparingOrders = orders.filter((order) => order.status === "PREPARING");
  const revenueOrders = orders.filter((order) => order.status !== "PENDING" && order.status !== "CANCELED");
  const totalRevenue = revenueOrders.reduce((sum, order) => sum + order.totalPrice, 0);
  const lowStockProducts = products.filter(isLowStockProduct);
  const lowStockAddons = addons.filter((addon) => addon.active && addon.stockQuantity <= LOW_STOCK_THRESHOLD);
  const lowStockCount = lowStockProducts.length + lowStockAddons.length;

  async function loadDashboard() {
    setLoading(true);
    setError("");

    try {
      const [orderData, productData, addonData] = await Promise.all([
        getAdminOrders(0, 1000),
        getAdminProducts(0, 1000),
        getAdminAddons(0, 1000),
      ]);
      setOrders(orderData);
      setProducts(productData);
      setAddons(addonData);
    } catch (loadError) {
      setError(loadError instanceof Error ? loadError.message : "대시보드 데이터를 불러오지 못했습니다.");
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    void loadDashboard();
  }, []);

  return (
    <section className="admin-section">
      <PanelHeader title="대시보드" />
      <Feedback notice="" error={error} />

      <div className="admin-dashboard-grid">
        <button
          className={`admin-metric-card ${paidOrders.length > 0 ? "is-alert" : ""}`}
          type="button"
          onClick={() => onMoveTab("orders")}
        >
          <span>처리할 새 주문</span>
          <strong>{paidOrders.length}</strong>
          <small>결제 완료</small>
        </button>
        <button className="admin-metric-card" type="button" onClick={() => onMoveTab("orders")}>
          <span>상품 준비중</span>
          <strong>{preparingOrders.length}</strong>
          <small>발송 전</small>
        </button>
        <div className="admin-metric-card">
          <span>총매출</span>
          <strong>{formatWon(totalRevenue)}</strong>
          <small>취소/대기 제외</small>
        </div>
        <button
          className={`admin-metric-card ${lowStockCount > 0 ? "is-alert" : ""}`}
          type="button"
          onClick={() => onMoveTab("products")}
        >
          <span>재고 보충 필요</span>
          <strong>{lowStockCount}</strong>
          <small>부족 재고 기준</small>
        </button>
      </div>

      <div className="admin-dashboard-detail-grid">
        <div className="admin-card">
          <div className="admin-card-title-row">
            <h2>새 주문</h2>
            <button className="admin-title-back-button" type="button" onClick={() => onMoveTab("orders")}>
              주문 관리로 이동
              <span aria-hidden="true" />
            </button>
          </div>
          {loading ? (
            <p className="admin-empty">새 주문을 불러오는 중입니다.</p>
          ) : paidOrders.length === 0 ? (
            <p className="admin-empty">처리할 새 주문이 없습니다.</p>
          ) : (
            <div className="admin-dashboard-list">
              {paidOrders.slice(0, 6).map((order) => (
                <button className="admin-dashboard-row" type="button" key={order.orderId} onClick={() => onOpenOrder(order)}>
                  <span>
                    <strong>{order.receiverName}</strong>
                    <small>{order.orderNumber}</small>
                  </span>
                  <span>
                    <strong>{formatWon(order.totalPrice)}</strong>
                    <small>{formatDate(order.createdAt)}</small>
                  </span>
                </button>
              ))}
            </div>
          )}
        </div>

        <div className="admin-card">
          <div className="admin-card-title-row">
            <h2>상품 준비중</h2>
            <button className="admin-title-back-button" type="button" onClick={() => onMoveTab("orders")}>
              주문 관리로 이동
              <span aria-hidden="true" />
            </button>
          </div>
          {loading ? (
            <p className="admin-empty">상품 준비중 주문을 불러오는 중입니다.</p>
          ) : preparingOrders.length === 0 ? (
            <p className="admin-empty">상품 준비중 주문이 없습니다.</p>
          ) : (
            <div className="admin-dashboard-list">
              {preparingOrders.slice(0, 6).map((order) => (
                <button className="admin-dashboard-row" type="button" key={order.orderId} onClick={() => onOpenOrder(order)}>
                  <span>
                    <strong>{order.receiverName}</strong>
                    <small>{order.orderNumber}</small>
                  </span>
                  <span>
                    <strong>{formatWon(order.totalPrice)}</strong>
                    <small>{formatDate(order.createdAt)}</small>
                  </span>
                </button>
              ))}
            </div>
          )}
        </div>

        <div className="admin-card admin-dashboard-stock-card">
          <div className="admin-card-title-row">
            <h2>재고 보충 대상</h2>
            <button className="admin-title-back-button" type="button" onClick={() => onMoveTab("products")}>
              상품 관리로 이동
              <span aria-hidden="true" />
            </button>
          </div>
          {loading ? (
            <p className="admin-empty">재고 상태를 불러오는 중입니다.</p>
          ) : lowStockCount === 0 ? (
            <p className="admin-empty">재고 보충이 필요한 상품이 없습니다.</p>
          ) : (
            <div className="admin-dashboard-list">
              {lowStockProducts.map((product) => (
                <button className="admin-dashboard-row" type="button" key={`product-${product.id}`} onClick={() => onOpenProduct(product)}>
                  <span>
                    <strong>{product.name}</strong>
                    <small>상품 / {product.categoryDescription}</small>
                  </span>
                  <span>
                    <strong>{formatDashboardProductStock(product)}</strong>
                    <small>재고 보충 필요</small>
                  </span>
                </button>
              ))}
              {lowStockAddons.map((addon) => (
                <button className="admin-dashboard-row" type="button" key={`addon-${addon.id}`} onClick={() => onOpenAddon(addon)}>
                  <span>
                    <strong>{addon.name}</strong>
                    <small>추가상품 / {addon.typeDescription}</small>
                  </span>
                  <span>
                    <strong>{addon.stockQuantity}개</strong>
                    <small>재고 보충 필요</small>
                  </span>
                </button>
              ))}
            </div>
          )}
        </div>
      </div>
    </section>
  );
}

function isLowStockProduct(product: AdminProduct) {
  if (!product.active) {
    return false;
  }

  if (product.category !== "POSTER") {
    return product.stockQuantity <= LOW_STOCK_THRESHOLD;
  }

  const activeSizeOptions = product.sizeOptions?.filter((option) => option.active) ?? [];

  if (activeSizeOptions.length === 0) {
    return true;
  }

  return activeSizeOptions.some((option) => option.stockQuantity <= LOW_STOCK_THRESHOLD);
}

function formatDashboardProductStock(product: AdminProduct) {
  if (product.category !== "POSTER") {
    return `${product.stockQuantity}개`;
  }

  const activeSizeOptions = product.sizeOptions?.filter((option) => option.active) ?? [];

  if (activeSizeOptions.length === 0) {
    return "활성 사이즈 없음";
  }

  return activeSizeOptions
    .map((option) => `${option.sizeName} ${option.stockQuantity}`)
    .join(" / ");
}

function OrdersPanel({
  menuKey,
  initialOrderId,
  onInitialOrderOpened,
}: {
  menuKey: number;
  initialOrderId: number | null;
  onInitialOrderOpened: () => void;
}) {
  const [orders, setOrders] = useState<AdminOrder[]>([]);
  const [statusCounts, setStatusCounts] = useState<Partial<Record<OrderStatus, number>>>({});
  const [ordersPage, setOrdersPage] = useState<PageResponse<AdminOrder>>(() => createEmptyPage<AdminOrder>());
  const [currentPage, setCurrentPage] = useState(0);
  const [selectedOrder, setSelectedOrder] = useState<AdminOrder | null>(null);
  const [isDetailView, setIsDetailView] = useState(false);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [notice, setNotice] = useState("");

  const summary = useMemo(() => {
    return ORDER_STATUSES.map((status) => ({
      ...status,
      count: statusCounts[status.value] ?? 0,
    }));
  }, [statusCounts]);

  async function loadOrders(page = currentPage) {
    setLoading(true);
    setError("");

    try {
      const [data, counts] = await Promise.all([getAdminOrdersPage(page), getAdminOrderStatusCounts()]);

      setOrders(data.content);
      setStatusCounts(counts);
      setOrdersPage(data);
      setCurrentPage(data.page);

      if (selectedOrder) {
        const latest = data.content.find((order) => order.orderId === selectedOrder.orderId);
        setSelectedOrder(latest ?? null);
      }
    } catch (loadError) {
      setError(loadError instanceof Error ? loadError.message : "주문 목록을 불러오지 못했습니다.");
    } finally {
      setLoading(false);
    }
  }

  async function openOrder(orderId: number, pushHistory = true) {
    setError("");

    try {
      setSelectedOrder(await getAdminOrder(orderId));
      setIsDetailView(true);
      if (pushHistory) {
        setAdminHistory(`${ADMIN_TAB_PATHS.orders}/${orderId}`);
      }
    } catch (loadError) {
      setError(loadError instanceof Error ? loadError.message : "주문 상세를 불러오지 못했습니다.");
    }
  }

  function closeOrderDetail(pushHistory = true) {
    setSelectedOrder(null);
    setIsDetailView(false);
    if (pushHistory) {
      setAdminHistory(ADMIN_TAB_PATHS.orders);
    }
  }

  useEffect(() => {
    void loadOrders();
  }, [currentPage]);

  useEffect(() => {
    closeOrderDetail(false);
  }, [menuKey]);

  useEffect(() => {
    if (initialOrderId === null) {
      return;
    }

    void openOrder(initialOrderId, false);
    onInitialOrderOpened();
  }, [initialOrderId]);

  useEffect(() => {
    const syncRoute = () => {
      if (getAdminTabFromPath() !== "orders") {
        return;
      }

      const orderId = getAdminRouteId(ADMIN_TAB_PATHS.orders);

      if (orderId) {
        void openOrder(orderId, false);
        return;
      }

      closeOrderDetail(false);
    };

    syncRoute();
    window.addEventListener("popstate", syncRoute);
    return () => window.removeEventListener("popstate", syncRoute);
  }, []);

  async function handleStatusUpdate(orderId: number, status: OrderStatus, carrier: string, trackingNumber: string) {
    setNotice("");
    setError("");

    try {
      const updated = await updateAdminOrderStatus(orderId, {
        status,
        carrier: status === "SHIPPED" ? carrier : undefined,
        trackingNumber: status === "SHIPPED" ? trackingNumber : undefined,
      });
      setSelectedOrder(updated);
      setNotice("주문 상태가 변경되었습니다.");
      await loadOrders(currentPage);
    } catch (updateError) {
      setError(updateError instanceof Error ? updateError.message : "주문 상태 변경에 실패했습니다.");
    }
  }

  async function handleCancel(orderId: number, cancelReason: string) {
    setNotice("");
    setError("");

    try {
      const updated = await cancelAdminOrder(orderId, cancelReason, createIdempotencyKey());
      setSelectedOrder(updated);
      setNotice("주문이 취소되었습니다.");
      await loadOrders(currentPage);
    } catch (cancelError) {
      setError(cancelError instanceof Error ? cancelError.message : "주문 취소에 실패했습니다.");
    }
  }

  if (isDetailView && selectedOrder) {
    return (
      <section className="admin-section">
        <PanelHeader
          title="주문 관리"
          description=""
          action={
            <button className="admin-title-back-button" type="button" onClick={() => closeOrderDetail()}>
              목록으로 돌아가기
              <span aria-hidden="true" />
            </button>
          }
        />
        <Toast message={notice} onClose={() => setNotice("")} />
        <h2 className="admin-section-title">주문 상세내역</h2>
        <div className="admin-order-detail-page">
          <OrderDetail order={selectedOrder} error={error} onStatusUpdate={handleStatusUpdate} onCancel={handleCancel} />
        </div>
      </section>
    );
  }

  return (
    <section className="admin-section">
      <PanelHeader title="주문 관리" />
      <div className="admin-summary-grid">
        {summary.map((item) => (
          <div className={`admin-summary-card ${item.value === "PAID" && item.count > 0 ? "is-alert" : ""}`} key={item.value}>
            <span>{item.label}</span>
            <strong>{item.count}</strong>
          </div>
        ))}
      </div>
      <Feedback notice={notice} error={error} />
      <Toast message={notice} onClose={() => setNotice("")} />

      <h2 className="admin-section-title">주문 목록</h2>
      <div className="admin-card admin-order-list-card">
        {loading ? (
          <p className="admin-empty">주문 목록을 불러오는 중입니다.</p>
        ) : (
          <>
            <div className="admin-table-wrap admin-desktop-order-list">
              <table className="admin-table admin-order-table">
                <thead>
                  <tr>
                    <th>주문번호</th>
                    <th>수령자</th>
                    <th>상태</th>
                    <th>결제금액</th>
                    <th>주문일</th>
                    <th>배송정보</th>
                  </tr>
                </thead>
                <tbody>
                  {orders.length === 0 ? (
                    <tr>
                      <td className="admin-empty-row" colSpan={6}>
                        해당하는 주문이 없습니다.
                      </td>
                    </tr>
                  ) : (
                    orders.map((order) => (
                      <tr key={order.orderId}>
                        <td>
                          <button className="admin-order-number-button" type="button" onClick={() => void openOrder(order.orderId)}>
                            {order.orderNumber}
                          </button>
                        </td>
                        <td>{order.receiverName}</td>
                        <td>
                          <span className={`admin-status-badge status-${order.status.toLowerCase()}`}>{order.statusDescription}</span>
                        </td>
                        <td className="admin-order-price-cell">{formatNumber(order.totalPrice)}</td>
                        <td>{formatDateOnly(order.createdAt)}</td>
                        <td>
                          {order.carrier && order.trackingNumber ? `${order.carrier} / ${order.trackingNumber}` : "-"}
                        </td>
                      </tr>
                    ))
                  )}
                </tbody>
              </table>
            </div>

            <div className="admin-mobile-order-list">
              {orders.length === 0 ? (
                <p className="admin-empty">해당하는 주문이 없습니다.</p>
              ) : (
                orders.map((order) => (
                  <button className="admin-mobile-order-card" type="button" key={order.orderId} onClick={() => void openOrder(order.orderId)}>
                    <span className="admin-mobile-order-number">{order.orderNumber}</span>
                    <span className="admin-mobile-order-main">
                      <strong>{order.receiverName}</strong>
                      <span className={`admin-status-badge status-${order.status.toLowerCase()}`}>{order.statusDescription}</span>
                    </span>
                    <span className="admin-mobile-order-price">{formatNumber(order.totalPrice)}</span>
                    <span className="admin-mobile-order-meta">
                      <span>주문일</span>
                      <strong>{formatDateOnly(order.createdAt)}</strong>
                    </span>
                    <span className="admin-mobile-order-meta">
                      <span>배송정보</span>
                      <strong>{order.carrier && order.trackingNumber ? `${order.carrier} / ${order.trackingNumber}` : "-"}</strong>
                    </span>
                  </button>
                ))
              )}
            </div>
          </>
        )}
      </div>
      <AdminPagination pageInfo={ordersPage} onChangePage={setCurrentPage} />
    </section>
  );
}

function OrderDetail({
  order,
  error,
  onStatusUpdate,
  onCancel,
}: {
  order: AdminOrder;
  error: string;
  onStatusUpdate: (orderId: number, status: OrderStatus, carrier: string, trackingNumber: string) => Promise<void>;
  onCancel: (orderId: number, cancelReason: string) => Promise<void>;
}) {
  const [status, setStatus] = useState<OrderStatus>(order.status);
  const [carrier, setCarrier] = useState(order.carrier ?? "");
  const [trackingNumber, setTrackingNumber] = useState(order.trackingNumber ?? "");
  const [cancelReasonSelection, setCancelReasonSelection] = useState("");
  const [customCancelReason, setCustomCancelReason] = useState("");
  const [cancelReasonError, setCancelReasonError] = useState("");
  const [statusModalOpen, setStatusModalOpen] = useState(false);
  const [cancelModalOpen, setCancelModalOpen] = useState(false);
  const [cancelSubmitting, setCancelSubmitting] = useState(false);
  const canChangeStatus = Boolean(getNextOrderStatus(order.status));
  const selectedStatusLabel = ORDER_UPDATE_STATUSES.find((item) => item.value === status)?.label ?? status;

  const openCancelModal = () => {
    setCancelReasonSelection("");
    setCustomCancelReason("");
    setCancelReasonError("");
    setCancelModalOpen(true);
  };

  useEffect(() => {
    setStatus(getNextOrderStatus(order.status) || order.status);
    setCarrier(order.carrier ?? "");
    setTrackingNumber(order.trackingNumber ?? "");
    setCancelReasonSelection("");
    setCustomCancelReason("");
    setCancelReasonError("");
    setStatusModalOpen(false);
    setCancelModalOpen(false);
    setCancelSubmitting(false);
  }, [order]);

  return (
    <div className="admin-detail">
      <div className="admin-detail-grid">
        <section className="admin-detail-block">
          <h3>주문정보</h3>
          <dl className="admin-detail-list">
            <div>
              <dt>주문번호</dt>
              <dd>{order.orderNumber}</dd>
            </div>
            <div>
              <dt>주문일</dt>
              <dd>{formatDate(order.createdAt)}</dd>
            </div>
            <div>
              <dt>주문처리상태</dt>
              <dd>
                <span className={`admin-status-badge status-${order.status.toLowerCase()}`}>{order.statusDescription}</span>
              </dd>
            </div>
            {order.status === "CANCELED" && order.cancelReason && (
              <div>
                <dt>취소사유</dt>
                <dd>{order.cancelReason}</dd>
              </div>
            )}
          </dl>
        </section>

        <section className="admin-detail-block">
          <h3>결제정보</h3>
          <dl className="admin-detail-list">
            <div>
              <dt>결제방법</dt>
              <dd>{formatPaymentMethod(order.paymentMethod)}</dd>
            </div>
            <div>
              <dt>상품금액</dt>
              <dd>{formatWon(order.productTotalPrice)}</dd>
            </div>
            <div>
              <dt>배송비</dt>
              <dd>{formatWon(order.deliveryFee + order.remoteAreaDeliveryFee)}</dd>
            </div>
            <div>
              <dt>총 결제금액</dt>
              <dd>{formatWon(order.totalPrice)}</dd>
            </div>
          </dl>
        </section>
      </div>

      <section className="admin-detail-block">
        <h3>주문상품</h3>
        <div className="admin-order-items">
          {order.items.map((item) => (
            <div className="admin-order-item" key={item.orderItemId}>
              <img src={item.productImageUrl} alt={item.productName} />
              <div>
                <strong>{item.productName}</strong>
                <p>{formatWon(item.productPrice)} / {item.quantity}개</p>
                {formatOrderSizeOptionLabel(item) && (
                  <p>{formatOrderSizeOptionLabel(item)}</p>
                )}
                {(item.addons ?? []).map((addon) => (
                  <p key={addon.orderItemAddonId}>
                    추가상품: {addon.addonName} (+{formatWon(addon.addonPrice)}) / {addon.quantity}개
                  </p>
                ))}
                {(item.addons ?? []).length === 0 && item.addonName && (
                  <p>
                    추가상품: {item.addonName} (+{formatWon(item.addonPrice)}) / {item.addonQuantity}개
                  </p>
                )}
              </div>
            </div>
          ))}
        </div>
      </section>

      <section className="admin-detail-block">
        <h3>배송지정보</h3>
        <dl className="admin-detail-list">
          <div>
            <dt>받는 분</dt>
            <dd>{order.receiverName}</dd>
          </div>
          <div>
            <dt>연락처</dt>
            <dd>{order.receiverPhone}</dd>
          </div>
          <div>
            <dt>주소</dt>
            <dd>
              ({order.zipCode}) {order.address}, {order.detailAddress}
            </dd>
          </div>
          <div>
            <dt>배송 메모</dt>
            <dd>{order.deliveryMemo || "-"}</dd>
          </div>
          <div>
            <dt>택배사</dt>
            <dd>{order.carrier || "-"}</dd>
          </div>
          <div>
            <dt>송장번호</dt>
            <dd>{order.trackingNumber || "-"}</dd>
          </div>
        </dl>
      </section>

      <div className="admin-order-action-grid">
        <form
          className="admin-inline-form admin-action-card"
          onSubmit={(event) => {
            event.preventDefault();
            setStatusModalOpen(true);
          }}
        >
          <h3>배송 처리</h3>
          <label>
            주문 상태
            <select
              value={ORDER_UPDATE_STATUSES.some((item) => item.value === status) ? status : ""}
              onChange={(event) => setStatus(event.target.value as OrderStatus)}
              disabled={!canChangeStatus}
            >
              {!canChangeStatus && <option value="">변경 불가</option>}
              {ORDER_UPDATE_STATUSES.map((item) => (
                <option key={item.value} value={item.value}>
                  {item.label}
                </option>
              ))}
            </select>
          </label>
          {status === "SHIPPED" && (
            <div className="admin-shipping-grid">
              <label>
                택배사
                <input value={carrier} onChange={(event) => setCarrier(event.target.value)} placeholder="CJ대한통운" />
              </label>
              <label>
                송장번호
                <input value={trackingNumber} onChange={(event) => setTrackingNumber(event.target.value)} placeholder="1234567890" />
              </label>
            </div>
          )}
          <button className="admin-primary-button" type="submit" disabled={!canChangeStatus}>
            상태 변경
          </button>
        </form>
      </div>

      <AdminErrorMessage message={error} />

      {statusModalOpen && (
        <div className="admin-modal-backdrop" role="dialog" aria-modal="true" aria-labelledby="admin-status-title">
          <form
            className="admin-modal-card"
            onSubmit={async (event) => {
              event.preventDefault();
              await onStatusUpdate(order.orderId, status, carrier, trackingNumber);
              setStatusModalOpen(false);
            }}
          >
            <h3 id="admin-status-title">상태 변경</h3>
            <p className="admin-modal-description">
              주문 상태를 <strong>{selectedStatusLabel}</strong> 상태로 변경하시겠습니까?
            </p>
            <div className="admin-modal-actions">
              <button className="admin-outline-button" type="button" onClick={() => setStatusModalOpen(false)}>
                닫기
              </button>
              <button className="admin-primary-button" type="submit">
                변경하기
              </button>
            </div>
          </form>
        </div>
      )}

      <div className="admin-detail-bottom-actions">
        <button className="admin-outline-button" type="button" onClick={openCancelModal}>
          주문 취소
        </button>
      </div>

      {cancelModalOpen && (
        <div className="admin-modal-backdrop" role="dialog" aria-modal="true" aria-labelledby="admin-cancel-title">
          <form
            className="admin-modal-card"
            onSubmit={async (event) => {
              event.preventDefault();
              if (cancelSubmitting) {
                return;
              }

              const trimmedCustomCancelReason = customCancelReason.trim();

              if (!cancelReasonSelection) {
                setCancelReasonError("취소 사유를 선택해주세요.");
                return;
              }

              if (cancelReasonSelection === "기타" && !trimmedCustomCancelReason) {
                setCancelReasonError("취소 사유를 입력해주세요.");
                return;
              }

              const cancelReason = cancelReasonSelection === "기타" ? trimmedCustomCancelReason : cancelReasonSelection;
              setCancelSubmitting(true);
              setCancelReasonError("");

              try {
                await onCancel(order.orderId, cancelReason);
                setCancelModalOpen(false);
              } finally {
                setCancelSubmitting(false);
              }
            }}
          >
            <h3 id="admin-cancel-title">주문 취소</h3>
            <label>
              취소 사유
              <select
                value={cancelReasonSelection}
                onChange={(event) => {
                  setCancelReasonSelection(event.target.value);
                  setCancelReasonError("");
                }}
                disabled={cancelSubmitting}
              >
                <option value="">취소 사유를 선택해주세요.</option>
                {ADMIN_CANCEL_REASON_OPTIONS.map((reason) => (
                  <option key={reason} value={reason}>
                    {reason}
                  </option>
                ))}
              </select>
              {cancelReasonSelection === "기타" && (
                <textarea
                  value={customCancelReason}
                  onChange={(event) => {
                    setCustomCancelReason(event.target.value);
                    setCancelReasonError("");
                  }}
                  placeholder="취소 사유를 입력해주세요."
                  rows={4}
                  disabled={cancelSubmitting}
                />
              )}
              <span className="admin-modal-field-hint">고객에게 안내되는 내용입니다.</span>
              {cancelReasonError && <span className="admin-modal-field-error">{cancelReasonError}</span>}
            </label>
            <p className="admin-modal-warning">취소 후에는 주문이 더 이상 진행되지 않습니다.</p>
            <div className="admin-modal-actions">
              <button
                className="admin-outline-button"
                type="button"
                onClick={() => setCancelModalOpen(false)}
                disabled={cancelSubmitting}
              >
                닫기
              </button>
              <button className="admin-primary-button" type="submit" disabled={cancelSubmitting}>
                {cancelSubmitting ? "취소 처리 중..." : "취소하기"}
              </button>
            </div>
          </form>
        </div>
      )}
    </div>
  );
}

function ProductsPanel({
  menuKey,
  initialProduct,
  onInitialProductOpened,
}: {
  menuKey: number;
  initialProduct: AdminProduct | null;
  onInitialProductOpened: () => void;
}) {
  const [products, setProducts] = useState<AdminProduct[]>([]);
  const [productsPage, setProductsPage] = useState<PageResponse<AdminProduct>>(() => createEmptyPage<AdminProduct>());
  const [currentPage, setCurrentPage] = useState(0);
  const [form, setForm] = useState<ProductSaveRequest>(emptyProductForm);
  const [editingOriginalImageUrls, setEditingOriginalImageUrls] = useState<Record<ProductImageField, string>>(
    emptyProductOriginalImageUrls,
  );
  const [editingId, setEditingId] = useState<number | null>(null);
  const [isFormView, setIsFormView] = useState(false);
  const [statusTarget, setStatusTarget] = useState<AdminProduct | null>(null);
  const [deleteTarget, setDeleteTarget] = useState<AdminProduct | null>(null);
  const [error, setError] = useState("");
  const [notice, setNotice] = useState("");

  async function loadProducts(page = currentPage) {
    setError("");

    try {
      const data = await getAdminProductsPage(page);
      setProducts(data.content);
      setProductsPage(data);
      setCurrentPage(data.page);
    } catch (loadError) {
      setError(loadError instanceof Error ? loadError.message : "상품 목록을 불러오지 못했습니다.");
    }
  }

  useEffect(() => {
    void loadProducts();
  }, [currentPage]);

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setNotice("");
    setError("");

    if (!form.imageUrl.trim()) {
      setError("대표 이미지는 필수입니다.");
      return;
    }

    if (form.category === "POSTER") {
      if (form.sizeOptions.length === 0) {
        setError("포스터 상품은 사이즈 옵션을 1개 이상 등록해주세요.");
        return;
      }

      const invalidSizeOption = form.sizeOptions.some(
        (option) => !option.sizeName.trim() || option.additionalPrice < 0 || option.stockQuantity < 0,
      );

      if (invalidSizeOption) {
        setError("사이즈명, 추가금, 재고를 확인해주세요.");
        return;
      }
    }

    const requestBody: ProductSaveRequest = {
      ...form,
      stockQuantity: form.category === "POSTER" ? 0 : form.stockQuantity,
      sizeOptions: form.category === "POSTER" ? form.sizeOptions : [],
    };

    try {
      if (editingId) {
        await updateAdminProduct(editingId, requestBody);
        setNotice("상품이 수정되었습니다.");
      } else {
        await createAdminProduct(requestBody);
        setNotice("상품이 등록되었습니다.");
      }

      setForm(emptyProductForm);
      setEditingOriginalImageUrls(emptyProductOriginalImageUrls);
      setEditingId(null);
      setIsFormView(false);
      setAdminHistory(ADMIN_TAB_PATHS.products);
      await loadProducts(currentPage);
    } catch (submitError) {
      setError(submitError instanceof Error ? submitError.message : "상품 저장에 실패했습니다.");
    }
  }

  async function handleDeactivate(productId: number) {
    setNotice("");
    setError("");

    try {
      await deactivateAdminProduct(productId);
      await loadProducts(currentPage);
    } catch (deactivateError) {
      setError(deactivateError instanceof Error ? deactivateError.message : "상품 판매중지에 실패했습니다.");
    }
  }

  async function handleActivate(productId: number) {
    setNotice("");
    setError("");

    try {
      await activateAdminProduct(productId);
      await loadProducts(currentPage);
    } catch (activateError) {
      setError(activateError instanceof Error ? activateError.message : "상품 판매재개에 실패했습니다.");
    }
  }

  async function handleStatusConfirm() {
    if (!statusTarget) {
      return;
    }

    if (statusTarget.active) {
      await handleDeactivate(statusTarget.id);
    } else {
      await handleActivate(statusTarget.id);
    }

    setStatusTarget(null);
  }

  async function handleDeleteConfirm() {
    if (!deleteTarget) {
      return;
    }

    setNotice("");
    setError("");

    try {
      await deleteAdminProduct(deleteTarget.id);
      setDeleteTarget(null);
      await loadProducts(currentPage);
    } catch (deleteError) {
      setError(deleteError instanceof Error ? deleteError.message : "상품 삭제에 실패했습니다.");
    }
  }

  function openStatusModal(product: AdminProduct) {
    setNotice("");
    setError("");
    setStatusTarget(product);
  }

  function openDeleteModal(product: AdminProduct) {
    setNotice("");
    setError("");
    setDeleteTarget(product);
  }

  function startEdit(product: AdminProduct, pushHistory = true) {
    const sizeOptions = product.sizeOptions?.map((option) => ({
      id: option.id,
      sizeName: option.sizeName,
      additionalPrice: option.additionalPrice,
      stockQuantity: option.stockQuantity,
      active: option.active,
    })) ?? [];

    setEditingId(product.id);
    setEditingOriginalImageUrls({
      imageUrl: product.imageUrl,
      detailImageUrl: product.detailImageUrl ?? "",
    });
    setForm({
      name: product.name,
      category: product.category,
      price: product.price,
      imageUrl: product.imageUrl,
      detailImageUrl: product.detailImageUrl ?? "",
      description: product.description,
      stockQuantity: product.stockQuantity,
      sizeOptions: product.category === "POSTER" ? ensurePosterSizeOptions(sizeOptions) : [],
    });
    setIsFormView(true);
    if (pushHistory) {
      setAdminHistory(`${ADMIN_TAB_PATHS.products}/${product.id}/edit`);
    }
  }

  function closeProductForm(pushHistory = true) {
    setEditingId(null);
    setForm(emptyProductForm);
    setEditingOriginalImageUrls(emptyProductOriginalImageUrls);
    setIsFormView(false);
    if (pushHistory) {
      setAdminHistory(ADMIN_TAB_PATHS.products);
    }
  }

  useEffect(() => {
    closeProductForm(false);
    setStatusTarget(null);
    setDeleteTarget(null);
    setError("");
    setNotice("");
  }, [menuKey]);

  useEffect(() => {
    if (!initialProduct) {
      return;
    }

    startEdit(initialProduct, false);
    onInitialProductOpened();
  }, [initialProduct?.id]);

  useEffect(() => {
    if (getAdminTabFromPath() !== "products") {
      return;
    }

    const productId = getAdminRouteId(ADMIN_TAB_PATHS.products);

    if (productId && normalizeAdminPath().endsWith("/edit")) {
      const product = products.find((item) => item.id === productId);

      if (product) {
        startEdit(product, false);
      }
    }
  }, [products]);

  useEffect(() => {
    const syncRoute = () => {
      if (getAdminTabFromPath() !== "products") {
        return;
      }

      const path = normalizeAdminPath();

      if (path === `${ADMIN_TAB_PATHS.products}/new`) {
        setEditingId(null);
        setForm(emptyProductForm);
        setEditingOriginalImageUrls(emptyProductOriginalImageUrls);
        setIsFormView(true);
        return;
      }

      const productId = getAdminRouteId(ADMIN_TAB_PATHS.products);

      if (productId && path.endsWith("/edit")) {
        const product = products.find((item) => item.id === productId);

        if (product) {
          startEdit(product, false);
        }

        return;
      }

      if (path === ADMIN_TAB_PATHS.products) {
        closeProductForm(false);
      }
    };

    syncRoute();
    window.addEventListener("popstate", syncRoute);
    return () => window.removeEventListener("popstate", syncRoute);
  }, [products]);

  function openCreateForm() {
    setEditingId(null);
    setForm(emptyProductForm);
    setEditingOriginalImageUrls(emptyProductOriginalImageUrls);
    setIsFormView(true);
    setAdminHistory(`${ADMIN_TAB_PATHS.products}/new`);
  }

  function closeForm() {
    closeProductForm();
  }

  if (isFormView) {
    return (
      <section className="admin-section">
        <PanelHeader
          title="상품 관리"
          action={
            <button className="admin-title-back-button" type="button" onClick={closeForm}>
              목록으로 돌아가기
              <span aria-hidden="true" />
            </button>
          }
        />
        <Feedback notice={notice} error={error} />
        <Toast message={notice} onClose={() => setNotice("")} />
        <h2 className="admin-section-title">{editingId ? "상품 수정" : "상품 등록"}</h2>
        <ProductForm
          form={form}
          editingId={editingId}
          originalImageUrls={editingOriginalImageUrls}
          onChange={setForm}
          onSubmit={handleSubmit}
          onCancel={closeForm}
        />
      </section>
    );
  }

  return (
    <section className="admin-section">
      <PanelHeader
        title="상품 관리"
        action={
          <button className="admin-outline-button" type="button" onClick={openCreateForm}>
            상품등록
          </button>
        }
      />
      <Feedback notice={notice} error={error} />
      <Toast message={notice} onClose={() => setNotice("")} />
      <ProductList products={products} onEdit={startEdit} onStatusChange={openStatusModal} onDelete={openDeleteModal} />
      <AdminPagination pageInfo={productsPage} onChangePage={setCurrentPage} />
      {statusTarget && (
        <StatusConfirmModal
          itemLabel="상품"
          itemName={statusTarget.name}
          isActive={statusTarget.active}
          onCancel={() => setStatusTarget(null)}
          onConfirm={handleStatusConfirm}
        />
      )}
      {deleteTarget && (
        <DeleteConfirmModal
          itemLabel="상품"
          itemName={deleteTarget.name}
          onCancel={() => setDeleteTarget(null)}
          onConfirm={handleDeleteConfirm}
        />
      )}
    </section>
  );
}

function ProductForm({
  form,
  editingId,
  originalImageUrls,
  onChange,
  onSubmit,
  onCancel,
}: {
  form: ProductSaveRequest;
  editingId: number | null;
  originalImageUrls: Record<ProductImageField, string>;
  onChange: (form: ProductSaveRequest) => void;
  onSubmit: (event: FormEvent<HTMLFormElement>) => void;
  onCancel: () => void;
}) {
  const imageInputRef = useRef<HTMLInputElement | null>(null);
  const detailImageInputRef = useRef<HTMLInputElement | null>(null);
  const lastUploadedImageUrlsRef = useRef<Record<ProductImageField, string>>({
    imageUrl: "",
    detailImageUrl: "",
  });
  const currentImageUrlsRef = useRef<Record<ProductImageField, string>>({
    imageUrl: form.imageUrl,
    detailImageUrl: form.detailImageUrl,
  });
  const [uploadingField, setUploadingField] = useState<ProductImageField | null>(null);
  const [uploadError, setUploadError] = useState<string | null>(null);

  useEffect(() => {
    currentImageUrlsRef.current = {
      imageUrl: form.imageUrl,
      detailImageUrl: form.detailImageUrl,
    };
  }, [form.imageUrl, form.detailImageUrl]);

  async function uploadImageFile(file: File, field: ProductImageField) {
    const extensionStart = file.name.lastIndexOf(".");
    const fileExtension = extensionStart >= 0 ? file.name.slice(extensionStart).toLowerCase() : "";

    if (!ADMIN_IMAGE_ALLOWED_TYPES.has(file.type) || !ADMIN_IMAGE_ALLOWED_EXTENSIONS.has(fileExtension)) {
      setUploadError("jpg, png, webp 이미지만 업로드할 수 있습니다.");
      return;
    }

    if (file.size > ADMIN_IMAGE_MAX_SIZE) {
      setUploadError("이미지는 최대 5MB까지 업로드할 수 있습니다.");
      return;
    }

    setUploadingField(field);
    setUploadError(null);

    try {
      const response =
        field === "imageUrl"
          ? await uploadAdminProductImage(file)
          : await uploadAdminProductDetailImage(file);

      currentImageUrlsRef.current[field] = response.imageUrl;
      onChange({ ...form, [field]: response.imageUrl });

      const previousUploadedImageUrl = lastUploadedImageUrlsRef.current[field];

      if (
        previousUploadedImageUrl &&
        previousUploadedImageUrl !== response.imageUrl &&
        previousUploadedImageUrl !== originalImageUrls[field]
      ) {
        void deleteAdminImage(previousUploadedImageUrl)
          .catch((deleteError) => {
            console.warn("미사용 이미지 삭제 요청에 실패했습니다.", deleteError);
          });
      }

      lastUploadedImageUrlsRef.current[field] = response.imageUrl;
    } catch (error) {
      setUploadError(error instanceof Error ? error.message : "이미지 업로드 실패");
    } finally {
      setUploadingField(null);
    }
  }

  function handleImageDrop(event: DragEvent<HTMLInputElement>, field: ProductImageField) {
    event.preventDefault();

    const file = event.dataTransfer.files[0];

    if (!file) {
      return;
    }

    void uploadImageFile(file, field);
  }

  function handleImageSelect(event: ChangeEvent<HTMLInputElement>, field: ProductImageField) {
    const file = event.target.files?.[0];
    event.target.value = "";

    if (!file) {
      return;
    }

    void uploadImageFile(file, field);
  }

  function updateCategory(category: ProductCategory) {
    onChange({
      ...form,
      category,
      stockQuantity: category === "POSTER" ? 0 : form.stockQuantity,
      sizeOptions: category === "POSTER" ? ensurePosterSizeOptions(form.sizeOptions) : [],
    });
  }

  function addSizeOption() {
    onChange({
      ...form,
      sizeOptions: [
        ...form.sizeOptions,
        createEmptySizeOption(),
      ],
    });
  }

  function updateSizeOption(index: number, patch: Partial<ProductSizeOptionSaveRequest>) {
    onChange({
      ...form,
      sizeOptions: form.sizeOptions.map((option, optionIndex) =>
        optionIndex === index ? { ...option, ...patch } : option,
      ),
    });
  }

  function removeSizeOption(index: number) {
    const nextSizeOptions = form.sizeOptions.filter((_, optionIndex) => optionIndex !== index);

    onChange({
      ...form,
      sizeOptions: form.category === "POSTER" ? ensurePosterSizeOptions(nextSizeOptions) : nextSizeOptions,
    });
  }

  return (
    <form className="admin-form admin-management-form" onSubmit={onSubmit}>
      <label>
        상품명
        <input value={form.name} onChange={(event) => onChange({ ...form, name: event.target.value })} />
      </label>
      <label>
        카테고리
        <select
          className="admin-product-category-select"
          value={form.category}
          onChange={(event) => updateCategory(event.target.value as ProductCategory)}
        >
          {PRODUCT_CATEGORIES.map((category) => (
            <option key={category.value} value={category.value}>
              {category.label}
            </option>
          ))}
        </select>
      </label>
      <label>
        가격
        <input type="number" min="0" value={form.price} onChange={(event) => onChange({ ...form, price: Number(event.target.value) })} />
      </label>
      {form.category === "POSTER" ? (
        <div className="admin-size-option-field">
          <div className="admin-size-option-header">
            <span>사이즈 옵션</span>
            <button className="admin-outline-button" type="button" onClick={addSizeOption}>
              사이즈 추가
            </button>
          </div>
          <div className="admin-size-option-list">
            {form.sizeOptions.map((option, index) => (
              <div className="admin-size-option-row" key={`${option.id ?? "new"}-${index}`}>
                <label>
                  사이즈명
                  <input
                    value={option.sizeName}
                    onChange={(event) => updateSizeOption(index, { sizeName: event.target.value })}
                  />
                </label>
                <label>
                  추가금
                  <input
                    type="number"
                    min="0"
                    value={option.additionalPrice}
                    onChange={(event) => updateSizeOption(index, { additionalPrice: Number(event.target.value) })}
                  />
                </label>
                <label>
                  재고
                  <input
                    type="number"
                    min="0"
                    value={option.stockQuantity}
                    onChange={(event) => updateSizeOption(index, { stockQuantity: Number(event.target.value) })}
                  />
                </label>
                <label className="admin-size-option-active">
                  활성
                  <input
                    type="checkbox"
                    checked={option.active}
                    onChange={(event) => updateSizeOption(index, { active: event.target.checked })}
                  />
                </label>
                <button className="admin-outline-button" type="button" onClick={() => removeSizeOption(index)}>
                  삭제
                </button>
              </div>
            ))}
          </div>
        </div>
      ) : (
        <label>
          재고 수량
          <input
            type="number"
            min="0"
            value={form.stockQuantity}
            onChange={(event) => onChange({ ...form, stockQuantity: Number(event.target.value) })}
          />
        </label>
      )}
      <label>
        대표 이미지
        <div className="admin-image-field">
          <input
            value={form.imageUrl}
            onChange={(event) => onChange({ ...form, imageUrl: event.target.value })}
            onDragOver={(event) => event.preventDefault()}
            onDrop={(event) => handleImageDrop(event, "imageUrl")}
          />
          <input
            ref={imageInputRef}
            className="admin-file-input"
            type="file"
            accept=".jpg,.jpeg,.png,.webp,image/jpeg,image/png,image/webp"
            onChange={(event) => handleImageSelect(event, "imageUrl")}
          />
          <button
            className="admin-outline-button"
            type="button"
            disabled={uploadingField === "imageUrl"}
            onClick={() => imageInputRef.current?.click()}
          >
            {uploadingField === "imageUrl" ? "업로드중" : "파일 선택"}
          </button>
        </div>
        {form.imageUrl && (
          <div className="admin-image-preview">
            <img src={form.imageUrl} alt="상품 대표 이미지 미리보기" />
          </div>
        )}
      </label>
      <label>
        상세 이미지
        <div className="admin-image-field">
          <input
            value={form.detailImageUrl}
            onChange={(event) => onChange({ ...form, detailImageUrl: event.target.value })}
            onDragOver={(event) => event.preventDefault()}
            onDrop={(event) => handleImageDrop(event, "detailImageUrl")}
          />
          <input
            ref={detailImageInputRef}
            className="admin-file-input"
            type="file"
            accept=".jpg,.jpeg,.png,.webp,image/jpeg,image/png,image/webp"
            onChange={(event) => handleImageSelect(event, "detailImageUrl")}
          />
          <button
            className="admin-outline-button"
            type="button"
            disabled={uploadingField === "detailImageUrl"}
            onClick={() => detailImageInputRef.current?.click()}
          >
            {uploadingField === "detailImageUrl" ? "업로드중" : "파일 선택"}
          </button>
        </div>
        {form.detailImageUrl && (
          <div className="admin-image-preview admin-image-preview-detail">
            <img src={form.detailImageUrl} alt="상품 상세 이미지 미리보기" />
          </div>
        )}
      </label>
      {uploadError && <p className="admin-inline-error">{uploadError}</p>}
      <label>
        설명
        <textarea value={form.description} onChange={(event) => onChange({ ...form, description: event.target.value })} rows={4} />
      </label>
      <div className="admin-button-row">
        <button className="admin-outline-button" type="button" onClick={onCancel}>
          닫기
        </button>
        <button className="admin-primary-button" type="submit">
          {editingId ? "수정하기" : "등록하기"}
        </button>
      </div>
    </form>
  );
}

function ProductList({
  products,
  onEdit,
  onStatusChange,
  onDelete,
}: {
  products: AdminProduct[];
  onEdit: (product: AdminProduct) => void;
  onStatusChange: (product: AdminProduct) => void;
  onDelete: (product: AdminProduct) => void;
}) {
  return (
    <>
      <h2 className="admin-section-title">상품 목록</h2>
      <div className="admin-card admin-list-card">
        <div className="admin-table-wrap">
          <table className="admin-table admin-product-table">
            <thead>
              <tr>
                <th>상품</th>
                <th>카테고리</th>
                <th>가격</th>
                <th>재고</th>
                <th>상태</th>
                <th>관리</th>
              </tr>
            </thead>
            <tbody>
              {products.length === 0 ? (
                <tr>
                  <td className="admin-empty-row" colSpan={6}>
                    해당하는 상품이 없습니다.
                  </td>
                </tr>
              ) : (
                products.map((product) => (
                  <tr key={product.id} className={product.active ? undefined : "is-private"}>
                    <td>
                      <div className="admin-product-cell">
                        <img src={product.imageUrl} alt={product.name} />
                        <span>{product.name}</span>
                      </div>
                    </td>
                    <td className="admin-center-cell">{product.categoryDescription}</td>
                    <td className="admin-price-cell">{formatNumber(product.price)}</td>
                    <td className="admin-center-cell">{formatProductStock(product)}</td>
                    <td className="admin-center-cell">{product.active ? "판매중" : "판매중지"}</td>
                    <td>
                      <div className="admin-mini-actions">
                        <button type="button" onClick={() => onEdit(product)}>
                          수정
                        </button>
                        {product.active ? (
                          <button type="button" onClick={() => onStatusChange(product)}>
                            판매중지
                          </button>
                        ) : (
                          <button type="button" onClick={() => onStatusChange(product)}>
                            판매재개
                          </button>
                        )}
                        <button type="button" onClick={() => onDelete(product)}>
                          삭제
                        </button>
                      </div>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </div>
    </>
  );
}

function formatProductStock(product: AdminProduct) {
  if (product.category !== "POSTER") {
    return product.stockQuantity;
  }

  if (!product.sizeOptions || product.sizeOptions.length === 0) {
    return "-";
  }

  return product.sizeOptions
    .map((option) => `${option.sizeName} ${option.stockQuantity}`)
    .join(" / ");
}

function AddonsPanel({
  menuKey,
  initialAddon,
  onInitialAddonOpened,
}: {
  menuKey: number;
  initialAddon: AdminAddon | null;
  onInitialAddonOpened: () => void;
}) {
  const [addons, setAddons] = useState<AdminAddon[]>([]);
  const [addonsPage, setAddonsPage] = useState<PageResponse<AdminAddon>>(() => createEmptyPage<AdminAddon>());
  const [currentPage, setCurrentPage] = useState(0);
  const [form, setForm] = useState<AddonSaveRequest>(emptyAddonForm);
  const [editingId, setEditingId] = useState<number | null>(null);
  const [isFormView, setIsFormView] = useState(false);
  const [statusTarget, setStatusTarget] = useState<AdminAddon | null>(null);
  const [deleteTarget, setDeleteTarget] = useState<AdminAddon | null>(null);
  const [error, setError] = useState("");
  const [notice, setNotice] = useState("");

  async function loadAddons(page = currentPage) {
    setError("");

    try {
      const data = await getAdminAddonsPage(page);
      setAddons(data.content);
      setAddonsPage(data);
      setCurrentPage(data.page);
    } catch (loadError) {
      setError(loadError instanceof Error ? loadError.message : "추가상품 목록을 불러오지 못했습니다.");
    }
  }

  useEffect(() => {
    void loadAddons();
  }, [currentPage]);

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setNotice("");
    setError("");

    try {
      if (editingId) {
        await updateAdminAddon(editingId, form);
        setNotice("추가상품이 수정되었습니다.");
      } else {
        await createAdminAddon(form);
        setNotice("추가상품이 등록되었습니다.");
      }

      setForm(emptyAddonForm);
      setEditingId(null);
      setIsFormView(false);
      setAdminHistory(ADMIN_TAB_PATHS.addons);
      await loadAddons(currentPage);
    } catch (submitError) {
      setError(submitError instanceof Error ? submitError.message : "추가상품 저장에 실패했습니다.");
    }
  }

  async function handleDeactivate(addonId: number) {
    setNotice("");
    setError("");

    try {
      await deactivateAdminAddon(addonId);
      await loadAddons(currentPage);
    } catch (deactivateError) {
      setError(deactivateError instanceof Error ? deactivateError.message : "추가상품 판매중지에 실패했습니다.");
    }
  }

  async function handleActivate(addonId: number) {
    setNotice("");
    setError("");

    try {
      await activateAdminAddon(addonId);
      await loadAddons(currentPage);
    } catch (activateError) {
      setError(activateError instanceof Error ? activateError.message : "추가상품 판매재개에 실패했습니다.");
    }
  }

  async function handleStatusConfirm() {
    if (!statusTarget) {
      return;
    }

    if (statusTarget.active) {
      await handleDeactivate(statusTarget.id);
    } else {
      await handleActivate(statusTarget.id);
    }

    setStatusTarget(null);
  }

  async function handleDeleteConfirm() {
    if (!deleteTarget) {
      return;
    }

    setNotice("");
    setError("");

    try {
      await deleteAdminAddon(deleteTarget.id);
      setDeleteTarget(null);
      await loadAddons(currentPage);
    } catch (deleteError) {
      setError(deleteError instanceof Error ? deleteError.message : "추가상품 삭제에 실패했습니다.");
    }
  }

  function openStatusModal(addon: AdminAddon) {
    setNotice("");
    setError("");
    setStatusTarget(addon);
  }

  function openDeleteModal(addon: AdminAddon) {
    setNotice("");
    setError("");
    setDeleteTarget(addon);
  }

  function startEdit(addon: AdminAddon, pushHistory = true) {
    setEditingId(addon.id);
    setForm({
      name: addon.name,
      type: addon.type,
      price: addon.price,
      stockQuantity: addon.stockQuantity,
    });
    setIsFormView(true);
    if (pushHistory) {
      setAdminHistory(`${ADMIN_TAB_PATHS.addons}/${addon.id}/edit`);
    }
  }

  function closeAddonForm(pushHistory = true) {
    setEditingId(null);
    setForm(emptyAddonForm);
    setIsFormView(false);
    if (pushHistory) {
      setAdminHistory(ADMIN_TAB_PATHS.addons);
    }
  }

  useEffect(() => {
    closeAddonForm(false);
    setStatusTarget(null);
    setDeleteTarget(null);
    setError("");
    setNotice("");
  }, [menuKey]);

  useEffect(() => {
    if (!initialAddon) {
      return;
    }

    startEdit(initialAddon, false);
    onInitialAddonOpened();
  }, [initialAddon?.id]);

  useEffect(() => {
    if (getAdminTabFromPath() !== "addons") {
      return;
    }

    const addonId = getAdminRouteId(ADMIN_TAB_PATHS.addons);

    if (addonId && normalizeAdminPath().endsWith("/edit")) {
      const addon = addons.find((item) => item.id === addonId);

      if (addon) {
        startEdit(addon, false);
      }
    }
  }, [addons]);

  useEffect(() => {
    const syncRoute = () => {
      if (getAdminTabFromPath() !== "addons") {
        return;
      }

      const path = normalizeAdminPath();

      if (path === `${ADMIN_TAB_PATHS.addons}/new`) {
        setEditingId(null);
        setForm(emptyAddonForm);
        setIsFormView(true);
        return;
      }

      const addonId = getAdminRouteId(ADMIN_TAB_PATHS.addons);

      if (addonId && path.endsWith("/edit")) {
        const addon = addons.find((item) => item.id === addonId);

        if (addon) {
          startEdit(addon, false);
        }

        return;
      }

      if (path === ADMIN_TAB_PATHS.addons) {
        closeAddonForm(false);
      }
    };

    syncRoute();
    window.addEventListener("popstate", syncRoute);
    return () => window.removeEventListener("popstate", syncRoute);
  }, [addons]);

  function openCreateForm() {
    setEditingId(null);
    setForm(emptyAddonForm);
    setIsFormView(true);
    setAdminHistory(`${ADMIN_TAB_PATHS.addons}/new`);
  }

  function closeForm() {
    closeAddonForm();
  }

  if (isFormView) {
    const addonTypeOptions: Array<{ value: AddonType; label: string }> =
      form.type === "FRAME" ? [...ADDON_TYPES, LEGACY_ADDON_TYPE] : ADDON_TYPES;

    return (
      <section className="admin-section">
        <PanelHeader
          title="추가상품 관리"
          action={
            <button className="admin-title-back-button" type="button" onClick={closeForm}>
              목록으로 돌아가기
              <span aria-hidden="true" />
            </button>
          }
        />
        <Feedback notice={notice} error={error} />
        <Toast message={notice} onClose={() => setNotice("")} />
        <h2 className="admin-section-title">{editingId ? "추가상품 수정" : "추가상품 등록"}</h2>
        <form className="admin-form admin-management-form" onSubmit={handleSubmit}>
          <label>
            추가상품명
            <input value={form.name} onChange={(event) => setForm({ ...form, name: event.target.value })} />
          </label>
          <label>
            종류
            <select value={form.type} onChange={(event) => setForm({ ...form, type: event.target.value as AddonType })}>
              {addonTypeOptions.map((type) => (
                <option key={type.value} value={type.value}>
                  {type.label}
                </option>
              ))}
            </select>
          </label>
          <label>
            가격
            <input type="number" min="0" value={form.price} onChange={(event) => setForm({ ...form, price: Number(event.target.value) })} />
          </label>
          <label>
            재고 수량
            <input
              type="number"
              min="0"
              value={form.stockQuantity}
              onChange={(event) => setForm({ ...form, stockQuantity: Number(event.target.value) })}
            />
          </label>
          <div className="admin-button-row">
            <button className="admin-outline-button" type="button" onClick={closeForm}>
              닫기
            </button>
            <button className="admin-primary-button" type="submit">
              {editingId ? "수정하기" : "등록하기"}
            </button>
          </div>
        </form>
      </section>
    );
  }

  return (
    <section className="admin-section">
      <PanelHeader
        title="추가상품 관리"
        action={
          <button className="admin-outline-button" type="button" onClick={openCreateForm}>
            추가상품등록
          </button>
        }
      />
      <Feedback notice={notice} error={error} />
      <Toast message={notice} onClose={() => setNotice("")} />
      <h2 className="admin-section-title">추가상품 목록</h2>
      <div className="admin-card admin-list-card">
        <div className="admin-table-wrap">
          <table className="admin-table admin-addon-table">
            <thead>
              <tr>
                <th>이름</th>
                <th>종류</th>
                <th>가격</th>
                <th>재고</th>
                <th>상태</th>
                <th>관리</th>
              </tr>
            </thead>
            <tbody>
              {addons.length === 0 ? (
                <tr>
                  <td className="admin-empty-row" colSpan={6}>
                    해당하는 추가상품이 없습니다.
                  </td>
                </tr>
              ) : (
                addons.map((addon) => (
                  <tr key={addon.id} className={addon.active ? undefined : "is-private"}>
                    <td>{addon.name}</td>
                    <td className="admin-center-cell">{addon.typeDescription}</td>
                    <td className="admin-price-cell">{formatNumber(addon.price)}</td>
                    <td className="admin-center-cell">{addon.stockQuantity}</td>
                    <td className="admin-center-cell">{addon.active ? "판매중" : "판매중지"}</td>
                    <td>
                      <div className="admin-mini-actions">
                        <button type="button" onClick={() => startEdit(addon)}>
                          수정
                        </button>
                        {addon.active ? (
                          <button type="button" onClick={() => openStatusModal(addon)}>
                            판매중지
                          </button>
                        ) : (
                          <button type="button" onClick={() => openStatusModal(addon)}>
                            판매재개
                          </button>
                        )}
                        <button type="button" onClick={() => openDeleteModal(addon)}>
                          삭제
                        </button>
                      </div>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </div>
      <AdminPagination pageInfo={addonsPage} onChangePage={setCurrentPage} />
      {statusTarget && (
        <StatusConfirmModal
          itemLabel="추가상품"
          itemName={statusTarget.name}
          isActive={statusTarget.active}
          onCancel={() => setStatusTarget(null)}
          onConfirm={handleStatusConfirm}
        />
      )}
      {deleteTarget && (
        <DeleteConfirmModal
          itemLabel="추가상품"
          itemName={deleteTarget.name}
          onCancel={() => setDeleteTarget(null)}
          onConfirm={handleDeleteConfirm}
        />
      )}
    </section>
  );
}

function AdminPagination<T>({
  pageInfo,
  onChangePage,
}: {
  pageInfo: PageResponse<T>;
  onChangePage: (page: number) => void;
}) {
  if (pageInfo.totalPages <= 1) {
    return null;
  }

  return (
    <nav className="admin-pagination" aria-label="페이지 이동">
      <button className="is-prev" type="button" aria-label="이전 페이지" disabled={pageInfo.first} onClick={() => onChangePage(pageInfo.page - 1)}>
        <span aria-hidden="true" />
      </button>
      <span>
        {pageInfo.page + 1} / {pageInfo.totalPages}
      </span>
      <button className="is-next" type="button" aria-label="다음 페이지" disabled={pageInfo.last} onClick={() => onChangePage(pageInfo.page + 1)}>
        <span aria-hidden="true" />
      </button>
    </nav>
  );
}

function DeleteConfirmModal({
  itemLabel,
  itemName,
  onCancel,
  onConfirm,
}: {
  itemLabel: string;
  itemName: string;
  onCancel: () => void;
  onConfirm: () => Promise<void>;
}) {
  return (
    <div className="admin-modal-backdrop" role="dialog" aria-modal="true" aria-labelledby="admin-delete-title">
      <form
        className="admin-modal-card"
        onSubmit={async (event) => {
          event.preventDefault();
          await onConfirm();
        }}
      >
        <h3 id="admin-delete-title">{itemLabel} 삭제</h3>
        <p className="admin-modal-description">
          {itemLabel} <strong>{itemName}</strong>을(를) 삭제하시겠습니까?
          <br />
          삭제 후 기본 목록에서 보이지 않으며, 고객 장바구니에서도 제거됩니다.
        </p>
        <div className="admin-modal-actions">
          <button className="admin-outline-button" type="button" onClick={onCancel}>
            닫기
          </button>
          <button className="admin-primary-button" type="submit">
            삭제하기
          </button>
        </div>
      </form>
    </div>
  );
}

function StatusConfirmModal({
  itemLabel,
  itemName,
  isActive,
  onCancel,
  onConfirm,
}: {
  itemLabel: string;
  itemName: string;
  isActive: boolean;
  onCancel: () => void;
  onConfirm: () => Promise<void>;
}) {
  const actionLabel = isActive ? "판매중지" : "판매재개";

  return (
    <div className="admin-modal-backdrop" role="dialog" aria-modal="true" aria-labelledby="admin-sale-status-title">
      <form
        className="admin-modal-card"
        onSubmit={async (event) => {
          event.preventDefault();
          await onConfirm();
        }}
      >
        <h3 id="admin-sale-status-title">판매 상태 변경</h3>
        <p className="admin-modal-description">
          {itemLabel} <strong>{itemName}</strong>을(를) {actionLabel}하시겠습니까?
        </p>
        <div className="admin-modal-actions">
          <button className="admin-outline-button" type="button" onClick={onCancel}>
            닫기
          </button>
          <button className="admin-primary-button" type="submit">
            확인
          </button>
        </div>
      </form>
    </div>
  );
}

function NoticeVisibilityConfirmModal({
  notice,
  onCancel,
  onConfirm,
}: {
  notice: Notice;
  onCancel: () => void;
  onConfirm: () => Promise<void>;
}) {
  const actionLabel = notice.visible ? "비공개" : "공개";

  return (
    <div className="admin-modal-backdrop" role="dialog" aria-modal="true" aria-labelledby="admin-notice-visibility-title">
      <form
        className="admin-modal-card"
        onSubmit={async (event) => {
          event.preventDefault();
          await onConfirm();
        }}
      >
        <h3 id="admin-notice-visibility-title">공지 공개상태 변경</h3>
        <p className="admin-modal-description">
          <strong>{notice.title}</strong>을(를) {actionLabel}하시겠습니까?
        </p>
        <div className="admin-modal-actions">
          <button className="admin-outline-button" type="button" onClick={onCancel}>
            닫기
          </button>
          <button className="admin-primary-button" type="submit">
            확인
          </button>
        </div>
      </form>
    </div>
  );
}

function CustomersPanel() {
  const [members, setMembers] = useState<AdminMember[]>([]);
  const [membersPage, setMembersPage] = useState<PageResponse<AdminMember>>(() => createEmptyPage<AdminMember>());
  const [status, setStatus] = useState<MemberStatusFilter>("ALL");
  const [currentPage, setCurrentPage] = useState(0);
  const [selectedMember, setSelectedMember] = useState<AdminMember | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  async function loadMembers(page = currentPage) {
    setLoading(true);
    setError("");

    try {
      const data = await getAdminMembersPage(status, page);
      setMembers(data.content);
      setMembersPage(data);
      setCurrentPage(data.page);
    } catch (loadError) {
      setError(loadError instanceof Error ? loadError.message : "고객 목록을 불러오지 못했습니다.");
    } finally {
      setLoading(false);
    }
  }

  async function openMember(memberId: number, pushHistory = true) {
    setError("");

    try {
      setSelectedMember(await getAdminMember(memberId));
      if (pushHistory) {
        setAdminHistory(`${ADMIN_TAB_PATHS.customers}/${memberId}`);
      }
    } catch (loadError) {
      setError(loadError instanceof Error ? loadError.message : "고객 상세를 불러오지 못했습니다.");
    }
  }

  function closeMemberDetail(pushHistory = true) {
    setSelectedMember(null);
    if (pushHistory) {
      setAdminHistory(ADMIN_TAB_PATHS.customers);
    }
  }

  useEffect(() => {
    void loadMembers();
  }, [status, currentPage]);

  useEffect(() => {
    const syncRoute = () => {
      if (getAdminTabFromPath() !== "customers") {
        return;
      }

      const memberId =
        getAdminRouteId(ADMIN_TAB_PATHS.customers) ??
        getAdminRouteId("/admin/members");

      if (memberId) {
        void openMember(memberId, false);
        return;
      }

      closeMemberDetail(false);
    };

    syncRoute();
    window.addEventListener("popstate", syncRoute);
    return () => window.removeEventListener("popstate", syncRoute);
  }, []);

  return (
    <section className="admin-section">
      <PanelHeader title="고객관리" />
      <Feedback notice="" error={error} />
      <div className="admin-list-top-filter">
        <select
          aria-label="회원상태"
          value={status}
          onChange={(event) => {
            setCurrentPage(0);
            setStatus(event.target.value as MemberStatusFilter);
          }}
        >
          {MEMBER_STATUS_FILTERS.map((item) => (
            <option key={item.value} value={item.value}>
              {item.label}
            </option>
          ))}
        </select>
      </div>
      <div className="admin-card admin-list-card admin-community-list-card">
        {loading ? (
          <p className="admin-empty">고객 목록을 불러오는 중입니다.</p>
        ) : (
          <div className="admin-table-wrap">
            <table className="admin-table admin-community-table admin-customer-table">
              <thead>
                <tr>
                  <th>이메일</th>
                  <th>이름</th>
                  <th>연락처</th>
                  <th>상태</th>
                  <th>가입일</th>
                </tr>
              </thead>
              <tbody>
                {members.length === 0 ? (
                  <tr>
                    <td className="admin-empty-row" colSpan={5}>
                      해당하는 고객이 없습니다.
                    </td>
                  </tr>
                ) : (
                  members.map((member) => (
                    <tr key={member.id}>
                      <td>
                        <button className="admin-order-number-button" type="button" onClick={() => void openMember(member.id)}>
                          {member.email}
                        </button>
                      </td>
                      <td className="admin-center-cell">{member.name}</td>
                      <td className="admin-center-cell">{member.phone}</td>
                      <td className="admin-center-cell">{member.active ? "활성" : "탈퇴"}</td>
                      <td className="admin-center-cell">{formatDateOnly(member.createdAt)}</td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>
        )}
      </div>
      <AdminPagination pageInfo={membersPage} onChangePage={setCurrentPage} />

      {selectedMember && (
        <div className="admin-modal-backdrop" role="dialog" aria-modal="true" aria-labelledby="admin-member-title">
          <section className="admin-modal-card">
            <h3 id="admin-member-title">고객 상세</h3>
            <dl className="admin-detail-list">
              <div>
                <dt>이메일</dt>
                <dd>{selectedMember.email}</dd>
              </div>
              <div>
                <dt>이름</dt>
                <dd>{selectedMember.name}</dd>
              </div>
              <div>
                <dt>연락처</dt>
                <dd>{selectedMember.phone}</dd>
              </div>
              <div>
                <dt>상태</dt>
                <dd>{selectedMember.active ? "활성" : "탈퇴"}</dd>
              </div>
              <div>
                <dt>가입일</dt>
                <dd>{formatDate(selectedMember.createdAt)}</dd>
              </div>
            </dl>
            <div className="admin-modal-actions admin-member-modal-actions">
              <button className="admin-primary-button" type="button" onClick={() => closeMemberDetail()}>
                확인
              </button>
            </div>
          </section>
        </div>
      )}
    </section>
  );
}

function NoticesPanel() {
  const [notices, setNotices] = useState<Notice[]>([]);
  const [noticesPage, setNoticesPage] = useState<PageResponse<Notice>>(() => createEmptyPage<Notice>());
  const [keyword, setKeyword] = useState("");
  const [appliedKeyword, setAppliedKeyword] = useState("");
  const [visibility, setVisibility] = useState<NoticeVisibilityFilter>("ALL");
  const [currentPage, setCurrentPage] = useState(0);
  const [form, setForm] = useState<NoticeSaveRequest>(emptyNoticeForm);
  const [editingId, setEditingId] = useState<number | null>(null);
  const [isFormOpen, setIsFormOpen] = useState(false);
  const [selectedNotice, setSelectedNotice] = useState<Notice | null>(null);
  const [visibilityTarget, setVisibilityTarget] = useState<Notice | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [notice, setNotice] = useState("");

  async function loadNotices(page = currentPage) {
    setLoading(true);
    setError("");

    try {
      const data = await getAdminNoticesPage(appliedKeyword, visibility, page);
      setNotices(data.content);
      setNoticesPage(data);
      setCurrentPage(data.page);
    } catch (loadError) {
      setError(loadError instanceof Error ? loadError.message : "공지사항을 불러오지 못했습니다.");
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    void loadNotices();
  }, [appliedKeyword, visibility, currentPage]);

  function closeNoticeView(pushHistory = true) {
    setSelectedNotice(null);
    setIsFormOpen(false);
    setEditingId(null);
    setForm(emptyNoticeForm);
    if (pushHistory) {
      setAdminHistory(ADMIN_TAB_PATHS.notices);
    }
  }

  function openCreateForm() {
    setEditingId(null);
    setForm(emptyNoticeForm);
    setSelectedNotice(null);
    setVisibilityTarget(null);
    setIsFormOpen(true);
    setNotice("");
    setError("");
    setAdminHistory(`${ADMIN_TAB_PATHS.notices}/new`);
  }

  function openEditForm(item: Notice, pushHistory = true) {
    setEditingId(item.id);
    setForm({ title: item.title, content: item.content });
    setSelectedNotice(null);
    setVisibilityTarget(null);
    setIsFormOpen(true);
    setNotice("");
    setError("");
    if (pushHistory) {
      setAdminHistory(`${ADMIN_TAB_PATHS.notices}/${item.id}/edit`);
    }
  }

  async function openNotice(noticeId: number, pushHistory = true) {
    setNotice("");
    setError("");

    try {
      setSelectedNotice(await getAdminNotice(noticeId));
      setIsFormOpen(false);
      setEditingId(null);
      if (pushHistory) {
        setAdminHistory(`${ADMIN_TAB_PATHS.notices}/${noticeId}`);
      }
    } catch (loadError) {
      setError(loadError instanceof Error ? loadError.message : "공지 상세를 불러오지 못했습니다.");
    }
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setNotice("");
    setError("");

    try {
      if (editingId) {
        await updateAdminNotice(editingId, form);
        setNotice("공지사항이 수정되었습니다.");
      } else {
        await createAdminNotice(form);
        setNotice("공지사항이 등록되었습니다.");
      }

      setIsFormOpen(false);
      setEditingId(null);
      setForm(emptyNoticeForm);
      setAdminHistory(ADMIN_TAB_PATHS.notices);
      await loadNotices(currentPage);
    } catch (submitError) {
      setError(submitError instanceof Error ? submitError.message : "공지사항 저장에 실패했습니다.");
    }
  }

  async function toggleVisibility(item: Notice) {
    setNotice("");
    setError("");

    try {
      if (item.visible) {
        await hideAdminNotice(item.id);
      } else {
        await showAdminNotice(item.id);
      }

      setVisibilityTarget(null);
      await loadNotices(currentPage);
    } catch (submitError) {
      setError(submitError instanceof Error ? submitError.message : "공개 상태 변경에 실패했습니다.");
    }
  }

  useEffect(() => {
    const syncRoute = () => {
      if (getAdminTabFromPath() !== "notices") {
        return;
      }

      const path = normalizeAdminPath();
      const noticeId = getAdminRouteId(ADMIN_TAB_PATHS.notices);

      if (path === `${ADMIN_TAB_PATHS.notices}/new`) {
        setEditingId(null);
        setForm(emptyNoticeForm);
        setSelectedNotice(null);
        setIsFormOpen(true);
        return;
      }

      if (noticeId && path.endsWith("/edit")) {
        void getAdminNotice(noticeId)
          .then((item) => openEditForm(item, false))
          .catch((loadError) => setError(loadError instanceof Error ? loadError.message : "공지 상세를 불러오지 못했습니다."));
        return;
      }

      if (noticeId) {
        void openNotice(noticeId, false);
        return;
      }

      closeNoticeView(false);
    };

    syncRoute();
    window.addEventListener("popstate", syncRoute);
    return () => window.removeEventListener("popstate", syncRoute);
  }, []);

  if (selectedNotice) {
    return (
      <section className="admin-section">
        <PanelHeader
          title="공지관리"
          action={
            <button className="admin-title-back-button" type="button" onClick={() => closeNoticeView()}>
              목록으로 돌아가기 <span aria-hidden="true" />
            </button>
          }
        />
        <section className="admin-card admin-page-form">
          <h2>{selectedNotice.title}</h2>
          <dl className="admin-detail-list">
            <div>
              <dt>공개상태</dt>
              <dd>{selectedNotice.visibleDescription}</dd>
            </div>
            <div>
              <dt>등록일</dt>
              <dd>{formatDateOnly(selectedNotice.createdAt)}</dd>
            </div>
          </dl>
          <p className="admin-readable-text">{selectedNotice.content}</p>
        </section>
      </section>
    );
  }

  if (isFormOpen) {
    return (
      <section className="admin-section">
        <PanelHeader
          title="공지관리"
          action={
            <button className="admin-title-back-button" type="button" onClick={() => closeNoticeView()}>
              목록으로 돌아가기 <span aria-hidden="true" />
            </button>
          }
        />
        <Feedback notice={notice} error={error} />
        <form className="admin-card admin-page-form admin-form" onSubmit={handleSubmit}>
          <h2>{editingId ? "공지 수정" : "공지 등록"}</h2>
          <label>
            제목
            <input value={form.title} onChange={(event) => setForm({ ...form, title: event.target.value })} />
          </label>
          <label>
            내용
            <textarea value={form.content} onChange={(event) => setForm({ ...form, content: event.target.value })} rows={18} />
          </label>
          <div className="admin-button-row">
            <button className="admin-outline-button" type="button" onClick={() => closeNoticeView()}>
              취소
            </button>
            <button className="admin-primary-button" type="submit">
              {editingId ? "수정하기" : "등록하기"}
            </button>
          </div>
        </form>
      </section>
    );
  }

  return (
    <section className="admin-section">
      <PanelHeader
        title="공지관리"
        action={
          <button className="admin-primary-button" type="button" onClick={openCreateForm}>
            공지등록
          </button>
        }
      />
      <Feedback notice={notice} error={error} />
      <Toast message={notice} onClose={() => setNotice("")} />
      <div className="admin-list-top-filter">
        <select
          aria-label="공개상태"
          value={visibility}
          onChange={(event) => {
            setCurrentPage(0);
            setVisibility(event.target.value as NoticeVisibilityFilter);
          }}
        >
          {NOTICE_VISIBILITY_FILTERS.map((item) => (
            <option key={item.value} value={item.value}>
              {item.label}
            </option>
          ))}
        </select>
      </div>
      <div className="admin-card admin-list-card admin-community-list-card">
        {loading ? (
          <p className="admin-empty">공지사항을 불러오는 중입니다.</p>
        ) : (
          <div className="admin-table-wrap">
            <table className="admin-table admin-community-table admin-notice-table">
              <thead>
                <tr>
                  <th>제목</th>
                  <th>공개상태</th>
                  <th>등록일</th>
                  <th>관리</th>
                </tr>
              </thead>
              <tbody>
                {notices.length === 0 ? (
                  <tr>
                    <td className="admin-empty-row" colSpan={4}>
                      해당하는 공지사항이 없습니다.
                    </td>
                  </tr>
                ) : (
                  notices.map((item) => (
                    <tr key={item.id} className={!item.visible ? "is-muted-row" : ""}>
                      <td>
                        <button className="admin-order-number-button" type="button" onClick={() => void openNotice(item.id)}>
                          {item.title}
                          {isCreatedToday(item.createdAt) && <b className="admin-new-icon" aria-label="새 글">N</b>}
                        </button>
                      </td>
                      <td className="admin-center-cell">{item.visibleDescription}</td>
                      <td className="admin-center-cell">{formatDateOnly(item.createdAt)}</td>
                      <td>
                        <div className="admin-mini-actions">
                          <button type="button" onClick={() => openEditForm(item)}>수정</button>
                          <button type="button" onClick={() => setVisibilityTarget(item)}>
                            {item.visible ? "비공개" : "공개"}
                          </button>
                        </div>
                      </td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>
        )}
      </div>
      <AdminPagination pageInfo={noticesPage} onChangePage={setCurrentPage} />
      <div className="admin-filter-row admin-community-search">
        <form
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
          <button className="admin-outline-button" type="submit">SEARCH</button>
        </form>
      </div>
      {visibilityTarget && (
        <NoticeVisibilityConfirmModal
          notice={visibilityTarget}
          onCancel={() => setVisibilityTarget(null)}
          onConfirm={() => toggleVisibility(visibilityTarget)}
        />
      )}
    </section>
  );
}

function BoardsPanel() {
  const [boards, setBoards] = useState<AdminBoard[]>([]);
  const [boardsPage, setBoardsPage] = useState<PageResponse<AdminBoard>>(() => createEmptyPage<AdminBoard>());
  const [keyword, setKeyword] = useState("");
  const [appliedKeyword, setAppliedKeyword] = useState("");
  const [currentPage, setCurrentPage] = useState(0);
  const [selectedBoard, setSelectedBoard] = useState<AdminBoard | null>(null);
  const [answer, setAnswer] = useState("");
  const [answerEditing, setAnswerEditing] = useState(false);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [notice, setNotice] = useState("");

  async function loadBoards(page = currentPage) {
    setLoading(true);
    setError("");

    try {
      const data = await getAdminBoardsPage(appliedKeyword, page);
      setBoards(data.content);
      setBoardsPage(data);
      setCurrentPage(data.page);
    } catch (loadError) {
      setError(loadError instanceof Error ? loadError.message : "게시글을 불러오지 못했습니다.");
    } finally {
      setLoading(false);
    }
  }

  async function openBoard(boardId: number, pushHistory = true) {
    setError("");

    try {
      const data = await getAdminBoard(boardId);
      setSelectedBoard(data);
      setAnswer(data.answer ?? "");
      setAnswerEditing(false);
      if (pushHistory) {
        setAdminHistory(`${ADMIN_TAB_PATHS.boards}/${boardId}`);
      }
    } catch (loadError) {
      setError(loadError instanceof Error ? loadError.message : "게시글 상세를 불러오지 못했습니다.");
    }
  }

  function closeBoardDetail(pushHistory = true) {
    setSelectedBoard(null);
    setAnswer("");
    setAnswerEditing(false);
    if (pushHistory) {
      setAdminHistory(ADMIN_TAB_PATHS.boards);
    }
  }

  async function submitAnswer(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();

    if (!selectedBoard) {
      return;
    }

    setNotice("");
    setError("");

    try {
      const hasAnswer = Boolean(selectedBoard.answer);
      const data = await answerAdminBoard(selectedBoard.id, answer);
      setSelectedBoard(data);
      setAnswerEditing(false);
      setNotice(hasAnswer ? "답변이 수정되었습니다." : "답변이 등록되었습니다.");
      await loadBoards(currentPage);
    } catch (submitError) {
      setError(submitError instanceof Error ? submitError.message : "답변 등록에 실패했습니다.");
    }
  }

  useEffect(() => {
    void loadBoards();
  }, [appliedKeyword, currentPage]);

  useEffect(() => {
    const syncRoute = () => {
      if (getAdminTabFromPath() !== "boards") {
        return;
      }

      const boardId =
        getAdminRouteId(ADMIN_TAB_PATHS.boards) ??
        getAdminRouteId("/admin/boards");

      if (boardId) {
        void openBoard(boardId, false);
        return;
      }

      closeBoardDetail(false);
    };

    syncRoute();
    window.addEventListener("popstate", syncRoute);
    return () => window.removeEventListener("popstate", syncRoute);
  }, []);

  if (selectedBoard) {
    return (
      <section className="admin-section">
        <PanelHeader
          title="문의관리"
          action={
            <button className="admin-title-back-button" type="button" onClick={() => closeBoardDetail()}>
              목록으로 돌아가기
              <span aria-hidden="true" />
            </button>
          }
        />
        <Feedback notice={notice} error={error} />
        <Toast message={notice} onClose={() => setNotice("")} />
        <h2 className="admin-section-title">문의 상세</h2>
        <div className="admin-detail">
          <section className="admin-detail-block">
            <h3>문의정보</h3>
            <dl className="admin-detail-list">
              <div>
                <dt>문의종류</dt>
                <dd>{selectedBoard.typeDescription}</dd>
              </div>
              <div>
                <dt>제목</dt>
                <dd>{selectedBoard.title}</dd>
              </div>
              <div>
                <dt>작성자</dt>
                <dd>{selectedBoard.writerName} / {selectedBoard.writerEmail}</dd>
              </div>
              <div>
                <dt>공개상태</dt>
                <dd>{selectedBoard.visibilityDescription}</dd>
              </div>
              <div>
                <dt>답변상태</dt>
                <dd>{selectedBoard.statusDescription}</dd>
              </div>
              <div>
                <dt>작성일</dt>
                <dd>{formatDate(selectedBoard.createdAt)}</dd>
              </div>
            </dl>
          </section>
          <section className="admin-detail-block">
            <h3>문의내용</h3>
            <p className="admin-readable-text">{selectedBoard.content}</p>
          </section>
          {selectedBoard.answer && (
            <section className="admin-detail-block">
              <h3>관리자 답변</h3>
              <p className="admin-readable-text">{selectedBoard.answer}</p>
            </section>
          )}
          {selectedBoard.answer && !answerEditing && (
            <div className="admin-detail-answer-actions">
              <button className="admin-primary-button" type="button" onClick={() => setAnswerEditing(true)}>
                답변 수정
              </button>
            </div>
          )}
          {(!selectedBoard.answer || answerEditing) && (
            <form className="admin-detail-block admin-form" onSubmit={submitAnswer}>
              <h3>관리자 답변</h3>
              <textarea value={answer} onChange={(event) => setAnswer(event.target.value)} rows={8} />
              <button className="admin-primary-button" type="submit">
                {selectedBoard.answer ? "답변 수정" : "답변 등록"}
              </button>
            </form>
          )}
        </div>
      </section>
    );
  }

  return (
    <section className="admin-section">
      <PanelHeader title="문의관리" />
      <Feedback notice="" error={error} />
      <div className="admin-card admin-list-card admin-community-list-card">
        {loading ? (
          <p className="admin-empty">게시글을 불러오는 중입니다.</p>
        ) : (
          <div className="admin-table-wrap">
            <table className="admin-table admin-community-table admin-board-table">
              <thead>
                <tr>
                  <th>문의종류</th>
                  <th>제목</th>
                  <th>작성자</th>
                  <th>공개상태</th>
                  <th>답변상태</th>
                  <th>작성일</th>
                </tr>
              </thead>
              <tbody>
                {boards.length === 0 ? (
                  <tr>
                    <td className="admin-empty-row" colSpan={6}>
                      해당하는 문의가 없습니다.
                    </td>
                  </tr>
                ) : (
                  boards.map((board) => (
                    <tr key={board.id}>
                      <td className="admin-center-cell">{board.typeDescription}</td>
                      <td>
                        <button className="admin-order-number-button" type="button" onClick={() => void openBoard(board.id)}>
                          {board.title}
                          {isCreatedToday(board.createdAt) && <b className="admin-new-icon" aria-label="새 글">N</b>}
                        </button>
                      </td>
                      <td className="admin-center-cell">{board.writerName}</td>
                      <td className="admin-center-cell">{board.visibilityDescription}</td>
                      <td className="admin-center-cell">{board.statusDescription}</td>
                      <td className="admin-center-cell">{formatDateOnly(board.createdAt)}</td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>
        )}
      </div>
      <AdminPagination pageInfo={boardsPage} onChangePage={setCurrentPage} />
      <form
        className="admin-filter-row admin-community-search"
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
        <button className="admin-outline-button" type="submit">SEARCH</button>
      </form>
    </section>
  );
}

function PasswordPanel() {
  const [currentPassword, setCurrentPassword] = useState("");
  const [newPassword, setNewPassword] = useState("");
  const [newPasswordConfirm, setNewPasswordConfirm] = useState("");
  const [notice, setNotice] = useState("");
  const [error, setError] = useState("");

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setNotice("");
    setError("");

    if (newPassword !== newPasswordConfirm) {
      setError("새 비밀번호가 일치하지 않습니다.");
      return;
    }

    try {
      await updateAdminPassword(currentPassword, newPassword);
      setCurrentPassword("");
      setNewPassword("");
      setNewPasswordConfirm("");
      setNotice("관리자 비밀번호가 변경되었습니다.");
    } catch (submitError) {
      setError(submitError instanceof Error ? submitError.message : "비밀번호 변경에 실패했습니다.");
    }
  }

  return (
    <section className="admin-section">
      <PanelHeader title="비밀번호 변경" />
      <Feedback notice={notice} error={error} />
      <Toast message={notice} onClose={() => setNotice("")} />
      <form className="admin-form admin-password-form" onSubmit={handleSubmit}>
        <label>
          현재 비밀번호
          <input type="password" value={currentPassword} onChange={(event) => setCurrentPassword(event.target.value)} />
        </label>
        <label>
          새 비밀번호
          <input type="password" value={newPassword} onChange={(event) => setNewPassword(event.target.value)} />
        </label>
        <label>
          새 비밀번호 확인
          <input type="password" value={newPasswordConfirm} onChange={(event) => setNewPasswordConfirm(event.target.value)} />
        </label>
        <button className="admin-primary-button" type="submit">
          변경하기
        </button>
      </form>
    </section>
  );
}

function PanelHeader({
  title,
  description,
  onRefresh,
  action,
}: {
  title: string;
  description?: string;
  onRefresh?: () => void | Promise<void>;
  action?: ReactNode;
}) {
  return (
    <header className={`admin-panel-header ${action ? "has-action" : ""}`}>
      <div>
        <h1>{title}</h1>
        {description && <span>{description}</span>}
      </div>
      {action}
      {onRefresh && (
        <button className="admin-outline-button" type="button" onClick={() => void onRefresh()}>
          새로고침
        </button>
      )}
    </header>
  );
}

function AdminErrorMessage({ message }: { message: string }) {
  if (!message) {
    return null;
  }

  return <p className="admin-error-message">{message}</p>;
}

function Feedback({ error }: { notice: string; error: string }) {
  return <AdminErrorMessage message={error} />;
}

function Toast({ message, onClose }: { message: string; onClose: () => void }) {
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
    <div className="admin-toast" role="status" aria-live="polite">
      {message}
    </div>
  );
}
