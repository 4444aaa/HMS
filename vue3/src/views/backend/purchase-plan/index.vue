<template>
  <div class="purchase-plan">
    <el-card class="box-card">
      <template #header>
        <div class="card-header">
          <h3>采购计划</h3>
          <el-button
            type="primary"
            @click="openCreate"
          >
            新增采购计划
          </el-button>
        </div>
      </template>

      <el-form
        :model="searchForm"
        :inline="true"
        class="search-form"
      >
        <el-form-item label="计划编号">
          <el-input
            v-model="searchForm.planNo"
            placeholder="请输入计划编号"
            clearable
          />
        </el-form-item>
        <el-form-item label="主题">
          <el-input
            v-model="searchForm.title"
            placeholder="请输入主题"
            clearable
          />
        </el-form-item>
        <el-form-item label="状态">
          <el-select
            v-model="searchForm.status"
            placeholder="请选择状态"
            clearable
            style="width: 140px"
          >
            <el-option
              label="草稿"
              :value="0"
            />
            <el-option
              label="已提交"
              :value="1"
            />
            <el-option
              label="已完结"
              :value="2"
            />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button
            type="primary"
            @click="handleSearch"
          >
            查询
          </el-button>
          <el-button @click="resetSearch">
            重置
          </el-button>
        </el-form-item>
      </el-form>

      <el-table
        v-loading="loading"
        :data="tableData"
        border
        style="width: 100%"
      >
        <el-table-column
          prop="planNo"
          label="计划编号"
          width="170"
        />
        <el-table-column
          prop="title"
          label="主题"
          min-width="130"
        />
        <el-table-column
          prop="status"
          label="状态"
          width="110"
        >
          <template #default="scope">
            <el-tag :type="getStatusTagType(scope.row.status)">
              {{ getStatusText(scope.row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column
          prop="createTime"
          label="创建时间"
          width="180"
        >
          <template #default="scope">
            {{ formatDate(scope.row.createTime) }}
          </template>
        </el-table-column>
        <el-table-column
          label="操作"
          width="280"
          fixed="right"
        >
          <template #default="scope">
            <el-button
              size="small"
              type="primary"
              @click="openDetail(scope.row)"
            >
              详情
            </el-button>
            <el-button
              v-if="scope.row.status === 0"
              size="small"
              type="warning"
              @click="openEdit(scope.row)"
            >
              编辑
            </el-button>
            <el-button
              v-if="scope.row.status === 0"
              size="small"
              type="success"
              @click="submitPlan(scope.row)"
            >
              提交
            </el-button>
            <el-button
              v-if="scope.row.status === 0"
              size="small"
              type="danger"
              @click="deletePlan(scope.row)"
            >
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-container">
        <el-pagination
          :current-page="currentPage"
          :page-size="pageSize"
          :page-sizes="[10, 20, 50, 100]"
          background
          layout="total, sizes, prev, pager, next, jumper"
          :total="total"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
    </el-card>

    <!-- 创建计划 -->
    <el-dialog
      v-model="createVisible"
      :title="isEdit ? '编辑采购计划' : '新增采购计划'"
      width="900px"
      @closed="resetCreate"
    >
      <div class="create-doc-layout">
        <div
          v-if="!isEdit"
          class="items-header create-basic-header-row"
        >
          <div class="items-title">
            基本信息
          </div>
        </div>
        <div
          v-if="!isEdit"
          class="create-meta-strip"
        >
          <div class="create-meta-line">
            <span class="create-meta-item">计划单号：{{ draftPreviewNo }}</span>
            <span class="create-meta-item">计划时间：{{ draftCreateTime }}</span>
            <span class="create-meta-item">创建人姓名：{{ currentUserDisplayName }}</span>
          </div>
        </div>
        <el-form
          :model="createForm"
          label-position="top"
          class="create-main-form create-main-form--top"
        >
          <el-form-item label="主题">
            <el-input
              v-model="createForm.title"
              placeholder="请输入主题"
            />
          </el-form-item>
          <el-form-item label="备注">
            <el-input
              v-model="createForm.remark"
              type="textarea"
              :rows="2"
              placeholder="可选"
            />
          </el-form-item>
        </el-form>
      </div>

      <div class="items-header">
        <div class="items-title">
          计划明细
        </div>
        <el-button
          type="primary"
          plain
          @click="addItem"
        >
          添加明细
        </el-button>
      </div>

      <div class="create-dialog-table-wrap">
        <el-table
          :data="createForm.items"
          border
          style="width: 100%"
        >
        <el-table-column
          label="药品"
          min-width="240"
        >
          <template #default="scope">
            <el-select
              v-model="scope.row.medicineId"
              placeholder="请输入药品名/编码检索"
              filterable
              remote
              :remote-method="filterMedicine"
              style="width: 100%"
            >
              <el-option
                v-for="m in filteredMedicineOptions"
                :key="m.id"
                :label="`${m.medicineName}（${m.medicineCode || '-'}）`"
                :value="m.id"
              />
            </el-select>
          </template>
        </el-table-column>
        <el-table-column
          label="计划数量"
          width="140"
        >
          <template #default="scope">
            <el-input-number
              v-model="scope.row.planQty"
              :min="1"
              :controls="true"
              style="width: 100%"
            />
          </template>
        </el-table-column>
        <el-table-column
          label="备注"
          min-width="200"
        >
          <template #default="scope">
            <el-input
              v-model="scope.row.remark"
              placeholder="可选"
            />
          </template>
        </el-table-column>
        <el-table-column
          label="操作"
          width="100"
        >
          <template #default="scope">
            <el-button
              type="danger"
              size="small"
              @click="removeItem(scope.$index)"
            >
              删除
            </el-button>
          </template>
        </el-table-column>
        </el-table>
      </div>

      <template #footer>
        <el-button @click="createVisible = false">
          取消
        </el-button>
        <el-button
          type="primary"
          :loading="saving"
          @click="savePlan"
        >
          {{ isEdit ? '保存' : '创建' }}
        </el-button>
      </template>
    </el-dialog>

    <!-- 详情 -->
    <el-dialog
      v-model="detailVisible"
      title="采购计划详情"
      width="900px"
    >
      <div class="doc-preview">
        <div class="doc-preview-title">
          采购计划单
        </div>
        <div class="doc-preview-meta">
          <span>计划单号：{{ detail.planNo || '-' }}</span>
          <span>计划时间：{{ formatDate(detail.createTime) }}</span>
          <span>创建人姓名：{{ currentUserDisplayName }}</span>
        </div>
        <div class="doc-preview-meta">
          <span>主题：{{ detail.title || '-' }}</span>
          <span>状态：{{ getStatusText(detail.status) }}</span>
        </div>
        <div class="doc-preview-meta">
          <span>备注：{{ detail.remark || '-' }}</span>
        </div>
      </div>
      <el-table
        :data="detail.items || []"
        border
        style="width: 100%"
      >
        <el-table-column
          prop="medicine.medicineName"
          label="药品"
          min-width="220"
        >
          <template #default="scope">
            {{ scope.row.medicine?.medicineName || scope.row.medicineId }}
          </template>
        </el-table-column>
        <el-table-column
          prop="planQty"
          label="计划数量"
          width="120"
        />
        <el-table-column
          prop="purchasedQty"
          label="已下单"
          width="120"
        />
        <el-table-column
          label="剩余可下单"
          width="140"
        >
          <template #default="scope">
            {{ (scope.row.planQty || 0) - (scope.row.purchasedQty || 0) }}
          </template>
        </el-table-column>
        <el-table-column
          prop="remark"
          label="备注"
          min-width="200"
        />
      </el-table>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { useUserStore } from '@/store/user'
import request from '@/utils/request'
import { ElMessageBox } from 'element-plus'
import { formatDate } from '@/utils/dateUtils'
import { previewDocumentNo, formatDraftCreateTime } from '@/utils/draftDocumentPreview'

const userStore = useUserStore()
const currentUserDisplayName = computed(() => userStore.userInfo?.name || userStore.userInfo?.username || '-')

const loading = ref(false)
const saving = ref(false)
const tableData = ref([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(10)

const searchForm = reactive({
  planNo: '',
  title: '',
  status: undefined
})

const createVisible = ref(false)
const isEdit = ref(false)
const editingPlanId = ref(null)
const detailVisible = ref(false)
const detail = reactive({})

const medicineOptions = ref([])
const filteredMedicineOptions = ref([])
const createForm = reactive({
  title: '',
  remark: '',
  items: []
})

const draftPreviewNo = ref('')
const draftCreateTime = ref('')

const fetchMedicines = async () => {
  await request.get('/medicine/list', {}, {
    showDefaultMsg: false,
    onSuccess: (res) => {
      medicineOptions.value = res || []
      filteredMedicineOptions.value = res || []
    }
  })
}

const filterMedicine = (query) => {
  const q = (query || '').toLowerCase().trim()
  if (!q) {
    filteredMedicineOptions.value = medicineOptions.value
    return
  }
  filteredMedicineOptions.value = medicineOptions.value.filter(m => {
    const name = (m.medicineName || '').toLowerCase()
    const code = (m.medicineCode || '').toLowerCase()
    return name.includes(q) || code.includes(q)
  })
}

const fetchPlans = async () => {
  loading.value = true
  try {
    await request.get('/purchasePlan/page', {
      planNo: searchForm.planNo,
      title: searchForm.title,
      status: searchForm.status,
      currentPage: currentPage.value,
      size: pageSize.value
    }, {
      showDefaultMsg: false,
      onSuccess: (res) => {
        tableData.value = res.records || []
        total.value = res.total || 0
      }
    })
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  currentPage.value = 1
  fetchPlans()
}

const resetSearch = () => {
  searchForm.planNo = ''
  searchForm.title = ''
  searchForm.status = undefined
  handleSearch()
}

const handleSizeChange = (val) => {
  pageSize.value = val
  fetchPlans()
}

const handleCurrentChange = (val) => {
  currentPage.value = val
  fetchPlans()
}

const openCreate = async () => {
  isEdit.value = false
  editingPlanId.value = null
  draftPreviewNo.value = previewDocumentNo('PP')
  draftCreateTime.value = formatDraftCreateTime()
  createVisible.value = true
  if (medicineOptions.value.length === 0) {
    await fetchMedicines()
  }
  if (createForm.items.length === 0) {
    addItem()
  }
}

const openEdit = async (row) => {
  isEdit.value = true
  editingPlanId.value = row.id
  createVisible.value = true
  if (medicineOptions.value.length === 0) {
    await fetchMedicines()
  }
  await request.get(`/purchasePlan/${row.id}`, {}, {
    showDefaultMsg: false,
    onSuccess: (res) => {
      createForm.title = res?.title || ''
      createForm.remark = res?.remark || ''
      createForm.items = (res?.items || []).map(it => ({
        medicineId: it.medicineId,
        planQty: it.planQty || 1,
        remark: it.remark || ''
      }))
    }
  })
}

const addItem = () => {
  createForm.items.push({
    medicineId: null,
    planQty: 1,
    remark: ''
  })
}

const removeItem = (idx) => {
  createForm.items.splice(idx, 1)
}

const resetCreate = () => {
  isEdit.value = false
  editingPlanId.value = null
  draftPreviewNo.value = ''
  draftCreateTime.value = ''
  createForm.title = ''
  createForm.remark = ''
  createForm.items = []
}

const savePlan = async () => {
  const items = (createForm.items || []).filter(i => i.medicineId && i.planQty > 0)
  if (items.length === 0) return
  saving.value = true
  try {
    const payload = {
      title: createForm.title,
      remark: createForm.remark,
      items
    }
    if (isEdit.value && editingPlanId.value) {
      await request.put(`/purchasePlan/${editingPlanId.value}`, payload, {
        successMsg: '编辑采购计划成功',
        onSuccess: () => {
          createVisible.value = false
          fetchPlans()
        }
      })
    } else {
      await request.post('/purchasePlan', { ...payload, planNo: draftPreviewNo.value }, {
        successMsg: '创建采购计划成功',
        onSuccess: () => {
          createVisible.value = false
          fetchPlans()
        }
      })
    }
  } finally {
    saving.value = false
  }
}

const openDetail = async (row) => {
  await request.get(`/purchasePlan/${row.id}`, {}, {
    showDefaultMsg: false,
    onSuccess: (res) => {
      Object.assign(detail, res || {})
      detailVisible.value = true
    }
  })
}

const submitPlan = async (row) => {
  await ElMessageBox.confirm(`确定提交采购计划 ${row.planNo} 吗？提交后才能拆分生成采购单。`, '提示', { type: 'warning' })
  await request.put(`/purchasePlan/submit/${row.id}`, {}, {
    successMsg: '提交成功',
    onSuccess: () => fetchPlans()
  })
}

const deletePlan = async (row) => {
  await ElMessageBox.confirm(`确定删除采购计划 ${row.planNo} 吗？`, '提示', { type: 'warning' })
  await request.delete(`/purchasePlan/${row.id}`, {
    successMsg: '删除成功',
    onSuccess: () => fetchPlans()
  })
}

const getStatusText = (status) => {
  switch (status) {
    case 0: return '草稿'
    case 1: return '已提交'
    case 2: return '已完结'
    default: return status
  }
}
const getStatusTagType = (status) => {
  switch (status) {
    case 0: return 'info'
    case 1: return 'success'
    case 2: return 'warning'
    default: return ''
  }
}

onMounted(() => {
  fetchPlans()
})
</script>

<style lang="scss" scoped>
.purchase-plan {
  .card-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
  }
  .search-form {
    margin-bottom: 16px;
  }
  .pagination-container {
    display: flex;
    justify-content: flex-end;
    margin-top: 16px;
  }
  .items-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin: 10px 0;
    padding: 0 12px;
    box-sizing: border-box;
    .items-title {
      font-weight: 600;
      font-size: 14px;
      line-height: 22px;
      color: var(--el-text-color-primary);
      text-align: left;
    }
  }
  /* 位于 create-doc-layout 内，与下方元信息、表单左缘对齐，避免重复 padding */
  .create-doc-layout > .items-header.create-basic-header-row {
    padding-left: 0;
    padding-right: 0;
    margin-top: 0;
    margin-bottom: 8px;
    justify-content: flex-start;
  }
  .doc-preview {
    border: 1px solid #dcdfe6;
    border-radius: 4px;
    padding: 10px 12px;
    margin-bottom: 12px;
    background: #fafafa;
  }
  .create-doc-layout {
    width: 100%;
    box-sizing: border-box;
    padding: 0 12px;
  }
  .create-meta-strip {
    margin-bottom: 12px;
  }
  .create-meta-line {
    display: flex;
    flex-wrap: wrap;
    gap: 16px 24px;
    align-items: center;
  }
  .create-meta-item {
    font-size: var(--el-form-label-font-size, 14px);
    line-height: var(--el-form-line-height, 22px);
    color: var(--el-text-color-regular);
  }
  .create-main-form {
    width: 100%;
    :deep(.el-form-item__content) {
      flex: 1;
      min-width: 0;
    }
    :deep(.el-input),
    :deep(.el-textarea) {
      width: 100%;
    }
    &.create-main-form--top {
      :deep(.el-form-item) {
        margin-bottom: 14px;
      }
      :deep(.el-form-item__label) {
        padding-bottom: 4px;
        line-height: 1.4;
      }
    }
  }
  .create-dialog-table-wrap {
    padding: 0 12px;
    box-sizing: border-box;
  }
  .doc-preview-title {
    font-weight: 700;
    margin-bottom: 8px;
  }
  .doc-preview-meta {
    display: flex;
    gap: 20px;
    margin-bottom: 6px;
    font-size: 13px;
    color: #606266;
  }
}
</style>

