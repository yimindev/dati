export default {
  common: {
    // General Actions
    save: "Save",
    cancel: "Cancel",
    confirm: "Confirm",
    edit: "Edit",
    delete: "Delete",
    create: "Create",
    update: "Update",
    remove: "Remove",
    search: "Search",
    testConnection: "Test Connection",
    back: "Back",
    refresh: "Refresh",
    yes: "Yes",
    no: "No",
    warning: "Warning",
    all: "Select All",
    none: "Deselect All",

    // Common Fields
    id: "ID",
    name: "Name",
    type: "Type",
    description: "Description",
    createdBy: "Creator",
    updatedAt: "Updated At",
    actions: "Actions",
    schema: "Schema",
    username: "Username",
    password: "Password",
    tableName: "Table Name",

    // Common Messages
    saveSuccess: "Saved successfully",
    deleteSuccess: "Deleted successfully",
    loadFailed: "Failed to load",
    operationFailed: "Operation failed",
    confirmDelete: "Are you sure you want to delete \"{name}\"?",

    // Form Placeholders
    placeholder: {
      name: "Enter name",
      type: "Select type",
      description: "Enter description (optional)",
    },

    // Validation Rules
    required: "Please enter {name}",
    selectRequired: "Please select {name}",
  },

  layout: {
    header: {
      brand: "DatI",
      workspace: "Workspace",
      itemOne: "Item One",
      itemTwo: "Item Two",
      info: "Info",
      orders: "Orders",
      theme: "Theme",
      language: "Language",
    },
    side: {
      expand: "Expand",
      collapse: "Collapse",
      mcpBuilder: "MCP Builder",
      sematicModels: "Sematic Models",
      dataSources: "Data Sources",
    },
  },

  datasource: {
    // Page Titles
    title: "Data Source Management",
    createTitle: "Create Data Source",
    editTitle: "Edit Data Source",

    // Data Source Specific Fields
    jdbcUrl: "JDBC Connection String",
    connectionName: "Connection Name",
    databaseType: "Database Type",

    // Specific Actions
    tableManage: "Table Manage",

    // Specific Messages
    testSuccess: "Connection test succeeded",
    testFailed: "Connection test failed",
    deleteConfirmMessage: 'Are you sure you want to delete data source "{name}"?',

    // List Page
    createButton: "Create Data Source",
    searchPlaceholder: "Name or ID",
  },

  tableInfo: {
    // Page Titles
    title: "Table Information Management",
    subtitle: "View and manage tables in this datasource",

    // Specific Actions
    addTable: "Add Tables",
    syncColumns: "Sync Columns",
    columnSettings: "Column Settings",
    configInfo: "Configure",

    // Form Related
    selectSchema: "Select Schema",
    availableTables: "Available Tables",
    alreadyAdded: "Already Added",
    selectedCount: "{count} table(s) selected",
    selectAtLeastOne: "Please select at least one table",
    addSelected: "Add Selected Tables",

    // Specific Messages
    addSuccess: "Added successfully",
    addFailed: "Failed to add",
    syncSuccess: "Synced successfully",
    syncFailed: "Failed to sync",
    removeConfirm: 'Are you sure you want to remove table "{name}"? Related column information will also be deleted.',
    removeSuccess: "Removed successfully",
    removeFailed: "Failed to remove",
  },
};
