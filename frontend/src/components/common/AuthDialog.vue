<script setup lang="ts">
import { ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { useI18n } from 'vue-i18n'
import { useDebounceFn } from '@vueuse/core'
import { UserFilled, View, EditPen, Delete } from '@element-plus/icons-vue'
import { grantAcl, listAcl, revokeAcl } from '~/api/permission'
import { searchUsers } from '~/api/user'
import type { AclEntry, UserBrief } from '~/api/types'

const { t } = useI18n()
const props = defineProps<{ resourceType: string; resourceId: string; visible: boolean }>()
const emit = defineEmits<{ (e: 'update:visible', v: boolean): void }>()

const loading = ref(false)
const granting = ref(false)
const togglingPublic = ref(false)
const revokingId = ref<string | null>(null)
const entries = ref<AclEntry[]>([])
const searching = ref(false)
const userOptions = ref<UserBrief[]>([])
const selectedUser = ref<UserBrief | null>(null)
const newPermission = ref<'VIEW' | 'EDIT'>('VIEW')
const isPublic = ref(false)

async function load() {
  if (!props.resourceId) return
  loading.value = true
  try {
    const allEntries = await listAcl(props.resourceType, props.resourceId)
    // 判断是否有 GROUP: ALL_USERS 的只读授权
    isPublic.value = allEntries.some(
      (e) => e.principal_type === 'GROUP' && e.principal_id === 'ALL_USERS'
    )
    // 列表仅展示针对用户个体的授权 (USER)
    entries.value = allEntries.filter((e) => e.principal_type === 'USER')
  } finally {
    loading.value = false
  }
}

function resetForm() {
  selectedUser.value = null
  userOptions.value = []
  newPermission.value = 'VIEW'
}

watch(
  () => props.visible,
  (val) => {
    if (val) {
      load()
      resetForm()
    }
  }
)

const remoteSearch = useDebounceFn(async (keyword: string) => {
  if (!keyword.trim()) {
    userOptions.value = []
    return
  }
  searching.value = true
  try {
    userOptions.value = await searchUsers(keyword.trim())
  } finally {
    searching.value = false
  }
}, 300)

async function handlePublicToggle(val: boolean | string | number) {
  const active = Boolean(val)
  // 开启公开影响所有登录用户，需确认；取消则还原开关
  if (active) {
    try {
      await ElMessageBox.confirm(
        t('permission.confirmPublic'),
        t('common.confirm'),
        {
          confirmButtonText: t('common.confirm'),
          cancelButtonText: t('common.cancel'),
          type: 'warning',
        }
      )
    } catch {
      isPublic.value = false
      return
    }
  }
  togglingPublic.value = true
  try {
    if (active) {
      await grantAcl(props.resourceType, props.resourceId, {
        principal_type: 'GROUP',
        principal_id: 'ALL_USERS',
        permission: 'VIEW',
      })
    } else {
      await revokeAcl(props.resourceType, props.resourceId, 'ALL_USERS', 'GROUP')
    }
    ElMessage.success(t(active ? 'permission.publicOn' : 'permission.revokeSuccess'))
    await load()
  } catch {
    // 失败还原
    isPublic.value = !active
  } finally {
    togglingPublic.value = false
  }
}

async function add() {
  if (!selectedUser.value || granting.value) return
  granting.value = true
  try {
    await grantAcl(props.resourceType, props.resourceId, {
      principal_type: 'USER',
      principal_id: selectedUser.value.id,
      permission: newPermission.value,
    })
    ElMessage.success(t('permission.grantSuccess'))
    resetForm()
    await load()
  } catch {
    // 403/400 已由 http.ts 统一提示
  } finally {
    granting.value = false
  }
}

async function revoke(entry: AclEntry) {
  revokingId.value = entry.principal_id
  try {
    await revokeAcl(props.resourceType, props.resourceId, entry.principal_id, entry.principal_type)
    ElMessage.success(t('permission.revokeSuccess'))
    await load()
  } catch {
    // 403 已由 http.ts 统一提示
  } finally {
    revokingId.value = null
  }
}

function getAvatarChar(name?: string): string {
  if (!name) return 'U'
  return name.trim().charAt(0).toUpperCase()
}
</script>

<template>
  <el-dialog
    :model-value="props.visible"
    :title="t('permission.title')"
    width="500px"
    destroy-on-close
    class="auth-dialog"
    @update:model-value="emit('update:visible', $event)"
  >
    <div v-loading="loading" class="flex flex-col gap-4">
      <!-- 1. 添加成员（主操作，置顶） -->
      <div class="flex gap-2 items-center">
        <el-select
          v-model="selectedUser"
          :placeholder="t('permission.principalPlaceholder')"
          filterable
          remote
          reserve-keyword
          :remote-method="remoteSearch"
          :loading="searching"
          value-key="id"
          class="flex-1! min-w-0!"
          @keyup.enter="add"
        >
          <template #prefix>
            <el-icon><UserFilled /></el-icon>
          </template>
          <el-option
            v-for="u in userOptions"
            :key="u.id"
            :label="u.display_name ? `${u.name} (${u.display_name})` : u.name"
            :value="u"
          />
        </el-select>

        <el-select v-model="newPermission" class="w-28!">
          <el-option :label="t('permission.roleViewer')" value="VIEW" />
          <el-option :label="t('permission.roleEditor')" value="EDIT" />
        </el-select>

        <el-button
          type="primary"
          :disabled="!selectedUser"
          :loading="granting"
          @click="add"
        >
          {{ t('permission.grant') }}
        </el-button>
      </div>

      <!-- 2. 成员统计 + 公开控制（次要操作收敛到 header） -->
      <div class="flex items-center justify-between pt-1 pb-1 border-b border-[var(--ep-border-color-lighter)]">
        <div class="flex items-center gap-2">
          <span class="text-xs font-semibold text-[var(--ep-text-color-regular)] tracking-wide uppercase">
            {{ t('permission.authorizedUsers') }} ({{ entries.length }})
          </span>
          <el-tag v-if="isPublic" size="small" type="warning" round class="font-normal">
            {{ t('permission.publicTag') }}
          </el-tag>
        </div>

        <div class="flex items-center gap-1.5">
          <span class="text-xs text-[var(--ep-text-color-secondary)]">{{ t('permission.publicLabel') }}</span>
          <el-tooltip :content="t('permission.publicHint')" placement="top">
            <span class="cursor-pointer text-xs text-[var(--ep-text-color-placeholder)]">ⓘ</span>
          </el-tooltip>
          <el-switch
            size="small"
            v-model="isPublic"
            :loading="togglingPublic"
            @change="handlePublicToggle"
          />
        </div>
      </div>

      <!-- 3. 成员授权列表（紧凑分隔行） -->
      <div class="min-h-[180px] max-h-[300px] overflow-y-auto pr-1 custom-scrollbar -mt-1">
        <div v-if="!loading && entries.length === 0" class="h-[180px] flex items-center justify-center">
          <el-empty :description="t('permission.empty')" :image-size="60" class="py-0!" />
        </div>

        <div
          v-for="entry in entries"
          :key="entry.id"
          class="flex items-center justify-between py-2.5 border-b border-[var(--ep-border-color-extra-light)] last:border-b-0"
        >
          <div class="flex items-center gap-3 min-w-0">
            <el-avatar :size="28" class="flex-shrink-0 bg-[var(--ep-color-primary-light-9)] text-[var(--ep-color-primary)] font-semibold text-xs">
              {{ getAvatarChar(entry.principal_name || entry.principal_id) }}
            </el-avatar>

            <span class="text-sm text-[var(--ep-text-color-primary)] truncate">
              {{ entry.principal_name || entry.principal_id }}
            </span>

            <el-tag
              size="small"
              round
              :type="entry.permission === 'EDIT' ? 'primary' : 'success'"
              class="font-normal flex-shrink-0"
            >
              <template #icon>
                <el-icon class="mr-0.5"><EditPen v-if="entry.permission === 'EDIT'" /><View v-else /></el-icon>
              </template>
              {{ entry.permission === 'EDIT' ? t('permission.roleEditor') : t('permission.roleViewer') }}
            </el-tag>
          </div>

          <el-button
            type="danger"
            text
            size="small"
            class="!px-2.5 !py-1 text-xs font-medium rounded-md hover:!bg-[var(--ep-color-danger-light-9)] transition-colors cursor-pointer"
            :loading="revokingId === entry.principal_id"
            @click="revoke(entry)"
          >
            <template #icon>
              <el-icon><Delete /></el-icon>
            </template>
            {{ t('permission.revoke') }}
          </el-button>
        </div>
      </div>
    </div>
  </el-dialog>
</template>
