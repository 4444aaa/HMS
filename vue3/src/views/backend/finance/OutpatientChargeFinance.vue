<template>
  <div class="finance-page">
    <el-card class="outer-card">
      <template #header>
        <div class="header-row">
          <span class="outer-title">门诊缴费</span>
        </div>
      </template>

      <el-card
        class="box-card doc-card"
        shadow="never"
      >
        <template #header>
          <div class="card-header">
            <h3>门诊缴费单</h3>
            <el-button
              type="primary"
              @click="openOutpatientCreate"
            >
              创建缴费单
            </el-button>
          </div>
        </template>

        <el-form
          :model="outpatientOrderSearch"
          :inline="true"
          class="search-form"
        >
          <el-form-item label="缴费单号">
            <el-input
              v-model="outpatientOrderSearch.orderNo"
              placeholder="请输入缴费单号"
              clearable
              style="width: 200px"
            />
          </el-form-item>
          <el-form-item label="状态">
            <el-select
              v-model="outpatientOrderSearch.status"
              placeholder="状态"
              style="width: 140px"
            >
              <el-option
                label="全部"
                :value="''"
              />
              <el-option
                label="待支付"
                :value="0"
              />
              <el-option
                label="已支付"
                :value="1"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="患者">
            <el-input
              v-model="outpatientOrderSearch.patientName"
              placeholder="姓名或患者编号"
              clearable
              style="width: 160px"
            />
          </el-form-item>
          <el-form-item label="收费员">
            <el-input
              v-model="outpatientOrderSearch.creatorName"
              placeholder="姓名或用户名"
              clearable
              style="width: 160px"
            />
          </el-form-item>
          <el-form-item label="创建日期">
            <el-date-picker
              v-model="outpatientOrderSearch.createDateRange"
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
              @click="searchOutpatientOrders"
            >
              查询
            </el-button>
            <el-button @click="resetOutpatientOrdersSearch">
              重置
            </el-button>
          </el-form-item>
        </el-form>

        <el-table
          v-loading="outpatientOrdersLoading"
          :data="outpatientOrders"
          border
          style="width: 100%"
        >
          <el-table-column
            prop="orderNo"
            label="缴费单号"
            min-width="170"
          />
          <el-table-column
            prop="patient.name"
            label="患者"
            width="120"
          />
          <el-table-column
            label="处方单ID"
            min-width="150"
          >
            <template #default="scope">
              {{ formatPrescriptionIds(scope.row) }}
            </template>
          </el-table-column>
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
                {{ scope.row.status === 0 ? '待支付' : '已支付' }}
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
            prop="chargeTime"
            label="支付时间"
            width="180"
          >
            <template #default="scope">
              {{ scope.row.chargeTime ? formatDateTime(scope.row.chargeTime) : '-' }}
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
                @click="openOutpatientOrderDetail(scope.row.id)"
              >
                详情
              </el-button>
              <el-button
                v-if="scope.row.status === 0"
                type="success"
                size="small"
                @click="payOutpatientOrder(scope.row.id)"
              >
                确认支付
              </el-button>
            </template>
          </el-table-column>
        </el-table>

        <div class="pagination-container">
          <el-pagination
            :current-page="outpatientOrderPage"
            :page-size="outpatientOrderPageSize"
            :page-sizes="[10, 20, 50, 100]"
            background
            layout="total, sizes, prev, pager, next, jumper"
            :total="outpatientOrderTotal"
            @size-change="onOutpatientOrderSizeChange"
            @current-change="onOutpatientOrderPageChange"
          />
        </div>
      </el-card>
    </el-card>

    <el-dialog
      v-model="outpatientCreateVisible"
      title="创建门诊缴费单"
      width="900px"
      destroy-on-close
      @closed="resetOutpatientCreate"
    >
      <el-form label-width="100px">
        <el-form-item
          label="患者"
          required
        >
          <el-select
            v-model="createOutpatientPatientId"
            filterable
            clearable
            placeholder="请先选择患者"
            style="width: 100%"
            @change="onOutpatientPatientChange"
          >
            <el-option
              v-for="opt in pendingPatientOptions"
              :key="opt.patientId"
              :label="`${opt.patientName}（待缴明细 ${opt.pendingLineCount} 行）`"
              :value="opt.patientId"
            />
          </el-select>
        </el-form-item>
        <el-form-item
          label="处方单"
          required
        >
          <el-select
            v-model="createOutpatientPrescriptionIds"
            multiple
            filterable
            collapse-tags
            collapse-tags-tooltip
            :disabled="!createOutpatientPatientId"
            placeholder="请选择处方单（可多选）"
            style="width: 100%"
            @change="onOutpatientPrescriptionChange"
          >
            <el-option
              v-for="opt in pendingPrescriptionOptions"
              :key="opt.prescriptionId"
              :label="`${opt.prescriptionNo}（待缴 ${opt.pendingLineCount} 行）`"
              :value="opt.prescriptionId"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="备注">
          <el-input
            v-model="createOutpatientRemark"
            type="textarea"
            :rows="2"
            placeholder="选填"
          />
        </el-form-item>
      </el-form>
      <el-table
        v-loading="createOutpatientDetailsLoading"
        :data="createOutpatientDetails"
        border
        max-height="360"
        @selection-change="onCreateOutpatientSelectionChange"
      >
        <el-table-column
          type="selection"
          width="55"
        />
        <el-table-column
          prop="prescriptionId"
          label="处方ID"
          width="90"
        />
        <el-table-column
          prop="prescriptionDetailId"
          label="处方明细ID"
          width="110"
        />
        <el-table-column
          prop="patient.name"
          label="患者"
          width="100"
        />
        <el-table-column
          prop="medicine.medicineName"
          label="药品"
          min-width="140"
        />
        <el-table-column
          prop="quantity"
          label="数量"
          width="80"
        />
        <el-table-column
          prop="unitPrice"
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
        <el-button @click="outpatientCreateVisible = false">
          取消
        </el-button>
        <el-button
          type="primary"
          :loading="createOutpatientSubmitting"
          @click="submitOutpatientCreate"
        >
          生成缴费单
        </el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="outpatientOrderDetailVisible"
      title="门诊缴费单详情"
      width="920px"
    >
      <div class="doc-preview">
        <div class="doc-preview-title">
          门诊缴费单
        </div>
        <div class="doc-preview-meta">
          <span>缴费单号：{{ outpatientOrderDetail.orderNo || '-' }}</span>
          <span>患者：{{ outpatientOrderDetail.patient?.name || '-' }}</span>
          <span>状态：{{ outpatientOrderDetail.status === 1 ? '已支付' : '待支付' }}</span>
        </div>
        <div class="doc-preview-meta">
          <span>关联处方单 ID：{{ formatPrescriptionIds(outpatientOrderDetail) }}</span>
          <span>总金额：{{ outpatientOrderDetail.totalAmount || 0 }} 元</span>
        </div>
        <div class="doc-preview-meta">
          <span>创建时间：{{ outpatientOrderDetail.createTime || '-' }}</span>
          <span>支付时间：{{ outpatientOrderDetail.chargeTime || '-' }}</span>
          <span>备注：{{ outpatientOrderDetail.remark || '-' }}</span>
        </div>
      </div>
      <el-table
        :data="outpatientOrderDetail.details || []"
        border
      >
        <el-table-column
          type="index"
          label="序号"
          width="60"
        />
        <el-table-column
          prop="prescriptionId"
          label="处方ID"
          width="90"
        />
        <el-table-column
          prop="prescriptionDetailId"
          label="处方明细ID"
          width="110"
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
          prop="unitPrice"
          label="单价"
          width="90"
        />
        <el-table-column
          prop="amount"
          label="金额"
          width="100"
        />
      </el-table>
      <template #footer>
        <el-button
          type="primary"
          @click="outpatientOrderDetailVisible = false"
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

