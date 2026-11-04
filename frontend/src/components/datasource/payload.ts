import type {DataSourcePayload} from "~/api/datasource";

/**
 * Builds the create/update payload. On edit an empty password means "keep the stored one",
 * so it is omitted instead of sent as an empty string, which the backend would treat as a
 * new password and fail authentication.
 */
export function toDatasourcePayload(form: DataSourcePayload, isEdit: boolean): DataSourcePayload {
  const payload: DataSourcePayload = { ...form };
  if (isEdit && !payload.password) {
    delete payload.password;
  }
  return payload;
}
