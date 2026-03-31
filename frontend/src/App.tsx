import { Dashboard } from "./components/Dashboard";
import { APP_NAME } from "./constants";

export function App() {
  return (
    <div style={{ maxWidth: 960, margin: "0 auto", padding: "1rem" }}>
      <header>
        <h1>{APP_NAME}</h1>
        <p>余剰食品の最適配車プラットフォーム</p>
      </header>
      <main>
        <Dashboard />
      </main>
    </div>
  );
}
