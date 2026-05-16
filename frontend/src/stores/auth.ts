import { defineStore } from "pinia";
import { ref, computed } from "vue";
import { login as apiLogin, register as apiRegister, me } from "~/api/auth";
import type { User } from "~/api/types";
import { useRouter } from "vue-router";

export const useAuthStore = defineStore("auth", () => {
  const router = useRouter();

  const token = ref<string | null>(localStorage.getItem("access_token"));
  const user = ref<User | null>(null);
  const loading = ref(false);

  const isLoggedIn = computed(() => !!token.value);

  async function login(name: string, password: string) {
    loading.value = true;
    try {
      const resp = await apiLogin("local", name, password);
      token.value = resp.token;
      localStorage.setItem("access_token", resp.token);
      await fetchUser();
      router.push("/");
    } finally {
      loading.value = false;
    }
  }

  async function register(
    name: string,
    password: string,
    displayName?: string
  ) {
    loading.value = true;
    try {
      await apiRegister({
        name,
        password,
        display_name: displayName,
      });
      router.push("/login");
    } finally {
      loading.value = false;
    }
  }

  async function fetchUser() {
    if (!token.value) return;
    try {
      user.value = await me();
    } catch {
      logout();
    }
  }

  function logout() {
    token.value = null;
    user.value = null;
    localStorage.removeItem("access_token");
    router.push("/login");
  }

  return {
    token,
    user,
    loading,
    isLoggedIn,
    login,
    register,
    fetchUser,
    logout,
  };
});
