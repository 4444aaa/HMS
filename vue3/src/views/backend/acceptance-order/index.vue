<template>
  <div class="acceptance-order">
    <el-card class="box-card">
      <template #header>
        <div class="card-header">
          <h3>验收单</h3>
          <el-button type="primary" @click="openCreate">新增验收单</el-button>
        </div>
      </template>

      <el-form :model="searchForm" :inline="true" class="search-form">
        <el-form-item label="验收单号">
          <el-input v-model="searchForm.acceptanceNo" placeholder="请输入验收单号" clearable />
        </el-form-item>
        <el-form-item label="采购单ID">
          <el-input v-model="searchForm.purchaseOrderId" placeholder="可选" clearable />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="searchForm.status" placeholder="请选择状态" clearable style="width: 140px">
            <el-option label="草稿" :value="0" />
            <el-option label="已完成" :value="1" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="resetSearch">重置</el-button>
        </el-form-item>
      </el-form>

      <el-table v-loading="loading" :data="tableData" border style="width: 100%">
        <el-table-column prop="acceptanceNo" label="验收单号" width="170" />
        <el-table-column prop="purchaseOrderId" label="采购单ID" width="110" />
        <el-table-column prop="status" label="状态" width="110">
          <template #default="scope">
            <el-tag :type="getStatusTagType(scope.row.status)">{{ getStatusText(scope.row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="acceptanceTime" label="验收时间" width="180">
          <template #default="scope">{{ formatDate(scope.row.acceptanceTime) }}</template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="180">
          <template #default="scope">{{ formatDate(scope.row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="260">
          <template #default="scope">
            <el-button size="small" type="primary" @click="openDetail(scope.row)">详情</el-button>
            <el-button
              size="small"
              type="warning"
              v-if="scope.row.status === 0"
              @click="openEdit(scope.row)"
            >编辑</el-button>
            <el-button
              size="small"
              type="success"
              v-if="scope.row.status === 0"
              @click="complete(scope.row)"
            >完成</el-button>
            <el-button
              size="small"
              type="danger"
              v-if="scope.row.status === 0"
              @click="deleteAcceptance(scope.row)"
            >删除</el-button>
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

    <!-- 创建验收单 -->
    <el-dialog v-model="createVisible" :title="isEdit ? '编辑验收单' : '新增验收单'" width="980px" @closed="resetCreate">
      <el-form :model="createForm" label-width="90px">
        <el-form-item label="采购单">
          <el-select v-model="createForm.purchaseOrderId" placeholder="请选择已发送采购单" filterable style="width: 360px" @change="onOrderChange">
            <el-option
              v-for="o in orderOptions"
              :key="o.id"
              :label="`${o.orderNo}（ID:${o.id}）`"
              :value="o.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="createForm.remark" type="textarea" :rows="2" placeholder="可选" />
        </el-form-item>
      </el-form>

      <el-alert
        title="验收明细必须来源采购单明细；到货数量≤下单数量；合格数量≤到货数量。"
        type="info"
        :closable="false"
        show-icon
        style="margin-bottom: 10px"
      />

      <el-table :data="createForm.items" border style="width: 100%">
        <el-table-column label="药品" min-width="220">
          <template #default="scope">
            {{ scope.row.medicine?.medicineName || scope.row.medicineId }}
          </template>
        </el-table-column>
        <el-table-column prop="orderedQty" label="下单数量" width="120" />
        <el-table-column label="到货数量" width="160">
          <template #default="scope">
            <el-input-number v-model="scope.row.receivedQty" :min="0" :max="scope.row.orderedQty" style="width: 100%" />
          </template>
        </el-table-column>
        <el-table-column label="合格数量" width="160">
          <template #default="scope">
            <el-input-number v-model="scope.row.qualifiedQty" :min="0" :max="scope.row.receivedQty" style="width: 100%" />
          </template>
        </el-table-column>
        <el-table-column label="批号" width="160">
          <template #default="scope">
            <el-input v-model="scope.row.batchNo" placeholder="可选" />
          </template>
        </el-table-column>
        <el-table-column label="生产日期" width="140">
          <template #default="scope">
            <el-date-picker v-model="scope.row.productionDate" type="date" value-format="YYYY-MM-DD" placeholder="可选" style="width: 100%" />
          </template>
        </el-table-column>
        <el-table-column label="有效期" width="140">
          <template #default="scope">
            <el-date-picker v-model="scope.row.expiryDate" type="date" value-format="YYYY-MM-DD" placeholder="可选" style="width: 100%" />
          </template>
        </el-table-column>
      </el-table>

      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveAcceptance">{{ isEdit ? '保存' : '创建' }}</el-button>
      </template>
    </el-dialog>

    <!-- 详情 -->
    <el-dialog v-model="detailVisible" title="验收单详情" width="980px">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="验收单号">{{ detail.acceptanceNo }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="getStatusTagType(detail.status)">{{ getStatusText(detail.status) }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="采购单">
          {{ detail.purchaseOrder?.orderNo || detail.purchaseOrderId }}
        </el-descriptions-item>
        <el-descriptions-item label="供应商">
          {{ detail.purchaseOrder?.supplier?.name || '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="备注" :span="2">{{ detail.remark || '-' }}</el-descriptions-item>
      </el-descriptions>

      <el-divider />
      <el-table :data="detail.items || []" border style="width: 100%">
        <el-table-column label="药品" min-width="220">
          <template #default="scope">
            {{ scope.row.medicine?.medicineName || scope.row.medicineId }}
          </template>
        </el-table-column>
        <el-table-column prop="orderedQty" label="下单数量" width="120" />
        <el-table-column prop="receivedQty" label="到货数量" width="120" />
        <el-table-column prop="qualifiedQty" label="合格数量" width="120" />
        <el-table-column prop="batchNo" label="批号" width="140" />
        <el-table-column prop="productionDate" label="生产日期" width="140" />
        <el-table-column prop="expiryDate" label="有效期" width="140" />
      </el-table>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import request from '@/utils/request'
import { ElMessageBox } from 'element-plus'
import { formatDate } from '@/utils/dateUtils'

const loading = ref(false)
const saving = ref(false)
const tableData = ref([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(10)

const searchForm = reactive({
  acceptanceNo: '',
  purchaseOrderId: '',
  status: undefined
})

const createVisible = ref(false)
const isEdit = ref(false)
const editingAcceptanceId = ref(null)
const detailVisible = ref(false)
const detail = reactive({})

const orderOptions = ref([])
const createForm = reactive({
  purchaseOrderId: null,
  remark: '',
  items: []
})

const fetchAcceptances = async () => {
  loading.value = true
  try {
    await request.get('/purchaseAcceptance/page', {
      acceptanceNo: searchForm.acceptanceNo,
      purchaseOrderId: searchForm.purchaseOrderId ? Number(searchForm.purchaseOrderId) : undefined,
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
  fetchAcceptances()
}
const resetSearch = () => {
  searchForm.acceptanceNo = ''
  searchForm.purchaseOrderId = ''
  searchForm.status = undefined
  handleSearch()
}
const handleSizeChange = (val) => {
  pageSize.value = val
  fetchAcceptances()
}
const handleCurrentChange = (val) => {
  currentPage.value = val
  fetchAcceptances()
}

const openCreate = async () => {
  isEdit.value = false
  editingAcceptanceId.value = null
  createVisible.value = true
  await request.get('/purchaseOrder/page', {
    status: 1,
    currentPage: 1,
    size: 999
  }, {
    showDefaultMsg: false,
    onSuccess: (res) => {
      orderOptions.value = res.records || []
    }
  })
}

const openEdit = async (row) => {
  isEdit.value = true
  editingAcceptanceId.value = row.id
  createVisible.value = true
  await request.get('/purchaseOrder/page', {
    status: 1,
    currentPage: 1,
    size: 999
  }, {
    showDefaultMsg: false,
    onSuccess: (res) => {
      orderOptions.value = res.records || []
    }
  })
  await request.get(`/purchaseAcceptance/${row.id}`, {}, {
    showDefaultMsg: false,
    onSuccess: (res) => {
      createForm.purchaseOrderId = res?.purchaseOrderId || null
      createForm.remark = res?.remark || ''
      createForm.items = (res?.items || []).map(it => ({
        purchaseOrderItemId: it.purchaseOrderItemId,
        medicineId: it.medicineId,
        medicine: it.medicine,
        orderedQty: it.orderedQty || 0,
        receivedQty: it.receivedQty || 0,
        qualifiedQty: it.qualifiedQty || 0,
        batchNo: it.batchNo || '',
        productionDate: it.productionDate || '',
        expiryDate: it.expiryDate || ''
      }))
    }
  })
}

const onOrderChange = async (orderId) => {
  if (!orderId) return
  await request.get(`/purchaseOrder/${orderId}`, {}, {
    showDefaultMsg: false,
    onSuccess: (res) => {
      const items = (res.items || []).map(it => ({
        purchaseOrderItemId: it.id,
        medicineId: it.medicineId,
        medicine: it.medicine,
        orderedQty: it.orderQty || 0,
        receivedQty: it.orderQty || 0,
        qualifiedQty: it.orderQty || 0,
        batchNo: '',
        productionDate: '',
        expiryDate: ''
      }))
      createForm.items = items
    }
  })
}

const resetCreate = () => {
  isEdit.value = false
  editingAcceptanceId.value = null
  createForm.purchaseOrderId = null
  createForm.remark = ''
  createForm.items = []
}

const saveAcceptance = async () => {
  if (!createForm.purchaseOrderId) return
  const items = (createForm.items || []).map(i => ({
    purchaseOrderItemId: i.purchaseOrderItemId,
    receivedQty: i.receivedQty,
    qualifiedQty: i.qualifiedQty,
    batchNo: i.batchNo,
    productionDate: i.productionDate,
    expiryDate: i.expiryDate
  }))
  saving.value = true
  try {
    const payload = {
      purchaseOrderId: createForm.purchaseOrderId,
      remark: createForm.remark,
      items
    }
    if (isEdit.value && editingAcceptanceId.value) {
      await request.put(`/purchaseAcceptance/${editingAcceptanceId.value}`, payload, {
        successMsg: '编辑验收单成功',
        onSuccess: () => {
          createVisible.value = false
          fetchAcceptances()
        }
      })
    } else {
      await request.post('/purchaseAcceptance', payload, {
        successMsg: '创建验收单成功',
        onSuccess: () => {
          createVisible.value = false
          fetchAcceptances()
        }
      })
    }
  } finally {
    saving.value = false
  }
}

const openDetail = async (row) => {
  await request.get(`/purchaseAcceptance/${row.id}`, {}, {
    showDefaultMsg: false,
    onSuccess: (res) => {
      Object.assign(detail, res || {})
      detailVisible.value = true
    }
  })
}

const complete = async (row) => {
  await ElMessageBox.confirm(`确定完成验收单 ${row.acceptanceNo} 吗？完成后才能创建入库单。`, '提示', { type: 'warning' })
  await request.put(`/purchaseAcceptance/complete/${row.id}`, {}, {
    successMsg: '完成成功',
    onSuccess: () => fetchAcceptances()
  })
}

const deleteAcceptance = async (row) => {
  await ElMessageBox.confirm(`确定删除验收单 ${row.acceptanceNo} 吗？`, '提示', { type: 'warning' })
  await request.delete(`/purchaseAcceptance/${row.id}`, {
    successMsg: '删除成功',
    onSuccess: () => fetchAcceptances()
  })
}

const getStatusText = (status) => {
  switch (status) {
    case 0: return '草稿'
    case 1: return '已完成'
    default: return status
  }
}
const getStatusTagType = (status) => {
  switch (status) {
    case 0: return 'info'
    case 1: return 'success'
    default: return ''
  }
}

onMounted(() => {
  fetchAcceptances()
})
</script>

<style lang="scss" scoped>
.acceptance-order {
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
}
</style>

