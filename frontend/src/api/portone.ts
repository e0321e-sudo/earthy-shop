import type { OrderResponse } from "./orders";

const PORTONE_SCRIPT_URL = "https://cdn.portone.io/v2/browser-sdk.js";

export type PortOnePayMethod = "CARD" | "MOBILE";
type PortOneProductType = "PRODUCT_TYPE_REAL" | "PRODUCT_TYPE_DIGITAL";
type PortOneWindowType = "IFRAME" | "POPUP" | "REDIRECTION" | "UI";

const PORTONE_PAYMENT_CONTEXT_PREFIX = "earthyPortOnePayment";

interface PortOnePaymentResult {
  code?: string;
  message?: string;
  paymentId?: string;
  transactionType?: string;
  txId?: string;
}

interface PortOnePaymentRequest {
  storeId: string;
  channelKey: string;
  paymentId: string;
  orderName: string;
  totalAmount: number;
  currency: "CURRENCY_KRW";
  payMethod: PortOnePayMethod;
  productType?: PortOneProductType;
  windowType?: {
    pc?: PortOneWindowType;
    mobile?: PortOneWindowType;
  };
  redirectUrl?: string;
  forceRedirect?: boolean;
  customer?: {
    fullName?: string;
    phoneNumber?: string;
    email?: string;
  };
}

interface PortOneBrowserSdk {
  requestPayment: (request: PortOnePaymentRequest) => Promise<PortOnePaymentResult>;
}

declare global {
  interface Window {
    PortOne?: PortOneBrowserSdk;
  }
}

export interface StoredPortOnePaymentContext {
  orderId: number;
  orderNumber: string;
  amount: number;
}

function loadPortOneScript(): Promise<void> {
  if (window.PortOne) {
    return Promise.resolve();
  }

  const existingScript = document.querySelector<HTMLScriptElement>(`script[src="${PORTONE_SCRIPT_URL}"]`);

  if (existingScript) {
    return new Promise((resolve, reject) => {
      existingScript.addEventListener("load", () => resolve(), { once: true });
      existingScript.addEventListener("error", () => reject(new Error("PortOne 결제창을 불러오지 못했습니다.")), {
        once: true,
      });
    });
  }

  return new Promise((resolve, reject) => {
    const script = document.createElement("script");
    script.src = PORTONE_SCRIPT_URL;
    script.async = true;
    script.onload = () => resolve();
    script.onerror = () => reject(new Error("PortOne 결제창을 불러오지 못했습니다."));
    document.head.appendChild(script);
  });
}

function createPaymentId(order: OrderResponse) {
  return `${order.orderNumber}-${Date.now()}`;
}

function isMobilePaymentEnvironment() {
  if (typeof window === "undefined") {
    return false;
  }

  const userAgent = window.navigator.userAgent;
  const hasMobileUserAgent = /Android|iPhone|iPad|iPod|BlackBerry|IEMobile|Opera Mini/i.test(userAgent);
  const hasTouchMobileViewport =
    window.matchMedia("(hover: none) and (pointer: coarse)").matches &&
    window.matchMedia("(max-width: 1024px)").matches;

  return hasMobileUserAgent || hasTouchMobileViewport;
}

function createPortOneRedirectUrl() {
  const configuredRedirectUrl = import.meta.env.VITE_PORTONE_REDIRECT_URL?.trim();
  const redirectUrl = new URL(
    configuredRedirectUrl || `${window.location.origin}${window.location.pathname}`,
    window.location.origin
  );

  redirectUrl.searchParams.set("paymentResult", "portone");

  if (redirectUrl.protocol !== "http:" && redirectUrl.protocol !== "https:") {
    throw new Error("PortOne 모바일 결제 복귀 URL은 http 또는 https 주소여야 합니다.");
  }

  return redirectUrl.toString();
}

function storePortOnePaymentContext(paymentId: string, order: OrderResponse) {
  const context: StoredPortOnePaymentContext = {
    orderId: order.orderId,
    orderNumber: order.orderNumber,
    amount: order.totalPrice,
  };

  sessionStorage.setItem(`${PORTONE_PAYMENT_CONTEXT_PREFIX}:${paymentId}`, JSON.stringify(context));
}

export function getStoredPortOnePaymentContext(paymentId: string): StoredPortOnePaymentContext | undefined {
  const storedContext = sessionStorage.getItem(`${PORTONE_PAYMENT_CONTEXT_PREFIX}:${paymentId}`);

  if (!storedContext) {
    return undefined;
  }

  try {
    return JSON.parse(storedContext) as StoredPortOnePaymentContext;
  } catch {
    return undefined;
  }
}

export function clearStoredPortOnePaymentContext(paymentId: string) {
  sessionStorage.removeItem(`${PORTONE_PAYMENT_CONTEXT_PREFIX}:${paymentId}`);
}

export async function requestPortOnePayment(
  order: OrderResponse,
  payMethod: PortOnePayMethod,
  customerEmail: string
) {
  await loadPortOneScript();

  if (!window.PortOne) {
    throw new Error("PortOne 결제창을 사용할 수 없습니다.");
  }

  const normalizedEmail = customerEmail.trim();

  if (!normalizedEmail) {
    throw new Error("회원 이메일을 확인할 수 없어 결제를 진행할 수 없습니다.");
  }

  const storeId = import.meta.env.VITE_PORTONE_STORE_ID;
  const channelKey = import.meta.env.VITE_PORTONE_CHANNEL_KEY;

  if (!storeId || !channelKey) {
    throw new Error("PortOne 결제 설정이 필요합니다.");
  }

  const paymentId = createPaymentId(order);
  const useMobileRedirect = isMobilePaymentEnvironment();

  if (useMobileRedirect) {
    storePortOnePaymentContext(paymentId, order);
  }

  const request: PortOnePaymentRequest = {
    storeId,
    channelKey,
    paymentId,
    orderName: order.orderNumber,
    totalAmount: order.totalPrice,
    currency: "CURRENCY_KRW",
    payMethod,
    ...(payMethod === "MOBILE" ? { productType: "PRODUCT_TYPE_REAL" as const } : {}),
    ...(useMobileRedirect
      ? {
          windowType: {
            pc: "IFRAME" as const,
            mobile: "REDIRECTION" as const,
          },
          redirectUrl: createPortOneRedirectUrl(),
          forceRedirect: true,
        }
      : {}),
    customer: {
      fullName: order.receiverName,
      phoneNumber: order.receiverPhone,
      email: normalizedEmail,
    },
  };

  if (import.meta.env.DEV) {
    console.info("[PORTONE REQUEST MODE]", {
      payMethod: request.payMethod,
      useMobileRedirect,
      windowType: request.windowType,
      redirectUrl: request.redirectUrl,
      forceRedirect: request.forceRedirect,
      hasStoreId: Boolean(request.storeId),
      hasChannelKey: Boolean(request.channelKey),
    });
  }

  const result = await window.PortOne.requestPayment(request);

  if (result.code) {
    throw new Error(result.message ?? "결제가 취소되었거나 실패했습니다.");
  }

  return result.paymentId ?? paymentId;
}
