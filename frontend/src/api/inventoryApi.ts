import type { Demand, Supply } from "../types/inventory";

/** Response from listing supplies. */
export interface ListSuppliesResult {
  readonly supplies: readonly Supply[];
  readonly totalCount: number;
  readonly nextPageToken: string;
}

/** Response from listing demands. */
export interface ListDemandsResult {
  readonly demands: readonly Demand[];
  readonly totalCount: number;
  readonly nextPageToken: string;
}

/** API client interface for the InventoryService. */
export interface InventoryApi {
  listSupplies(): Promise<ListSuppliesResult>;
  listDemands(): Promise<ListDemandsResult>;
}
