import { API_V1_PATH } from "@/06-shared/api/request/config";
import { request } from "@/06-shared/api/request";
import type { Order, OrderState, OrderStatusAnalytics } from "../model/types";

const ORDERS_API_PATH = `${API_V1_PATH}/orders`;

export const getOrders = (): Promise<Order[]> => request<Order[]>(ORDERS_API_PATH);

export const searchOrders = (filters: {
  status?: OrderState;
  createdAfter?: string;
  createdBefore?: string;
}): Promise<Order[]> => {
  const searchParams = new URLSearchParams();
  if (filters.status) searchParams.set("status", filters.status);
  if (filters.createdAfter) searchParams.set("createdAfter", filters.createdAfter);
  if (filters.createdBefore) searchParams.set("createdBefore", filters.createdBefore);
  return request<Order[]>(`${ORDERS_API_PATH}/search?${searchParams.toString()}`);
};

export const getOrder = (orderId: number): Promise<Order> =>
  request<Order>(`${ORDERS_API_PATH}/${orderId}`);

export const createOrder = async (bookId: number): Promise<Order> => {
  return request<Order>(ORDERS_API_PATH, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ bookId })
  });
};

export const confirmOrder = async (orderId: number): Promise<void> => {
  await request<void>(`${ORDERS_API_PATH}/${orderId}/confirm`, { method: "POST" });
};

export const completeOrder = async (orderId: number): Promise<void> => {
  await request<void>(`${ORDERS_API_PATH}/${orderId}/complete`, { method: "POST" });
};

export const cancelOrder = async (orderId: number): Promise<void> => {
  await request<void>(`${ORDERS_API_PATH}/${orderId}/cancel`, { method: "POST" });
};

export const getOrderStatusAnalytics = (
  from: string,
  to: string,
  orderBy: OrderState,
  start: number,
  limit: number,
): Promise<OrderStatusAnalytics> => {
  const searchParams = new URLSearchParams({ from, to, orderBy, start: String(start), limit: String(limit) });
  return request<OrderStatusAnalytics>(`${ORDERS_API_PATH}/analytics?${searchParams.toString()}`);
};
