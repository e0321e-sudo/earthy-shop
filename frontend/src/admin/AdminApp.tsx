import { FormEvent, ReactNode, useEffect, useMemo, useState } from "react";
import {
  AddonSaveRequest,
  AdminAddon,
  AdminOrder,
  AdminProduct,
  AddonType,
  ProductCategory,
  ProductSaveRequest,
  OrderStatus,
  activateAdminAddon,
  activateAdminProduct,
  cancelAdminOrder,
  createAdminAddon,
  createAdminProduct,
  deactivateAdminAddon,
  deactivateAdminProduct,
  deleteAdminAddon,
  deleteAdminProduct,
  getAdminAddons,
  getAdminAddonsPage,
  getAdminOrder,
  getAdminOrders,
  getAdminOrdersPage,
  getAdminProducts,
  getAdminProductsPage,
  PageResponse,
  getStoredAdminAccessToken,
  loginAdmin,
  logoutAdmin,
  onAdminAuthCleared,
  updateAdminAddon,
  updateAdminOrderStatus,
  updateAdminPassword,
  updateAdminProduct,
} from "./api";
import "./admin.css";

type AdminTab = "dashboard" | "products" | "addons" | "orders" | "customers" | "boards" | "password";

const PRODUCT_CATEGORIES: Array<{ value: ProductCategory; label: string }> = [
  { value: "POSTCARD", label: "엽서" },
  { value: "POSTER", label: "포스터" },
  { value: "ETC", label: "기타" },
];

const ADDON_TYPES: Array<{ value: AddonType; label: string }> = [{ value: "FRAME", label: "액자" }];

