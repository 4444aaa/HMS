<template>
  <div class="outpatient-charge-page">
    <el-card class="box-card" shadow="hover">
      <div class="filter-container">
        <div class="filter-header">
          <div class="header-icon">
            <el-icon><Wallet /></el-icon>
          </div>
          <h4>筛选缴费单</h4>
        </div>
        <el-radio-group v-model="statusFilter" size="large" @change="handleSearch">
          <el-radio-button :label="null">全部缴费单</el-radio-button>
          <el-radio-button :label="0">待支付</el-radio-button>
          <el-radio-button :label="1">已支付</el-radio-button>
        </el-radio-group>
      </div>

      <el-table
        v-loading="loading"
        :data="tableData"
        border
        style="width: 100%"
        :row-class-name="tableRowClassName"
      >
        <el-table-column prop="orderNo" label="缴费单号" width="180" />
        <el-table-column label="处方单ID" min-width="140">
          <template #default="scope">
            {{ formatPrescriptionIds(scope.row) }}
          </template>
        </el-table-column>
        <el-table-column prop="patient.name" label="患者" width="120">
          <template #default="scope">
            {{ scope.row.patient?.name || '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="totalAmount" label="金额(元)" width="120" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="scope">
            <el-tag :type="scope.row.status === 1 ? 'success' : 'warning'">
              {{ scope.row.status === 1 ? '已支付' : '待支付' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="170" />
        <el-table-column prop="chargeTime" label="支付时间" width="170">
          <template #default="scope">
            {{ scope.row.chargeTime || '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" min-width="180">
          <template #default="scope">
            {{ scope.row.remark || '-' }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="scope">
            <el-button size="small" type="primary" round @click="handleView(scope.row)">详情</el-button>
            <el-button
              v-if="scope.row.status === 0"
              size="small"
              type="success"
              round
              @click="handlePay(scope.row)"
            >
              立即支付
            </el-button>
          </template>
        </el-table-column>

        <template #empty>
          <el-empty description="暂无就诊缴费单">
            <template #image>
              <el-icon class="empty-icon"><Wallet /></el-icon>
            </template>
          </el-empty>
        </template>
      </el-table>

      <div class="pagination-container">
        <el-pagination
          :current-page="currentPage"
          :page-size="pageSize"
          :page-sizes="[10, 20, 50]"
          background
          layout="total, sizes, prev, pager, next, jumper"
          :total="total"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
    </el-card>

    <el-dialog v-model="detailVisible" title="缴费单详情" width="860px" class="charge-dialog">
      <div class="dialog-header-info">
        <div class="header-icon">
          <el-icon><Wallet /></el-icon>
        </div>
        <div class="header-content">
          <h3>{{ currentOrder.orderNo || '缴费单详情' }}</h3>
          <p>请核对缴费信息后完成支付</p>
        </div>
      </div>

      <el-descriptions :column="2" border>
        <el-descriptions-item label="缴费单号">{{ currentOrder.orderNo }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="currentOrder.status === 1 ? 'success' : 'warning'">
            {{ currentOrder.status === 1 ? '已支付' : '待支付' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="总金额">￥{{ currentOrder.totalAmount || 0 }}</el-descriptions-item>
        <el-descriptions-item label="支付时间">{{ currentOrder.chargeTime || '-' }}</el-descriptions-item>
        <el-descriptions-item label="关联处方单 ID" :span="2">{{ formatPrescriptionIds(currentOrder) }}</el-descriptions-item>
        <el-descriptions-item label="备注" :span="2">{{ currentOrder.remark || '-' }}</el-descriptions-item>
      </el-descriptions>

      <el-table :data="currentOrder.details || []" border style="width: 100%; margin-top: 14px">
        <el-table-column prop="prescriptionId" label="处方ID" width="100" />
        <el-table-column prop="prescriptionDetailId" label="处方明细ID" width="110" />
        <el-table-column prop="medicine.medicineName" label="药品" min-width="140">
          <template #default="scope">
            {{ scope.row.medicine?.medicineName || scope.row.medicineId }}
          </template>
        </el-table-column>
        <el-table-column prop="quantity" label="数量" width="80" />
        <el-table-column prop="unitPrice" label="单价" width="100" />
        <el-table-column prop="amount" label="金额" width="100" />
      </el-table>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import request from '@/utils/request'
import { ElMessageBox } from 'element-plus'
import { Wallet } from '@element-plus/icons-vue'

const loading = ref(false)
const tableData = ref([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(10)
const statusFilter = ref(null)

const detailVisible = ref(false)
const currentOrder = reactive({})

onMounted(() => {
  fetchOrders()
})

const fetchOrders = async () => {
  loading.value = true
  try {
    await request.get('/finance/outpatient-orders/my/page', {
      status: statusFilter.value,
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

const handleSizeChange = (val) => {
  pageSize.value = val
  fetchOrders()
}

const handleCurrentChange = (val) => {
  currentPage.value = val
  fetchOrders()
}

const formatPrescriptionIds = (order) => {
  const list = order?.prescriptionIds
  if (Array.isArray(list) && list.length) {
    return list.join('、')
  }
  const fromDetails = [...new Set((order?.details || []).map((d) => d.prescriptionId).filter((id) => id != null))]
  return fromDetails.length ? fromDetails.sort((a, b) => a - b).join('、') : '-'
}

const handleView = async (row) => {
  await request.get(`/finance/outpatient-orders/${row.id}`, {}, {
    showDefaultMsg: false,
    onSuccess: (res) => {
      Object.assign(currentOrder, res || {})
      detailVisible.value = true
    }
  })
}

const handlePay = async (row) => {
  await ElMessageBox.confirm(`确认支付缴费单 ${row.orderNo} 吗？`, '提示', { type: 'warning' })
  await request.put(`/finance/outpatient-orders/my/pay/${row.id}`, {}, {
    successMsg: '支付成功',
    onSuccess: () => {
      fetchOrders()
      if (detailVisible.value && currentOrder.id === row.id) {
        handleView(row)
      }
    }
  })
}

const tableRowClassName = ({ rowIndex }) => {
  return rowIndex % 2 === 0 ? 'even-row' : 'odd-row'
}
</script>

<style scoped>
.outpatient-charge-page {
  padding: 20px;
  background-color: #f9f7f2;
  min-height: calc(100vh - 160px);
}

.box-card {
  border-radius: 12px;
  border: none;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.05) !important;
}

.filter-container {
  background-color: #f7fbff;
  padding: 15px;
  border-radius: 8px;
  margin-bottom: 20px;
  border-left: 4px solid #a8d8ea;
  display: grid;
  grid-template-columns: 1fr auto 1fr;
  justify-items: center;
  align-items: center;
  gap: 16px;
}

.filter-header {
  display: flex;
  align-items: center;
  color: #3a5463;
  justify-self: start;
}

.header-icon {
  margin-right: 10px;
  background-color: #a8d8ea;
  width: 32px;
  height: 32px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
}

.filter-header h4 {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
}

.filter-container :deep(.el-radio-group) {
  grid-column: 2;
  transform: scale(0.92);
  transform-origin: center center;
}

.pagination-container {
  margin-top: 20px;
  text-align: right;
}

.empty-icon {
  font-size: 60px;
  color: #a8d8ea;
}

.dialog-header-info {
  display: flex;
  margin-bottom: 18px;
  align-items: center;
}

.dialog-header-info .header-content h3 {
  margin: 0 0 4px 0;
  color: #3a5463;
  font-size: 18px;
}

.dialog-header-info .header-content p {
  margin: 0;
  color: #5a7385;
  font-size: 14px;
}

:deep(.el-dialog) {
  border-radius: 12px;
}

:deep(.el-dialog__header) {
  background-color: #f0f9ff;
}

:deep(.el-table th) {
  background-color: #f0f9ff !important;
  color: #3a5463;
}

:deep(.el-table .even-row) {
  background-color: #f7fbff;
}

:deep(.el-table .odd-row) {
  background-color: #ffffff;
}

@media (max-width: 768px) {
  .filter-container {
    display: flex;
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
