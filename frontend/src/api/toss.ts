import type { OrderResponse } from "./orders";

const TOSS_SCRIPT_URL = "https://js.tosspayments.com/v1/payment";
const DEFAULT_TOSS_CLIENT_KEY = "test_ck_6bJXmgo28eAnx9kpo1my8LAnGKWx";

interface TossPaymentInstance {
  requestPayment: (
    method: "카드",
    options: {
      amount: number;
      orderId: string;
      orderName: string;
      customerName: string;
      successUrl: string;
      failUrl: string;
    }
  ) => Promise<void>;
}

interface TossPaymentsConstructor {
  (clientKey: string): TossPaymentInstance;
}

declare global {
  interface Window {
    TossPayments?: TossPaymentsConstructor;
  }
}

function loadTossScript(): Promise<void> {
  if (window.TossPayments) {
    return Promise.resolve();
  }

  const existingScript = document.querySelector<HTMLScriptElement>(`script[src="${TOSS_SCRIPT_URL}"]`);

  if (existingScript) {
    return new Promise((resolve, reject) => {
      existingScript.addEventListener("load", () => resolve(), { once: true });
      existingScript.addEventListener("error", () => reject(new Error("Toss 결제창을 불러오지 못했습니다.")), {
        once: true,
      });
    });
  }

  return new Promise((resolve, reject) => {
    const script = document.createElement("script");
    script.src = TOSS_SCRIPT_URL;
    script.async = true;
    script.onload = () => resolve();
    script.onerror = () => reject(new Error("Toss 결제창을 불러오지 못했습니다."));
    document.head.appendChild(script);
  });
}

function getPaymentReturnUrl(result: "success" | "fail") {
  const url = new URL(window.location.href);
  url.search = "";
  url.hash = "";
  url.searchParams.set("paymentResult", result);
  return url.toString();
}

function createOrderName(order: OrderResponse) {
  const firstItemName = order.items[0]?.productName ?? "EARTHY 상품";
  const extraItemCount = Math.max(order.items.length - 1, 0);
  return extraItemCount > 0 ? `${firstItemName} 외 ${extraItemCount}개` : firstItemName;
}

export async function requestTossPayment(order: OrderResponse) {
  await loadTossScript();

  if (!window.TossPayments) {
    throw new Error("Toss 결제창을 사용할 수 없습니다.");
  }

  const clientKey = import.meta.env.VITE_TOSS_CLIENT_KEY ?? DEFAULT_TOSS_CLIENT_KEY;
  const tossPayments = window.TossPayments(clientKey);

  sessionStorage.setItem(`earthyPaymentOrder:${order.orderNumber}`, String(order.orderId));
  sessionStorage.setItem(`earthyPaymentOrderData:${order.orderNumber}`, JSON.stringify(order));

  await tossPayments.requestPayment("카드", {
    amount: order.totalPrice,
    orderId: order.orderNumber,
    orderName: createOrderName(order),
    customerName: order.receiverName,
    successUrl: getPaymentReturnUrl("success"),
    failUrl: getPaymentReturnUrl("fail"),
  });
}
