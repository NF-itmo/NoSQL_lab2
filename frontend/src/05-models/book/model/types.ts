export type BookCharacteristics = {
  genre?: string;
  publisher?: string;
  publicationYear?: number;
  pageCount?: number;
};

export type BookLocation = {
  hall: number;
  rack: number;
  shelf: number;
};

export type Book = {
  id: number;
  title: string;
  author: string;
  location: BookLocation;
  description: string;
  available: boolean;
  views: number;
  characteristics: BookCharacteristics;
};

export type BookListItem = Omit<Book, "description">;

export type BookFilters = {
  title?: string;
  available?: boolean;
};

export type CreateBookRequest = Omit<Book, "available" | "views">;
