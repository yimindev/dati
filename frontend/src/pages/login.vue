<script setup lang="ts">
import { ref } from "vue";
import { useI18n } from "vue-i18n";
import { Lock, User } from "@element-plus/icons-vue";
import { ElMessage } from "element-plus";
import { notifyError } from "~/api/http";
import { useAuthStore } from "~/stores/auth";

const { t } = useI18n();
const authStore = useAuthStore();

const form = ref({
  name: "",
  password: "",
});

async function handleLogin() {
  if (!form.value.name || !form.value.password) {
    ElMessage.warning(t("auth.inputUsernamePassword"));
    return;
  }
  try {
    await authStore.login(form.value.name, form.value.password);
  } catch (err: any) {
    notifyError(err, t("auth.loginFailed"));
  }
}

async function handleGuestLogin() {
  try {
    await authStore.loginAsGuest();
  } catch (err: any) {
    notifyError(err, t("auth.guestLoginFailed"));
  }
}
</script>

<template>
  <div
    class="min-h-screen flex items-center justify-center bg-[var(--ep-fill-color-light)]"
  >
    <el-card class="w-96">
      <template #header>
        <div class="text-center text-xl font-bold">
          {{ t("auth.loginTitle") }}
        </div>
      </template>

      <el-form :model="form" label-position="top" @submit.prevent="handleLogin">
        <el-form-item :label="t('common.username')">
          <el-input
            v-model="form.name"
            :placeholder="t('common.placeholder.name')"
            :prefix-icon="User"
          />
        </el-form-item>

        <el-form-item :label="t('common.password')">
          <el-input
            v-model="form.password"
            type="password"
            :placeholder="t('common.placeholder.password')"
            :prefix-icon="Lock"
            show-password
          />
        </el-form-item>

        <el-form-item class="!mb-4">
          <el-button
            type="primary"
            native-type="submit"
            class="w-full"
            :loading="authStore.loading"
          >
            {{ t("auth.login") }}
          </el-button>
        </el-form-item>
      </el-form>

      <div class="text-center text-sm">
        <span class="text-[var(--ep-text-color-regular)]">{{
          t("auth.noAccount")
        }}</span>
        {{ " " }}
        <RouterLink
          to="/register"
          class="text-[var(--ep-color-primary)] hover:underline"
        >
          {{ t("auth.toRegister") }}
        </RouterLink>
      </div>

      <template #footer>
        <div class="flex items-center justify-between py-0.5">
          <span class="text-xs text-[var(--ep-text-color-secondary)]">
            {{ t("auth.guestFooterHint") }}
          </span>
          <el-button
            type="primary"
            link
            class="!p-0 !text-xs font-medium"
            :loading="authStore.loading"
            @click="handleGuestLogin"
          >
            {{ t("auth.guestLoginAction") }}
          </el-button>
        </div>
      </template>
    </el-card>
  </div>
</template>
