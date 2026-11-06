import { API_V1_PATH } from "@/06-shared/api/request/config";
import { request } from "@/06-shared/api/request";
import type { Location, LocationPayload } from "../model/types";

const LOCATIONS_API_PATH = `${API_V1_PATH}/locations`;

export const getLocations = (): Promise<Location[]> => request<Location[]>(LOCATIONS_API_PATH);

export const createLocation = (payload: LocationPayload): Promise<Location> => request<Location>(LOCATIONS_API_PATH, {
  method: "POST",
  headers: { "Content-Type": "application/json" },
  body: JSON.stringify(payload),
});

export const updateLocation = (id: string, payload: LocationPayload): Promise<Location> => request<Location>(`${LOCATIONS_API_PATH}/${id}`, {
  method: "PUT",
  headers: { "Content-Type": "application/json" },
  body: JSON.stringify(payload),
});
