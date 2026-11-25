<template>
  <div class="page">
    <el-card shadow="never" class="panel">
      <template #header>
        <div class="header-row">
          <span>学院管理</span>
          <el-button type="primary" @click="open()">新增学院</el-button>
        </div>
      </template>
      <div class="toolbar">
        <el-input v-model="query.keyword" placeholder="编码 / 名称" clearable style="width: 220px" @keyup.enter="search" />
        <el-button type="primary" plain @click="search">查询</el-button>
      </div>
      <el-table :data="rows" class="admin-table">
        <el-table-column prop="code" label="编码" width="120" />
        <el-table-column prop="name" label="名称" min-width="180" />
        <el-table-column label="操作" width="180" align="center">
          <template #default="{ row }">
            <div class="table-ops">
              <el-button link type="primary" @click="openDetail(row)">详情</el-button>
              <el-button link type="primary" @click="open(row)">编辑</el-button>
              <el-button link type="danger" @click="remove(row)">删除</el-button>
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
            @current-change="load"
            @size-change="load"
        />
      </div>
    </el-card>

    <DetailDialog v-model="detailVisible" title="学院详情" :fields="detailFields" />

    <el-dialog v-model="visible" :title="form.id ? '编辑学院' : '新增学院'" width="420px">
      <el-form label-width="72px">
        <el-form-item label="编码"><el-input v-model="form.code" /></el-form-item>
        <el-form-item label="名称"><el-input v-model="form.name" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="visible = false">取消</el-button>
        <el-button type="primary" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/api/request'
import DetailDialog from '@/components/DetailDialog.vue'

const rows = ref([])
const total = ref(0)
const visible = ref(false)
const detailVisible = ref(false)
const detailRow = ref(null)
const query = reactive({ keyword: '', page: 1, size: 10 })
const form = reactive({ id: null, code: '', name: '' })

const detailFields = computed(() => {
  const row = detailRow.value || {}
  return [
    { label: '编码', value: row.code },
    { label: '名称', value: row.name }
  ]
})

async function load() {
  const data = await request.get('/admin/edu/colleges', {
    params: { keyword: query.keyword, page: query.page, size: query.size }
  })
  rows.value = data?.list || []
  total.value = data?.total || 0
}

  function search() {
    query.page = 1
    load()
  }

function openDetail(row) {
  detailRow.value = row
  detailVisible.value = true
}

function open(row) {
  form.id = row?.id || null
  form.code = row?.code || ''
  form.name = row?.name || ''
  visible.value = true
}

async function save() {
  await request.post('/admin/edu/colleges', {
    id: form.id,
    code: form.code,
    name: form.name
  })
  ElMessage.success('已保存')
  visible.value = false
  load()
}

async function remove(row) {
  await ElMessageBox.confirm(`删除学院「${row.name}」？`, '提示', { type: 'warning' })
  await request.delete(`/admin/edu/colleges/${row.id}`)
  ElMessage.success('删除成功')
  load()
}

onMounted(load)
</script>