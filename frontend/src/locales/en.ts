export default {
  header: {
    brand: "DataConnAI",
    workspace: "Workspace",
    itemOne: "item one",
    itemTwo: "item two",
    info: "Info",
    orders: "Orders",
    theme: "Theme",
    language: "Language",
  },

  side: {
    mcpBuilder: "MCP Builder",
    sematicModels: "Sematic Models",
    dataSources: "Data Sources",
  },

  datasource: {
    common: {
      cancel: "Cancel",
      confirm: "Confirm",
      testConnection: "Test Connection",
      update: "Update",
      create: "Create",
    },
    dialog: {
      createTitle: "Create Data Source",
      editTitle: "Edit Data Source",
    },
    form: {
      fields: {
        name: "Connection Name",
        type: "Database Type",
        username: "Username",
        password: "Password",
        description: "Description",
      },
      placeholders: {
        name: "Enter connection name",
        type: "Select database type",
        jdbc: "Enter JDBC connection string",
        username: "Enter database username",
        password: "Enter database password",
        description: "Enter description (optional)",
      },
      rules: {
        nameRequired: "Please enter connection name",
        nameLength: "Length should be 1 to 100 characters",
        typeRequired: "Please select database type",
        jdbcRequired: "Please enter JDBC connection string",
        usernameRequired: "Please enter username",
        passwordRequired: "Please enter password",
      },
    },
    table: {
      columns: {
        id: "ID",
        name: "Connection Name",
        type: "Type",
        createdBy: "Creator",
        description: "Description",
        updatedAt: "Updated At",
        actions: "Actions",
      },
      actions: {
        edit: "Edit",
        delete: "Delete",
      },
    },
    page: {
      createButton: "Create Data Source",
      deleteConfirmTitle: "Delete Confirmation",
      deleteConfirmMessage: 'Are you sure to delete data source "{name}"?',
    },
    messages: {
      loadFailed: "Failed to load data sources",
      testSuccess: "Connection test succeeded",
      testFailed: "Connection test failed",
      updateSuccess: "Updated successfully",
      createSuccess: "Created successfully",
      operateFailed: "Operation failed",
      deleteSuccess: "Deleted successfully",
      deleteFailed: "Delete failed",
    },
  },
}
