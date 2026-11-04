import { describe, expect, it } from "vitest";
import { toDatasourcePayload } from "../payload";
import type { DataSourcePayload } from "~/api/datasource";

const form: DataSourcePayload = {
  name: "Analytics",
  description: "",
  jdbc_url: "jdbc:postgresql://localhost:5432/app",
  username: "postgres",
  password: "",
  type: "POSTGRESQL",
};

describe("toDatasourcePayload", () => {
  it("keeps a typed password on create", () => {
    const payload = toDatasourcePayload({ ...form, password: "secret" }, false);

    expect(payload.password).toBe("secret");
  });

  it("omits an empty password on edit so the stored password is kept", () => {
    const payload = toDatasourcePayload(form, true);

    expect("password" in payload).toBe(false);
    expect(payload.name).toBe("Analytics");
  });

  it("sends a new password on edit when one is typed", () => {
    const payload = toDatasourcePayload(
      { ...form, password: "new-secret" },
      true,
    );

    expect(payload.password).toBe("new-secret");
  });

  it("does not mutate the form object", () => {
    toDatasourcePayload(form, true);

    expect("password" in form).toBe(true);
  });
});
