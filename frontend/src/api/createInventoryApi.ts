import { createGrpcInventoryApi } from "./grpcInventoryApi";
import type { InventoryApi } from "./inventoryApi";
import { createMockInventoryApi } from "./mockInventoryApi";

/**
 * Creates an InventoryApi instance.
 *
 * When VITE_API_BASE_URL is set, connects to the real gRPC backend
 * via Connect protocol. Otherwise falls back to static mock data.
 */
export function createInventoryApi(): InventoryApi {
  const baseUrl = import.meta.env.VITE_API_BASE_URL;
  if (typeof baseUrl === "string" && baseUrl.length > 0) {
    return createGrpcInventoryApi(baseUrl);
  }
  return createMockInventoryApi();
}
