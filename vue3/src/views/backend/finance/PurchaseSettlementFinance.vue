<template>
  <div class="finance-page">
    <el-card class="outer-card">
      <template #header>
        <div class="header-row">
          <span class="outer-title">采购结算</span>
        </div>
      </template>

      <el-card
        class="box-card doc-card"
        shadow="never"
      >
        <template #header>
          <div class="card-header">
            <h3>采购结算单</h3>
            <el-button
              type="primary"
              @click="openSettlementCreate"
            >
              创建结算单
            </el-button>
          </div>
        </template>

        <el-form
          :model="settlementOrderSearch"
          :inline="true"
          class="search-form"
        >
          <el-form-item label="结算单号">
            <el-input
              v-model="settlementOrderSearch.settlementNo"
              placeholder="请输入结算单号"
              clearable
              style="width: 200px"
            />
          </el-form-item>
          <el-form-item label="状态">
            <el-select
              v-model="settlementOrderSearch.status"
              placeholder="状态"
              style="width: 140px"
            >
              <el-option
                label="全部"
                :value="''"
              />
              <el-option
                label="待结算"
                :value="0"
              />
              <el-option
                label="已结算"
                :value="1"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="供应商名称">
            <el-input
              v-model="settlementOrderSearch.supplierName"
              placeholder="供应商名称"
              clearable
              style="width: 160px"
            />
          </el-form-item>
          <el-form-item label="经办人">
            <el-input
              v-model="settlementOrderSearch.creatorName"
              placeholder="姓名或用户名"
              clearable
              style="width: 160px"
            />
          </el-form-item>
          <el-form-item label="创建日期">
            <el-date-picker
              v-model="settlementOrderSearch.createDateRange"
              type="daterange"
              range-separator="至"
              start-placeholder="开始"
              end-placeholder="结束"
              value-format="YYYY-MM-DD"
              clearable
              style="width: 260px"
            />
          </el-form-item>
          <el-form-item>
            <el-button
              type="primary"
              @click="searchSettlementOrders"
            >
              查询
            </el-button>
            <el-button @click="resetSettlementOrdersSearch">
              重置
            </el-button>
          </el-form-item>
        </el-form>

        <el-table
          v-loading="settlementOrdersLoading"
          :data="settlementOrders"
          border
          style="width: 100%"
        >
          <el-table-column
            prop="settlementNo"
            label="结算单号"
            min-width="170"
          />
          <el-table-column
            label="入库单ID"
            min-width="150"
          >
            <template #default="scope">
              {{ formatStockInIds(scope.row) }}
            </template>
          </el-table-column>
          <el-table-column
            prop="supplier.name"
            label="供应商"
            width="180"
          />
          <el-table-column
            prop="totalAmount"
            label="总金额"
            width="110"
          />
          <el-table-column
            prop="status"
            label="状态"
            width="100"
          >
            <template #default="scope">
              <el-tag :type="scope.row.status === 0 ? 'warning' : 'success'">
                {{ scope.row.status === 0 ? '待结算' : '已结算' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column
            prop="createTime"
            label="创建时间"
            width="180"
          >
            <template #default="scope">
              {{ formatDateTime(scope.row.createTime) }}
            </template>
          </el-table-column>
          <el-table-column
            label="操作"
            width="200"
            fixed="right"
          >
            <template #default="scope">
              <el-button
                type="primary"
                size="small"
                @click="openSettlementDetail(scope.row.id)"
              >
                详情
              </el-button>
              <el-button
                v-if="scope.row.status === 0"
                type="success"
                size="small"
                @click="settleOrder(scope.row.id)"
              >
                确认结算
              </el-button>
            </template>
          </el-table-column>
        </el-table>

        <div class="pagination-container">
          <el-pagination
            :current-page="settlementOrderPage"
            :page-size="settlementOrderPageSize"
            :page-sizes="[10, 20, 50, 100]"
            background
            layout="total, sizes, prev, pager, next, jumper"
            :total="settlementOrderTotal"
            @size-change="onSettlementOrderSizeChange"
            @current-change="onSettlementOrderPageChange"
          />
        </div>
      </el-card>
    </el-card>

    <el-dialog
      v-model="settlementCreateVisible"
      title="创建采购结算单"
      width="900px"
      destroy-on-close
      @closed="resetSettlementCreate"
    >
      <el-form label-width="100px">
        <el-form-item
          label="供应商"
          required
        >
          <el-select
            v-model="createSettlementSupplierId"
            filterable
            clearable
            placeholder="请先选择供应商"
            style="width: 100%"
            @change="onSettlementSupplierChange"
          >
            <el-option
              v-for="opt in pendingSupplierOptions"
              :key="opt.supplierId"
              :label="`${opt.supplierName}（待结明细 ${opt.pendingLineCount} 行）`"
              :value="opt.supplierId"
            />
          </el-select>
        </el-form-item>
        <el-form-item
          label="入库单"
          required
        >
          <el-select
            v-model="createSettlementStockInIds"
            multiple
            filterable
            collapse-tags
            collapse-tags-tooltip
            :disabled="!createSettlementSupplierId"
            placeholder="请选择入库单（可多选）"
            style="width: 100%"
            @change="onSettlementStockInChange"
          >
            <el-option
              v-for="opt in pendingStockInOptions"
              :key="opt.stockInId"
              :label="`${opt.stockInNo}（待结 ${opt.pendingLineCount} 行）`"
              :value="opt.stockInId"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="备注">
          <el-input
            v-model="createSettlementRemark"
            type="textarea"
            :rows="2"
            placeholder="选填"
          />
        </el-form-item>
      </el-form>
      <el-table
        v-loading="createSettlementDetailsLoading"
        :data="createSettlementDetails"
        border
        max-height="360"
        @selection-change="onCreateSettlementSelectionChange"
      >
        <el-table-column
          type="selection"
          width="55"
        />
        <el-table-column
          prop="stockInId"
          label="入库单ID"
          width="100"
        />
        <el-table-column
          prop="supplier.name"
          label="供应商"
          width="140"
        />
        <el-table-column
          prop="medicine.medicineName"
          label="药品"
          min-width="160"
        />
        <el-table-column
          prop="quantity"
          label="数量"
          width="80"
        />
        <el-table-column
          prop="unitCost"
          label="单价"
          width="90"
        />
        <el-table-column
          prop="amount"
          label="金额"
          width="90"
        />
      </el-table>
      <template #footer>
        <el-button @click="settlementCreateVisible = false">
          取消
        </el-button>
        <el-button
          type="primary"
          :loading="createSettlementSubmitting"
          @click="submitSettlementCreate"
        >
          生成结算单
        </el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="settlementDetailVisible"
      title="采购结算单详情"
      width="980px"
    >
      <div class="doc-preview">
        <div class="doc-preview-title">
          采购结算单
        </div>
        <div class="doc-preview-meta">
          <span>结算时间：{{ settlementDetail.settlementTime || settlementDetail.createTime || '-' }}</span>
          <span>结算单号：{{ settlementDetail.settlementNo || '-' }}</span>
          <span>状态：{{ settlementDetail.status === 1 ? '已结算' : '待结算' }}</span>
        </div>
        <div class="doc-preview-meta">
          <span>关联入库单 ID：{{ formatStockInIds(settlementDetail) }}</span>
        </div>
        <div class="doc-preview-meta">
          <span>供应商名称：{{ settlementDetail.supplier?.name || '-' }}</span>
          <span>联系人：{{ settlementDetail.supplier?.contactName || '-' }}</span>
          <span>联系电话：{{ settlementDetail.supplier?.contactPhone || '-' }}</span>
          <span>地址：{{ settlementDetail.supplier?.address || '-' }}</span>
        </div>
        <div class="doc-preview-meta">
          <span>总金额：{{ settlementDetail.totalAmount || 0 }} 元</span>
          <span>备注：{{ settlementDetail.remark || '-' }}</span>
        </div>
      </div>
      <el-table
        :data="settlementDetail.details || []"
        border
      >
        <el-table-column
          type="index"
          label="序号"
          width="60"
        />
        <el-table-column
          prop="stockInId"
          label="入库单ID"
          width="110"
        />
        <el-table-column
          prop="medicine.medicineName"
          label="药品"
          min-width="180"
        />
        <el-table-column
          prop="quantity"
          label="数量"
          width="90"
        />
        <el-table-column
          prop="unitCost"
          label="单价"
          width="100"
        />
        <el-table-column
          prop="amount"
          label="金额"
          width="110"
        />
      </el-table>
      <template #footer>
        <el-button
          type="primary"
          @click="settlementDetailVisible = false"
        >
          关闭
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'
import { formatDateTime } from '@/utils/dateUtils'

const settlementOrders = ref([])
const settlementOrdersLoading = ref(false)
const settlementOrderPage = ref(1)
const settlementOrderPageSize = ref(10)
const settlementOrderTotal = ref(0)
const settlementOrderSearch = reactive({
  settlementNo: '',
  status: '',
  supplierName: '',
  creatorName: '',
  createDateRange: null
})

const settlementDetailVisible = ref(false)
const settlementDetail = reactive({})

const settlementCreateVisible = ref(false)
const pendingSupplierOptions = ref([])
const createSettlementSupplierId = ref(null)
const pendingStockInOptions = ref([])
const createSettlementStockInIds = ref([])
const createSettlementDetails = ref([])
const createSettlementDetailsLoading = ref(false)
const createSettlementRemark = ref('')
const createSettlementSelectedIds = ref([])
const createSettlementSubmitting = ref(false)

const formatStockInIds = (order) => {
  const list = order?.stockInIds
  if (Array.isArray(list) && list.length) {
    return list.join('、')
  }
  const fromDetails = [...new Set((order?.details || []).map((d) => d.stockInId).filter((id) => id != null))]
  return fromDetails.length ? fromDetails.sort((a, b) => a - b).join('、') : '-'
}

const buildSettlementOrderParams = () => {
  const params = {
    currentPage: settlementOrderPage.value,
    size: settlementOrderPageSize.value
  }
  if (settlementOrderSearch.settlementNo) {
    params.settlementNo = settlementOrderSearch.settlementNo
  }
  if (settlementOrderSearch.status !== '') {
    params.status = settlementOrderSearch.status
  }
  const sn = (settlementOrderSearch.supplierName || '').trim()
  if (sn) {
    params.supplierName = sn
  }
  const cn = (settlementOrderSearch.creatorName || '').trim()
  if (cn) {
    params.creatorName = cn
  }
  if (Array.isArray(settlementOrderSearch.createDateRange) && settlementOrderSearch.createDateRange.length === 2) {
    params.createDateStart = settlementOrderSearch.createDateRange[0]
    params.createDateEnd = settlementOrderSearch.createDateRange[1]
  }
  return params
}

const loadSettlementOrders = async () => {
  settlementOrdersLoading.value = true
  try {
    const page = await request.get('/finance/settlement-orders/page', buildSettlementOrderParams(), { showDefaultMsg: false })
    settlementOrders.value = page.records || []
    settlementOrderTotal.value = page.total || 0
  } finally {
    settlementOrdersLoading.value = false
  }
}

const searchSettlementOrders = () => {
  settlementOrderPage.value = 1
  loadSettlementOrders()
}

const resetSettlementOrdersSearch = () => {
  settlementOrderSearch.settlementNo = ''
  settlementOrderSearch.status = ''
  settlementOrderSearch.supplierName = ''
  settlementOrderSearch.creatorName = ''
  settlementOrderSearch.createDateRange = null
  searchSettlementOrders()
}

const onSettlementOrderSizeChange = (val) => {
  settlementOrderPageSize.value = val
  settlementOrderPage.value = 1
  loadSettlementOrders()
}

const onSettlementOrderPageChange = (val) => {
  settlementOrderPage.value = val
  loadSettlementOrders()
}

const openSettlementCreate = async () => {
  settlementCreateVisible.value = true
  const list = await request.get('/finance/settlement-details/pending-suppliers', {}, { showDefaultMsg: false })
  pendingSupplierOptions.value = list || []
  pendingStockInOptions.value = []
  if (!pendingSupplierOptions.value.length) {
    ElMessage.info('当前暂无可选供应商（无待生成结算单的过账入库明细）')
  }
}

const resetSettlementCreate = () => {
  createSettlementSupplierId.value = null
  pendingSupplierOptions.value = []
  pendingStockInOptions.value = []
  createSettlementStockInIds.value = []
  createSettlementDetails.value = []
  createSettlementRemark.value = ''
  createSettlementSelectedIds.value = []
}

const onSettlementSupplierChange = async () => {
  createSettlementStockInIds.value = []
  createSettlementDetails.value = []
  createSettlementSelectedIds.value = []
  if (!createSettlementSupplierId.value) {
    pendingStockInOptions.value = []
    return
  }
  const list = await request.get('/finance/settlement-details/pending-stock-ins', {
    supplierId: createSettlementSupplierId.value
  }, { showDefaultMsg: false })
  pendingStockInOptions.value = list || []
  if (!pendingStockInOptions.value.length) {
    ElMessage.info('该供应商暂无可选入库单')
  }
}

const loadCreateSettlementDetails = async () => {
  if (!createSettlementSupplierId.value || !createSettlementStockInIds.value.length) {
    createSettlementDetails.value = []
    return
  }
  createSettlementDetailsLoading.value = true
  try {
    const csv = createSettlementStockInIds.value.join(',')
    const page = await request.get('/finance/settlement-details/page', {
      supplierId: createSettlementSupplierId.value,
      stockInIds: csv,
      status: 0,
      currentPage: 1,
      size: 500
    }, { showDefaultMsg: false })
    createSettlementDetails.value = page.records || []
  } finally {
    createSettlementDetailsLoading.value = false
  }
}

const onSettlementStockInChange = () => {
  loadCreateSettlementDetails()
}

const onCreateSettlementSelectionChange = (rows) => {
  createSettlementSelectedIds.value = (rows || []).map((r) => r.id)
}

const submitSettlementCreate = async () => {
  if (!createSettlementSupplierId.value) {
    ElMessage.warning('请先选择供应商')
    return
  }
  if (!createSettlementStockInIds.value.length) {
    ElMessage.warning('请先选择入库单')
    return
  }
  if (!createSettlementSelectedIds.value.length) {
    ElMessage.warning('请勾选要合并进结算单的明细')
    return
  }
  createSettlementSubmitting.value = true
  try {
    const created = await request.post('/finance/settlement-orders', {
      detailIds: createSettlementSelectedIds.value,
      remark: createSettlementRemark.value || undefined
    }, { successMsg: '结算单生成成功' })
    settlementCreateVisible.value = false
    await loadSettlementOrders()
    if (created?.id != null) {
      await openSettlementDetail(created.id)
    }
  } finally {
    createSettlementSubmitting.value = false
  }
}

const settleOrder = async (id) => {
  await request.put(`/finance/settlement-orders/settle/${id}`, {}, { successMsg: '结算完成' })
  await loadSettlementOrders()
  if (settlementDetailVisible.value && settlementDetail.id === id) {
    await openSettlementDetail(id)
  }
}

const openSettlementDetail = async (id) => {
  const res = await request.get(`/finance/settlement-orders/${id}`)
  Object.keys(settlementDetail).forEach((key) => delete settlementDetail[key])
  Object.assign(settlementDetail, res || {})
  settlementDetailVisible.value = true
}

onMounted(() => {
  loadSettlementOrders()
})
</script>

<style scoped>
.finance-page {
  padding: 0;
}
.outer-card {
  border-radius: 8px;
}
.outer-title {
  font-size: 16px;
  font-weight: 600;
}
.header-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.doc-card {
  border: 1px solid var(--el-border-color-lighter);
}
.doc-card :deep(.el-card__header) {
  padding: 14px 18px;
  border-bottom: 1px solid var(--el-border-color-lighter);
}
.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}
.card-header h3 {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
}
.search-form {
  margin-bottom: 12px;
}
.pagination-container {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}
.doc-preview {
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  padding: 10px 12px;
  margin: 12px 0;
  background: #fafafa;
}
.doc-preview-title {
  font-weight: 700;
  margin-bottom: 8px;
}
.doc-preview-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 20px;
  margin-bottom: 6px;
  font-size: 13px;
  color: #606266;
}
</style>
