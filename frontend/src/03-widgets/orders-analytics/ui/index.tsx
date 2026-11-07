import { useState } from "react";
import { getOrderStatusAnalytics, type BookStatusAnalytics, type OrderState } from "@/05-models/order";
import { useErrorNotify } from "@/06-shared/lib/errorNotifier";
import { Pagination } from "@/06-shared/ui/pagination";
import styles from "./index.module.css";

const ANALYTICS_PAGE_SIZE = 10;
const states: OrderState[] = ["PENDING", "IN_PROGRESS", "DONE", "ABORTED"];
const stateTitles: Record<OrderState, string> = {
  PENDING: "Ожидает",
  IN_PROGRESS: "Обрабатывается",
  DONE: "Выдана",
  ABORTED: "Отменена",
};

const toDateTimeInput = (date: Date) => {
  const offset = date.getTimezoneOffset() * 60_000;
  return new Date(date.getTime() - offset).toISOString().slice(0, 16);
};

export const OrdersAnalytics = () => {
  const [from, setFrom] = useState(() => toDateTimeInput(new Date(Date.now() - 30 * 24 * 60 * 60 * 1000)));
  const [to, setTo] = useState(() => toDateTimeInput(new Date()));
  const [orderBy, setOrderBy] = useState<OrderState>("PENDING");
  const [books, setBooks] = useState<BookStatusAnalytics[]>([]);
  const [page, setPage] = useState(0);
  const [isLoading, setIsLoading] = useState(false);
  const [wasSearched, setWasSearched] = useState(false);
  const showError = useErrorNotify();

  const load = async (nextPage: number) => {
    if (!from || !to) {
      showError("Укажите начало и окончание интервала");
      return;
    }

    if (new Date(from) > new Date(to)) {
      showError("Начало интервала должно быть раньше окончания");
      return;
    }

    setIsLoading(true);
    try {
      const analytics = await getOrderStatusAnalytics(
        new Date(from).toISOString(),
        new Date(to).toISOString(),
        orderBy,
        nextPage * ANALYTICS_PAGE_SIZE,
        ANALYTICS_PAGE_SIZE + 1,
      );
      setBooks(analytics.books);
      setPage(nextPage);
      setWasSearched(true);
    } catch (error) {
      showError(error);
    } finally {
      setIsLoading(false);
    }
  };

  const visibleBooks = books.slice(0, ANALYTICS_PAGE_SIZE);

  return (
    <section className={styles.analytics}>
      <h2>Изменения статусов</h2>
      <div className={styles.filters}>
        <label>
          <span>С</span>
          <input type="datetime-local" value={from} onChange={(event) => setFrom(event.target.value)} />
        </label>
        <label>
          <span>По</span>
          <input type="datetime-local" value={to} onChange={(event) => setTo(event.target.value)} />
        </label>
        <label>
          <span>Сортировать по</span>
          <select value={orderBy} onChange={(event) => setOrderBy(event.target.value as OrderState)}>
            {states.map((state) => <option key={state} value={state}>{stateTitles[state]}</option>)}
          </select>
        </label>
        <button onClick={() => load(0)} disabled={isLoading}>
          {isLoading ? "Загрузка..." : "Найти"}
        </button>
      </div>

      {wasSearched && !isLoading && visibleBooks.length === 0 && (
        <p className={styles.message}>За выбранный интервал изменений нет</p>
      )}

      {visibleBooks.length > 0 && <>
        <div className={`${styles.row} ${styles.head}`}>
          <span>Книга</span><span>Ожидает</span><span>Обрабатывается</span><span>Выдана</span><span>Отменена</span>
        </div>
        {visibleBooks.map((book) => (
          <div className={styles.row} key={book.bookId}>
            <span>#{book.bookId} {book.bookTitle}</span>
            <span>{book.pendingCount}</span>
            <span>{book.inProgressCount}</span>
            <span>{book.doneCount}</span>
            <span>{book.abortedCount}</span>
          </div>
        ))}
        <Pagination
          page={page}
          hasNextPage={books.length > ANALYTICS_PAGE_SIZE}
          onPrevious={() => load(page - 1)}
          onNext={() => load(page + 1)}
        />
      </>}
    </section>
  );
};
