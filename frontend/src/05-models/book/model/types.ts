import type { Location } from "@/05-models/location";

export type BookCharacteristics = {
  genre?: string;
  publisher?: string;
  publicationYear?: number;
  pageCount?: number;
};

export type Book = {
  id: number;
  title: string;
  author: string;
  locationId: string;
  location?: Location;
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

export type CreateBookRequest = Omit<Book, "available" | "views" | "location">;
