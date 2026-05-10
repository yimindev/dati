<script setup lang="ts">
import { ref } from "vue";
import { useI18n } from "vue-i18n";
import { User, UserFilled, Lock } from "@element-plus/icons-vue";
import { ElMessage } from "element-plus";
import { useAuthStore } from "~/stores/auth";

const { t } = useI18n();
const authStore = useAuthStore();

const form = ref({
  name: "",
  password: "",
  confirmPassword: "",
  displayName: "",
});

async function handleRegister() {
  if (!form.value.name || !form.value.password) {
    ElMessage.warning(t("auth.inputUsernamePassword"));
    return;
  }
  if (form.value.password !== form.value.confirmPassword) {
    ElMessage.warning(t("auth.passwordMismatch"));
    return;
  }
  try {
    await authStore.register(
      form.value.name,
      form.value.password,
      form.value.displayName || undefined
    );
    ElMessage.success(t("auth.registerSuccess"));
  } catch (err: any) {
    ElMessage.error(err.message || t("auth.registerFailed"));
  }
}
</script>

<template>
  <div class="min-h-screen flex items-center justify-center bg-[var(--ep-fill-color-light)]">
    <el-card class="w-96">
      <template #header>
        <div class="text-center text-xl font-bold">{{ t("auth.registerTitle") }}</div>
      </template>

      <el-form :model="form" label-position="top" @submit.prevent="handleRegister">
        <el-form-item :label="t('common.username')">
          <el-input
            v-model="form.name"
            :placeholder="t('common.placeholder.name')"
            :prefix-icon="User"
          />
        </el-form-item>

        <el-form-item :label="t('auth.displayName')">
          <el-input
            v-model="form.displayName"
            :placeholder="t('auth.displayNamePlaceholder')"
            :prefix-icon="UserFilled"
          />
        </el-form-item>

        <el-form-item :label="t('common.password')">
          <el-input
            v-model="form.password"
            type="password"
            :placeholder="t('auth.passwordPlaceholder')"
            :prefix-icon="Lock"
            show-password
          />
        </el-form-item>

        <el-form-item :label="t('auth.confirmPassword')">
          <el-input
            v-model="form.confirmPassword"
            type="password"
            :placeholder="t('auth.confirmPasswordPlaceholder')"
            :prefix-icon="Lock"
            show-password
          />
        </el-form-item>

        <el-form-item>
          <el-button
            type="primary"
            class="w-full"
            :loading="authStore.loading"
            @click="handleRegister"
          >
            {{ t("auth.register") }}
          </el-button>
        </el-form-item>
      </el-form>

      <div class="text-center text-sm">
        <span class="text-[var(--ep-text-color-regular)]">{{ t("auth.hasAccount") }}</span>
        <RouterLink to="/login" class="text-[var(--ep-color-primary)] hover:underline">
          {{ t("auth.toLogin") }}
        </RouterLink>
      </div>
    </el-card>
  </div>
</template>
