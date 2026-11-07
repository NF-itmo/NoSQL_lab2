import {
  deleteBook,
  releaseBook,
  removeBook,
  setBookAvailability,
  type Book,
} from "@/05-models/book";
import { addOrder, createOrder } from "@/05-models/order";
import { useErrorNotify } from "@/06-shared/lib/errorNotifier";
import { PopupButton, PopupWindow } from "@/06-shared/ui/popups";
import styles from "./index.module.css";

type Props = {
  book: Book | null;
  onClose: () => void;
};

export const BookPopup = ({ book, onClose }: Props) => {
  const showError = useErrorNotify();

  const create = async () => {
    if (!book) {
      return;
    }

    try {
      const order = await createOrder(book.id);
      addOrder(order);
      onClose();
    } catch (error) {
      showError(error);
    }
  };

  const remove = async () => {
    if (!book) {
      return;
    }

    try {
      await deleteBook(book.id);
      removeBook(book.id);
      onClose();
    } catch (error) {
      showError(error);
    }
  };

  const release = async () => {
    if (!book) {
      return;
    }

    try {
      await releaseBook(book.id);
      setBookAvailability(book.id, true);
      onClose();
    } catch (error) {
      showError(error);
    }
  };

  if (!book) {
    return null;
  }

  return (
    <PopupWindow name={book.title} onClose={onClose}>
      <p>Автор: {book.author}</p>
      {book.characteristics.genre && <p>Жанр: {book.characteristics.genre}</p>}
      {book.characteristics.publisher && <p>Издатель: {book.characteristics.publisher}</p>}
      {book.characteristics.publicationYear && <p>Год издания: {book.characteristics.publicationYear}</p>}
      {book.characteristics.pageCount && <p>Страниц: {book.characteristics.pageCount}</p>}
      {book.location && (
        <p>Расположение: зал {book.location.hall}, стеллаж {book.location.rack}, полка {book.location.shelf}</p>
      )}
      <p>Статус: {book.available ? "доступна" : "выдана"}</p>
      <p>Просмотры: {book.views}</p>
      <div className={styles.description}>
        <p>Описание:</p>
        <p>{book.description}</p>
      </div>
      {
        book.available && (
          <PopupButton name="Оформить заявку" onClick={create}/>
        )
      }
      {
        !book.available && (
          <PopupButton name="Высвободить книгу" onClick={release}/>
        )
      }
      <PopupButton name="Удалить книгу" onClick={remove}/>
    </PopupWindow>
  );
};
