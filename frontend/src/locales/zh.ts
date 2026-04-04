export default {
  home: {
    subtitle: "数据智能平台",
  },

  common: {
    // 通用操作
    save: "保存",
    cancel: "取消",
    confirm: "确认",
    edit: "编辑",
    delete: "删除",
    create: "创建",
    update: "更新",
    remove: "移除",
    search: "搜索",
    testConnection: "测试连接",
    back: "返回",
    refresh: "刷新",
    yes: "是",
    no: "否",
    warning: "警告",
    all: "全选",
    none: "取消全选",
    clear: "清空",

    // 通用字段
    id: "ID",
    name: "名称",
    type: "类型",
    description: "描述",
    createdBy: "创建人",
    updatedAt: "更新时间",
    actions: "操作",
    total: "共 {total} 条",
    totalItems: "个",
    selectedItems: "个已选",
    schema: "Schema",
    username: "用户名",
    password: "密码",
    tableName: "表名",

    // 通用消息
    saveSuccess: "保存成功",
    deleteSuccess: "删除成功",
    loadFailed: "加载失败",
    operationFailed: "操作失败",
    confirmDelete: "确定要删除「{name}」吗？",

    // 表单占位符
    placeholder: {
      name: "请输入名称",
      type: "请选择类型",
      description: "请输入描述（可选）",
    },

    // 校验规则
    required: "请输入{name}",
    selectRequired: "请选择{name}",
    nameLengthError: "长度在 1 到 100 个字符",
  },

  layout: {
    header: {
      brand: "DatI",
      workspace: "工作台",
      itemOne: "项目一",
      itemTwo: "项目二",
      info: "信息",
      orders: "订单",
      theme: "主题",
      language: "语言",
    },
    side: {
      expand: "展开",
      collapse: "收起",
      mcpBuilder: "构建MCP",
      semanticModels: "语义模型",
      semanticManagement: "语义管理",
      dataSources: "数据源",
    },
  },

  datasource: {
    // 页面标题
    title: "数据源管理",
    createTitle: "创建数据源",
    editTitle: "编辑数据源",

    // 数据源特有字段
    jdbcUrl: "JDBC连接字符串",
    connectionName: "连接名称",
    databaseType: "数据库类型",

    // 特有操作
    tableManage: "表管理",

    // 特有消息
    testSuccess: "连接测试成功",
    testFailed: "连接测试失败",
    deleteConfirmMessage: '确定要删除数据源「{name}」吗？',

    // 列表页
    createButton: "创建数据源",
    searchPlaceholder: "名称或ID",
  },

  subject: {
    // 页面标题
    title: "主题管理",
    createTitle: "创建主题",
    editTitle: "编辑主题",

    // 字段
    datasource: "关联数据源",
    tableCount: "关联表数量",
    selectDatasource: "请选择数据源",
    tableManagement: "表管理",
    termManagement: "术语管理",
    basicInfo: "基本信息",

    // 操作
    addTable: "添加表",
    removeTable: "移除表",
    addTerm: "添加术语",
    editTerm: "编辑术语",
    deleteTerm: "删除术语",
    addRelation: "添加关联",
    removeRelation: "移除关联",

    // 术语关联
    linkedTables: "已关联",
    linkToTable: "关联到表",
    linkToField: "关联到字段",
    selectTable: "请选择表",
    selectField: "请选择字段（可选）",
    fieldOptional: "不选则关联整张表",
    tableLevel: "整表",
    fieldLevel: "字段",

    // 列表页
    createButton: "创建主题",
    searchPlaceholder: "搜索主题名称",
    noSubject: "暂无主题",
    noTables: "暂无关联表",
    noTerms: "暂无术语",

    // 确认消息
    deleteConfirmMessage: "确定要删除主题「{name}」吗？删除后相关术语和关联也会被删除。",
    removeTableConfirm: "确定要移除表「{name}」吗？",
    removeTermConfirm: "确定要删除术语「{name}」吗？",
    removeRelationConfirm: "确定要移除该关联吗？",

    // 成功消息
    createSuccess: "主题创建成功",
    updateSuccess: "主题更新成功",
    deleteSuccess: "主题删除成功",
    addTableSuccess: "添加表成功",
    removeTableSuccess: "移除表成功",
    addTermSuccess: "术语创建成功",
    updateTermSuccess: "术语更新成功",
    deleteTermSuccess: "术语删除成功",
    addRelationSuccess: "关联添加成功",
    removeRelationSuccess: "关联移除成功",
  },

  tableInfo: {
    // 页面标题
    title: "表信息管理",
    subtitle: "查看和管理数据源中的表",

    // 特有操作
    addTable: "添加表",
    syncColumns: "同步列",
    columnSettings: "列配置",
    configInfo: "编辑",

    // 字段
    displayName: "显示名称",

    // 表单相关
    selectSchema: "请选择 Schema",
    availableTables: "可选表",
    selectedTables: "已选表",
    noSelected: "暂无选中表",
    alreadyAdded: "已添加",
    selectedCount: "已选择 {count} 个表",
    selectAtLeastOne: "请至少选择一个表",
    addSelected: "添加选中表",

    // 特有消息
    addSuccess: "添加成功",
    addFailed: "添加失败",
    syncSuccess: "同步成功",
    syncFailed: "同步失败",
    removeConfirm: "确定要移除表「{name}」吗？移除后相关列信息也会被删除。",
    removeSuccess: "移除成功",
    removeFailed: "移除失败",
  },

  column: {
    // 列名
    columnName: "列名",
    description: "描述",
    type: "类型",
    displayName: "显示名称",

    // 搜索
    searchPlaceholder: "搜索列名或描述",

    // 操作
    editTitle: "编辑列信息",

    // 表单
    enterDescription: "请输入列描述",
  },
};
