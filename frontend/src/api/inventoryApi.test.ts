import { describe, expect, test } from "vitest";
import { MOCK_DEMANDS, MOCK_SUPPLIES } from "../components/mockData";
import { createMockInventoryApi } from "./mockInventoryApi";

describe("mockInventoryApi", () => {
  test("listSupplies returns mock data", async () => {
    const api = createMockInventoryApi();
    const result = await api.listSupplies();
    expect(result.supplies).toEqual(MOCK_SUPPLIES);
    expect(result.totalCount).toBe(MOCK_SUPPLIES.length);
    expect(result.nextPageToken).toBe("");
  });

  test("listDemands returns mock data", async () => {
    const api = createMockInventoryApi();
    const result = await api.listDemands();
    expect(result.demands).toEqual(MOCK_DEMANDS);
    expect(result.totalCount).toBe(MOCK_DEMANDS.length);
    expect(result.nextPageToken).toBe("");
  });
});
