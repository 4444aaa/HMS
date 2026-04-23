<template>
  <div class="prescription-management">
    <el-card class="box-card">
      <template #header>
        <div class="card-header">
          <h3>处方管理</h3>
          <el-button
            type="primary"
            @click="handleAdd"
          >
            新增处方
          </el-button>
        </div>
      </template>

      <!-- 搜索表单 -->
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
        <el-form-item label="医生姓名">
          <el-input
            v-model="searchForm.doctorName"
            placeholder="请输入医生姓名"
            clearable
          />
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
        <el-form-item label="状态">
          <el-select
            v-model="searchForm.status"
            placeholder="请选择状态"
            style="width: 130px"
          >
            <el-option
              label="全部"
              :value="''"
            />
            <el-option
              label="待提交"
              :value="0"
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

      <!-- 处方列表 -->
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
          label="患者信息"
          min-width="240"
        >
          <template #default="scope">
            <div class="patient-info-cell">
              <div>
                {{ scope.row.patient?.name || '未知' }}
                <el-tag
                  size="small"
                  type="info"
                >
                  {{ patientSexText(scope.row.patient?.sex) }}
                </el-tag>
              </div>
              <div class="text-muted">
                年龄：{{ patientAgeText(scope.row.patient) }}
              </div>
              <div class="text-muted">
                既往病史：{{ scope.row.patient?.medicalHistory || '无' }}
              </div>
              <div class="text-muted">
                过敏史：{{ scope.row.patient?.allergies || '无' }}
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column
          label="医生信息"
          width="150"
        >
          <template #default="scope">
            {{ scope.row.doctor?.name || '未知' }}
            <el-tag size="small">
              {{ scope.row.doctor?.title || '未知' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column
          label="就诊记录"
          width="180"
        >
          <template #default="scope">
            <div v-if="scope.row.medicalRecord">
              <div>{{ scope.row.medicalRecord?.recordNo || '未知' }}</div>
              <div class="text-muted">
                {{ scope.row.medicalRecord?.recordDate || '' }}
              </div>
            </div>
            <span v-else>未关联记录</span>
          </template>
        </el-table-column>
        <el-table-column
          label="主诉"
          min-width="180"
        >
          <template #default="scope">
            <div
              v-if="scope.row.medicalRecord?.appointment?.symptoms"
              class="ellipsis"
            >
              {{ scope.row.medicalRecord.appointment.symptoms }}
            </div>
            <span v-else>无</span>
          </template>
        </el-table-column>
        <el-table-column
          prop="prescriptionDate"
          label="处方日期"
          width="120"
          sortable
        />
        <el-table-column
          prop="diagnosis"
          label="诊断结果"
          width="200"
        >
          <template #default="scope">
            <div
              v-if="scope.row.diagnosis"
              class="ellipsis"
            >
              {{ scope.row.diagnosis }}
            </div>
            <span v-else>{{ scope.row.medicalRecord?.diagnosis || '—' }}</span>
          </template>
        </el-table-column>
        <el-table-column
          prop="status"
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
          prop="createTime"
          label="创建时间"
          sortable
        />
        <el-table-column
          label="操作"
          width="250"
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
              v-if="scope.row.status === 0" 
              type="success" 
              size="small" 
              @click="handleSubmitPrescription(scope.row.id)"
            >
              提交
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
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

    <!-- 处方详情对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogType === 'add' ? '新增处方' : dialogType === 'edit' ? '编辑处方' : '处方详情'"
      width="800px"
      @closed="resetForm"
    >
      <div
        v-if="dialogType === 'view'"
        class="prescription-view-detail"
      >
        <div class="view-header-line">
          <div>
            处方编号：{{ prescriptionForm.prescriptionNo || '-' }} ｜ 处方日期：{{ prescriptionForm.prescriptionDate || '-' }}
          </div>
        </div>

        <div class="section-divider">
          <span>患者信息</span>
        </div>
        <el-descriptions
          :column="2"
          border
          size="small"
        >
          <el-descriptions-item label="患者">
            {{ prescriptionForm.patient?.name || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="年龄/性别">
            {{ patientAgeText(prescriptionForm.patient) }} / {{ patientSexText(prescriptionForm.patient?.sex) }}
          </el-descriptions-item>
          <el-descriptions-item label="既往病史">
            {{ prescriptionForm.patient?.medicalHistory || '无' }}
          </el-descriptions-item>
          <el-descriptions-item label="过敏史">
            {{ prescriptionForm.patient?.allergies || '无' }}
          </el-descriptions-item>
        </el-descriptions>

        <div class="section-divider">
          <span>诊断信息</span>
        </div>
        <el-descriptions
          :column="1"
          border
          size="small"
          class="diagnosis-descriptions"
        >
          <el-descriptions-item
            label="患者主诉"
          >
            {{ prescriptionForm.medicalRecord?.appointment?.symptoms || '无' }}
          </el-descriptions-item>
          <el-descriptions-item
            label="诊断结果"
          >
            {{ prescriptionForm.diagnosis || prescriptionForm.medicalRecord?.diagnosis || '无' }}
          </el-descriptions-item>
          <el-descriptions-item
            label="处方备注"
          >
            {{ prescriptionForm.notes || '无' }}
          </el-descriptions-item>
        </el-descriptions>

        <div class="section-divider">
          <span>处方明细</span>
        </div>
        <div class="prescription-detail-list">
          <div
            v-for="(detail, index) in (prescriptionForm.details || [])"
            :key="detail.id || `${detail.medicineId || 'm'}-${index}`"
            class="prescription-detail-item"
          >
            <div class="detail-line">
              <span><strong>药品名称：</strong>{{ detail.medicine?.medicineName || '-' }}</span>
              <span><strong>对应病症：</strong>{{ detail.medicalRecordDetail?.symptomName || '-' }}</span>
              <span><strong>对应治疗方案：</strong>{{ detail.medicalRecordDetail?.treatmentPlan || '-' }}</span>
              <span><strong>数量：</strong>{{ detail.quantity ?? '-' }}</span>
            </div>
            <div class="detail-line detail-line-muted">
              <span><strong>规格：</strong>{{ detail.medicine?.specification || '-' }}</span>
              <span><strong>用量：</strong>{{ detail.dosage || '-' }}</span>
              <span><strong>频次：</strong>{{ detail.frequency || '-' }}</span>
              <span><strong>用药天数：</strong>{{ detail.days ?? '-' }}</span>
              <span><strong>用法：</strong>{{ detail.usage || '-' }}</span>
            </div>
          </div>
          <el-empty
            v-if="!prescriptionForm.details || prescriptionForm.details.length === 0"
            description="暂无处方明细"
            :image-size="60"
          />
        </div>

        <div class="view-meta-line">
          <span>
            医生信息：{{ prescriptionForm.doctor?.name || '-' }}
          </span>
          <span>
            创建时间：{{ prescriptionForm.createTime || '-' }}
          </span>
        </div>
      </div>

      <el-form
        v-else
        ref="prescriptionFormRef"
        :model="prescriptionForm"
        :rules="prescriptionFormRules"
        label-width="100px"
      >
        <el-descriptions
          v-if="prescriptionForm.patientId"
          :column="2"
          border
          size="small"
          class="patient-header"
        >
          <el-descriptions-item label="患者">
            {{ selectedPatient?.name || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="性别">
            {{ patientSexText(selectedPatient?.sex) }}
          </el-descriptions-item>
          <el-descriptions-item label="年龄">
            {{ patientAgeText(selectedPatient) }}
          </el-descriptions-item>
          <el-descriptions-item label="既往病史">
            {{ selectedPatient?.medicalHistory || '无' }}
          </el-descriptions-item>
          <el-descriptions-item label="过敏史">
            {{ selectedPatient?.allergies || '无' }}
          </el-descriptions-item>
          <el-descriptions-item
            label="患者主诉"
            :span="2"
          >
            {{ currentRecordInfo.symptoms || '无' }}
          </el-descriptions-item>
        </el-descriptions>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item
              label="患者"
              prop="patientId"
            >
              <el-select
                v-model="prescriptionForm.patientId"
                placeholder="请先选择患者"
                filterable
                style="width: 100%"
                @change="onPatientChange"
              >
                <el-option
                  v-for="patient in patientOptions"
                  :key="patient.id"
                  :label="patient.name"
                  :value="patient.id"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item
              label="就诊记录"
              prop="recordId"
            >
              <el-select
                v-model="prescriptionForm.recordId"
                placeholder="请再选择病历"
                filterable
                style="width: 100%"
                @change="onRecordChange"
              >
                <el-option
                  v-for="record in recordOptions"
                  :key="record.id"
                  :label="`${record.recordNo} (${record.recordDate})`"
                  :value="record.id"
                />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item
              label="医生"
              prop="doctorId"
            >
              <el-input
                :value="currentRecordInfo.doctorName"
                readonly
                placeholder="选择病历后自动带出"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item
              label="处方日期"
              prop="prescriptionDate"
            >
              <el-input
                :value="currentRecordInfo.recordDate"
                readonly
                placeholder="选择病历后自动带出"
              />
            </el-form-item>
          </el-col>
        </el-row>
        
        <el-form-item label="诊断结果">
          <el-input
            v-model="prescriptionForm.diagnosis"
            type="textarea"
            :rows="2"
            placeholder="自动同步病历诊断结果"
          />
        </el-form-item>
        
        <el-form-item label="处方备注">
          <el-input
            v-model="prescriptionForm.notes"
            type="textarea"
            :rows="2"
            placeholder="请输入处方备注"
          />
        </el-form-item>
        
        <el-divider>药品明细</el-divider>
        
        <div class="medicine-details">
          <div v-if="prescriptionForm.details && prescriptionForm.details.length > 0">
            <el-table
              :data="prescriptionForm.details"
              border
              style="width: 100%; margin-bottom: 20px;"
            >
              <el-table-column
                label="药品名称"
                prop="medicine.medicineName"
                width="180"
              />
              <el-table-column
                label="所属病症"
                width="160"
              >
                <template #default="scope">
                  {{ scope.row.medicalRecordDetail?.symptomName || '-' }}
                </template>
              </el-table-column>
              <el-table-column
                label="对应治疗方案"
                min-width="180"
              >
                <template #default="scope">
                  {{ scope.row.medicalRecordDetail?.treatmentPlan || '-' }}
                </template>
              </el-table-column>
              <el-table-column
                label="规格"
                prop="medicine.specification"
                width="120"
              />
              <el-table-column
                label="用量"
                prop="dosage"
                width="100"
              />
              <el-table-column
                label="频次"
                prop="frequency"
                width="100"
              />
              <el-table-column
                label="用药天数"
                prop="days"
                width="100"
              />
              <el-table-column
                label="用法"
                prop="usage"
                width="120"
              />
              <el-table-column
                label="数量"
                prop="quantity"
                width="80"
              />
              <el-table-column
                v-if="dialogType !== 'view'"
                label="操作"
                width="120"
              >
                <template #default="scope">
                  <el-button
                    type="danger"
                    size="small"
                    @click="removeMedicine(scope.$index)"
                  >
                    删除
                  </el-button>
                </template>
              </el-table-column>
            </el-table>
          </div>
          
          <div
            v-else
            class="no-medicine"
          >
            <el-empty
              description="暂无药品信息"
              :image-size="60"
            />
          </div>
          
          <div
            v-if="dialogType !== 'view'"
            class="add-medicine-form"
          >
            <el-row :gutter="20">
              <el-col :span="8">
                <el-form-item label="病症明细">
                  <el-select
                    v-model="currentMedicine.medicalRecordDetailId"
                    placeholder="请选择病症明细"
                    filterable
                  >
                    <el-option
                      v-for="d in recordDetailOptions"
                      :key="d.id"
                      :label="`${d.symptomName}${d.treatmentPlan ? ' - ' + d.treatmentPlan : ''}`"
                      :value="d.id"
                    />
                  </el-select>
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item
                  label="药品"
                  prop="currentMedicine.medicineId"
                >
                  <el-select
                    v-model="currentMedicine.medicineId"
                    placeholder="请选择药品"
                    filterable
                  >
                    <el-option
                      v-for="medicine in medicineOptions"
                      :key="medicine.id"
                      :label="`${medicine.medicineName} (${medicine.specification || ''})`"
                      :value="medicine.id"
                    />
                  </el-select>
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item
                  label="用量"
                  prop="currentMedicine.dosage"
                >
                  <el-input
                    v-model="currentMedicine.dosage"
                    placeholder="如: 5ml"
                  />
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item
                  label="频次"
                  prop="currentMedicine.frequency"
                >
                  <el-select
                    v-model="currentMedicine.frequency"
                    placeholder="请选择频次"
                  >
                    <el-option
                      label="一日一次"
                      value="一日一次"
                    />
                    <el-option
                      label="一日两次"
                      value="一日两次"
                    />
                    <el-option
                      label="一日三次"
                      value="一日三次"
                    />
                    <el-option
                      label="需要时使用"
                      value="需要时使用"
                    />
                  </el-select>
                </el-form-item>
              </el-col>
            </el-row>
            
            <el-row :gutter="20">
              <el-col :span="8">
                <el-form-item
                  label="用药天数"
                  prop="currentMedicine.days"
                >
                  <el-input-number
                    v-model="currentMedicine.days"
                    :min="1"
                    :max="30"
                  />
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item
                  label="用法"
                  prop="currentMedicine.usage"
                >
                  <el-select
                    v-model="currentMedicine.usage"
                    placeholder="请选择用法"
                  >
                    <el-option
                      label="口服"
                      value="口服"
                    />
                    <el-option
                      label="外用"
                      value="外用"
                    />
                    <el-option
                      label="注射"
                      value="注射"
                    />
                    <el-option
                      label="滴眼"
                      value="滴眼"
                    />
                  </el-select>
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item
                  label="数量"
                  prop="currentMedicine.quantity"
                >
                  <el-input-number
                    v-model="currentMedicine.quantity"
                    :min="1"
                  />
                </el-form-item>
              </el-col>
            </el-row>
            
            <div class="add-medicine-button">
              <el-button
                type="primary"
                @click="addMedicine"
              >
                添加药品
              </el-button>
            </div>
          </div>
        </div>
      </el-form>
      
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="dialogVisible = false">{{ dialogType === 'view' ? '关闭' : '取消' }}</el-button>
          <el-button
            v-if="dialogType !== 'view'"
            type="primary"
            @click="submitForm"
          >确定</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, nextTick, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import request from '@/utils/request'
import { ElMessage } from 'element-plus'
import { prescriptionStatusLabel, prescriptionStatusTagType } from '@/utils/prescriptionStatus'

// 列表数据
const tableData = ref([])
const loading = ref(false)
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(10)
const route = useRoute()
const router = useRouter()

// 搜索表单
const searchForm = reactive({
  prescriptionNo: '',
  patientName: '',
  doctorName: '',
  status: ''
})

// 日期范围
const dateRange = ref([])

// 患者、医生、就诊记录和药品选项
const patientOptions = ref([])
const recordOptions = ref([])
const medicineOptions = ref([])
const recordDetailOptions = ref([])
const currentRecordInfo = reactive({
  doctorName: '',
  recordDate: '',
  symptoms: ''
})
const selectedPatient = computed(() => patientOptions.value.find(item => item.id === prescriptionForm.patientId))

// 新增/编辑对话框
const dialogVisible = ref(false)
const dialogType = ref('add') // add, edit, view
const prescriptionFormRef = ref(null)
const prescriptionForm = reactive({
  id: null,
  patientId: null,
  patient: null,
  doctorId: null,
  doctor: null,
  recordId: null,
  medicalRecord: null,
  prescriptionNo: '',
  prescriptionDate: '',
  createTime: '',
  diagnosis: '',
  status: 0,
  notes: '',
  details: []
})

// 当前正在添加的药品
const currentMedicine = reactive({
  medicalRecordDetailId: null,
  medicineId: null,
  dosage: '',
  frequency: '',
  days: 7,
  usage: '',
  quantity: 1
})

// 表单验证规则
const prescriptionFormRules = {
  patientId: [{ required: true, message: '请选择患者', trigger: 'change' }],
  doctorId: [{ required: true, message: '请选择医生', trigger: 'change' }],
  recordId: [{ required: true, message: '请选择就诊记录', trigger: 'change' }],
  prescriptionDate: [{ required: true, message: '请选择处方日期', trigger: 'change' }]
}

// 初始化
onMounted(() => {
  fetchPatients()
  fetchMedicines()
  fetchPrescriptions()
  openAddFromRouteQuery()
})

const openAddFromRouteQuery = async () => {
  if (route.query.autoAdd !== '1') {
    return
  }
  const recordId = Number(route.query.recordId)
  if (!recordId) {
    return
  }
  dialogType.value = 'add'
  dialogVisible.value = true
  await nextTick()

  let patientId = Number(route.query.patientId)
  if (!patientId) {
    await request.get(`/medical-record/${recordId}`, {}, {
      onSuccess: (res) => {
        patientId = Number(res?.patientId || 0)
      }
    })
  }

  if (patientId) {
    prescriptionForm.patientId = patientId
    await onPatientChange(patientId)
  }
  prescriptionForm.recordId = recordId
  await onRecordChange(recordId)

  router.replace({ path: '/back/prescription' })
}

// 获取患者列表
const fetchPatients = async () => {
  try {
    await request.get('/patient/list', {}, {
      onSuccess: (res) => {
        patientOptions.value = res || []
      }
    })
  } catch (error) {
    console.error('获取患者列表失败:', error)
  }
}

// 获取药品列表
const fetchMedicines = async () => {
  try {
    await request.get('/medicine/list', {}, {
      onSuccess: (res) => {
        medicineOptions.value = res || []
      }
    })
  } catch (error) {
    console.error('获取药品列表失败:', error)
  }
}

// 获取处方列表
const fetchPrescriptions = async () => {
  loading.value = true
  try {
    const params = {
      currentPage: currentPage.value,
      size: pageSize.value
    }

    // 添加日期范围
    if (dateRange.value && dateRange.value.length === 2) {
      params.startDate = dateRange.value[0]
      params.endDate = dateRange.value[1]
    }

    // 添加其他搜索条件
    if (searchForm.prescriptionNo) {
      params.prescriptionNo = searchForm.prescriptionNo
    }
    if (searchForm.patientName) {
      params.patientName = searchForm.patientName
    }
    if (searchForm.doctorName) {
      params.doctorName = searchForm.doctorName
    }
    if (searchForm.status !== '') {
      params.status = searchForm.status
    }

    await request.get('/prescription/page', params, {
      onSuccess: (res) => {
        tableData.value = res.records
        total.value = res.total
      }
    })
  } catch (error) {
    console.error('获取处方列表失败:', error)
  } finally {
    loading.value = false
  }
}

// 搜索
const handleSearch = () => {
  currentPage.value = 1
  fetchPrescriptions()
}

// 重置搜索
const resetSearch = () => {
  searchForm.prescriptionNo = ''
  searchForm.patientName = ''
  searchForm.doctorName = ''
  searchForm.status = ''
  dateRange.value = []
  currentPage.value = 1
  fetchPrescriptions()
}

// 新增处方
const handleAdd = () => {
  dialogType.value = 'add'
  dialogVisible.value = true
  
  // 设置默认值
  prescriptionForm.prescriptionDate = ''
  prescriptionForm.status = 0
  prescriptionForm.details = []
}

// 查看处方
const handleView = async (row) => {
  dialogType.value = 'view'
  dialogVisible.value = true
  
  try {
    await request.get(`/prescription/${row.id}`, {}, {
      onSuccess: (res) => {
        Object.keys(prescriptionForm).forEach(key => {
          prescriptionForm[key] = res[key]
        })
        currentRecordInfo.doctorName = res.doctor?.name || ''
        currentRecordInfo.recordDate = res.prescriptionDate || ''
        currentRecordInfo.symptoms = res.medicalRecord?.appointment?.symptoms || ''
        recordOptions.value = res.patientId ? [{ id: res.recordId, recordNo: res.medicalRecord?.recordNo, recordDate: res.medicalRecord?.recordDate }] : []
        recordDetailOptions.value = res.medicalRecord?.details || []
      }
    })
  } catch (error) {
    console.error('获取处方详情失败:', error)
  }
}

// 提交处方（进入待取药，同步至处方取药）
const handleSubmitPrescription = async (id) => {
  try {
    await request.put(`/prescription/submit/${id}`, {}, {
      successMsg: '提交成功，已同步至处方取药',
      onSuccess: () => {
        fetchPrescriptions()
      }
    })
  } catch (error) {
    console.error('提交处方失败:', error)
  }
}

// 添加药品到处方
const addMedicine = () => {
  // 验证药品信息
  if (!currentMedicine.medicalRecordDetailId) {
    ElMessage.warning('请选择病症明细')
    return
  }
  if (!currentMedicine.medicineId) {
    ElMessage.warning('请选择药品')
    return
  }
  if (!currentMedicine.dosage) {
    ElMessage.warning('请输入用量')
    return
  }
  if (!currentMedicine.frequency) {
    ElMessage.warning('请选择频次')
    return
  }
  if (!currentMedicine.usage) {
    ElMessage.warning('请选择用法')
    return
  }
  
  // 获取选中的药品信息
  const medicine = medicineOptions.value.find(m => m.id === currentMedicine.medicineId)
  
  // 创建药品明细对象
  const detailRef = recordDetailOptions.value.find(d => d.id === currentMedicine.medicalRecordDetailId)
  const detail = {
    medicalRecordDetailId: currentMedicine.medicalRecordDetailId,
    medicalRecordDetail: detailRef,
    medicineId: currentMedicine.medicineId,
    dosage: currentMedicine.dosage,
    frequency: currentMedicine.frequency,
    days: currentMedicine.days,
    usage: currentMedicine.usage,
    quantity: currentMedicine.quantity,
    medicine: medicine // 添加药品信息，用于显示
  }
  
  // 添加到处方明细列表
  if (!prescriptionForm.details) {
    prescriptionForm.details = []
  }
  prescriptionForm.details.push(detail)
  
  // 重置当前药品表单
  currentMedicine.medicalRecordDetailId = null
  currentMedicine.medicineId = null
  currentMedicine.dosage = ''
  currentMedicine.frequency = ''
  currentMedicine.days = 7
  currentMedicine.usage = ''
  currentMedicine.quantity = 1
  
  ElMessage.success('药品添加成功')
}

// 移除药品
const removeMedicine = (index) => {
  prescriptionForm.details.splice(index, 1)
}

// 提交表单
const submitForm = () => {
  prescriptionFormRef.value.validate(async (valid) => {
    if (valid) {
      if (prescriptionForm.details.length === 0) {
        ElMessage.warning('请至少添加一种药品')
        return
      }
      
      if (dialogType.value === 'add') {
        // 新增处方
        await request.post('/prescription', prescriptionForm, {
          successMsg: '新增处方成功',
          onSuccess: () => {
            dialogVisible.value = false
            fetchPrescriptions()
          }
        })
      }
    }
  })
}

// 重置表单
const resetForm = () => {
  if (prescriptionFormRef.value) {
    prescriptionFormRef.value.resetFields()
  }
  Object.keys(prescriptionForm).forEach(key => {
    prescriptionForm[key] = key === 'id' ? null : key === 'details' ? [] : null
  })
  
  // 重置当前药品表单
  currentMedicine.medicineId = null
  currentMedicine.medicalRecordDetailId = null
  currentMedicine.dosage = ''
  currentMedicine.frequency = ''
  currentMedicine.days = 7
  currentMedicine.usage = ''
  currentMedicine.quantity = 1
  
  // 清空就诊记录选项
  recordOptions.value = []
  recordDetailOptions.value = []
  currentRecordInfo.doctorName = ''
  currentRecordInfo.recordDate = ''
  currentRecordInfo.symptoms = ''
}

const onPatientChange = async (patientId) => {
  prescriptionForm.recordId = null
  prescriptionForm.doctorId = null
  prescriptionForm.prescriptionDate = ''
  currentRecordInfo.doctorName = ''
  currentRecordInfo.recordDate = ''
  currentRecordInfo.symptoms = ''
  recordDetailOptions.value = []
  if (!patientId) {
    recordOptions.value = []
    return
  }
  await request.get('/medical-record/page', {
    patientId,
    currentPage: 1,
    size: 999
  }, {
    onSuccess: (res) => {
      recordOptions.value = res.records || []
    }
  })
}

const onRecordChange = async (recordId) => {
  if (!recordId) {
    prescriptionForm.doctorId = null
    prescriptionForm.prescriptionDate = ''
    currentRecordInfo.doctorName = ''
    currentRecordInfo.recordDate = ''
    currentRecordInfo.symptoms = ''
    recordDetailOptions.value = []
    return
  }
  await request.get(`/medical-record/${recordId}`, {}, {
    onSuccess: (res) => {
      prescriptionForm.patientId = res.patientId || prescriptionForm.patientId
      prescriptionForm.doctorId = res.doctorId || null
      prescriptionForm.prescriptionDate = res.recordDate || ''
      prescriptionForm.diagnosis = res.diagnosis || ''
      currentRecordInfo.doctorName = res.doctor?.name || ''
      currentRecordInfo.recordDate = res.recordDate || ''
      currentRecordInfo.symptoms = res.appointment?.symptoms || ''
      recordDetailOptions.value = res.details || []
    }
  })
}

// 格式化日期
const formatDate = (date) => {
  const d = new Date(date)
  const year = d.getFullYear()
  const month = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

// 分页操作
const handleSizeChange = (val) => {
  pageSize.value = val
  fetchPrescriptions()
}

const handleCurrentChange = (val) => {
  currentPage.value = val
  fetchPrescriptions()
}

const patientSexText = (sex) => sex || '未知'

const patientAgeText = (patient) => {
  if (!patient) return '未知'
  if (patient.age !== null && patient.age !== undefined) return `${patient.age}岁`
  if (!patient.birthday) return '未知'
  const birth = new Date(patient.birthday)
  if (Number.isNaN(birth.getTime())) return '未知'
  const currentYear = new Date().getFullYear()
  return `${Math.max(currentYear - birth.getFullYear(), 0)}岁`
}
</script>

<style scoped>
.prescription-management {
  padding: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.search-form {
  margin-bottom: 20px;
}

.pagination-container {
  margin-top: 20px;
  text-align: right;
}

.ellipsis {
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 180px;
}

.patient-info-cell {
  line-height: 1.5;
}

.patient-header {
  margin-bottom: 16px;
}

.medicine-details {
  margin-top: 20px;
}

.no-medicine {
  padding: 20px;
  text-align: center;
}

.add-medicine-form {
  margin-top: 20px;
  padding: 20px;
  border: 1px dashed #dcdfe6;
  border-radius: 4px;
}

.add-medicine-button {
  margin-top: 20px;
  text-align: center;
}

.text-muted {
  color: #909399;
  font-size: 12px;
}

.prescription-view-detail {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.section-divider {
  display: flex;
  align-items: center;
  margin: 8px 0 2px;
  color: #303133;
  font-weight: 600;
}

.section-divider::before,
.section-divider::after {
  content: "";
  flex: 1;
  height: 1px;
  background-color: #dcdfe6;
}

.section-divider::before {
  margin-right: 12px;
}

.section-divider::after {
  margin-left: 12px;
}

.view-header-line {
  margin-bottom: 4px;
  color: #606266;
  line-height: 1.8;
}

.view-meta-line {
  margin-top: 6px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px 14px;
  color: #606266;
  font-size: 13px;
}

:deep(.detail-table th) {
  background-color: #f5f7fa !important;
  font-weight: 600;
  color: #606266;
  font-size: 13px;
  padding: 8px 10px !important;
}

:deep(.detail-table td) {
  font-size: 13px;
  color: #606266;
  line-height: 1.5;
  padding: 8px 10px !important;
  vertical-align: top;
}

:deep(.detail-table .cell) {
  white-space: pre-wrap;
  word-break: break-word;
  line-height: 1.5;
}

:deep(.prescription-view-detail .el-descriptions__label) {
  font-size: 13px;
  padding: 8px 10px !important;
}

:deep(.prescription-view-detail .el-descriptions__content) {
  font-size: 13px;
  line-height: 1.5;
  padding: 8px 10px !important;
}

:deep(.diagnosis-descriptions .el-descriptions__table) {
  table-layout: fixed;
}

:deep(.diagnosis-descriptions .el-descriptions__label) {
  width: 96px;
  min-width: 96px;
  max-width: 96px;
  white-space: normal;
}

.prescription-detail-list {
  border: 1px solid #ebeef5;
  border-radius: 4px;
  overflow: hidden;
}

.prescription-detail-item {
  padding: 10px;
  border-bottom: 1px solid #ebeef5;
}

.prescription-detail-item:last-child {
  border-bottom: none;
}

.detail-line {
  display: flex;
  flex-wrap: wrap;
  gap: 12px 18px;
  font-size: 13px;
  line-height: 1.5;
  color: #303133;
}

.detail-line + .detail-line {
  margin-top: 6px;
}

.detail-line-muted {
  color: #606266;
  font-size: 12px;
}
</style> 