import { describe, expect, test, vi } from "vitest";

describe("createInventoryApi", () => {
  test("returns mock api when VITE_API_BASE_URL is not set", async () => {
    vi.stubEnv("VITE_API_BASE_URL", "");
    const { createInventoryApi } = await import("./createInventoryApi");
    const api = createInventoryApi();
    const result = await api.listSupplies();
    expect(result.supplies.length).toBeGreaterThan(0);
    vi.unstubAllEnvs();
  });
});
