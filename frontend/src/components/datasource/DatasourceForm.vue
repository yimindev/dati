<script setup lang="ts">
import { ref, computed, onMounted } from "vue";
import { useI18n } from "vue-i18n";
import type { FormInstance, FormRules } from "element-plus";
import type { DataSourcePayload } from "~/api/datasource";
import { useSystemStore } from "~/stores/system";

// i18n
const { t } = useI18n();

// Store
const systemStore = useSystemStore();

// Props & Emits
interface Props {
  modelValue: DataSourcePayload;
}

interface Emits {
  (e: "update:modelValue", value: DataSourcePayload): void;
  (e: "test-connection"): void;
}

const props = defineProps<Props>();
defineEmits<Emits>();

// 表单引用
const formRef = ref<FormInstance>();

// 支持的数据库类型列表
const supportedTypes = computed(() => systemStore.supportedDatabaseTypes);

// 当前选中的数据库类型元数据及动态 placeholder
const currentTypeMeta = computed(() =>
  supportedTypes.value.find((item) => item.type === props.modelValue.type)
);

const jdbcUrlPlaceholder = computed(() =>
  currentTypeMeta.value?.jdbc_url_template || t("datasource.jdbcUrl")
);

onMounted(() => {
  systemStore.loadConfig();
});

// 表单验证规则
const rules: FormRules = {
  name: [
    { required: true, message: t("common.required", { name: t("datasource.connectionName") }), trigger: "blur" },
    { min: 1, max: 100, message: t("common.nameLengthError"), trigger: "blur" },
  ],
  type: [{ required: true, message: t("common.selectRequired", { name: t("datasource.databaseType") }), trigger: "change" }],
  jdbc_url: [
    { required: true, message: t("common.required", { name: "JDBC URL" }), trigger: "blur" },
  ],
  username: [{ required: true, message: t("common.required", { name: t("common.username") }), trigger: "blur" }],
  password: [{ required: true, message: t("common.required", { name: t("common.password") }), trigger: "blur" }],
};

// 暴露验证方法
const validate = async () => {
  if (!formRef.value) return false;
  try {
    await formRef.value.validate();
    return true;
  } catch {
    return false;
  }
};

const resetValidation = () => {
  formRef.value?.clearValidate();
};

// 暴露给父组件
defineExpose({
  validate,
  resetValidation,
});
</script>

<template>
  <el-form
    ref="formRef"
    :model="modelValue"
    :rules="rules"
    label-width="120px"
    @submit.prevent
  >
    <el-form-item :label="t('datasource.connectionName')" prop="name">
      <el-input
        v-model="modelValue.name"
        :placeholder="t('common.placeholder.name')"
        maxlength="100"
      />
    </el-form-item>

    <el-form-item :label="t('datasource.databaseType')" prop="type">
      <el-select
        v-model="modelValue.type"
        :placeholder="t('common.placeholder.type')"
        style="width: 100%"
      >
        <el-option
          v-for="item in supportedTypes"
          :key="item.type"
          :label="item.label"
          :value="item.type"
        />
      </el-select>
    </el-form-item>

    <el-form-item :label="t('datasource.jdbcUrl')" prop="jdbc_url">
      <el-input
        v-model="modelValue.jdbc_url"
        :placeholder="jdbcUrlPlaceholder"
        :rows="2"
        type="textarea"
      />
    </el-form-item>

    <el-form-item :label="t('common.username')" prop="username">
      <el-input
        v-model="modelValue.username"
        :placeholder="t('common.username')"
      />
    </el-form-item>

    <el-form-item :label="t('common.password')" prop="password">
      <el-input
        v-model="modelValue.password"
        type="password"
        :placeholder="t('common.password')"
        show-password
      />
    </el-form-item>

    <el-form-item :label="t('common.description')">
      <el-input
        v-model="modelValue.description"
        :placeholder="t('common.placeholder.description')"
        :rows="3"
        type="textarea"
        maxlength="500"
      />
    </el-form-item>
  </el-form>
</template>

