<template>
  <div class="page">
    <el-card shadow="never" class="panel">
      <template #header>
        <div class="header-row">
          <span>系统日志</span>
          <span class="header-tip">登录、注册与后台操作记录</span>
        </div>
      </template>

      <div class="toolbar">
        <el-input
          v-model="query.keyword"
          placeholder="搜索用户名/模块"
          clearable
          style="width: 220px"
          @keyup.enter="loadData"
        />
        <el-select v-model="query.type" placeholder="全部类型" clearable style="width: 140px">
          <el-option label="登录" value="LOGIN" />
          <el-option label="注册" value="REGISTER" />
          <el-option label="操作" value="OPERATION" />
          <el-option label="错误" value="ERROR" />
        </el-select>
        <el-button type="primary" plain @click="loadData">查询</el-button>
      </div>

      <el-table v-loading="loading" :data="tableData" class="admin-table">
        <el-table-column prop="username" label="用户" width="120" />
        <el-table-column prop="type" label="类型" width="100">
          <template #default="{ row }">
            <el-tag :type="typeTag(row.type)" effect="light">{{ typeLabel(row.type) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="module" label="模块" width="120" />
        <el-table-column prop="action" label="操作" min-width="140" />
        <el-table-column prop="ip" label="IP" width="140" />
        <el-table-column prop="detail" label="详情" min-width="200" show-overflow-tooltip />
        <el-table-column prop="createdAt" label="时间" min-width="170" />
        <el-table-column label="操作" width="140" fixed="right" align="center">
          <template #default="{ row }">
            <div class="table-ops">
              <el-button link type="primary" @click="openDetail(row)">详情</el-button>
              <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <div class="pager">
        <el-pagination
          v-model:current-page="query.page"
          v-model:page-size="query.size"
          :total="total"
          layout="total, prev, pager, next"
          @current-change="loadData"
          @size-change="loadData"
        />
      </div>
    </el-card>

    <DetailDialog v-model="detailVisible" title="日志详情" :fields="detailFields" />
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { deleteLog, getLogPage } from '@/api/log'
import DetailDialog from '@/components/DetailDialog.vue'

const loading = ref(false)
const tableData = ref([])
const total = ref(0)
const detailVisible = ref(false)
const detailRow = ref(null)

const query = reactive({ keyword: '', type: '', page: 1, size: 10 })

const detailFields = computed(() => {
  const row = detailRow.value || {}
  return [
    { label: '用户', value: row.username },
    { label: '类型', type: 'tag', value: typeLabel(row.type), tagType: typeTag(row.type) },
    { label: '模块', value: row.module },
    { label: '操作', value: row.action },
    { label: 'IP', value: row.ip },
    { label: '详情', value: row.detail },
    { label: '时间', value: row.createdAt }
  ]
})

function typeLabel(type) {
  return { LOGIN: '登录', REGISTER: '注册', OPERATION: '操作', ERROR: '错误' }[type] || type || '-'
}

function typeTag(type) {
  return { LOGIN: 'success', REGISTER: 'warning', OPERATION: 'primary', ERROR: 'danger' }[type] || 'info'
}

async function loadData() {
  loading.value = true
  try {
    const params = {
      keyword: query.keyword,
      type: query.type,
      page: query.page,
      size: query.size
    }
    if (!params.type) delete params.type
    const data = await getLogPage(params)
    tableData.value = data.list || []
    total.value = data.total || 0
  } finally {
    loading.value = false
  }
}

function openDetail(row) {
  detailRow.value = row
  detailVisible.value = true
}

async function handleDelete(row) {
  await ElMessageBox.confirm('确定删除该日志记录吗？', '提示', { type: 'warning' })
  await deleteLog(row.id)
  ElMessage.success('删除成功')
  loadData()
}

onMounted(loadData)
</script>

<style scoped>
.header-tip {
  font-size: 13px;
  font-weight: 400;
  color: var(--el-text-color-secondary);
}
</style>
