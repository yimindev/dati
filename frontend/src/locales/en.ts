export default {
  home: {
    subtitle: "Data Intelligence Platform",
  },

  common: {
    // General Actions
    save: "Save",
    cancel: "Cancel",
    close: "Close",
    confirm: "Confirm",
    confirmSync: "Sync",
    edit: "Edit",
    delete: "Delete",
    create: "Create",
    update: "Update",
    remove: "Remove",
    search: "Search",
    testConnection: "Test Connection",
    back: "Back",
    nextStep: "Next",
    refresh: "Refresh",
    yes: "Yes",
    no: "No",
    warning: "Warning",
    all: "Select All",
    none: "Deselect All",
    clear: "Clear",
    selected: "Selected",

    // Common Fields
    id: "ID",
    name: "Name",
    type: "Type",
    description: "Description",
    createdBy: "Creator",
    updatedAt: "Updated At",
    actions: "Actions",
    total: "Total {total}",
    totalItems: "items",
    selectedItems: "selected",
    schema: "Schema",
    username: "Username",
    password: "Password",
    tableName: "Table Name",
    aliases: "Aliases",
    aliasesPlaceholder: "Enter alias and press Enter to add",

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
    nameLengthError: "Length must be between 1 and 100 characters",
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
      semanticModels: "Semantic Models",
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

  subject: {
    // Page Titles
    title: "Subject Management",
    createTitle: "Create Subject",
    editTitle: "Edit Subject",

    // Fields
    datasource: "Associated Data Source",
    selectDatasource: "Select a data source",
    tableManagement: "Table Management",
    termManagement: "Term Management",
    basicInfo: "Basic Information",

    // Actions
    addTable: "Add Table",
    removeTable: "Remove Table",
    addTerm: "Add Term",
    editTerm: "Edit Term",
    deleteTerm: "Delete Term",
    addRelation: "Add Relation",
    removeRelation: "Remove Relation",
    manageRelation: "Manage Relation",
    linkedEntities: "Linked Entities",

    // Term Relations
    selectTable: "Please select a table",
    tableLevel: "Table",
    fieldLevel: "Field",

    // Relation Dialog
    selectType: "Select Type",
    selectTableStep: "Select Table",
    selectTarget: "Select Target",
    searchTable: "Search table...",
    filterSchema: "Schema",
    searchField: "Search field...",
    existingRelations: "Existing Relations",
    noRelations: "No relations yet",
    noSearchResults: "No matching results",
    noFieldsInTable: "No fields found in this table",
    relationAlreadyExists: "The selected relation already exists",
    fieldConfig: "Field Configuration",
    tableLevelRelation: "Table-level Relation",

    // List Page
    createButton: "Create Subject",
    searchPlaceholder: "Search subject name",
    noSubject: "No subjects yet",
    noTables: "No linked tables",
    noTerms: "No terms yet",
    removeTableConfirm: "Are you sure you want to remove table \"{name}\"?",
    removeTermConfirm: "Are you sure you want to delete term \"{name}\"?",
    removeRelationConfirm: "Are you sure you want to remove this relation?",

    // Success Messages
    createSuccess: "Subject created successfully",
    updateSuccess: "Subject updated successfully",
    deleteSuccess: "Subject deleted successfully",
    addTableSuccess: "Table added successfully",
    removeTableSuccess: "Table removed successfully",
    addTermSuccess: "Term created successfully",
    updateTermSuccess: "Term updated successfully",
    deleteTermSuccess: "Term deleted successfully",
    addRelationResult: "{added} added, {skipped} duplicates skipped",
    removeRelationSuccess: "Relation removed successfully",
  },

  tableInfo: {
    // Page Titles
    title: "Table Information Management",
    subtitle: "View and manage tables in this datasource",

    // Specific Actions
    addTable: "Add Tables",
    syncColumns: "Sync Columns",
    columnSettings: "Column Settings",
    configInfo: "Edit",

    // Form Related
    selectSchema: "Select Schema",
    availableTables: "Available Tables",
    selectedTables: "Selected Tables",
    noSelected: "No tables selected",
    alreadyAdded: "Already Added",
    selectedCount: "{count} table(s) selected",
    selectAtLeastOne: "Please select at least one table",
    addSelected: "Add Selected Tables",

    // Specific Messages
    addSuccess: "Added successfully",
    addFailed: "Failed to add",
    syncSuccess: "Synced successfully",
    syncFailed: "Failed to sync",
    syncColumnsConfirm: "Sync columns will update column definitions with the latest information from the database",
    syncColumnsOverwrite: "Overwrite existing descriptions with non-empty database comments",
    removeConfirm: 'Are you sure you want to remove table "{name}"? Related column information will also be deleted.',
    removeSuccess: "Removed successfully",
    removeFailed: "Failed to remove",
  },

  column: {
    // Column Names
    columnName: "Column Name",
    description: "Description",
    type: "Type",

    // Search
    searchPlaceholder: "Search by column name or description",

    // Actions
    editTitle: "Edit Column",

    // Form
    enterDescription: "Enter column description",
  },
};