const outpatientOrders = ref([])
const outpatientOrdersLoading = ref(false)
const outpatientOrderPage = ref(1)
const outpatientOrderPageSize = ref(10)
const outpatientOrderTotal = ref(0)
const outpatientOrderSearch = reactive({
  orderNo: '',
  status: '',
  patientName: '',
  creatorName: '',
  createDateRange: null
})

const outpatientOrderDetailVisible = ref(false)
const outpatientOrderDetail = reactive({})

const outpatientCreateVisible = ref(false)
const pendingPatientOptions = ref([])
const createOutpatientPatientId = ref(null)
const pendingPrescriptionOptions = ref([])
const createOutpatientPrescriptionIds = ref([])
const createOutpatientDetails = ref([])
const createOutpatientDetailsLoading = ref(false)
const createOutpatientRemark = ref('')
const createOutpatientSelectedIds = ref([])
const createOutpatientSubmitting = ref(false)

const formatPrescriptionIds = (order) => {
  const list = order?.prescriptionIds
  if (Array.isArray(list) && list.length) {
    return list.join('、')
  }
  const fromDetails = [...new Set((order?.details || []).map((d) => d.prescriptionId).filter((id) => id != null))]
  return fromDetails.length ? fromDetails.sort((a, b) => a - b).join('、') : '-'
}

