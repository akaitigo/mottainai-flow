/** Food category matching proto FoodCategory enum values. */
export const FoodCategory = {
  UNSPECIFIED: 0,
  VEGETABLES: 1,
  FRUITS: 2,
  DAIRY: 3,
  MEAT: 4,
  SEAFOOD: 5,
  BREAD: 6,
  PREPARED: 7,
  CANNED: 8,
  FROZEN: 9,
  OTHER: 10,
} as const;

export type FoodCategoryValue =
  (typeof FoodCategory)[keyof typeof FoodCategory];

/** Human-readable labels for food categories. */
export const FOOD_CATEGORY_LABELS: Record<FoodCategoryValue, string> = {
  [FoodCategory.UNSPECIFIED]: "未分類",
  [FoodCategory.VEGETABLES]: "野菜",
  [FoodCategory.FRUITS]: "果物",
  [FoodCategory.DAIRY]: "乳製品",
  [FoodCategory.MEAT]: "肉類",
  [FoodCategory.SEAFOOD]: "魚介類",
  [FoodCategory.BREAD]: "パン",
  [FoodCategory.PREPARED]: "調理済み",
  [FoodCategory.CANNED]: "缶詰",
  [FoodCategory.FROZEN]: "冷凍食品",
  [FoodCategory.OTHER]: "その他",
};

/** Supply status matching proto SupplyStatus enum values. */
export const SupplyStatus = {
  UNSPECIFIED: 0,
  AVAILABLE: 1,
  MATCHED: 2,
  EXPIRED: 3,
  CANCELLED: 4,
} as const;

export type SupplyStatusValue =
  (typeof SupplyStatus)[keyof typeof SupplyStatus];

export const SUPPLY_STATUS_LABELS: Record<SupplyStatusValue, string> = {
  [SupplyStatus.UNSPECIFIED]: "未設定",
  [SupplyStatus.AVAILABLE]: "提供可能",
  [SupplyStatus.MATCHED]: "マッチ済み",
  [SupplyStatus.EXPIRED]: "期限切れ",
  [SupplyStatus.CANCELLED]: "キャンセル",
};

/** Demand status matching proto DemandStatus enum values. */
export const DemandStatus = {
  UNSPECIFIED: 0,
  ACTIVE: 1,
  MATCHED: 2,
  CANCELLED: 3,
} as const;

export type DemandStatusValue =
  (typeof DemandStatus)[keyof typeof DemandStatus];

export const DEMAND_STATUS_LABELS: Record<DemandStatusValue, string> = {
  [DemandStatus.UNSPECIFIED]: "未設定",
  [DemandStatus.ACTIVE]: "受付中",
  [DemandStatus.MATCHED]: "マッチ済み",
  [DemandStatus.CANCELLED]: "キャンセル",
};

/** Supply entry from the InventoryService. */
export interface Supply {
  readonly id: string;
  readonly providerId: string;
  readonly itemName: string;
  readonly category: FoodCategoryValue;
  readonly quantity: number;
  readonly unit: string;
  readonly expiryDate: string;
  readonly status: SupplyStatusValue;
  readonly description: string;
  readonly createdAt: string;
}

/** Demand entry from the InventoryService. */
export interface Demand {
  readonly id: string;
  readonly recipientId: string;
  readonly category: FoodCategoryValue;
  readonly desiredQuantity: number;
  readonly unit: string;
  readonly status: DemandStatusValue;
  readonly description: string;
  readonly createdAt: string;
}
