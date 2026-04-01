import { MOCK_DEMANDS, MOCK_SUPPLIES } from "../components/mockData";
import type {
  InventoryApi,
  ListDemandsResult,
  ListSuppliesResult,
} from "./inventoryApi";

export function createMockInventoryApi(): InventoryApi {
  return {
    async listSupplies(): Promise<ListSuppliesResult> {
      return {
        supplies: MOCK_SUPPLIES,
        totalCount: MOCK_SUPPLIES.length,
        nextPageToken: "",
      };
    },

    async listDemands(): Promise<ListDemandsResult> {
      return {
        demands: MOCK_DEMANDS,
        totalCount: MOCK_DEMANDS.length,
        nextPageToken: "",
      };
    },
  };
}