const buildOutpatientOrderParams = () => {
  const params = {
    currentPage: outpatientOrderPage.value,
    size: outpatientOrderPageSize.value
  }
  if (outpatientOrderSearch.orderNo) {
    params.orderNo = outpatientOrderSearch.orderNo
  }
  if (outpatientOrderSearch.status !== '') {
    params.status = outpatientOrderSearch.status
  }
  const pn = (outpatientOrderSearch.patientName || '').trim()
  if (pn) {
    params.patientName = pn
  }
  const cn = (outpatientOrderSearch.creatorName || '').trim()
  if (cn) {
    params.creatorName = cn
  }
  if (Array.isArray(outpatientOrderSearch.createDateRange) && outpatientOrderSearch.createDateRange.length === 2) {
    params.createDateStart = outpatientOrderSearch.createDateRange[0]
    params.createDateEnd = outpatientOrderSearch.createDateRange[1]
  }
  return params
}

const loadOutpatientOrders = async () => {
  outpatientOrdersLoading.value = true
  try {
    const page = await request.get('/finance/outpatient-orders/page', buildOutpatientOrderParams(), { showDefaultMsg: false })
    outpatientOrders.value = page.records || []
    outpatientOrderTotal.value = page.total || 0
  } finally {
    outpatientOrdersLoading.value = false
  }
}

const searchOutpatientOrders = () => {
  outpatientOrderPage.value = 1
  loadOutpatientOrders()
}

const resetOutpatientOrdersSearch = () => {
  outpatientOrderSearch.orderNo = ''
  outpatientOrderSearch.status = ''
  outpatientOrderSearch.patientName = ''
  outpatientOrderSearch.creatorName = ''
  outpatientOrderSearch.createDateRange = null
  searchOutpatientOrders()
}

const onOutpatientOrderSizeChange = (val) => {
  outpatientOrderPageSize.value = val
  outpatientOrderPage.value = 1
  loadOutpatientOrders()
}

const onOutpatientOrderPageChange = (val) => {
  outpatientOrderPage.value = val
  loadOutpatientOrders()
}

const openOutpatientCreate = async () => {
  outpatientCreateVisible.value = true
  const list = await request.get('/finance/outpatient-details/pending-patients', {}, { showDefaultMsg: false })
  pendingPatientOptions.value = list || []
  pendingPrescriptionOptions.value = []
  if (!pendingPatientOptions.value.length) {
    ElMessage.info('当前暂无可选患者（无待生成缴费单的已取药明细）')
  }
}

