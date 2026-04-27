<template>
  <div class="my-medical-records">
    <el-card
      class="records-card"
      shadow="hover"
    >
      <!-- 搜索表单 -->
      <div class="search-container">
        <div class="search-header">
          <div class="header-icon">
            <el-icon><Search /></el-icon>
          </div>
          <h4>查询记录</h4>
        </div>
        <el-form
          :model="searchForm"
          :inline="true"
          class="search-form"
        >
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
          <el-form-item label="医生姓名">
            <el-input
              v-model="searchForm.doctorName"
              placeholder="请输入医生姓名"
              clearable
            />
          </el-form-item>
          <el-form-item>
            <el-button
              type="primary"
              round
              @click="handleSearch"
            >
              <el-icon><Search /></el-icon>搜索
            </el-button>
            <el-button
              round
              @click="resetSearch"
            >
              <el-icon><Refresh /></el-icon>重置
            </el-button>
          </el-form-item>
        </el-form>
      </div>

      <!-- 病历处方列表 -->
      <div
        v-if="loading"
        class="loading-container"
      >
        <el-skeleton
          :rows="5"
          animated
        />
      </div>
      
      <div
        v-else-if="medicalRecords.length === 0"
        class="empty-container"
      >
        <el-empty
          description="暂无病历处方记录"
          :image-size="120"
        >
          <template #image>
            <el-icon class="empty-icon">
              <FirstAidKit />
            </el-icon>
          </template>
        </el-empty>
      </div>
      
      <div
        v-else
        class="records-container"
      >
        <el-timeline>
          <el-timeline-item
            v-for="record in medicalRecords"
            :key="record.id"
            :timestamp="record.recordDate"
            placement="top"
            :type="getTimelineItemType(record)"
          >
            <el-card class="record-card">
              <template #header>
                <div class="record-header">
                  <div class="record-title">
                    <span class="record-no">{{ record.recordNo }}</span>
                    <el-tag
                      size="small"
                      effect="plain"
                    >
                      {{ record.doctor?.department?.deptName || '未知科室' }}
                    </el-tag>
                  </div>
                  <div class="record-actions">
                    <el-button
                      type="primary"
                      size="small"
                      @click="handleViewRecord(record)"
                    >
                      查看详情
                    </el-button>
                  </div>
                </div>
              </template>
              
              <el-descriptions
                :column="2"
                border
                size="small"
              >
                <el-descriptions-item label="就诊医生">
                  {{ record.doctor?.name || '未知' }}
                  <el-tag size="small">
                    {{ record.doctor?.title || '' }}
                  </el-tag>
                </el-descriptions-item>
                <el-descriptions-item label="就诊日期">
                  {{ record.recordDate }}
                </el-descriptions-item>
                <el-descriptions-item
                  label="诊断结果"
                  :span="2"
                >
                  {{ record.diagnosis || '暂无诊断' }}
                </el-descriptions-item>
                <el-descriptions-item
                  label="治疗方案"
                  :span="2"
                >
                  {{ record.treatment || '暂无治疗方案' }}
                </el-descriptions-item>
                <el-descriptions-item
                  label="病症明细"
                  :span="2"
                >
                  <div v-if="record.details && record.details.length > 0">
                    <div
                      v-for="d in record.details"
                      :key="d.id"
                    >
                      {{ d.symptomName }}：{{ d.treatmentPlan || '无治疗方案' }}
                    </div>
                  </div>
                  <span v-else>暂无病症明细</span>
                </el-descriptions-item>
                <el-descriptions-item
                  v-if="record.followUp"
                  label="随访日期"
                >
                  {{ record.followUp }}
                </el-descriptions-item>
              </el-descriptions>
              
              <div
                v-if="record.prescriptions && record.prescriptions.length > 0"
                class="prescription-section"
              >
                <div class="section-divider">
                  <span>处方信息</span>
                </div>
                
                <el-collapse>
                  <el-collapse-item 
                    v-for="prescription in record.prescriptions" 
                    :key="prescription.id"
                    :title="`处方 ${prescription.prescriptionNo} (${prescription.prescriptionDate})`"
                  >
                    <div class="prescription-info">
                        <div class="prescription-header">
                        <div>
                          <el-tag :type="prescriptionStatusTagType(prescription.status)">
                            {{ prescriptionStatusLabel(prescription.status) }}
                          </el-tag>
                        </div>
                        <div>
                          <el-button
                            type="primary"
                            size="small"
                            @click="handleViewPrescription(prescription)"
                          >
                            查看处方详情
                          </el-button>
                        </div>
                      </div>
                      
                      <div
                        v-if="prescription.details && prescription.details.length > 0"
                        class="medicine-list"
                      >
                        <el-table
                          :data="prescription.details"
                          border
                          style="width: 100%"
                        >
                          <el-table-column
                            label="病症"
                            min-width="140"
                          >
                            <template #default="scope">
                              {{ scope.row.medicalRecordDetail?.symptomName || '-' }}
                            </template>
                          </el-table-column>
                          <el-table-column
                            prop="medicine.medicineName"
                            label="药品名称"
                          />
                          <el-table-column
                            prop="dosage"
                            label="用量"
                          />
                          <el-table-column
                            prop="frequency"
                            label="频次"
                          />
                          <el-table-column
                            prop="days"
                            label="用药天数"
                          />
                          <el-table-column
                            prop="usage"
                            label="用法"
                          />
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
                    </div>
                  </el-collapse-item>
                </el-collapse>
              </div>
            </el-card>
          </el-timeline-item>
        </el-timeline>
      </div>

      <!-- 分页 -->
      <div
        v-if="medicalRecords.length > 0"
        class="pagination-container"
      >
        <el-pagination
          :current-page="currentPage"
          :page-size="pageSize"
          :page-sizes="[5, 10, 20, 50]"
          background
          layout="total, sizes, prev, pager, next, jumper"
          :total="total"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
    </el-card>
    
    <!-- 健康提示卡片 -->
    <el-card
      class="health-tips-card"
      shadow="hover"
    >
      <div class="tips-header">
        <el-icon><InfoFilled /></el-icon>
        <h4>健康小贴士</h4>
      </div>
      <ul class="tips-list">
        <li>定期查看您的病历处方记录，有助于了解自己的健康状况变化</li>
        <li>按时服药、遵医嘱治疗是康复的关键</li>
        <li>建议保存您的历史病历处方记录，方便日后就医参考</li>
        <li>如有疑问，请随时咨询您的主治医生</li>
      </ul>
    </el-card>

    <!-- 病历详情对话框 -->
    <el-dialog
      v-model="recordDialogVisible"
      title="病历详情"
      width="700px"
      class="record-dialog"
    >
      <div
        v-if="currentRecord.id"
        class="record-view-detail"
      >
        <div class="view-header-line">
          <div>
            记录编号：{{ currentRecord.recordNo || '-' }} ｜ 就诊日期：{{ currentRecord.recordDate || '-' }}
          </div>
        </div>

        <div class="section-divider section-divider-detail">
          <span>患者信息</span>
        </div>
        <el-descriptions :column="2" border size="small">
          <el-descriptions-item label="患者">
            {{ currentRecord.patient?.name || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="年龄/性别">
            {{ patientAgeText(currentRecord.patient) }} / {{ currentRecord.patient?.sex || '未知' }}
          </el-descriptions-item>
          <el-descriptions-item label="既往病史">
            {{ currentRecord.patient?.medicalHistory || '无' }}
          </el-descriptions-item>
          <el-descriptions-item label="过敏史">
            {{ currentRecord.patient?.allergies || '无' }}
          </el-descriptions-item>
        </el-descriptions>

        <div class="section-divider section-divider-detail">
          <span>诊断信息</span>
        </div>
        <el-descriptions :column="1" border size="small" class="diagnosis-descriptions">
          <el-descriptions-item label="患者主诉">
            {{ currentRecord.appointment?.symptoms || '无' }}
          </el-descriptions-item>
          <el-descriptions-item label="诊断结果">
            {{ currentRecord.diagnosis || '暂无诊断' }}
          </el-descriptions-item>
          <el-descriptions-item label="治疗方案">
            {{ currentRecord.treatment || '暂无治疗方案' }}
          </el-descriptions-item>
          <el-descriptions-item label="医生备注">
            {{ currentRecord.notes || '无' }}
          </el-descriptions-item>
        </el-descriptions>

        <div class="section-divider section-divider-detail">
          <span>病症明细</span>
        </div>
        <div class="record-detail-list">
          <div
            v-for="(detail, index) in (currentRecord.details || [])"
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
            v-if="!currentRecord.details || currentRecord.details.length === 0"
            description="暂无病症明细"
            :image-size="60"
          />
        </div>

        <div class="view-meta-line">
          <span>
            医生信息：{{ currentRecord.doctor?.name || '-' }}<span v-if="currentRecord.doctor?.title">（{{ currentRecord.doctor.title }}）</span><span v-if="currentRecord.doctor?.department?.deptName"> - {{ currentRecord.doctor.department.deptName }}</span>
          </span>
          <span>
            创建时间：{{ currentRecord.createTime || '-' }}
          </span>
        </div>
      </div>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="recordDialogVisible = false">关闭</el-button>
        </span>
      </template>
    </el-dialog>

    <!-- 处方详情对话框 -->
    <el-dialog
      v-model="prescriptionDialogVisible"
      title="处方详情"
      width="700px"
      class="prescription-dialog"
    >
      <div
        v-if="currentPrescription.id"
        class="prescription-view-detail"
      >
        <div class="view-header-line">
          <div>
            处方编号：{{ currentPrescription.prescriptionNo || '-' }} ｜ 处方日期：{{ currentPrescription.prescriptionDate || '-' }}
          </div>
        </div>

        <div class="section-divider section-divider-detail">
          <span>患者信息</span>
        </div>
        <el-descriptions :column="2" border size="small">
          <el-descriptions-item label="患者">
            {{ currentPrescription.patient?.name || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="年龄/性别">
            {{ patientAgeText(currentPrescription.patient) }} / {{ currentPrescription.patient?.sex || '未知' }}
          </el-descriptions-item>
          <el-descriptions-item label="既往病史">
            {{ currentPrescription.patient?.medicalHistory || '无' }}
          </el-descriptions-item>
          <el-descriptions-item label="过敏史">
            {{ currentPrescription.patient?.allergies || '无' }}
          </el-descriptions-item>
        </el-descriptions>

        <div class="section-divider section-divider-detail">
          <span>诊断信息</span>
        </div>
        <el-descriptions :column="1" border size="small" class="diagnosis-descriptions">
          <el-descriptions-item label="患者主诉">
            {{ currentPrescription.medicalRecord?.appointment?.symptoms || '无' }}
          </el-descriptions-item>
          <el-descriptions-item label="诊断结果">
            {{ currentPrescription.diagnosis || currentPrescription.medicalRecord?.diagnosis || '无' }}
          </el-descriptions-item>
          <el-descriptions-item label="处方备注">
            {{ currentPrescription.notes || '无' }}
          </el-descriptions-item>
        </el-descriptions>

        <div class="section-divider section-divider-detail">
          <span>处方明细</span>
        </div>
        <div class="prescription-detail-list">
          <div
            v-for="(detail, index) in (currentPrescription.details || [])"
            :key="detail.id || `${detail.medicineId || 'm'}-${index}`"
            class="prescription-detail-item"
          >
            <div class="detail-line">
              <span><strong>药品名称：</strong>{{ detail.medicine?.medicineName || '-' }}</span>
              <span><strong>对应病症：</strong>{{ detail.medicalRecordDetail?.symptomName || '-' }}</span>
              <span><strong>对应治疗方案：</strong>{{ detail.medicalRecordDetail?.treatmentPlan || '-' }}</span>
              <span><strong>数量：</strong>{{ detail.quantity ?? '-' }}</span>
              <span><strong>售价：</strong>{{ formatMoney(detail.salePrice) }}</span>
              <span><strong>金额：</strong>{{ formatMoney(detail.amount) }}</span>
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
            v-if="!currentPrescription.details || currentPrescription.details.length === 0"
            description="暂无处方明细"
            :image-size="60"
          />
        </div>

        <div class="view-meta-line">
          <span>
            处方总金额：{{ formatMoney(currentPrescription.totalAmount) }}
          </span>
        </div>

        <div class="view-meta-line">
          <span>
            医生信息：{{ currentPrescription.doctor?.name || '-' }}
          </span>
          <span>
            创建时间：{{ currentPrescription.createTime || '-' }}
          </span>
        </div>
      </div>

      <template #footer>
        <span class="dialog-footer">
          <el-button @click="prescriptionDialogVisible = false">关闭</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed, watch } from 'vue'
import request from '@/utils/request'
import { useUserStore } from '@/store/user'
import { ElMessage } from 'element-plus'
import { 
  Search, 
  Refresh, 
  FirstAidKit, 
  InfoFilled
} from '@element-plus/icons-vue'
import { prescriptionStatusLabel, prescriptionStatusTagType } from '@/utils/prescriptionStatus'

// 用户信息
const userStore = useUserStore()
const patientId = computed(() => userStore.patientInfo?.id)
// 列表数据
const medicalRecords = ref([])
const loading = ref(false)
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(10)

// 搜索表单
const searchForm = reactive({
  doctorName: '',
  startDate: '',
  endDate: ''
})

// 日期范围
const dateRange = ref([])

// 对话框
const recordDialogVisible = ref(false)
const prescriptionDialogVisible = ref(false)
const currentRecord = ref({})
const currentPrescription = ref({})

// 监听日期范围变化
watch(dateRange, (val) => {
  if (val && val.length === 2) {
    searchForm.startDate = val[0]
    searchForm.endDate = val[1]
  } else {
    searchForm.startDate = ''
    searchForm.endDate = ''
  }
})

// 初始化
onMounted(() => {
  if (userStore.isLoggedIn) {
    fetchMyMedicalRecords()
  }
})

// 获取我的就诊记录
const fetchMyMedicalRecords = async () => {
  loading.value = true
  try {
    await request.get('/medical-record/page', {
      patientId: patientId.value,
      doctorName: searchForm.doctorName,
      startDate: searchForm.startDate,
      endDate: searchForm.endDate,
      currentPage: currentPage.value,
      size: pageSize.value
    }, {
      onSuccess: async (res) => {
        // 获取就诊记录
        medicalRecords.value = res.records || []
        total.value = res.total || 0
        
        // 为每个就诊记录加载处方信息
        for (const record of medicalRecords.value) {
          await loadRecordDetail(record)
          await loadPrescriptions(record)
        }
      }
    })
  } catch (error) {
    console.error('获取病历处方失败:', error)
    ElMessage.error('获取病历处方失败')
  } finally {
    loading.value = false
  }
}

const loadRecordDetail = async (record) => {
  try {
    await request.get(`/medical-record/${record.id}`, {}, {
      onSuccess: (res) => {
        Object.assign(record, res || {})
      }
    })
  } catch (error) {
    console.error('获取就诊明细失败:', error)
  }
}

// 加载处方信息
const loadPrescriptions = async (record) => {
  try {
    await request.get(`/prescription/record/${record.id}`, {}, {
      onSuccess: (res) => {
        record.prescriptions = res || []
      }
    })
  } catch (error) {
    console.error('获取处方信息失败:', error)
  }
}

// 搜索
const handleSearch = () => {
  currentPage.value = 1
  fetchMyMedicalRecords()
}

// 重置搜索
const resetSearch = () => {
  searchForm.doctorName = ''
  dateRange.value = []
  currentPage.value = 1
  fetchMyMedicalRecords()
}

// 查看就诊记录详情
const handleViewRecord = (record) => {
  currentRecord.value = record
  recordDialogVisible.value = true
}

// 查看处方详情
const handleViewPrescription = async (prescription) => {
  try {
    await request.get(`/prescription/${prescription.id}`, {}, {
      onSuccess: (res) => {
        currentPrescription.value = res || prescription
        prescriptionDialogVisible.value = true
      }
    })
  } catch (error) {
    console.error('获取处方详情失败:', error)
    currentPrescription.value = prescription
    prescriptionDialogVisible.value = true
  }
}

// 根据记录类型获取时间线类型
const getTimelineItemType = (record) => {
  if (record.followUp) {
    const today = new Date()
    const followUpDate = new Date(record.followUp)
    
    if (followUpDate > today) {
      return 'warning' // 即将随访
    } else if (followUpDate.toDateString() === today.toDateString()) {
      return 'success' // 今天随访
    }
  }
  
  return 'primary' // 默认类型
}

const formatMoney = (value) => {
  const num = Number(value)
  if (!Number.isFinite(num)) return '￥0.00'
  return `￥${num.toFixed(2)}`
}

// 分页操作
const handleSizeChange = (val) => {
  pageSize.value = val
  fetchMyMedicalRecords()
}

const handleCurrentChange = (val) => {
  currentPage.value = val
  fetchMyMedicalRecords()
}

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

<style>
.my-medical-records {
  padding: 20px;
  background-color: #f9f7f2;
}

.records-card {
  margin-bottom: 25px;
  border-radius: 12px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.05) !important;
}

.search-container {
  background-color: #f7fbff;
  padding: 15px;
  border-radius: 8px;
  margin-bottom: 20px;
  border-left: 4px solid #a8d8ea;
}

.search-header {
  display: flex;
  align-items: center;
  margin-bottom: 15px;
  color: #3a5463;
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

.search-form .el-form-item {
  margin-bottom: 10px;
}

.search-form .el-button {
  padding: 10px 20px;
}

.records-container {
  padding: 10px;
}

.record-card {
  margin-bottom: 15px;
  border-radius: 8px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.05) !important;
  border: none;
  overflow: hidden;
  transition: transform 0.3s, box-shadow 0.3s;
}

.record-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 5px 15px rgba(0, 0, 0, 0.1) !important;
}

.record-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 0;
}

