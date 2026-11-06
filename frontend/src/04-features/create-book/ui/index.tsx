import { useState } from "react";
import { addBook, createBook } from "@/05-models/book";
import { createLocation } from "@/05-models/location";
import { useErrorNotify } from "@/06-shared/lib/errorNotifier";
import { PopupButton, PopupInput, PopupTextarea, PopupWindow } from "@/06-shared/ui/popups";
import styles from "./index.module.css";

export const CreateBook = () => {
  const [isOpen, setIsOpen] = useState<boolean>(false);
  
  const [id, setId] = useState<string>("");
  const [title, setTitle] = useState<string>("");
  const [author, setAuthor] = useState<string>("");
  const [hall, setHall] = useState<string>("");
  const [rack, setRack] = useState<string>("");
  const [shelf, setShelf] = useState<string>("");
  const [genre, setGenre] = useState<string>("");
  const [publisher, setPublisher] = useState<string>("");
  const [publicationYear, setPublicationYear] = useState<string>("");
  const [pageCount, setPageCount] = useState<string>("");
  const [description, setDescription] = useState<string>("");

  const showError = useErrorNotify();

  const reset = () => {
    setId("");
    setTitle("");
    setAuthor("");
    setHall("");
    setRack("");
    setShelf("");
    setGenre("");
    setPublisher("");
    setPublicationYear("");
    setPageCount("");
    setDescription("");
  };

  const submit = async () => {
    const bookId = Number(id);

    if (!Number.isInteger(bookId) || bookId <= 0) {
      showError("ID книги должен быть положительным целым числом");
      return;
    }

    if (!title.trim() || !author.trim() || !description.trim()) {
      showError("Заполните все поля книги");
      return;
    }

    const year = publicationYear ? Number(publicationYear) : undefined;
    const pages = pageCount ? Number(pageCount) : undefined;
    const location = [hall, rack, shelf].map(Number);
    if ((year !== undefined && (!Number.isInteger(year) || year < 0)) ||
      (pages !== undefined && (!Number.isInteger(pages) || pages <= 0)) ||
      location.some((value) => !Number.isInteger(value) || value <= 0)) {
      showError("Проверьте характеристики и расположение книги");
      return;
    }

    try {
      const createdLocation = await createLocation({
        hall: location[0],
        rack: location[1],
        shelf: location[2],
      });
      const book = await createBook({
        id: bookId,
        title: title.trim(),
        author: author.trim(),
        locationId: createdLocation.id,
        description: description.trim(),
        characteristics: {
          genre: genre.trim() || undefined,
          publisher: publisher.trim() || undefined,
          publicationYear: year,
          pageCount: pages,
        },
      });
      addBook(book);
      reset();
      setIsOpen(false);
    } catch (error) {
      showError(error);
    }
  };

  return (
    <>
      <button className={styles.openButton} onClick={() => setIsOpen(true)}>
        [+]
      </button>

      {isOpen && (
        <PopupWindow name="Новая книга" onClose={() => setIsOpen(false)}>
          <div className={styles.section}>
            <p className={styles.sectionTitle}>Книга</p>
            <PopupInput name="ID" value={id} setInput={setId} autoFocus={true}/>
            <PopupInput name="Название" value={title} setInput={setTitle}/>
            <PopupInput name="Автор" value={author} setInput={setAuthor}/>
            <PopupTextarea name="Описание" value={description} setInput={setDescription}/>
          </div>

          <div className={styles.section}>
            <p className={styles.sectionTitle}>Расположение</p>
            <PopupInput name="Зал" value={hall} setInput={setHall}/>
            <PopupInput name="Стеллаж" value={rack} setInput={setRack}/>
            <PopupInput name="Полка" value={shelf} setInput={setShelf}/>
          </div>

          <div className={styles.section}>
            <p className={styles.sectionTitle}>Справочная информация</p>
            <PopupInput name="Жанр" value={genre} setInput={setGenre} optional/>
            <PopupInput name="Издатель" value={publisher} setInput={setPublisher} optional/>
            <PopupInput name="Год издания" value={publicationYear} setInput={setPublicationYear} optional/>
            <PopupInput name="Страниц" value={pageCount} setInput={setPageCount} optional/>
          </div>
          <PopupButton name="Добавить" onClick={submit}/>
        </PopupWindow>
      )}
    </>
  );
};
