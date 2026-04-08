<template>
  <div class="finance-page">
    <el-card>
      <template #header>
        <div class="header-row">
          <span>财务管理</span>
        </div>
      </template>

      <el-tabs v-model="activeTab">
        <el-tab-pane label="门诊缴费" name="outpatient">
          <div class="actions">
            <el-button type="primary" @click="generateOutpatientDetails">生成缴费明细</el-button>
            <el-button type="success" @click="createOutpatientOrder">生成缴费单</el-button>
          </div>
          <el-table :data="outpatientDetails" @selection-change="onOutpatientSelectionChange" border>
            <el-table-column type="selection" width="55" />
            <el-table-column prop="id" label="明细ID" width="90" />
            <el-table-column prop="patient.name" label="患者" width="120" />
            <el-table-column prop="medicine.medicineName" label="药品" />
            <el-table-column prop="quantity" label="数量" width="90" />
            <el-table-column prop="unitPrice" label="单价" width="100" />
            <el-table-column prop="amount" label="金额" width="100" />
            <el-table-column prop="status" label="状态" width="110">
              <template #default="scope">
                <el-tag :type="scope.row.status === 0 ? 'warning' : 'success'">
                  {{ scope.row.status === 0 ? '未生成单据' : '已生成单据' }}
                </el-tag>
              </template>
            </el-table-column>
          </el-table>

          <div class="sub-title">缴费单</div>
          <el-table :data="outpatientOrders" border>
            <el-table-column prop="orderNo" label="缴费单号" />
            <el-table-column prop="patient.name" label="患者" width="120" />
            <el-table-column prop="totalAmount" label="总金额" width="110" />
            <el-table-column prop="status" label="状态" width="100">
              <template #default="scope">
                <el-tag :type="scope.row.status === 0 ? 'warning' : 'success'">
                  {{ scope.row.status === 0 ? '待支付' : '已支付' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="120">
              <template #default="scope">
                <el-button v-if="scope.row.status === 0" type="primary" size="small" @click="payOutpatientOrder(scope.row.id)">确认支付</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <el-tab-pane label="采购结算" name="purchase">
          <div class="actions">
            <el-button type="primary" @click="generateSettlementDetails">生成结算明细</el-button>
            <el-button type="success" @click="createSettlementOrder">生成结算单</el-button>
          </div>
          <el-table :data="settlementDetails" @selection-change="onSettlementSelectionChange" border>
            <el-table-column type="selection" width="55" />
            <el-table-column prop="id" label="明细ID" width="90" />
            <el-table-column prop="supplier.name" label="供应商" width="180" />
            <el-table-column prop="medicine.medicineName" label="药品" />
            <el-table-column prop="quantity" label="数量" width="90" />
            <el-table-column prop="unitCost" label="单价" width="100" />
            <el-table-column prop="amount" label="金额" width="100" />
            <el-table-column prop="status" label="状态" width="110">
              <template #default="scope">
                <el-tag :type="scope.row.status === 0 ? 'warning' : 'success'">
                  {{ scope.row.status === 0 ? '未生成单据' : '已生成单据' }}
                </el-tag>
              </template>
            </el-table-column>
          </el-table>

          <div class="sub-title">结算单</div>
          <el-table :data="settlementOrders" border>
            <el-table-column prop="settlementNo" label="结算单号" />
            <el-table-column prop="supplier.name" label="供应商" width="180" />
            <el-table-column prop="totalAmount" label="总金额" width="110" />
            <el-table-column prop="status" label="状态" width="100">
              <template #default="scope">
                <el-tag :type="scope.row.status === 0 ? 'warning' : 'success'">
                  {{ scope.row.status === 0 ? '待结算' : '已结算' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="120">
              <template #default="scope">
                <el-button v-if="scope.row.status === 0" type="primary" size="small" @click="settleOrder(scope.row.id)">确认结算</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import request from '@/utils/request'

const activeTab = ref('outpatient')
const outpatientDetails = ref([])
const outpatientOrders = ref([])
const selectedOutpatientIds = ref([])
const settlementDetails = ref([])
const settlementOrders = ref([])
const selectedSettlementIds = ref([])

const loadOutpatient = async () => {
  const detailsPage = await request.get('/finance/outpatient-details/page', { currentPage: 1, size: 50 })
  outpatientDetails.value = detailsPage.records || []
  const ordersPage = await request.get('/finance/outpatient-orders/page', { currentPage: 1, size: 50 })
  outpatientOrders.value = ordersPage.records || []
}

const loadSettlement = async () => {
  const detailsPage = await request.get('/finance/settlement-details/page', { currentPage: 1, size: 50 })
  settlementDetails.value = detailsPage.records || []
  const ordersPage = await request.get('/finance/settlement-orders/page', { currentPage: 1, size: 50 })
  settlementOrders.value = ordersPage.records || []
}

const generateOutpatientDetails = async () => {
  await request.post('/finance/outpatient-details/generate', {}, { successMsg: '缴费明细生成完成' })
  await loadOutpatient()
}

const createOutpatientOrder = async () => {
  await request.post('/finance/outpatient-orders', { detailIds: selectedOutpatientIds.value }, { successMsg: '缴费单生成成功' })
  await loadOutpatient()
}

const payOutpatientOrder = async (id) => {
  await request.put(`/finance/outpatient-orders/pay/${id}`, {}, { successMsg: '缴费单已支付' })
  await loadOutpatient()
}

const generateSettlementDetails = async () => {
  await request.post('/finance/settlement-details/generate', {}, { successMsg: '结算明细生成完成' })
  await loadSettlement()
}

const createSettlementOrder = async () => {
  await request.post('/finance/settlement-orders', { detailIds: selectedSettlementIds.value }, { successMsg: '结算单生成成功' })
  await loadSettlement()
}

const settleOrder = async (id) => {
  await request.put(`/finance/settlement-orders/settle/${id}`, {}, { successMsg: '结算完成' })
  await loadSettlement()
}

const onOutpatientSelectionChange = (rows) => {
  selectedOutpatientIds.value = rows.map((row) => row.id)
}

const onSettlementSelectionChange = (rows) => {
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
</style>
