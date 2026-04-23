<template>
  <div class="prescription-pickup">
    <el-card class="box-card">
      <template #header>
        <div class="card-header">
          <h3>处方取药</h3>
          <p class="sub">
            医生提交后的处方会出现在此处，药房确认发药后标记为已取药。
          </p>
        </div>
      </template>

      <el-form
        :model="searchForm"
        :inline="true"
        class="search-form"
      >
        <el-form-item label="处方编号">
          <el-input
            v-model="searchForm.prescriptionNo"
            placeholder="请输入处方编号"
            clearable
          />
        </el-form-item>
        <el-form-item label="患者姓名">
          <el-input
            v-model="searchForm.patientName"
            placeholder="请输入患者姓名"
            clearable
          />
        </el-form-item>
        <el-form-item label="状态">
          <el-select
            v-model="searchForm.status"
            placeholder="全部"
            clearable
            style="width: 160px"
          >
            <el-option
              label="全部"
              value=""
            />
            <el-option
              label="待取药"
              :value="1"
            />
            <el-option
              label="已取药"
              :value="2"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="处方日期">
          <el-date-picker
            v-model="dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            format="YYYY-MM-DD"
            value-format="YYYY-MM-DD"
          />
        </el-form-item>
        <el-form-item>
          <el-button
            type="primary"
            @click="handleSearch"
          >
            搜索
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
          prop="prescriptionNo"
          label="处方编号"
          width="180"
        />
        <el-table-column
          label="患者"
          width="140"
        >
          <template #default="scope">
            {{ scope.row.patient?.name || '未知' }}
          </template>
        </el-table-column>
        <el-table-column
          label="医生"
          width="140"
        >
          <template #default="scope">
            {{ scope.row.doctor?.name || '未知' }}
          </template>
        </el-table-column>
        <el-table-column
          prop="prescriptionDate"
          label="处方日期"
          width="120"
        />
        <el-table-column
          prop="diagnosis"
          label="诊断"
          min-width="160"
        >
          <template #default="scope">
            <span class="ellipsis">{{ scope.row.diagnosis || scope.row.medicalRecord?.diagnosis || '—' }}</span>
          </template>
        </el-table-column>
        <el-table-column
          label="状态"
          width="100"
        >
          <template #default="scope">
            <el-tag :type="prescriptionStatusTagType(scope.row.status)">
              {{ prescriptionStatusLabel(scope.row.status) }}
            </el-tag>
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
              @click="handleView(scope.row)"
            >
              查看
            </el-button>
            <el-button
              v-if="scope.row.status === 1"
              type="success"
              size="small"
              @click="handleMarkPickedUp(scope.row)"
            >
              标记已取药
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

    <el-dialog
      v-model="detailVisible"
      title="处方详情"
      width="720px"
      destroy-on-close
    >
      <div
        v-if="detail"
        class="detail-body"
      >
        <el-descriptions
          :column="2"
          border
          size="small"
        >
          <el-descriptions-item label="处方编号">
            {{ detail.prescriptionNo }}
          </el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="prescriptionStatusTagType(detail.status)">
              {{ prescriptionStatusLabel(detail.status) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="患者">
            {{ detail.patient?.name }}
          </el-descriptions-item>
          <el-descriptions-item label="医生">
            {{ detail.doctor?.name }}
          </el-descriptions-item>
          <el-descriptions-item
            label="诊断"
            :span="2"
          >
            {{ detail.diagnosis || detail.medicalRecord?.diagnosis || '—' }}
          </el-descriptions-item>
        </el-descriptions>
        <el-table
          v-if="detail.details?.length"
          :data="detail.details"
          border
          size="small"
          class="detail-table"
        >
          <el-table-column
            label="药品"
            min-width="140"
          >
            <template #default="scope">
              {{ scope.row.medicine?.medicineName || '—' }}
            </template>
          </el-table-column>
          <el-table-column
            prop="dosage"
            label="用量"
            width="90"
          />
          <el-table-column
            prop="frequency"
            label="频次"
            width="100"
          />
          <el-table-column
            prop="quantity"
            label="数量"
            width="70"
          />
        </el-table>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import request from '@/utils/request'
import { ElMessageBox } from 'element-plus'
import { prescriptionStatusLabel, prescriptionStatusTagType } from '@/utils/prescriptionStatus'

const loading = ref(false)
const tableData = ref([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(10)
const dateRange = ref([])

const searchForm = reactive({
  prescriptionNo: '',
  patientName: '',
  status: ''
})

const detailVisible = ref(false)
const detail = ref(null)

const fetchList = async () => {
  loading.value = true
  try {
    const params = {
      currentPage: currentPage.value,
      size: pageSize.value
    }
    if (dateRange.value?.length === 2) {
      params.startDate = dateRange.value[0]
      params.endDate = dateRange.value[1]
    }
    if (searchForm.prescriptionNo) params.prescriptionNo = searchForm.prescriptionNo
    if (searchForm.patientName) params.patientName = searchForm.patientName
    if (searchForm.status !== null && searchForm.status !== '') {
      params.status = searchForm.status
    }

    await request.get('/prescription/page', params, {
      onSuccess: (res) => {
        const records = (res.records || []).filter(item => item?.status === 1 || item?.status === 2)
        tableData.value = searchForm.status === 1 || searchForm.status === 2
          ? records.filter(item => item.status === searchForm.status)
          : records
        total.value = tableData.value.length
      }
    })
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  currentPage.value = 1
  fetchList()
}

const resetSearch = () => {
  searchForm.prescriptionNo = ''
  searchForm.patientName = ''
  searchForm.status = ''
  dateRange.value = []
  currentPage.value = 1
  fetchList()
}

const handleView = async (row) => {
  await request.get(`/prescription/${row.id}`, {}, {
    onSuccess: (res) => {
      detail.value = res
      detailVisible.value = true
    }
  })
}

const handleMarkPickedUp = (row) => {
  ElMessageBox.confirm(
    `确认患者已取走处方「${row.prescriptionNo}」的药品？`,
    '标记已取药',
    { type: 'warning' }
  ).then(async () => {
    await request.put(`/prescription/status/${row.id}?status=2`, {}, {
      successMsg: '已标记为已取药',
      onSuccess: () => fetchList()
    })
  }).catch(() => {})
}

const handleSizeChange = (val) => {
  pageSize.value = val
  fetchList()
}

const handleCurrentChange = (val) => {
  currentPage.value = val
  fetchList()
}

onMounted(() => {
  fetchList()
})
</script>

<style scoped>
.prescription-pickup {
  padding: 20px;
}

.card-header {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 6px;
}

.card-header h3 {
  margin: 0;
}

.sub {
  margin: 0;
  font-size: 13px;
  color: #909399;
}

.search-form {
  margin-bottom: 16px;
}

.pagination-container {
  margin-top: 16px;
  text-align: right;
}

.ellipsis {
  display: inline-block;
  max-width: 220px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.detail-table {
  margin-top: 12px;
}
</style>
