<template>
  <div class="medical-record-management">
    <el-card class="box-card">
      <template #header>
        <div class="card-header">
          <h3>病历管理</h3>
          <el-button
            type="primary"
            @click="handleAdd"
          >
            新增病历
          </el-button>
        </div>
      </template>

      <!-- 搜索表单 -->
      <el-form
        :model="searchForm"
        :inline="true"
        class="search-form"
      >
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
        <el-form-item label="就诊日期">
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
              label="未提交"
              :value="0"
            />
            <el-option
              label="已提交"
              :value="1"
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

      <!-- 病历列表 -->
      <el-table
        v-loading="loading"
        :data="tableData"
        border
        style="width: 100%"
      >
        <el-table-column
          prop="recordNo"
          label="记录编号"
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
          label="科室"
          width="120"
        >
          <template #default="scope">
            {{ scope.row.doctor?.department?.deptName || '未知科室' }}
          </template>
        </el-table-column>
        <el-table-column
          prop="recordDate"
          label="就诊日期"
          width="120"
          sortable
        />
        <el-table-column
          label="主诉"
          min-width="180"
        >
          <template #default="scope">
            <div
              v-if="scope.row.appointment?.symptoms"
              class="ellipsis"
            >
              {{ scope.row.appointment.symptoms }}
            </div>
            <span v-else>无</span>
          </template>
        </el-table-column>
        <el-table-column
          prop="status"
          label="状态"
          width="100"
        >
          <template #default="scope">
            <el-tag :type="scope.row.status === 1 ? 'success' : 'info'">
              {{ scope.row.status === 1 ? '已提交' : '未提交' }}
            </el-tag>
          </template>
        </el-table-column>
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
            <span v-else>暂无诊断</span>
          </template>
        </el-table-column>
        <el-table-column
          label="操作"
          width="220"
          fixed="right"
        >
          <template #default="scope">
            <div class="action-row">
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
                @click="handlePrescription(scope.row)"
              >
                处方
              </el-button>
            </div>
            <div
              v-if="scope.row.status !== 1"
              class="action-row"
            >
              <el-button
                type="success"
                size="small"
                @click="handleSubmit(scope.row)"
              >
                提交
              </el-button>
              <el-button
                type="warning"
                size="small"
                @click="handleEdit(scope.row)"
              >
                编辑
              </el-button>
              <el-popconfirm
                title="确定删除此病历吗？"
                @confirm="handleDelete(scope.row.id)"
              >
                <template #reference>
                  <el-button
                    type="danger"
                    size="small"
                  >
                    删除
                  </el-button>
                </template>
              </el-popconfirm>
            </div>
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

    <!-- 新增/编辑病历对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogType === 'add' ? '新增病历' : dialogType === 'edit' ? '编辑病历' : '查看病历'"
      width="650px"
      @closed="resetForm"
    >
      <div
        v-if="dialogType === 'view'"
        class="record-view-detail"
      >
        <div class="view-header-line">
          <div>
            记录编号：{{ recordForm.recordNo || '-' }} ｜ 就诊日期：{{ recordForm.recordDate || '-' }}
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
            {{ recordForm.patient?.name || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="年龄/性别">
            {{ patientAgeText(recordForm.patient) }} / {{ patientSexText(recordForm.patient?.sex) }}
          </el-descriptions-item>
          <el-descriptions-item label="既往病史">
            {{ recordForm.patient?.medicalHistory || '无' }}
          </el-descriptions-item>
          <el-descriptions-item label="过敏史">
            {{ recordForm.patient?.allergies || '无' }}
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
            {{ recordForm.appointment?.symptoms || '无' }}
          </el-descriptions-item>
          <el-descriptions-item label="诊断结果">
            {{ recordForm.diagnosis || '无' }}
          </el-descriptions-item>
          <el-descriptions-item label="治疗方案">
            {{ recordForm.treatment || '无' }}
          </el-descriptions-item>
          <el-descriptions-item label="医生备注">
            {{ recordForm.notes || '无' }}
          </el-descriptions-item>
        </el-descriptions>

        <div class="section-divider">
          <span>病症明细</span>
        </div>
        <div class="record-detail-list">
          <div
            v-for="(detail, index) in (recordForm.details || [])"
            :key="detail.id || `d-${index}`"
            class="record-detail-item"
          >
            <div class="detail-line">
              <span><strong>病症名称：</strong>{{ detail.symptomName || '-' }}</span>
            </div>
            <div class="detail-line detail-line-muted">
              <span><strong>对应治疗方案：</strong>{{ detail.treatmentPlan || '-' }}</span>
            </div>
          </div>
          <el-empty
            v-if="!recordForm.details || recordForm.details.length === 0"
            description="暂无病症明细"
            :image-size="60"
          />
        </div>

        <div class="view-meta-line">
          <span>
            医生信息：{{ recordForm.doctor?.name || '-' }}<span v-if="recordForm.doctor?.title">（{{ recordForm.doctor.title }}）</span><span v-if="recordForm.doctor?.department?.deptName"> - {{ recordForm.doctor.department.deptName }}</span>
          </span>
          <span>
            创建时间：{{ recordForm.createTime || '-' }}
          </span>
        </div>
      </div>

      <el-form
        v-else
        ref="recordFormRef"
        :model="recordForm"
        :rules="recordFormRules"
        label-width="100px"
      >
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item
              label="患者"
              prop="patientId"
            >
              <el-select
                v-model="recordForm.patientId"
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
              label="关联预约"
              prop="appointmentId"
            >
              <el-select
                v-model="recordForm.appointmentId"
                placeholder="请再选择预约"
                filterable
                clearable
                style="width: 100%"
                @change="onAppointmentChange"
              >
                <el-option
                  v-for="appointment in appointmentOptions"
                  :key="appointment.id"
                  :label="`${appointment.appointmentNo} (${appointment.appointmentDate} ${appointment.timeSlot})`"
                  :value="appointment.id"
                />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="患者主诉">
          <el-input
            :value="currentAppointmentInfo.symptoms"
            type="textarea"
            :rows="2"
            readonly
            placeholder="选择预约后自动带出"
          />
        </el-form-item>
        
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item
              label="医生"
              prop="doctorId"
            >
              <el-input
                :value="currentAppointmentInfo.doctorName"
                readonly
                placeholder="选择预约后自动带出"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item
              label="就诊日期"
              prop="recordDate"
            >
              <el-input
                :value="currentAppointmentInfo.recordDate"
                readonly
                placeholder="选择预约后自动带出"
              />
            </el-form-item>
          </el-col>
        </el-row>
        
        <el-form-item
          label="诊断结果"
          prop="diagnosis"
        >
          <el-input
            v-model="recordForm.diagnosis"
            type="textarea"
            :rows="3"
            placeholder="请输入诊断结果"
          />
        </el-form-item>
        
        <el-form-item
          label="治疗方案"
          prop="treatment"
        >
          <el-input
            v-model="recordForm.treatment"
            type="textarea"
            :rows="3"
            placeholder="请输入治疗方案"
          />
        </el-form-item>

        <el-divider>病症明细（每条可配置治疗方案）</el-divider>
        <div
          v-if="dialogType !== 'view'"
          class="detail-toolbar"
        >
          <el-button
            type="primary"
            plain
            @click="addDetail"
          >
            添加病症明细
          </el-button>
        </div>
        <el-table
          :data="recordForm.details"
          border
          style="width: 100%; margin-bottom: 12px;"
        >
          <el-table-column
            label="病症名称"
            min-width="180"
          >
            <template #default="scope">
              <el-input
                v-if="dialogType !== 'view'"
                v-model="scope.row.symptomName"
                placeholder="请输入病症名称"
              />
              <span v-else>{{ scope.row.symptomName || '-' }}</span>
            </template>
          </el-table-column>
          <el-table-column
            label="治疗方案"
            min-width="260"
          >
            <template #default="scope">
              <el-input
                v-if="dialogType !== 'view'"
                v-model="scope.row.treatmentPlan"
                placeholder="请输入对应治疗方案"
              />
              <span v-else>{{ scope.row.treatmentPlan || '-' }}</span>
            </template>
          </el-table-column>
          <el-table-column
            v-if="dialogType !== 'view'"
            label="操作"
            width="100"
          >
            <template #default="scope">
              <el-button
                type="danger"
                size="small"
                @click="removeDetail(scope.$index)"
              >
                删除
              </el-button>
            </template>
          </el-table-column>
        </el-table>
        
        <el-form-item label="医生备注">
          <el-input
            v-model="recordForm.notes"
            type="textarea"
            :rows="2"
            placeholder="请输入医生备注"
          />
        </el-form-item>
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

    <!-- 处方管理对话框 -->
    <el-dialog
      v-model="prescriptionDialogVisible"
      title="处方管理"
      width="800px"
    >
      <div
        v-if="currentRecord.id"
        class="prescription-container"
      >
        <div class="record-info">
          <h4>就诊信息</h4>
          <el-descriptions
            :column="3"
            border
            size="small"
          >
            <el-descriptions-item label="患者">
              {{ currentRecord.patient?.name }}
            </el-descriptions-item>
            <el-descriptions-item label="医生">
              {{ currentRecord.doctor?.name }}
            </el-descriptions-item>
            <el-descriptions-item label="就诊日期">
              {{ currentRecord.recordDate }}
            </el-descriptions-item>
            <el-descriptions-item
              label="诊断结果"
              :span="3"
            >
              {{ currentRecord.diagnosis || '无' }}
            </el-descriptions-item>
            <el-descriptions-item label="既往病史">
              {{ currentRecord.patient?.medicalHistory || '无' }}
            </el-descriptions-item>
            <el-descriptions-item label="过敏史">
              {{ currentRecord.patient?.allergies || '无' }}
            </el-descriptions-item>
            <el-descriptions-item
              label="患者主诉"
              :span="3"
            >
              {{ currentRecord.appointment?.symptoms || '无' }}
            </el-descriptions-item>
          </el-descriptions>
        </div>

        <div class="prescription-list">
          <div class="section-header">
            <h4>处方列表</h4>
            <el-button
              type="primary"
              size="small"
              @click="handleAddPrescription"
            >
              新增处方
            </el-button>
          </div>
          
          <el-table
            v-loading="prescriptionLoading"
            :data="prescriptionList"
            border
            style="width: 100%"
          >
            <el-table-column
              prop="prescriptionNo"
              label="处方编号"
              width="180"
            />
            <el-table-column
              prop="prescriptionDate"
              label="处方日期"
              width="120"
            />
            <el-table-column
              prop="diagnosis"
              label="诊断"
              width="200"
            >
              <template #default="scope">
                <div
                  v-if="scope.row.diagnosis"
                  class="ellipsis"
                >
                  {{ scope.row.diagnosis }}
                </div>
                <span v-else>同病历</span>
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
              label="操作"
              width="250"
            >
              <template #default="scope">
                <el-button
                  type="primary"
                  size="small"
                  @click="handleViewPrescription(scope.row)"
                >
                  查看
                </el-button>
                <el-button 
                  v-if="scope.row.status === 0" 
                  type="success" 
                  size="small" 
                  @click="handleSubmitPrescriptionFromRecord(scope.row.id)"
                >
                  提交
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import request from '@/utils/request'
import { ElMessage, ElMessageBox } from 'element-plus'
import { prescriptionStatusLabel, prescriptionStatusTagType } from '@/utils/prescriptionStatus'

// 列表数据
const tableData = ref([])
const loading = ref(false)
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(10)
const router = useRouter()
const route = useRoute()

// 搜索表单
const searchForm = reactive({
  patientName: '',
  doctorName: '',
  status: ''
})

// 日期范围
const dateRange = ref([])

// 患者和医生选项
const patientOptions = ref([])
const doctorOptions = ref([])
const appointmentOptions = ref([])
const currentAppointmentInfo = reactive({
  doctorName: '',
  recordDate: '',
  symptoms: ''
})

// 新增/编辑对话框
const dialogVisible = ref(false)
const dialogType = ref('add') // add, edit, view
const recordFormRef = ref(null)
const recordForm = reactive({
  id: null,
  patientId: null,
  patient: null,
  doctorId: null,
  doctor: null,
  appointmentId: null,
  appointment: null,
  recordNo: '',
  diagnosis: '',
  treatment: '',
  details: [],
  recordDate: '',
  createTime: '',
  notes: '',
  followUp: null
})

// 表单验证规则
const recordFormRules = {
  patientId: [{ required: true, message: '请选择患者', trigger: 'change' }],
  doctorId: [{ required: true, message: '请选择医生', trigger: 'change' }],
  appointmentId: [{ required: true, message: '请选择关联预约', trigger: 'change' }],
  recordDate: [{ required: true, message: '请选择就诊日期', trigger: 'change' }],
  diagnosis: [{ required: true, message: '请输入诊断结果', trigger: 'blur' }]
}

// 处方相关
const prescriptionDialogVisible = ref(false)
const prescriptionLoading = ref(false)
const prescriptionList = ref([])
const currentRecord = reactive({})

// 初始化
onMounted(async () => {
  await Promise.all([fetchPendingOutpatientPatients(), fetchDoctors()])
  fetchMedicalRecords()
  initCreateFromRoute()
})

// 获取有门诊预约且未完成就诊(已就诊)的患者列表
const fetchPendingOutpatientPatients = async () => {
  try {
    await request.get('/appointment/page', {
      currentPage: 1,
      size: 1000,
      status: 2
    }, {
      onSuccess: (res) => {
        const records = res?.records || []
        const patientMap = new Map()
        records.forEach((item) => {
          const patient = item?.patient
          if (patient?.id && !patientMap.has(patient.id)) {
            patientMap.set(patient.id, patient)
          }
        })
        patientOptions.value = Array.from(patientMap.values())
      }
    })
  } catch (error) {
    console.error('获取待完成就诊患者失败:', error)
  }
}

// 获取医生列表
const fetchDoctors = async () => {
  try {
    await request.get('/doctor/list', {}, {
      onSuccess: (res) => {
        doctorOptions.value = res || []
      }
    })
  } catch (error) {
    console.error('获取医生列表失败:', error)
  }
}

// 获取病历列表
const fetchMedicalRecords = async () => {
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

    // 添加患者姓名和医生姓名
    if (searchForm.patientName) {
      params.patientName = searchForm.patientName
    }
    if (searchForm.doctorName) {
      params.doctorName = searchForm.doctorName
    }
    if (searchForm.status !== '') {
      params.status = searchForm.status
    }

    await request.get('/medical-record/page', params, {
      onSuccess: (res) => {
        tableData.value = res.records
        total.value = res.total
      }
    })
  } catch (error) {
    console.error('获取病历列表失败:', error)
  } finally {
    loading.value = false
  }
}

// 搜索
const handleSearch = () => {
  currentPage.value = 1
  fetchMedicalRecords()
}

// 重置搜索
const resetSearch = () => {
  searchForm.patientName = ''
  searchForm.doctorName = ''
  searchForm.status = ''
  dateRange.value = []
  currentPage.value = 1
  fetchMedicalRecords()
}

// 新增病历
const handleAdd = () => {
  dialogType.value = 'add'
  dialogVisible.value = true
  
  // 设置默认值
  recordForm.recordDate = ''
}

const initCreateFromRoute = async () => {
  if (route.query.autoAdd !== '1') {
    return
  }
  handleAdd()
  const patientId = Number(route.query.patientId)
  const appointmentId = Number(route.query.appointmentId)
  if (patientId) {
    await ensurePatientOption(patientId)
    recordForm.patientId = patientId
    await fetchAppointments()
  }
  if (appointmentId) {
    recordForm.appointmentId = appointmentId
    onAppointmentChange(appointmentId)
  }
}

// 编辑病历
const handleEdit = (row) => {
  dialogType.value = 'edit'
  openRecordDetail(row.id)
}

// 查看病历
const handleView = (row) => {
  dialogType.value = 'view'
  openRecordDetail(row.id)
}

// 处理处方
const handlePrescription = async (row) => {
  await request.get(`/medical-record/${row.id}`, {}, {
    onSuccess: (res) => {
      Object.assign(currentRecord, res || {})
    }
  })
  
  // 显示处方对话框
  prescriptionDialogVisible.value = true
  
  // 加载处方列表
  await fetchPrescriptions(row.id)
}

// 获取病历关联的处方列表
const fetchPrescriptions = async (recordId) => {
  prescriptionLoading.value = true
  try {
    await request.get(`/prescription/record/${recordId}`, {}, {
      onSuccess: (res) => {
        prescriptionList.value = res || []
      }
    })
  } catch (error) {
    console.error('获取处方列表失败:', error)
  } finally {
    prescriptionLoading.value = false
  }
}

// 新增处方
const handleAddPrescription = () => {
  if (!currentRecord.id) {
    ElMessage.warning('请先选择有效病历')
    return
  }
  prescriptionDialogVisible.value = false
  router.push({
    path: '/back/prescription',
    query: {
      autoAdd: '1',
      recordId: String(currentRecord.id),
      patientId: currentRecord.patientId ? String(currentRecord.patientId) : ''
    }
  })
}

// 查看处方详情
const handleViewPrescription = (prescription) => {
  // TODO: 跳转到处方详情页面
  ElMessageBox.alert(
    `处方编号: ${prescription.prescriptionNo}<br>
     处方日期: ${prescription.prescriptionDate}<br>
     诊断: ${prescription.diagnosis || '同病历'}<br>
     状态: ${prescriptionStatusLabel(prescription.status)}<br>
     药品数量: ${prescription.details?.length || 0}种`,
    '处方详情',
    {
      dangerouslyUseHTMLString: true,
      confirmButtonText: '确定'
    }
  )
}

// 提交处方至药房取药
const handleSubmitPrescriptionFromRecord = async (id) => {
  try {
    await request.put(`/prescription/submit/${id}`, {}, {
      successMsg: '提交成功，已同步至处方取药',
      onSuccess: () => {
        fetchPrescriptions(currentRecord.id)
      }
    })
  } catch (error) {
    console.error('提交处方失败:', error)
  }
}

// 删除病历
const handleDelete = async (id) => {
  try {
    await request.delete(`/medical-record/${id}`, {
      successMsg: '删除成功',
      onSuccess: () => {
        fetchMedicalRecords()
      }
    })
  } catch (error) {
    console.error('删除病历失败:', error)
  }
}

const handleSubmit = async (row) => {
  await ElMessageBox.confirm(`确定提交病历 ${row.recordNo} 吗？提交后不可编辑和删除。`, '提示', { type: 'warning' })
  await request.put(`/medical-record/submit/${row.id}`, {}, {
    successMsg: '病历提交成功',
    onSuccess: () => {
      fetchMedicalRecords()
    }
  })
}

// 提交表单
const submitForm = () => {
  recordFormRef.value.validate(async (valid) => {
    if (valid) {
      if (dialogType.value === 'add') {
        // 新增病历
        const payload = {
          ...recordForm,
          details: (recordForm.details || []).filter(d => d?.symptomName)
        }
        await request.post('/medical-record', payload, {
          successMsg: '新增病历成功',
          onSuccess: () => {
            dialogVisible.value = false
            fetchMedicalRecords()
          }
        })
      } else {
        // 编辑病历
        const payload = {
          ...recordForm,
          details: (recordForm.details || []).filter(d => d?.symptomName)
        }
        await request.put(`/medical-record/${recordForm.id}`, payload, {
          successMsg: '编辑病历成功',
          onSuccess: () => {
            dialogVisible.value = false
            fetchMedicalRecords()
          }
        })
      }
    }
  })
}

// 重置表单
const resetForm = () => {
  if (recordFormRef.value) {
    recordFormRef.value.resetFields()
  }
  Object.keys(recordForm).forEach(key => {
    recordForm[key] = key === 'details' ? [] : null
  })
  
  // 清空预约选项
  appointmentOptions.value = []
  currentAppointmentInfo.doctorName = ''
  currentAppointmentInfo.recordDate = ''
  currentAppointmentInfo.symptoms = ''
}

const openRecordDetail = async (id) => {
  await request.get(`/medical-record/${id}`, {}, {
    onSuccess: async (res) => {
      await ensurePatientOption(res.patientId)
      Object.keys(recordForm).forEach(key => {
        recordForm[key] = key === 'details' ? (res.details || []) : res[key]
      })
      currentAppointmentInfo.doctorName = res.doctor?.name || ''
      currentAppointmentInfo.recordDate = res.recordDate || ''
      currentAppointmentInfo.symptoms = res.appointment?.symptoms || ''
      await fetchAppointments()
      dialogVisible.value = true
    }
  })
}

const ensurePatientOption = async (patientId) => {
  if (!patientId) return
  if (patientOptions.value.some(item => item.id === patientId)) return
  try {
    await request.get(`/patient/${patientId}`, {}, {
      onSuccess: (res) => {
        if (res?.id) {
          patientOptions.value = [...patientOptions.value, res]
        }
      }
    })
  } catch (error) {
    console.error('获取患者详情失败:', error)
  }
}

const addDetail = () => {
  recordForm.details.push({
    symptomName: '',
    treatmentPlan: ''
  })
}

const removeDetail = (index) => {
  recordForm.details.splice(index, 1)
}

const fetchAppointments = async () => {
  if (!recordForm.patientId) {
    appointmentOptions.value = []
    return
  }
  
  try {
    const params = {
      patientId: recordForm.patientId,
      currentPage: 1,
      size: 200,
      status: 2
    }
    
    await request.get('/appointment/page', params, {
      onSuccess: (res) => {
        appointmentOptions.value = res.records || []
      }
    })
  } catch (error) {
    console.error('获取预约列表失败:', error)
  }
}

const onPatientChange = async () => {
  recordForm.appointmentId = null
  recordForm.doctorId = null
  recordForm.recordDate = ''
  currentAppointmentInfo.doctorName = ''
  currentAppointmentInfo.recordDate = ''
  currentAppointmentInfo.symptoms = ''
  await fetchAppointments()
}

const onAppointmentChange = (appointmentId) => {
  if (!appointmentId) {
    recordForm.doctorId = null
    recordForm.recordDate = ''
    currentAppointmentInfo.doctorName = ''
    currentAppointmentInfo.recordDate = ''
    currentAppointmentInfo.symptoms = ''
    return
  }
  const appt = appointmentOptions.value.find(item => item.id === appointmentId)
  if (!appt) return
  recordForm.doctorId = appt.doctorId || null
  recordForm.recordDate = appt.appointmentDate || ''
  currentAppointmentInfo.recordDate = appt.appointmentDate || ''
  currentAppointmentInfo.symptoms = appt.symptoms || ''
  const doctor = doctorOptions.value.find(item => item.id === appt.doctorId)
  currentAppointmentInfo.doctorName = doctor?.name || ''
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
  fetchMedicalRecords()
}

const handleCurrentChange = (val) => {
  currentPage.value = val
  fetchMedicalRecords()
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
.medical-record-management {
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

.text-muted {
  color: #909399;
  font-size: 12px;
}

.prescription-container {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}

.record-info {
  margin-bottom: 10px;
}

.detail-toolbar {
  margin-bottom: 10px;
}

.action-row {
  display: flex;
  gap: 6px;
  margin-bottom: 4px;
}

.record-view-detail {
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

:deep(.record-view-detail .el-descriptions__label) {
  font-size: 13px;
  padding: 8px 10px !important;
}

:deep(.record-view-detail .el-descriptions__content) {
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

.record-detail-list {
  border: 1px solid #ebeef5;
  border-radius: 4px;
  overflow: hidden;
}

.record-detail-item {
  padding: 10px;
  border-bottom: 1px solid #ebeef5;
}

.record-detail-item:last-child {
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