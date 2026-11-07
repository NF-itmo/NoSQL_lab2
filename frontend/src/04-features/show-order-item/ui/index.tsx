import { setBookAvailability } from "@/05-models/book";
import { cancelOrder, completeOrder, confirmOrder, removeOrder, setOrderStatus, type Order } from "@/05-models/order";
import { useErrorNotify } from "@/06-shared/lib/errorNotifier";
import styles from "./index.module.css";

type Props = {
  order: Order;
};

export const ShowOrderItem = ({ order }: Props) => {
  const showError = useErrorNotify();

  const cancel = async () => {
    try {
      await cancelOrder(order.id);
      removeOrder(order.id);
    } catch (error) {
      showError(error);
    }
  };

  const confirm = async () => {
    try {
      await confirmOrder(order.id);
      setOrderStatus(order.id, "IN_PROGRESS");
    } catch (error) {
      showError(error);
    }
  };

  const complete = async () => {
    try {
      await completeOrder(order.id);
      setOrderStatus(order.id, "DONE");
      setBookAvailability(order.bookId, false);
    } catch (error) {
      showError(error);
    }
  };

  return (
    <article className={styles.order}>
      <div>
        <p className={styles.orderTitle}>Заявка #{order.id}</p>
        <p>Книга #{order.bookId}</p>
        {order.bookLocation && (
          <p>
            Расположение: зал {order.bookLocation.hall}, стеллаж {order.bookLocation.rack}, полка {order.bookLocation.shelf}
          </p>
        )}
        <p>Статус: {statusLabel[order.status]}</p>
        <p>
          Истекает: {new Date(order.expiresAt).toLocaleString("ru-RU", {
            dateStyle: "short",
            timeStyle: "short",
          })}
        </p>
      </div>
      <div className={styles.actions}>
        {order.status === "PENDING" && <button onClick={confirm}>Взять в работу</button>}
        {order.status === "IN_PROGRESS" && <button onClick={complete}>Выдать</button>}
        {(order.status === "PENDING" || order.status === "IN_PROGRESS") && <button onClick={cancel}>Отменить</button>}
      </div>
    </article>
  );
};

const statusLabel = {
  PENDING: "ожидает",
  IN_PROGRESS: "обрабатывается",
  DONE: "выдана",
  ABORTED: "отменена",
} as const;