.record-title {
  display: flex;
  align-items: center;
}

.record-no {
  font-weight: bold;
  margin-right: 10px;
  color: #3a5463;
}

.record-actions .el-button {
  border-radius: 20px;
}

.section-divider {
  display: flex;
  align-items: center;
  margin: 15px 0;
  color: #5a7385;
}

.section-divider:before,
.section-divider:after {
  content: "";
  flex: 1;
  height: 1px;
  background-color: #e0e0e0;
}

.section-divider:before {
  margin-right: 10px;
}

.section-divider:after {
  margin-left: 10px;
}

.prescription-section {
  margin-top: 15px;
  padding-top: 10px;
  border-top: 1px dashed #e0e0e0;
}

.prescription-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}

.medicine-list {
  margin-top: 10px;
}

.pagination-container {
  margin-top: 20px;
  text-align: center;
}

.empty-container {
  padding: 40px 0;
  text-align: center;
}

.empty-icon {
  font-size: 60px;
  color: #a8d8ea;
}

.loading-container {
  padding: 20px;
}

.health-tips-card {
  margin-top: 25px;
  border-radius: 12px;
  background-color: #f8f9d7;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.05) !important;
}

.tips-header {
  display: flex;
  align-items: center;
  margin-bottom: 15px;
  color: #3a5463;
  border-bottom: 1px dashed #c4e3b2;
  padding-bottom: 10px;
}

