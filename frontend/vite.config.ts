import path from "node:path";
import react from "@vitejs/plugin-react";
import { defineConfig } from "vite";

export default defineConfig({
  plugins: [react()],
  resolve: {
    alias: {
      "@gen": path.resolve(import.meta.dirname, "src/gen"),
    },
  },
  server: {
    port: 3000,
    proxy: {
      "/mottainai.v1": {
        target: "http://localhost:9090",
        changeOrigin: true,
      },
    },
  },
  build: {
    outDir: "dist",
  },
});
