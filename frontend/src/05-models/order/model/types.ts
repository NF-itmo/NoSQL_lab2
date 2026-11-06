import type { Location } from "@/05-models/location";

export type OrderState = "PENDING" | "IN_PROGRESS" | "DONE" | "ABORTED";

export type Order = {
  id: number;
  bookId: number;
  bookLocation?: Location;
  createdAt: string;
  expiresAt: string;
  status: OrderState;
};

export type BookStatusAnalytics = {
  bookId: number;
  bookTitle: string;
  pendingCount: number;
  inProgressCount: number;
  doneCount: number;
  abortedCount: number;
};

export type OrderStatusAnalytics = {
  books: BookStatusAnalytics[];
};
