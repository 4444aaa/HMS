<template>
  <div class="stock-in-order">
    <el-card class="box-card">
      <template #header>
        <div class="card-header">
          <h3>入库单</h3>
          <el-button type="primary" @click="openCreate">新增入库单</el-button>
        </div>
      </template>

      <el-form :model="searchForm" :inline="true" class="search-form">
        <el-form-item label="入库单号">
          <el-input v-model="searchForm.stockInNo" placeholder="请输入入库单号" clearable />
        </el-form-item>
        <el-form-item label="验收单ID">
          <el-input v-model="searchForm.acceptanceId" placeholder="可选" clearable />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="searchForm.status" placeholder="请选择状态" clearable style="width: 140px">
            <el-option label="草稿" :value="0" />
            <el-option label="已过账" :value="1" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="resetSearch">重置</el-button>
        </el-form-item>
      </el-form>

      <el-table v-loading="loading" :data="tableData" border style="width: 100%">
        <el-table-column prop="stockInNo" label="入库单号" width="170" />
        <el-table-column prop="acceptanceId" label="验收单ID" width="110" />
        <el-table-column prop="status" label="状态" width="110">
          <template #default="scope">
            <el-tag :type="getStatusTagType(scope.row.status)">{{ getStatusText(scope.row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="stockInTime" label="入库时间" width="180">
          <template #default="scope">{{ formatDate(scope.row.stockInTime) }}</template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="180">
          <template #default="scope">{{ formatDate(scope.row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="200">
          <template #default="scope">
            <el-button size="small" type="primary" @click="openDetail(scope.row)">详情</el-button>
            <el-button
              size="small"
              type="success"
              v-if="scope.row.status === 0"
              @click="post(scope.row)"
            >过账</el-button>
            <el-button
              size="small"
              type="danger"
              v-if="scope.row.status === 0"
              @click="deleteStockIn(scope.row)"
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

    <!-- 创建入库单 -->
    <el-dialog v-model="createVisible" title="新增入库单" width="980px" @closed="resetCreate">
      <el-form :model="createForm" label-width="90px">
        <el-form-item label="验收单">
          <el-select v-model="createForm.acceptanceId" placeholder="请选择已完成验收单" filterable style="width: 360px" @change="onAcceptanceChange">
            <el-option
              v-for="a in acceptanceOptions"
              :key="a.id"
              :label="`${a.acceptanceNo}（ID:${a.id}）`"
              :value="a.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="createForm.remark" type="textarea" :rows="2" placeholder="可选" />
        </el-form-item>
      </el-form>

      <el-alert
        title="入库明细必须来源验收明细；入库数量不得超过验收合格数量（系统会做校验）。"
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
        <el-table-column prop="qualifiedQty" label="合格数量" width="120" />
        <el-table-column label="入库数量" width="160">
          <template #default="scope">
            <el-input-number v-model="scope.row.stockInQty" :min="0" :max="scope.row.qualifiedQty" style="width: 100%" />
          </template>
        </el-table-column>
        <el-table-column label="单位成本" width="160">
          <template #default="scope">
            <el-input-number v-model="scope.row.unitCost" :min="0" :precision="2" style="width: 100%" />
          </template>
        </el-table-column>
      </el-table>

      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="createStockIn">创建</el-button>
      </template>
    </el-dialog>

    <!-- 详情 -->
    <el-dialog v-model="detailVisible" title="入库单详情" width="980px">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="入库单号">{{ detail.stockInNo }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="getStatusTagType(detail.status)">{{ getStatusText(detail.status) }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="验收单">
          {{ detail.acceptance?.acceptanceNo || detail.acceptanceId }}
        </el-descriptions-item>
        <el-descriptions-item label="备注">{{ detail.remark || '-' }}</el-descriptions-item>
      </el-descriptions>

      <el-divider />
      <el-table :data="detail.items || []" border style="width: 100%">
        <el-table-column label="药品" min-width="220">
          <template #default="scope">
            {{ scope.row.medicine?.medicineName || scope.row.medicineId }}
          </template>
        </el-table-column>
        <el-table-column prop="stockInQty" label="入库数量" width="120" />
        <el-table-column prop="unitCost" label="单位成本" width="120" />
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
  stockInNo: '',
  acceptanceId: '',
  status: undefined
})

const createVisible = ref(false)
const detailVisible = ref(false)
const detail = reactive({})

const acceptanceOptions = ref([])
const createForm = reactive({
  acceptanceId: null,
  remark: '',
  items: []
})

const fetchStockIns = async () => {
  loading.value = true
  try {
    await request.get('/stockInOrder/page', {
      stockInNo: searchForm.stockInNo,
      acceptanceId: searchForm.acceptanceId ? Number(searchForm.acceptanceId) : undefined,
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
  fetchStockIns()
}
const resetSearch = () => {
  searchForm.stockInNo = ''
  searchForm.acceptanceId = ''
  searchForm.status = undefined
  handleSearch()
}
const handleSizeChange = (val) => {
  pageSize.value = val
  fetchStockIns()
}
const handleCurrentChange = (val) => {
  currentPage.value = val
  fetchStockIns()
}

const openCreate = async () => {
  createVisible.value = true
  await request.get('/purchaseAcceptance/page', {
    status: 1,
    currentPage: 1,
    size: 999
  }, {
    showDefaultMsg: false,
    onSuccess: (res) => {
      acceptanceOptions.value = res.records || []
    }
  })
}

const onAcceptanceChange = async (acceptanceId) => {
  if (!acceptanceId) return
  await request.get(`/purchaseAcceptance/${acceptanceId}`, {}, {
    showDefaultMsg: false,
    onSuccess: async (res) => {
      let orderPriceMap = {}
      const orderId = res?.purchaseOrder?.id || res?.purchaseOrderId
      if (orderId) {
        await request.get(`/purchaseOrder/${orderId}`, {}, {
          showDefaultMsg: false,
          onSuccess: (orderRes) => {
            const map = {}
            ;(orderRes?.items || []).forEach(oi => {
              map[oi.medicineId] = Number(oi.unitPrice || 0)
            })
            orderPriceMap = map
          }
        })
      }
      const items = (res.items || []).map(it => ({
        acceptanceItemId: it.id,
        medicineId: it.medicineId,
        medicine: it.medicine,
        qualifiedQty: it.qualifiedQty || 0,
        stockInQty: it.qualifiedQty || 0,
        unitCost: Number(orderPriceMap[it.medicineId] || it.medicine?.price || 0)
      }))
      createForm.items = items
    }
  })
}

const resetCreate = () => {
  createForm.acceptanceId = null
  createForm.remark = ''
  createForm.items = []
}

const createStockIn = async () => {
  if (!createForm.acceptanceId) return
  const items = (createForm.items || [])
    .filter(i => i.stockInQty && i.stockInQty > 0)
    .map(i => ({
      acceptanceItemId: i.acceptanceItemId,
      stockInQty: i.stockInQty,
      unitCost: i.unitCost
    }))
  if (items.length === 0) return

  saving.value = true
  try {
    await request.post('/stockInOrder', {
      acceptanceId: createForm.acceptanceId,
      remark: createForm.remark,
      items
    }, {
      successMsg: '创建入库单成功',
      onSuccess: () => {
        createVisible.value = false
        fetchStockIns()
      }
    })
  } finally {
    saving.value = false
  }
}

const openDetail = async (row) => {
  await request.get(`/stockInOrder/${row.id}`, {}, {
    showDefaultMsg: false,
    onSuccess: (res) => {
      Object.assign(detail, res || {})
      detailVisible.value = true
    }
  })
}

const post = async (row) => {
  await ElMessageBox.confirm(`确定过账入库单 ${row.stockInNo} 吗？过账将写入药品库存。`, '提示', { type: 'warning' })
  await request.put(`/stockInOrder/post/${row.id}`, {}, {
    successMsg: '过账成功',
    onSuccess: () => fetchStockIns()
  })
}

const deleteStockIn = async (row) => {
  await ElMessageBox.confirm(`确定删除入库单 ${row.stockInNo} 吗？`, '提示', { type: 'warning' })
  await request.delete(`/stockInOrder/${row.id}`, {
    successMsg: '删除成功',
    onSuccess: () => fetchStockIns()
  })
}

const getStatusText = (status) => {
  switch (status) {
    case 0: return '草稿'
    case 1: return '已过账'
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
  fetchStockIns()
})
</script>

<style lang="scss" scoped>
.stock-in-order {
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

