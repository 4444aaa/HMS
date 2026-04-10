<template>
  <div class="finance-page">
    <el-card>
      <template #header>
        <div class="header-row">
          <span>财务管理</span>
        </div>
      </template>

      <el-tabs v-model="activeTab">
        <el-tab-pane
          label="门诊缴费"
          name="outpatient"
        >
          <div class="actions">
            <el-button
              type="success"
              @click="createOutpatientOrder"
            >
              生成缴费单
            </el-button>
          </div>
          <el-table
            :data="outpatientDetails"
            border
            @selection-change="onOutpatientSelectionChange"
          >
            <el-table-column
              type="selection"
              width="55"
            />
            <el-table-column
              prop="id"
              label="明细ID"
              width="90"
            />
            <el-table-column
              prop="patient.name"
              label="患者"
              width="120"
            />
            <el-table-column
              prop="medicine.medicineName"
              label="药品"
            />
            <el-table-column
              prop="quantity"
              label="数量"
              width="90"
            />
            <el-table-column
              prop="unitPrice"
              label="单价"
              width="100"
            />
            <el-table-column
              prop="amount"
              label="金额"
              width="100"
            />
            <el-table-column
              prop="status"
              label="状态"
              width="110"
            >
              <template #default="scope">
                <el-tag :type="scope.row.status === 0 ? 'warning' : 'success'">
                  {{ scope.row.status === 0 ? '未生成单据' : '已生成单据' }}
                </el-tag>
              </template>
            </el-table-column>
          </el-table>

          <div class="sub-title">
            缴费单
          </div>
          <el-table
            :data="outpatientOrders"
            border
          >
            <el-table-column
              prop="orderNo"
              label="缴费单号"
            />
            <el-table-column
              prop="patient.name"
              label="患者"
              width="120"
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
                  {{ scope.row.status === 0 ? '待支付' : '已支付' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column
              label="操作"
              width="120"
            >
              <template #default="scope">
                <el-button
                  v-if="scope.row.status === 0"
                  type="primary"
                  size="small"
                  @click="payOutpatientOrder(scope.row.id)"
                >
                  确认支付
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <el-tab-pane
          label="采购结算"
          name="purchase"
        >
          <div class="actions">
            <el-button
              type="success"
              @click="createSettlementOrder"
            >
              生成结算单
            </el-button>
          </div>
          <el-table
            :data="settlementDetails"
            border
            @selection-change="onSettlementSelectionChange"
          >
            <el-table-column
              type="selection"
              width="55"
            />
            <el-table-column
              prop="id"
              label="明细ID"
              width="90"
            />
            <el-table-column
              prop="supplier.name"
              label="供应商"
              width="180"
            />
            <el-table-column
              prop="medicine.medicineName"
              label="药品"
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
              width="100"
            />
            <el-table-column
              prop="status"
              label="状态"
              width="110"
            >
              <template #default="scope">
                <el-tag :type="scope.row.status === 0 ? 'warning' : 'success'">
                  {{ scope.row.status === 0 ? '未生成单据' : '已生成单据' }}
                </el-tag>
              </template>
            </el-table-column>
          </el-table>
          <div
            v-if="selectedSettlementRows.length > 0"
            class="doc-preview"
          >
            <div class="doc-preview-title">
              拟生成结算单预览
            </div>
            <div class="doc-preview-meta">
              <span>供应商名称：{{ selectedSettlementRows[0]?.supplier?.name || '-' }}</span>
              <span>联系人：{{ selectedSettlementRows[0]?.supplier?.contactName || '-' }}</span>
              <span>联系电话：{{ selectedSettlementRows[0]?.supplier?.contactPhone || '-' }}</span>
              <span>地址：{{ selectedSettlementRows[0]?.supplier?.address || '-' }}</span>
            </div>
            <div class="doc-preview-meta">
              <span>明细数量：{{ selectedSettlementRows.length }}</span>
              <span>总金额：{{ selectedSettlementAmount }} 元</span>
            </div>
            <el-table
              :data="selectedSettlementRows"
              border
              size="small"
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
          </div>

          <div class="sub-title">
            结算单
          </div>
          <el-table
            :data="settlementOrders"
            border
          >
            <el-table-column
              prop="settlementNo"
              label="结算单号"
            />
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
              label="操作"
              width="190"
            >
              <template #default="scope">
                <el-button
                  type="primary"
                  size="small"
                  plain
                  @click="openSettlementDetail(scope.row.id)"
                >
                  详情
                </el-button>
                <el-button
                  v-if="scope.row.status === 0"
                  type="primary"
                  size="small"
                  @click="settleOrder(scope.row.id)"
                >
                  确认结算
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
      </el-tabs>
    </el-card>

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
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import request from '@/utils/request'

const activeTab = ref('outpatient')
const outpatientDetails = ref([])
const outpatientOrders = ref([])
const selectedOutpatientIds = ref([])
const settlementDetails = ref([])
const settlementOrders = ref([])
const selectedSettlementIds = ref([])
const selectedSettlementRows = ref([])
const settlementDetailVisible = ref(false)
const settlementDetail = reactive({})

const selectedSettlementAmount = computed(() => {
  return (selectedSettlementRows.value || []).reduce((sum, row) => {
    return sum + Number(row.amount || 0)
  }, 0)
})

const loadOutpatient = async () => {
  // 仅展示待处理明细，处理完成后自动隐藏
  const detailsPage = await request.get('/finance/outpatient-details/page', { currentPage: 1, size: 50, status: 0 })
  outpatientDetails.value = detailsPage.records || []
  const ordersPage = await request.get('/finance/outpatient-orders/page', { currentPage: 1, size: 50 })
  outpatientOrders.value = ordersPage.records || []
  selectedOutpatientIds.value = []
}

const loadSettlement = async () => {
  // 仅展示待处理明细，处理完成后自动隐藏
  const detailsPage = await request.get('/finance/settlement-details/page', { currentPage: 1, size: 50, status: 0 })
  settlementDetails.value = detailsPage.records || []
  const ordersPage = await request.get('/finance/settlement-orders/page', { currentPage: 1, size: 50 })
  settlementOrders.value = ordersPage.records || []
  selectedSettlementRows.value = []
  selectedSettlementIds.value = []
}

const createOutpatientOrder = async () => {
  await request.post('/finance/outpatient-orders', { detailIds: selectedOutpatientIds.value }, { successMsg: '缴费单生成成功' })
  await loadOutpatient()
}

const payOutpatientOrder = async (id) => {
  await request.put(`/finance/outpatient-orders/pay/${id}`, {}, { successMsg: '缴费单已支付' })
  await loadOutpatient()
}

const createSettlementOrder = async () => {
  await request.post('/finance/settlement-orders', { detailIds: selectedSettlementIds.value }, { successMsg: '结算单生成成功' })
  await loadSettlement()
}

const settleOrder = async (id) => {
  await request.put(`/finance/settlement-orders/settle/${id}`, {}, { successMsg: '结算完成' })
  await loadSettlement()
}

const openSettlementDetail = async (id) => {
  const res = await request.get(`/finance/settlement-orders/${id}`)
  Object.keys(settlementDetail).forEach((key) => delete settlementDetail[key])
  Object.assign(settlementDetail, res || {})
  settlementDetailVisible.value = true
}

const onOutpatientSelectionChange = (rows) => {
  selectedOutpatientIds.value = rows.map((row) => row.id)
}

const onSettlementSelectionChange = (rows) => {
  selectedSettlementRows.value = rows || []
  selectedSettlementIds.value = rows.map((row) => row.id)
}

onMounted(async () => {
  await loadOutpatient()
  await loadSettlement()
})
</script>

<style scoped>
.finance-page {
  padding: 20px 0;
}
.header-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.actions {
  margin-bottom: 12px;
  display: flex;
  gap: 8px;
}
.sub-title {
  margin: 16px 0 10px;
  font-weight: 600;
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
  gap: 20px;
  margin-bottom: 6px;
  font-size: 13px;
  color: #606266;
}
</style>
