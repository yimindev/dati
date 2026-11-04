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
  isEdit?: boolean;
}

const props = withDefaults(defineProps<Props>(), { isEdit: false });

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

const passwordPlaceholder = computed(() =>
  props.isEdit ? t("datasource.passwordKeepHint") : t("common.password")
);

onMounted(() => {
  systemStore.loadConfig();
});

// 表单验证规则：长度上限与后端 CreateDataSourceRequest 保持一致
const rules: FormRules = {
  name: [
    { required: true, message: t("common.required", { name: t("datasource.connectionName") }), trigger: "blur" },
    { min: 1, max: 64, message: t("common.nameLengthError"), trigger: "blur" },
  ],
  type: [{ required: true, message: t("common.selectRequired", { name: t("datasource.databaseType") }), trigger: "change" }],
  jdbc_url: [
    { required: true, message: t("common.required", { name: "JDBC URL" }), trigger: "blur" },
    { max: 256, message: t("common.lengthExceeded", { max: 256 }), trigger: "blur" },
  ],
  username: [
    { required: true, message: t("common.required", { name: t("common.username") }), trigger: "blur" },
    { max: 64, message: t("common.lengthExceeded", { max: 64 }), trigger: "blur" },
  ],
  description: [{ max: 1024, message: t("common.lengthExceeded", { max: 1024 }), trigger: "blur" }],
};

// 连接测试只依赖连接字段，连接名称/描述与连接可用性无关
const CONNECTION_FIELDS = ["type", "jdbc_url", "username", "password"];

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

const validateConnectionFields = async () => {
  if (!formRef.value) return false;
  try {
    await formRef.value.validateField(CONNECTION_FIELDS);
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
  validateConnectionFields,
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
        maxlength="64"
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
        maxlength="256"
      />
    </el-form-item>

    <el-form-item :label="t('common.username')" prop="username">
      <el-input
        v-model="modelValue.username"
        :placeholder="t('common.username')"
        maxlength="64"
      />
    </el-form-item>

    <el-form-item :label="t('common.password')" prop="password">
      <div class="w-full">
        <el-input
          v-model="modelValue.password"
          type="password"
          :placeholder="passwordPlaceholder"
          maxlength="512"
          autocomplete="new-password"
          show-password
        />
        <div
          v-if="isEdit"
          class="mt-1 text-xs leading-4 text-[var(--ep-text-color-placeholder)]"
        >
          {{ t('datasource.passwordEditHint') }}
        </div>
      </div>
    </el-form-item>

    <el-form-item :label="t('common.description')" prop="description">
      <el-input
        v-model="modelValue.description"
        :placeholder="t('common.placeholder.description')"
        :rows="3"
        type="textarea"
        maxlength="1024"
      />
    </el-form-item>
  </el-form>
</template>
