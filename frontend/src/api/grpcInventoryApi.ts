import { createClient } from "@connectrpc/connect";
import { createConnectTransport } from "@connectrpc/connect-web";
import {
  InventoryService,
  type ListDemandsResponse,
  type ListSuppliesResponse,
  type Demand as ProtoDemand,
  type FoodCategory as ProtoFoodCategory,
  type Supply as ProtoSupply,
} from "@gen/mottainai/v1/inventory_pb.js";
import type {
  Demand,
  DemandStatusValue,
  FoodCategoryValue,
  Supply,
  SupplyStatusValue,
} from "../types/inventory";
import type {
  InventoryApi,
  ListDemandsResult,
  ListSuppliesResult,
} from "./inventoryApi";

/** Typed client interface matching InventoryService unary RPCs. */
interface InventoryClient {
  listSupplies(request: Record<string, never>): Promise<ListSuppliesResponse>;
  listDemands(request: Record<string, never>): Promise<ListDemandsResponse>;
}

function toFoodCategoryValue(v: ProtoFoodCategory): FoodCategoryValue {
  return v as number as FoodCategoryValue;
}

function toSupplyStatusValue(v: number): SupplyStatusValue {
  return v as SupplyStatusValue;
}

function toDemandStatusValue(v: number): DemandStatusValue {
  return v as DemandStatusValue;
}

function timestampToIso(
  ts: { seconds: bigint; nanos: number } | undefined,
): string {
  if (ts === undefined) {
    return "";
  }
  return new Date(
    Number(ts.seconds) * 1000 + ts.nanos / 1_000_000,
  ).toISOString();
}

function mapProtoSupply(s: ProtoSupply): Supply {
  return {
    id: s.id,
    providerId: s.providerId,
    itemName: s.itemName,
    category: toFoodCategoryValue(s.category),
    quantity: s.quantity,
    unit: s.unit,
    expiryDate: timestampToIso(s.expiryDate),
    status: toSupplyStatusValue(s.status),
    description: s.description,
    createdAt: timestampToIso(s.createdAt),
  };
}

function mapProtoDemand(d: ProtoDemand): Demand {
  return {
    id: d.id,
    recipientId: d.recipientId,
    category: toFoodCategoryValue(d.category),
    desiredQuantity: d.desiredQuantity,
    unit: d.unit,
    status: toDemandStatusValue(d.status),
    description: d.description,
    createdAt: timestampToIso(d.createdAt),
  };
}

export function createGrpcInventoryApi(baseUrl: string): InventoryApi {
  const transport = createConnectTransport({ baseUrl });
  // TS6 + @connectrpc/connect v2 has a type inference issue with GenService.
  // The runtime behavior is correct; we apply a targeted cast here.
  const client = createClient(
    InventoryService,
    transport,
  ) as unknown as InventoryClient;

  return {
    async listSupplies(): Promise<ListSuppliesResult> {
      const response = await client.listSupplies({});
      return {
        supplies: response.supplies.map(mapProtoSupply),
        totalCount: response.totalCount,
        nextPageToken: response.nextPageToken,
      };
    },

    async listDemands(): Promise<ListDemandsResult> {
      const response = await client.listDemands({});
      return {
        demands: response.demands.map(mapProtoDemand),
        totalCount: response.totalCount,
        nextPageToken: response.nextPageToken,
      };
    },
  };
}
