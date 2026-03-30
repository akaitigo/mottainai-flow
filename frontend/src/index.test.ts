import { expect, test } from "vitest";
import { APP_NAME } from "./index";

test("app name is defined", () => {
  expect(APP_NAME).toBe("mottainai-flow");
});
