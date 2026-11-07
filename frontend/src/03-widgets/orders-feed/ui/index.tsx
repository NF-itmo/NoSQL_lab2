import { useEffect, useState } from "react";
import { initializeOrders, loadOrders, type OrderState, useOrders } from "@/05-models/order";
import { useErrorNotify } from "@/06-shared/lib/errorNotifier";
import { CreateOrder } from "@/04-features/create-order";
import { ShowOrderItem } from "@/04-features/show-order-item";
import { Pagination } from "@/06-shared/ui/pagination";
import { ORDERS_PER_PAGE } from "../config/paginationConfig";
import styles from "./index.module.css";

export const OrdersFeed = () => {
  const orders = useOrders();
  const [page, setPage] = useState<number>(0);
  const [isLoading, setIsLoading] = useState<boolean>(true);
  const [status, setStatus] = useState<OrderState | "">("");
  const [createdAfter, setCreatedAfter] = useState("");
  const [createdBefore, setCreatedBefore] = useState("");
  const showError = useErrorNotify();

  useEffect(() => {
    initializeOrders()
      .catch(showError)
      .finally(() => setIsLoading(false));
  }, []);

  const search = () => {
    setIsLoading(true);
    loadOrders({
      status: status || undefined,
      createdAfter: createdAfter ? new Date(createdAfter).toISOString() : undefined,
      createdBefore: createdBefore ? new Date(createdBefore).toISOString() : undefined,
    }).catch(showError).finally(() => setIsLoading(false));
  };

  const pagesCount = Math.ceil(orders.length / ORDERS_PER_PAGE);
  const currentPage = Math.min(page, Math.max(pagesCount - 1, 0));
  const pageStart = currentPage * ORDERS_PER_PAGE;
  const visibleOrders = orders.slice(pageStart, pageStart + ORDERS_PER_PAGE);

  useEffect(() => {
    setPage((current) => Math.min(current, Math.max(pagesCount - 1, 0)));
  }, [pagesCount]);

  return (
    <section className={styles.feed}>
      <div className={styles.header}>
        <h1 className={styles.title}>Заявки</h1>
        <CreateOrder/>
      </div>

      <div className={styles.filters}>
        <select value={status} onChange={(event) => setStatus(event.target.value as OrderState | "")}>
          <option value="">Все статусы</option>
          <option value="PENDING">Ожидает подтверждения</option>
          <option value="IN_PROGRESS">Готовится к выдаче</option>
          <option value="DONE">Выдана</option>
          <option value="ABORTED">Отменена</option>
        </select>
        <input type="datetime-local" value={createdAfter} onChange={(event) => setCreatedAfter(event.target.value)} />
        <input type="datetime-local" value={createdBefore} onChange={(event) => setCreatedBefore(event.target.value)} />
        <button onClick={search}>Найти</button>
      </div>

      <div className={styles.content}>
        {
          isLoading && (
            <p className={styles.message}>Загрузка заявок...</p>
          )
        }
        {
          (!isLoading && orders.length === 0) && (
            <p className={styles.message}>Активных заявок нет</p>
          )
        }

        {
          visibleOrders.map((order) => (
            <ShowOrderItem
              key={order.id}
              order={order}
            />
          ))
        }
      </div>

      <Pagination
        page={currentPage}
        hasNextPage={currentPage < pagesCount - 1}
        onPrevious={() => setPage((current) => Math.max(current - 1, 0))}
        onNext={() => setPage((current) => Math.min(current + 1, pagesCount - 1))}
      />
    </section>
  );
};
