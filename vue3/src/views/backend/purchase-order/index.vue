<template>
  <div class="purchase-order">
    <el-card class="box-card">
      <template #header>
        <div class="card-header">
          <h3>采购单</h3>
          <el-button type="primary" @click="openCreate">新增采购单</el-button>
        </div>
      </template>

      <el-form :model="searchForm" :inline="true" class="search-form">
        <el-form-item label="采购单号">
          <el-input v-model="searchForm.orderNo" placeholder="请输入采购单号" clearable />
        </el-form-item>
        <el-form-item label="计划ID">
          <el-input v-model="searchForm.planId" placeholder="可选" clearable />
        </el-form-item>
        <el-form-item label="供应商ID">
          <el-input v-model="searchForm.supplierId" placeholder="可选" clearable />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="searchForm.status" placeholder="请选择状态" clearable style="width: 140px">
            <el-option label="草稿" :value="0" />
            <el-option label="已发送" :value="1" />
            <el-option label="已验收完成" :value="2" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="resetSearch">重置</el-button>
        </el-form-item>
      </el-form>

      <el-table v-loading="loading" :data="tableData" border style="width: 100%">
        <el-table-column prop="orderNo" label="采购单号" width="170" />
        <el-table-column prop="planId" label="计划ID" width="100" />
        <el-table-column label="供应商" min-width="180">
          <template #default="scope">
            {{ supplierNameMap[scope.row.supplierId] || scope.row.supplierId }}
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="120">
          <template #default="scope">
            <el-tag :type="getStatusTagType(scope.row.status)">{{ getStatusText(scope.row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="totalAmount" label="金额" width="120" />
        <el-table-column prop="createTime" label="创建时间" width="180">
          <template #default="scope">{{ formatDate(scope.row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="320" fixed="right">
          <template #default="scope">
            <el-button size="small" type="primary" @click="openDetail(scope.row)">详情</el-button>
            <el-button
              size="small"
              type="success"
              v-if="scope.row.status === 0"
              @click="sendOrder(scope.row)"
            >发送</el-button>
            <el-button
              size="small"
              type="danger"
              v-if="scope.row.status === 0"
              @click="deleteOrder(scope.row)"
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

    <!-- 创建采购单 -->
    <el-dialog v-model="createVisible" title="新增采购单" width="980px" @closed="resetCreate">
      <el-form :model="createForm" label-width="90px">
        <el-form-item label="采购计划">
          <el-select v-model="createForm.planId" placeholder="请选择已提交计划" filterable style="width: 320px" @change="onPlanChange">
            <el-option
              v-for="p in planOptions"
              :key="p.id"
              :label="`${p.planNo} ${p.title ? ' - ' + p.title : ''}`"
              :value="p.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="供应商">
          <el-select v-model="createForm.supplierId" placeholder="请选择供应商" filterable style="width: 320px" @change="onSupplierChange">
            <el-option
              v-for="s in supplierOptions"
              :key="s.id"
              :label="`${s.name}${s.supplierCode ? '（' + s.supplierCode + '）' : ''}`"
              :value="s.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="createForm.remark" type="textarea" :rows="2" placeholder="可选" />
        </el-form-item>
      </el-form>

      <el-alert
        title="下单明细必须来源采购计划明细。仅填写下单数量>0的行会被提交。"
        type="info"
        :closable="false"
        show-icon
        style="margin-bottom: 10px"
      />

      <el-table :data="createForm.items" border style="width: 100%">
        <el-table-column label="药品" min-width="240">
          <template #default="scope">
            {{ scope.row.medicine?.medicineName || scope.row.medicineId }}
          </template>
        </el-table-column>
        <el-table-column label="计划剩余" width="120">
          <template #default="scope">
            {{ scope.row.remainingQty }}
          </template>
        </el-table-column>
        <el-table-column label="下单数量" width="160">
          <template #default="scope">
            <el-input-number v-model="scope.row.orderQty" :min="0" :max="scope.row.remainingQty" style="width: 100%" />
          </template>
        </el-table-column>
        <el-table-column label="单价" width="160">
          <template #default="scope">
            <el-input-number v-model="scope.row.unitPrice" :min="0" :precision="2" style="width: 100%" />
          </template>
        </el-table-column>
        <el-table-column label="备注" min-width="200">
          <template #default="scope">
            <el-input v-model="scope.row.remark" placeholder="可选" />
          </template>
        </el-table-column>
      </el-table>

      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="createOrder">创建</el-button>
      </template>
    </el-dialog>

    <!-- 详情 -->
    <el-dialog v-model="detailVisible" title="采购单详情" width="980px">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="采购单号">{{ detail.orderNo }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="getStatusTagType(detail.status)">{{ getStatusText(detail.status) }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="计划ID">{{ detail.planId }}</el-descriptions-item>
        <el-descriptions-item label="供应商">
          {{ detail.supplier?.name || detail.supplierId }}
        </el-descriptions-item>
        <el-descriptions-item label="备注" :span="2">{{ detail.remark || '-' }}</el-descriptions-item>
      </el-descriptions>

      <el-divider />
      <el-table :data="detail.items || []" border style="width: 100%">
        <el-table-column label="药品" min-width="240">
          <template #default="scope">
            {{ scope.row.medicine?.medicineName || scope.row.medicineId }}
          </template>
        </el-table-column>
        <el-table-column prop="orderQty" label="下单数量" width="120" />
        <el-table-column prop="unitPrice" label="单价" width="120" />
        <el-table-column prop="amount" label="金额" width="120" />
        <el-table-column prop="remark" label="备注" min-width="200" />
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
  orderNo: '',
  planId: '',
  supplierId: '',
  status: undefined
})

const createVisible = ref(false)
const detailVisible = ref(false)
const detail = reactive({})

const planOptions = ref([])
const supplierOptions = ref([])
const supplierNameMap = reactive({})

const createForm = reactive({
  planId: null,
  supplierId: null,
  remark: '',
  items: []
})
const supplierMedicineIdSet = ref(new Set())
const sourcePlanItems = ref([])

const fetchOrders = async () => {
  loading.value = true
  try {
    await request.get('/purchaseOrder/page', {
      orderNo: searchForm.orderNo,
      planId: searchForm.planId ? Number(searchForm.planId) : undefined,
      supplierId: searchForm.supplierId ? Number(searchForm.supplierId) : undefined,
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
  fetchOrders()
}
const resetSearch = () => {
  searchForm.orderNo = ''
  searchForm.planId = ''
  searchForm.supplierId = ''
  searchForm.status = undefined
  handleSearch()
}
const handleSizeChange = (val) => {
  pageSize.value = val
  fetchOrders()
}
const handleCurrentChange = (val) => {
  currentPage.value = val
  fetchOrders()
}

const openCreate = async () => {
  createVisible.value = true
  await fetchPlanOptions()
  await fetchSupplierOptions()
}

const fetchPlanOptions = async () => {
  await request.get('/purchasePlan/page', {
    status: 1,
    currentPage: 1,
    size: 999
  }, {
    showDefaultMsg: false,
    onSuccess: (res) => {
      planOptions.value = res.records || []
    }
  })
}

const fetchSupplierOptions = async () => {
  supplierOptions.value = []
}

const fetchSupplierNameMap = async () => {
  await request.get('/supplier/page', {
    currentPage: 1,
    size: 999,
    status: 1
  }, {
    showDefaultMsg: false,
    onSuccess: (res) => {
      const records = res.records || []
      Object.keys(supplierNameMap).forEach(key => delete supplierNameMap[key])
      records.forEach(s => {
        supplierNameMap[s.id] = s.name
      })
    }
  })
}

const onPlanChange = async (planId) => {
  if (!planId) return
  createForm.supplierId = null
  supplierMedicineIdSet.value = new Set()
  sourcePlanItems.value = []
  await request.get(`/purchasePlan/${planId}/suppliers`, {}, {
    showDefaultMsg: false,
    onSuccess: (res) => {
      supplierOptions.value = res || []
    }
  })
  await request.get(`/purchasePlan/${planId}`, {}, {
    showDefaultMsg: false,
    onSuccess: (res) => {
      const items = (res.items || []).map(it => ({
        planItemId: it.id,
        medicineId: it.medicineId,
        medicine: it.medicine,
        remainingQty: (it.planQty || 0) - (it.purchasedQty || 0),
        orderQty: 0,
        unitPrice: Number(it.medicine?.price || 0),
        remark: ''
      }))
      sourcePlanItems.value = items
      createForm.items = filterItemsBySupplier(sourcePlanItems.value)
    }
  })
}

const onSupplierChange = async (supplierId) => {
  if (!supplierId) {
    supplierMedicineIdSet.value = new Set()
    return
  }
  await request.get('/medicine/list', {}, {
    showDefaultMsg: false,
    onSuccess: (res) => {
      const ids = (res || []).filter(m => m.supplierId === supplierId).map(m => m.id)
      supplierMedicineIdSet.value = new Set(ids)
      createForm.items = filterItemsBySupplier(sourcePlanItems.value || [])
    }
  })
}

const filterItemsBySupplier = (items) => {
  if (!createForm.supplierId) return items
  if (supplierMedicineIdSet.value.size === 0) return []
  return (items || []).filter(i => supplierMedicineIdSet.value.has(i.medicineId))
}

const resetCreate = () => {
  createForm.planId = null
  createForm.supplierId = null
  createForm.remark = ''
  createForm.items = []
  sourcePlanItems.value = []
}

const createOrder = async () => {
  const items = (createForm.items || [])
    .filter(i => i.orderQty && i.orderQty > 0)
    .map(i => ({
      planItemId: i.planItemId,
      orderQty: i.orderQty,
      unitPrice: i.unitPrice,
      remark: i.remark
    }))
  if (!createForm.planId || !createForm.supplierId || items.length === 0) return

  saving.value = true
  try {
    await request.post('/purchaseOrder', {
      planId: createForm.planId,
      supplierId: createForm.supplierId,
      remark: createForm.remark,
      items
    }, {
      successMsg: '创建采购单成功',
      onSuccess: () => {
        createVisible.value = false
        fetchOrders()
      }
    })
  } finally {
    saving.value = false
  }
}

const openDetail = async (row) => {
  await request.get(`/purchaseOrder/${row.id}`, {}, {
    showDefaultMsg: false,
    onSuccess: (res) => {
      Object.assign(detail, res || {})
      detailVisible.value = true
    }
  })
}

const sendOrder = async (row) => {
  await ElMessageBox.confirm(`确定发送采购单 ${row.orderNo} 吗？发送后才能创建验收单。`, '提示', { type: 'warning' })
  await request.put(`/purchaseOrder/send/${row.id}`, {}, {
    successMsg: '发送成功',
    onSuccess: () => fetchOrders()
  })
}

const deleteOrder = async (row) => {
  await ElMessageBox.confirm(`确定删除采购单 ${row.orderNo} 吗？`, '提示', { type: 'warning' })
  await request.delete(`/purchaseOrder/${row.id}`, {
    successMsg: '删除成功',
    onSuccess: () => fetchOrders()
  })
}

const getStatusText = (status) => {
  switch (status) {
    case 0: return '草稿'
    case 1: return '已发送'
    case 2: return '已验收完成'
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
  fetchSupplierNameMap()
  fetchOrders()
})
</script>

<style lang="scss" scoped>
.purchase-order {
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

