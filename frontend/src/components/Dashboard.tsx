import { useEffect, useState } from "react";
import { createInventoryApi } from "../api/createInventoryApi";
import {
  DEMAND_STATUS_LABELS,
  type Demand,
  type DemandStatusValue,
  FOOD_CATEGORY_LABELS,
  type FoodCategoryValue,
  SUPPLY_STATUS_LABELS,
  type Supply,
  type SupplyStatusValue,
} from "../types/inventory";

function categoryLabel(value: FoodCategoryValue): string {
  return FOOD_CATEGORY_LABELS[value];
}

function supplyStatusLabel(value: SupplyStatusValue): string {
  return SUPPLY_STATUS_LABELS[value];
}

function demandStatusLabel(value: DemandStatusValue): string {
  return DEMAND_STATUS_LABELS[value];
}

const api = createInventoryApi();

export function Dashboard() {
  const [supplies, setSupplies] = useState<readonly Supply[]>([]);
  const [demands, setDemands] = useState<readonly Demand[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    let cancelled = false;

    async function fetchData() {
      try {
        const [suppliesResult, demandsResult] = await Promise.all([
          api.listSupplies(),
          api.listDemands(),
        ]);
        if (!cancelled) {
          setSupplies(suppliesResult.supplies);
          setDemands(demandsResult.demands);
        }
      } catch (err: unknown) {
        if (!cancelled) {
          const message =
            err instanceof Error ? err.message : "Failed to fetch data";
          setError(message);
        }
      } finally {
        if (!cancelled) {
          setLoading(false);
        }
      }
    }

    fetchData();

    return () => {
      cancelled = true;
    };
  }, []);

  if (loading) {
    return <p>Loading...</p>;
  }

  if (error !== null) {
    return <p role="alert">Error: {error}</p>;
  }

  return (
    <div>
      <section style={{ marginBottom: "2rem" }}>
        <h2>提供一覧 (Supply)</h2>
        <table
          style={{ width: "100%", borderCollapse: "collapse" }}
          aria-label="提供一覧"
        >
          <thead>
            <tr>
              <th style={thStyle}>品名</th>
              <th style={thStyle}>カテゴリ</th>
              <th style={thStyle}>数量</th>
              <th style={thStyle}>消費期限</th>
              <th style={thStyle}>ステータス</th>
            </tr>
          </thead>
          <tbody>
            {supplies.map((s) => (
              <tr key={s.id}>
                <td style={tdStyle}>{s.itemName}</td>
                <td style={tdStyle}>{categoryLabel(s.category)}</td>
                <td style={tdStyle}>
                  {s.quantity} {s.unit}
                </td>
                <td style={tdStyle}>
                  {new Date(s.expiryDate).toLocaleDateString("ja-JP")}
                </td>
                <td style={tdStyle}>{supplyStatusLabel(s.status)}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </section>

      <section>
        <h2>需要一覧 (Demand)</h2>
        <table
          style={{ width: "100%", borderCollapse: "collapse" }}
          aria-label="需要一覧"
        >
          <thead>
            <tr>
              <th style={thStyle}>カテゴリ</th>
              <th style={thStyle}>希望数量</th>
              <th style={thStyle}>ステータス</th>
              <th style={thStyle}>説明</th>
            </tr>
          </thead>
          <tbody>
            {demands.map((d) => (
              <tr key={d.id}>
                <td style={tdStyle}>{categoryLabel(d.category)}</td>
                <td style={tdStyle}>
                  {d.desiredQuantity} {d.unit}
                </td>
                <td style={tdStyle}>{demandStatusLabel(d.status)}</td>
                <td style={tdStyle}>{d.description}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </section>
    </div>
  );
}

const thStyle: React.CSSProperties = {
  textAlign: "left",
  borderBottom: "2px solid #333",
  padding: "0.5rem",
};

const tdStyle: React.CSSProperties = {
  borderBottom: "1px solid #ddd",
  padding: "0.5rem",
};