const resetOutpatientCreate = () => {
  createOutpatientPatientId.value = null
  pendingPatientOptions.value = []
  pendingPrescriptionOptions.value = []
  createOutpatientPrescriptionIds.value = []
  createOutpatientDetails.value = []
  createOutpatientRemark.value = ''
  createOutpatientSelectedIds.value = []
}

const onOutpatientPatientChange = async () => {
  createOutpatientPrescriptionIds.value = []
  createOutpatientDetails.value = []
  createOutpatientSelectedIds.value = []
  if (!createOutpatientPatientId.value) {
    pendingPrescriptionOptions.value = []
    return
  }
  const list = await request.get('/finance/outpatient-details/pending-prescriptions', {
    patientId: createOutpatientPatientId.value
  }, { showDefaultMsg: false })
  pendingPrescriptionOptions.value = list || []
  if (!pendingPrescriptionOptions.value.length) {
    ElMessage.info('该患者暂无可选处方')
  }
}

const loadCreateOutpatientDetails = async () => {
  if (!createOutpatientPatientId.value || !createOutpatientPrescriptionIds.value.length) {
    createOutpatientDetails.value = []
    return
  }
  createOutpatientDetailsLoading.value = true
  try {
    const csv = createOutpatientPrescriptionIds.value.join(',')
    const page = await request.get('/finance/outpatient-details/page', {
      patientId: createOutpatientPatientId.value,
      prescriptionIds: csv,
      status: 0,
      currentPage: 1,
      size: 500
    }, { showDefaultMsg: false })
    createOutpatientDetails.value = page.records || []
  } finally {
    createOutpatientDetailsLoading.value = false
  }
}

const onOutpatientPrescriptionChange = () => {
  loadCreateOutpatientDetails()
}

const onCreateOutpatientSelectionChange = (rows) => {
  createOutpatientSelectedIds.value = (rows || []).map((r) => r.id)
}

const submitOutpatientCreate = async () => {
  if (!createOutpatientPatientId.value) {
    ElMessage.warning('请先选择患者')
    return
  }
  if (!createOutpatientPrescriptionIds.value.length) {
    ElMessage.warning('请先选择处方单')
    return
  }
  if (!createOutpatientSelectedIds.value.length) {
    ElMessage.warning('请勾选要合并进缴费单的明细')
    return
  }
  const rows = createOutpatientDetails.value.filter((r) => createOutpatientSelectedIds.value.includes(r.id))
  const pids = [...new Set(rows.map((r) => r.patientId).filter((id) => id != null))]
  if (pids.length > 1) {
    ElMessage.error('一张缴费单仅允许同一患者的明细')
    return
  }
  createOutpatientSubmitting.value = true
  try {
    const created = await request.post('/finance/outpatient-orders', {
      detailIds: createOutpatientSelectedIds.value,
      remark: createOutpatientRemark.value || undefined
    }, { successMsg: '缴费单生成成功' })
    outpatientCreateVisible.value = false
    await loadOutpatientOrders()
    if (created?.id != null) {
      await openOutpatientOrderDetail(created.id)
    }
  } finally {
    createOutpatientSubmitting.value = false
  }
}

const payOutpatientOrder = async (id) => {
  await request.put(`/finance/outpatient-orders/pay/${id}`, {}, { successMsg: '缴费单已支付' })
  await loadOutpatientOrders()
  if (outpatientOrderDetailVisible.value && outpatientOrderDetail.id === id) {
    await openOutpatientOrderDetail(id)
  }
}

const openOutpatientOrderDetail = async (id) => {
  const res = await request.get(`/finance/outpatient-orders/${id}`)
  Object.keys(outpatientOrderDetail).forEach((key) => delete outpatientOrderDetail[key])
  Object.assign(outpatientOrderDetail, res || {})
  outpatientOrderDetailVisible.value = true
}

onMounted(() => {
  loadOutpatientOrders()
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
