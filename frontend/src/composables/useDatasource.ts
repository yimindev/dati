import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import type { DatasourceVO } from '~/api/datasource'
import { listDataSources } from '~/api/datasource'

export function useDatasource() {
  const loading = ref(false)
  const datasourceList = ref<DatasourceVO[]>([])

  // 加载数据源列表
  const loadDatasources = async () => {
    try {
      loading.value = true
      const response = await listDataSources()
      datasourceList.value = response.data || []
    } catch (error) {
      console.error('加载数据源失败:', error)
      ElMessage.error('加载数据源失败')
    } finally {
      loading.value = false
    }
  }

  return {
    loading,
    datasourceList,
    loadDatasources
  }
}
