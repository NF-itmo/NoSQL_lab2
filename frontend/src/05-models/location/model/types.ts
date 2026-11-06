export type Location = {
  id: string;
  hall: number;
  rack: number;
  shelf: number;
};

export type LocationPayload = Omit<Location, "id">;