const ORDER_STATUSES: Array<{ value: OrderStatus; label: string }> = [
  { value: "PENDING", label: "주문 대기" },
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

const emptyProductForm: ProductSaveRequest = {
  name: "",
  category: "POSTCARD",
  price: 0,
  imageUrl: "",
  description: "",
  stockQuantity: 0,
};

const emptyAddonForm: AddonSaveRequest = {
  name: "",
  type: "FRAME",
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

function formatNumber(value: number) {
  return value.toLocaleString("ko-KR");
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
  const [activeTab, setActiveTab] = useState<AdminTab>("dashboard");
  const [loginError, setLoginError] = useState("");
  const [isMobileMenuOpen, setIsMobileMenuOpen] = useState(false);
  const [ordersMenuKey, setOrdersMenuKey] = useState(0);

  useEffect(() => onAdminAuthCleared(() => setIsAuthed(false)), []);

  function changeTab(tab: AdminTab) {
    setActiveTab(tab);
    if (tab === "orders") {
      setOrdersMenuKey((current) => current + 1);
    }
    setIsMobileMenuOpen(false);
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

      <aside className="admin-sidebar">
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
          <button className={activeTab === "boards" ? "is-active" : ""} type="button" onClick={() => changeTab("boards")}>
            게시판관리
          </button>
          <button className={activeTab === "password" ? "is-active" : ""} type="button" onClick={() => changeTab("password")}>
            비밀번호변경
          </button>
        </nav>
        <button className="admin-logout" type="button" onClick={handleLogout}>
          로그아웃
        </button>
      </aside>

      <main className="admin-main">
        {activeTab === "dashboard" && <DashboardPanel onMoveTab={changeTab} />}
        {activeTab === "orders" && <OrdersPanel menuKey={ordersMenuKey} />}
        {activeTab === "products" && <ProductsPanel />}
        {activeTab === "addons" && <AddonsPanel />}
        {activeTab === "customers" && (
          <PlaceholderPanel
            title="고객관리"
            description="회원 목록 조회, 회원 상세 확인, 회원 상태 관리는 고객관리 API를 만든 뒤 연결할 예정입니다."
          />
        )}
        {activeTab === "boards" && (
          <PlaceholderPanel
            title="게시판관리"
            description="공지사항, 문의 게시판 목록과 답변 관리는 게시판 API를 만든 뒤 연결할 예정입니다."
          />
        )}
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
        {error && <p className="admin-message is-error">{error}</p>}
        <button className="admin-primary-button" type="submit" disabled={submitting}>
          {submitting ? "로그인 중" : "로그인"}
        </button>
      </form>
    </main>
  );
}

function DashboardPanel({ onMoveTab }: { onMoveTab: (tab: AdminTab) => void }) {
  const [orders, setOrders] = useState<AdminOrder[]>([]);
  const [products, setProducts] = useState<AdminProduct[]>([]);
  const [addons, setAddons] = useState<AdminAddon[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  const paidOrders = orders.filter((order) => order.status === "PAID");
  const pendingOrders = orders.filter((order) => order.status === "PENDING");
  const revenueOrders = orders.filter((order) => order.status !== "PENDING" && order.status !== "CANCELED");
  const totalRevenue = revenueOrders.reduce((sum, order) => sum + order.totalPrice, 0);
  const lowStockProducts = products.filter((product) => product.active && product.stockQuantity <= 0);
  const lowStockAddons = addons.filter((addon) => addon.active && addon.stockQuantity <= 0);
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
          <small>결제 완료 후 상품 준비 전</small>
        </button>
        <button className="admin-metric-card" type="button" onClick={() => onMoveTab("orders")}>
          <span>입금 대기</span>
          <strong>{pendingOrders.length}</strong>
          <small>주문 생성 후 결제 완료 전</small>
        </button>
        <div className="admin-metric-card">
          <span>총매출</span>
          <strong>{formatWon(totalRevenue)}</strong>
          <small>취소/주문대기 제외</small>
        </div>
        <button
          className={`admin-metric-card ${lowStockCount > 0 ? "is-alert" : ""}`}
          type="button"
          onClick={() => onMoveTab("products")}
        >
          <span>재고 보충 필요</span>
          <strong>{lowStockCount}</strong>
          <small>활성 상품 기준</small>
        </button>
      </div>

      <div className="admin-two-column">
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
                <button className="admin-dashboard-row" type="button" key={order.orderId} onClick={() => onMoveTab("orders")}>
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
                <button className="admin-dashboard-row" type="button" key={`product-${product.id}`} onClick={() => onMoveTab("products")}>
                  <span>
                    <strong>{product.name}</strong>
                    <small>상품 / {product.categoryDescription}</small>
                  </span>
                  <span>
                    <strong>{product.stockQuantity}개</strong>
                    <small>재고 보충 필요</small>
                  </span>
                </button>
              ))}
              {lowStockAddons.map((addon) => (
                <button className="admin-dashboard-row" type="button" key={`addon-${addon.id}`} onClick={() => onMoveTab("addons")}>
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

function OrdersPanel({ menuKey }: { menuKey: number }) {
  const [orders, setOrders] = useState<AdminOrder[]>([]);
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
      count: orders.filter((order) => order.status === status.value).length,
    }));
  }, [orders]);

  async function loadOrders(page = currentPage) {
    setLoading(true);
    setError("");

    try {
      const data = await getAdminOrdersPage(page);
      setOrders(data.content);
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

  async function openOrder(orderId: number) {
    setError("");

    try {
      setSelectedOrder(await getAdminOrder(orderId));
      setIsDetailView(true);
    } catch (loadError) {
      setError(loadError instanceof Error ? loadError.message : "주문 상세를 불러오지 못했습니다.");
    }
  }

  useEffect(() => {
    void loadOrders();
  }, [currentPage]);

  useEffect(() => {
    setIsDetailView(false);
  }, [menuKey]);

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
      const updated = await cancelAdminOrder(orderId, cancelReason);
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
            <button className="admin-title-back-button" type="button" onClick={() => setIsDetailView(false)}>
              목록으로 돌아가기
              <span aria-hidden="true" />
            </button>
          }
        />
        <Feedback notice={notice} error={error} />
        <Toast message={notice} onClose={() => setNotice("")} />
        <h2 className="admin-section-title">주문 상세내역</h2>
        <div className="admin-order-detail-page">
          <OrderDetail order={selectedOrder} onStatusUpdate={handleStatusUpdate} onCancel={handleCancel} />
        </div>
      </section>
    );
  }

  return (
    <section className="admin-section">
      <PanelHeader title="주문 관리" />
      <div className="admin-summary-grid">
        {summary.map((item) => (
          <div className="admin-summary-card" key={item.value}>
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
        ) : orders.length === 0 ? (
          <p className="admin-empty">주문이 없습니다.</p>
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
                  {orders.map((order) => (
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
                  ))}
                </tbody>
              </table>
            </div>

            <div className="admin-mobile-order-list">
              {orders.map((order) => (
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
              ))}
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
  onStatusUpdate,
  onCancel,
}: {
  order: AdminOrder;
  onStatusUpdate: (orderId: number, status: OrderStatus, carrier: string, trackingNumber: string) => Promise<void>;
  onCancel: (orderId: number, cancelReason: string) => Promise<void>;
}) {
  const [status, setStatus] = useState<OrderStatus>(order.status);
  const [carrier, setCarrier] = useState(order.carrier ?? "");
  const [trackingNumber, setTrackingNumber] = useState(order.trackingNumber ?? "");
  const [cancelReason, setCancelReason] = useState("");
  const [statusModalOpen, setStatusModalOpen] = useState(false);
  const [cancelModalOpen, setCancelModalOpen] = useState(false);
  const canChangeStatus = Boolean(getNextOrderStatus(order.status));
  const selectedStatusLabel = ORDER_UPDATE_STATUSES.find((item) => item.value === status)?.label ?? status;

  useEffect(() => {
    setStatus(getNextOrderStatus(order.status) || order.status);
    setCarrier(order.carrier ?? "");
    setTrackingNumber(order.trackingNumber ?? "");
    setCancelReason("");
    setStatusModalOpen(false);
    setCancelModalOpen(false);
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
              <dt>주문상태</dt>
              <dd>
                <span className={`admin-status-badge status-${order.status.toLowerCase()}`}>{order.statusDescription}</span>
              </dd>
            </div>
          </dl>
        </section>

        <section className="admin-detail-block">
          <h3>결제정보</h3>
          <dl className="admin-detail-list">
            <div>
              <dt>결제방법</dt>
              <dd>{order.paymentMethod || "-"}</dd>
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
                <p>상품 {item.quantity}개 / {formatWon(item.productPrice)}</p>
                {item.addonName && (
                  <p>
                    추가상품 {item.addonName} {item.addonQuantity}개 / {formatWon(item.addonPrice)}
                  </p>
                )}
                <p>합계 {formatWon(item.itemTotalPrice)}</p>
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
        <button className="admin-outline-button" type="button" onClick={() => setCancelModalOpen(true)}>
          주문 취소
        </button>
      </div>

      {cancelModalOpen && (
        <div className="admin-modal-backdrop" role="dialog" aria-modal="true" aria-labelledby="admin-cancel-title">
          <form
            className="admin-modal-card"
            onSubmit={(event) => {
              event.preventDefault();
              void onCancel(order.orderId, cancelReason);
            }}
          >
            <h3 id="admin-cancel-title">주문 취소</h3>
            <label>
              취소 사유
              <textarea
                value={cancelReason}
                onChange={(event) => setCancelReason(event.target.value)}
                placeholder="취소 사유를 입력해주세요."
                rows={4}
              />
            </label>
            <div className="admin-modal-actions">
              <button className="admin-outline-button" type="button" onClick={() => setCancelModalOpen(false)}>
                닫기
              </button>
              <button className="admin-primary-button" type="submit">
                취소하기
              </button>
            </div>
          </form>
        </div>
      )}
    </div>
  );
}

function ProductsPanel() {
  const [products, setProducts] = useState<AdminProduct[]>([]);
  const [productsPage, setProductsPage] = useState<PageResponse<AdminProduct>>(() => createEmptyPage<AdminProduct>());
  const [currentPage, setCurrentPage] = useState(0);
  const [form, setForm] = useState<ProductSaveRequest>(emptyProductForm);
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

    try {
      if (editingId) {
        await updateAdminProduct(editingId, form);
        setNotice("상품이 수정되었습니다.");
      } else {
        await createAdminProduct(form);
        setNotice("상품이 등록되었습니다.");
      }

      setForm(emptyProductForm);
      setEditingId(null);
      setIsFormView(false);
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

  function startEdit(product: AdminProduct) {
    setEditingId(product.id);
    setForm({
      name: product.name,
      category: product.category,
      price: product.price,
      imageUrl: product.imageUrl,
      description: product.description,
      stockQuantity: product.stockQuantity,
    });
    setIsFormView(true);
  }

  function openCreateForm() {
    setEditingId(null);
    setForm(emptyProductForm);
    setIsFormView(true);
  }

  function closeForm() {
    setEditingId(null);
    setForm(emptyProductForm);
    setIsFormView(false);
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
      {isFormView && (
        <div className="admin-modal-backdrop" role="dialog" aria-modal="true" aria-labelledby="admin-product-form-title">
          <ProductForm form={form} editingId={editingId} onChange={setForm} onSubmit={handleSubmit} onCancel={closeForm} />
        </div>
      )}
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
  onChange,
  onSubmit,
  onCancel,
}: {
  form: ProductSaveRequest;
  editingId: number | null;
  onChange: (form: ProductSaveRequest) => void;
  onSubmit: (event: FormEvent<HTMLFormElement>) => void;
  onCancel: () => void;
}) {
  return (
    <form className="admin-modal-card admin-form admin-form-modal" onSubmit={onSubmit}>
      <h3 id="admin-product-form-title">{editingId ? "상품 수정" : "상품 등록"}</h3>
      <label>
        상품명
        <input value={form.name} onChange={(event) => onChange({ ...form, name: event.target.value })} />
      </label>
      <label>
        카테고리
        <select value={form.category} onChange={(event) => onChange({ ...form, category: event.target.value as ProductCategory })}>
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
      <label>
        재고 수량
        <input
          type="number"
          min="0"
          value={form.stockQuantity}
          onChange={(event) => onChange({ ...form, stockQuantity: Number(event.target.value) })}
        />
      </label>
      <label>
        이미지 경로
        <input value={form.imageUrl} onChange={(event) => onChange({ ...form, imageUrl: event.target.value })} />
      </label>
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
              {products.map((product) => (
                <tr key={product.id} className={product.active ? undefined : "is-private"}>
                  <td>
                    <div className="admin-product-cell">
                      <img src={product.imageUrl} alt={product.name} />
                      <span>{product.name}</span>
                    </div>
                  </td>
                  <td className="admin-center-cell">{product.categoryDescription}</td>
                  <td className="admin-price-cell">{formatNumber(product.price)}</td>
                  <td className="admin-center-cell">{product.stockQuantity}</td>
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
              ))}
            </tbody>
          </table>
        </div>
      </div>
    </>
  );
}

function AddonsPanel() {
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

  function startEdit(addon: AdminAddon) {
    setEditingId(addon.id);
    setForm({
      name: addon.name,
      type: addon.type,
      price: addon.price,
      stockQuantity: addon.stockQuantity,
    });
    setIsFormView(true);
  }

  function openCreateForm() {
    setEditingId(null);
    setForm(emptyAddonForm);
    setIsFormView(true);
  }

  function closeForm() {
    setEditingId(null);
    setForm(emptyAddonForm);
    setIsFormView(false);
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
              {addons.map((addon) => (
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
              ))}
            </tbody>
          </table>
        </div>
      </div>
      <AdminPagination pageInfo={addonsPage} onChangePage={setCurrentPage} />
      {isFormView && (
        <div className="admin-modal-backdrop" role="dialog" aria-modal="true" aria-labelledby="admin-addon-form-title">
          <form className="admin-modal-card admin-form admin-form-modal" onSubmit={handleSubmit}>
            <h3 id="admin-addon-form-title">{editingId ? "추가상품 수정" : "추가상품 등록"}</h3>
            <label>
              추가상품명
              <input value={form.name} onChange={(event) => setForm({ ...form, name: event.target.value })} />
            </label>
            <label>
              종류
              <select value={form.type} onChange={(event) => setForm({ ...form, type: event.target.value as AddonType })}>
                {ADDON_TYPES.map((type) => (
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
        </div>
      )}
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
      <PanelHeader title="비밀번호 변경" description="관리자 계정 비밀번호 변경" />
      <Feedback notice={notice} error={error} />
      <Toast message={notice} onClose={() => setNotice("")} />
      <form className="admin-card admin-form admin-narrow-form" onSubmit={handleSubmit}>
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
        <p>ADMIN</p>
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

function PlaceholderPanel({ title, description }: { title: string; description: string }) {
  return (
    <section className="admin-section">
      <PanelHeader title={title} description={description} />
      <div className="admin-card admin-placeholder-card">
        <h2>준비중</h2>
        <p>{description}</p>
      </div>
    </section>
  );
}

function Feedback({ error }: { notice: string; error: string }) {
  if (!error) {
    return null;
  }

  return <p className="admin-message is-error">{error}</p>;
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