.tips-header .el-icon {
  margin-right: 10px;
  color: #76c893;
  font-size: 24px;
}

.tips-header h4 {
  margin: 0;
  font-size: 18px;
}

.tips-list {
  list-style: none;
  padding-left: 0;
}

.tips-list li {
  margin-bottom: 12px;
  padding-left: 25px;
  position: relative;
  color: #5a7385;
}

.tips-list li:before {
  content: "•";
  position: absolute;
  left: 8px;
  color: #76c893;
  font-size: 18px;
}

.notes-content {
  white-space: pre-line;
  background-color: #f9f9f9;
  padding: 10px;
  border-radius: 4px;
  border-left: 3px solid #a8d8ea;
}

.record-view-detail,
.prescription-view-detail {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.view-header-line {
  margin-bottom: 4px;
  color: #606266;
  line-height: 1.8;
}

.section-divider-detail {
  margin: 8px 0 2px;
  color: #303133;
  font-weight: 600;
}

.section-divider-detail:before,
.section-divider-detail:after {
  background-color: #dcdfe6;
}

.section-divider-detail:before {
  margin-right: 12px;
}

.section-divider-detail:after {
  margin-left: 12px;
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

.record-detail-list,
.prescription-detail-list {
  border: 1px solid #ebeef5;
  border-radius: 4px;
  overflow: hidden;
}

.record-detail-item,
.prescription-detail-item {
  padding: 10px;
  border-bottom: 1px solid #ebeef5;
}

.record-detail-item:last-child,
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

/* 对话框样式 */
:deep(.el-dialog) {
  border-radius: 12px;
  overflow: hidden;
}

:deep(.el-dialog__header) {
  background-color: #f0f9ff;
  padding: 15px 20px;
}

:deep(.el-dialog__title) {
  color: #3a5463;
  font-weight: 600;
}

:deep(.el-descriptions__title) {
  color: #3a5463;
}

:deep(.el-descriptions__label) {
  background-color: #f7fbff;
}

:deep(.el-descriptions__content) {
  color: #5a7385;
}

:deep(.record-view-detail .el-descriptions__label),
:deep(.prescription-view-detail .el-descriptions__label) {
  font-size: 13px;
  padding: 8px 10px !important;
}

:deep(.record-view-detail .el-descriptions__content),
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

/* 对话框样式 */
.dialog-header-info {
  display: flex;
  margin-bottom: 20px;
  align-items: center;
}

.dialog-header-info .header-icon {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  background-color: #a8d8ea;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 15px;
  color: white;
  font-size: 24px;
}

.dialog-header-info .header-content h3 {
  margin: 0 0 5px 0;
  color: #3a5463;
  font-size: 18px;
}

.dialog-header-info .header-content p {
  margin: 0;
  color: #5a7385;
  font-size: 14px;
}

.info-with-icon {
  display: flex;
  align-items: center;
}

.info-with-icon .el-icon {
  margin-right: 5px;
  color: #76c893;
}

.diagnosis-box, .treatment-box, .notes-box {
  background-color: #f9f9f9;
  padding: 10px 15px;
  border-radius: 6px;
  border-left: 3px solid #a8d8ea;
  color: #5a7385;
  line-height: 1.6;
}

.diagnosis-box {
  border-left-color: #ffd166;
}

.treatment-box {
  border-left-color: #76c893;
}

.notes-box {
  border-left-color: #a8d8ea;
}

.prescription-list, .medicine-detail {
  margin: 15px 0;
}

/* 表格样式 */
:deep(.el-table) {
  border-radius: 8px;
  overflow: hidden;
}

:deep(.el-table th) {
  background-color: #f0f9ff !important;
  color: #3a5463;
  font-weight: 600;
}

:deep(.el-table .even-row) {
  background-color: #f7fbff;
}

:deep(.el-table .odd-row) {
  background-color: #ffffff;
}

:deep(.el-table--enable-row-hover .el-table__body tr:hover > td) {
  background-color: #e6f4ff;
}

/* 对话框底部按钮 */
.dialog-footer {
  display: flex;
  justify-content: flex-end;
}

.dialog-footer .el-button {
  min-width: 100px;
}

/* 空状态 */
.no-prescription, .no-medicine {
  display: flex;
  justify-content: center;
  padding: 30px 0;
}

/* 响应式调整 */
@media (max-width: 768px) {
  .search-container {
    padding: 10px;
  }
  
  .el-form-item {
    margin-right: 0 !important;
  }
  
  .dialog-header-info .header-icon {
    width: 40px;
    height: 40px;
    font-size: 20px;
  }
  
  .dialog-header-info .header-content h3 {
    font-size: 16px;
  }
  
  .info-with-icon {
    flex-wrap: wrap;
  }
}
</style> 