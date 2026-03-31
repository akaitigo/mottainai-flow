import { describe, expect, test } from "vitest";
import {
  DEMAND_STATUS_LABELS,
  DemandStatus,
  FOOD_CATEGORY_LABELS,
  FoodCategory,
  SUPPLY_STATUS_LABELS,
  SupplyStatus,
} from "./inventory";

describe("FoodCategory", () => {
  test("has all expected categories", () => {
    expect(FoodCategory.UNSPECIFIED).toBe(0);
    expect(FoodCategory.VEGETABLES).toBe(1);
    expect(FoodCategory.OTHER).toBe(10);
  });

  test("labels cover all categories", () => {
    const categoryValues = Object.values(FoodCategory);
    for (const v of categoryValues) {
      expect(FOOD_CATEGORY_LABELS[v]).toBeDefined();
    }
  });
});

describe("SupplyStatus", () => {
  test("has all expected statuses", () => {
    expect(SupplyStatus.UNSPECIFIED).toBe(0);
    expect(SupplyStatus.AVAILABLE).toBe(1);
    expect(SupplyStatus.CANCELLED).toBe(4);
  });

  test("labels cover all statuses", () => {
    const statusValues = Object.values(SupplyStatus);
    for (const v of statusValues) {
      expect(SUPPLY_STATUS_LABELS[v]).toBeDefined();
    }
  });
});

describe("DemandStatus", () => {
  test("has all expected statuses", () => {
    expect(DemandStatus.UNSPECIFIED).toBe(0);
    expect(DemandStatus.ACTIVE).toBe(1);
    expect(DemandStatus.CANCELLED).toBe(3);
  });

  test("labels cover all statuses", () => {
    const statusValues = Object.values(DemandStatus);
    for (const v of statusValues) {
      expect(DEMAND_STATUS_LABELS[v]).toBeDefined();
    }
  });
});
