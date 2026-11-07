import styles from "./index.module.css"
import { BooksFeed } from "@/03-widgets/books-feed";
import { OrdersFeed } from "@/03-widgets/orders-feed";
import { Topbar } from "@/03-widgets/topbar";
import { OrdersAnalytics } from "@/03-widgets/orders-analytics";

type Props = {};

export const HomePage = (
  {}: Props
) => {
  return (
    <div className={styles.pageWrapper}>
      <Topbar/>
      <main className={styles.content}>
        <BooksFeed/>
        <OrdersFeed/>
        <OrdersAnalytics/>
      </main>
    </div>
  )
}
