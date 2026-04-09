<template>
  <div class="instance-management">
    <h2>实例管理</h2>
    
    <!-- 搜索区域 -->
    <el-card style="margin-top: 20px;">
      <el-form :inline="true" :model="queryForm">
        <el-form-item label="创建者ID">
          <el-input v-model="queryForm.creatorId" placeholder="请输入创建者ID" clearable />
        </el-form-item>
        <el-form-item label="实例ID">
          <el-input v-model="queryForm.instanceId" placeholder="请输入实例ID" clearable />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleQuery">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 表格区域 -->
    <el-card style="margin-top: 20px;">
      <el-table :data="tableData" border v-loading="loading">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="creatorId" label="创建者ID" width="150" />
        <el-table-column prop="instanceId" label="实例ID" width="180" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'">
              {{ row.status === 1 ? '已分配' : '未分配' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="lastHeartbeat" label="最后心跳时间" width="180">
          <template #default="{ row }">
            {{ formatTime(row.lastHeartbeat) }}
          </template>
        </el-table-column>
        <el-table-column prop="gmtCreate" label="创建时间" width="180">
          <template #default="{ row }">
            {{ formatTime(row.gmtCreate) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" fixed="right" width="180">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="handleEdit(row)">编辑</el-button>
            <el-button link type="danger" size="small" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <el-pagination
        v-model:current-page="queryForm.pageNum"
        v-model:page-size="queryForm.pageSize"
        :total="total"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="handleQuery"
        @current-change="handleQuery"
        style="margin-top: 20px; justify-content: flex-end;"
      />
    </el-card>

    <!-- 编辑对话框 -->
    <el-dialog v-model="editDialogVisible" title="编辑实例" width="500px">
      <el-form :model="editForm" :rules="editRules" ref="editFormRef" label-width="100px">
        <el-form-item label="实例ID" prop="instanceId">
          <el-input v-model="editForm.instanceId" placeholder="格式: ip:port" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmitEdit" :loading="submitLoading">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { queryInstances, updateInstance, deleteInstance } from '@/api'
import { ElMessage, ElMessageBox } from 'element-plus'

const loading = ref(false)
const submitLoading = ref(false)
const tableData = ref([])
const total = ref(0)
const editDialogVisible = ref(false)
const editFormRef = ref(null)

const queryForm = reactive({
  pageNum: 1,
  pageSize: 10,
  creatorId: '',
  instanceId: ''
})

const editForm = reactive({
  id: null,
  instanceId: ''
})

const editRules = {
  instanceId: [
    { required: true, message: '请输入实例ID', trigger: 'blur' },
    { pattern: /^\d+\.\d+\.\d+\.\d+:\d+$/, message: '格式必须为ip:port', trigger: 'blur' }
  ]
}

// 查询列表
const handleQuery = async () => {
  loading.value = true
  try {
    const result = await queryInstances(queryForm)
    tableData.value = result.list
    total.value = result.total
  } catch (error) {
    ElMessage.error('查询失败')
  } finally {
    loading.value = false
  }
}

// 重置
const handleReset = () => {
  queryForm.creatorId = ''
  queryForm.instanceId = ''
  queryForm.pageNum = 1
  handleQuery()
}

// 编辑
const handleEdit = (row) => {
  editForm.id = row.id
  editForm.instanceId = row.instanceId
  editDialogVisible.value = true
}

// 提交编辑
const handleSubmitEdit = async () => {
  if (!editFormRef.value) return
  
  await editFormRef.value.validate(async (valid) => {
    if (!valid) return
    
    submitLoading.value = true
    try {
      await updateInstance(editForm)
      ElMessage.success('更新成功')
      editDialogVisible.value = false
      handleQuery()
    } catch (error) {
      ElMessage.error(error.message || '更新失败')
    } finally {
      submitLoading.value = false
    }
  })
}

// 删除
const handleDelete = (row) => {
  ElMessageBox.confirm(
    `确定要删除实例 "${row.instanceId}" 吗？此操作不可恢复！`,
    '删除确认',
    {
      confirmButtonText: '确定删除',
      cancelButtonText: '取消',
      type: 'warning',
      distinguishCancelAndClose: true,
      beforeClose: (action, instance, done) => {
        if (action === 'confirm') {
          instance.confirmButtonLoading = true
          instance.confirmButtonText = '删除中...'
          deleteInstance(row.id)
            .then(() => {
              ElMessage.success('删除成功')
              handleQuery()
              done()
            })
            .catch((error) => {
              ElMessage.error(error.message || '删除失败')
              instance.confirmButtonLoading = false
              instance.confirmButtonText = '确定删除'
            })
        } else {
          done()
        }
      }
    }
  ).catch(() => {})
}

// 格式化时间
const formatTime = (time) => {
  if (!time) return '-'
  return new Date(time).toLocaleString('zh-CN')
}

onMounted(() => {
  handleQuery()
})
</script>

<style scoped>
.instance-management {
  padding: 20px;
}
</style>
