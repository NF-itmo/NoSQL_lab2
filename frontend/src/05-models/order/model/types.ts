import type { BookLocation } from "@/05-models/book";

export type OrderState = "PENDING" | "IN_PROGRESS" | "DONE" | "ABORTED";

export type Order = {
  id: number;
  bookId: number;
  bookLocation?: BookLocation;
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
