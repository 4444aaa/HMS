<template>
  <div class="appointment-page">
    <el-row :gutter="20" class="main-layout-row">
      <el-col :md="6" :sm="24" class="left-col">
        <el-card class="selection-card" shadow="hover">
          <div class="card-header-custom">
            <div class="header-icon"><el-icon><OfficeBuilding /></el-icon></div>
            <h3>选择科室</h3>
          </div>
          <div class="department-search">
            <el-input
              v-model="departmentKeyword"
              placeholder="输入科室名称检索"
              clearable
            />
          </div>
          <div class="department-list">
            <el-scrollbar class="department-scrollbar">
              <div class="dept-list">
                <div
                  v-for="dept in filteredDepartmentList"
                  :key="dept.id"
                  :class="['dept-item', selectedDepartmentId === dept.id ? 'dept-active' : '']"
                  @click="handleDepartmentChange(dept.id)"
                >
                  {{ dept.deptName }}
                </div>
                <el-empty
                  v-if="filteredDepartmentList.length === 0"
                  description="未找到匹配科室"
                />
              </div>
            </el-scrollbar>
          </div>
        </el-card>
      </el-col>

      <el-col :md="18" :sm="24" class="right-col">
        <el-card class="selection-card" shadow="hover">
          <div class="card-header-custom">
            <div class="header-icon"><el-icon><User /></el-icon></div>
            <h3>选择医生</h3>
          </div>
          <div class="doctor-list">
            <el-empty v-if="!selectedDepartmentId" description="请先选择科室" />
            <el-empty v-else-if="doctorList.length === 0" description="该科室暂无医生" />
            <el-scrollbar v-else height="260px">
              <div class="doctor-cards">
                <div
                  v-for="doctor in doctorList"
                  :key="doctor.id"
                  :class="['doctor-card', selectedDoctorId === doctor.id ? 'doctor-active' : '']"
                  @click="handleDoctorSelect(doctor)"
                >
                  <div class="doctor-avatar">
                    <el-avatar :size="60" :src="getDoctorAvatarUrl(doctor.user?.avatar) || '/img/default_avatar.png'" />
                  </div>
                  <div class="doctor-details">
                    <h4>{{ doctor.name }}</h4>
                    <div class="doctor-title">{{ doctor.title || '医师' }}</div>
                    <p class="doctor-expertise">{{ doctor.expertise || '擅长：全科医疗' }}</p>
                  </div>
                </div>
              </div>
            </el-scrollbar>
          </div>
        </el-card>

        <el-card class="schedule-card" shadow="hover">
          <template #header>
            <div class="card-header-custom">
              <div class="header-icon"><el-icon><Calendar /></el-icon></div>
              <h3>选择就诊时间</h3>
            </div>
          </template>

          <div v-if="!selectedDoctorId" class="empty-prompt">
            <el-icon :size="80" color="#a8d8ea"><Calendar /></el-icon>
            <p>请先在左侧选择科室和医生</p>
          </div>
          <div v-else class="time-select-panel">
            <el-form label-width="100px">
              <el-form-item label="就诊日期">
                <el-date-picker
                  v-model="selectedAppointmentDate"
                  type="date"
                  placeholder="请选择就诊日期"
                  :disabled-date="disabledDate"
                  value-format="YYYY-MM-DD"
                  style="width: 280px"
                />
              </el-form-item>
              <el-form-item label="时间段">
                <el-radio-group v-model="selectedTimeSlot">
                  <el-radio-button label="上午" />
                  <el-radio-button label="下午" />
                  <el-radio-button label="晚上" />
                </el-radio-group>
              </el-form-item>
            </el-form>
            <el-button type="primary" round @click="handleAppointment">去预约</el-button>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-dialog v-model="appointmentDialogVisible" title="确认预约信息" width="500px" class="appointment-dialog">
      <div class="dialog-content">
        <el-form ref="appointmentFormRef" :model="appointmentForm" :rules="appointmentFormRules" label-width="100px">
          <el-form-item label="就诊日期">
            <el-input v-model="appointmentInfo.date" disabled class="custom-disabled-input" />
          </el-form-item>
          <el-form-item label="就诊时间">
            <el-input v-model="appointmentInfo.timeSlot" disabled class="custom-disabled-input" />
          </el-form-item>
          <el-form-item label="科室">
            <el-input v-model="appointmentInfo.department" disabled class="custom-disabled-input" />
          </el-form-item>
          <el-form-item label="医生">
            <el-input v-model="appointmentInfo.doctor" disabled class="custom-disabled-input" />
          </el-form-item>
          <el-form-item label="症状描述" prop="symptoms">
            <el-input
              v-model="appointmentForm.symptoms"
              type="textarea"
              :rows="4"
              placeholder="请简要描述您的症状和就诊需求"
            />
          </el-form-item>
        </el-form>
      </div>
      <template #footer>
        <span class="dialog-footer">
          <el-button plain round @click="appointmentDialogVisible = false">取消</el-button>
          <el-button type="primary" round @click="submitAppointment">确认预约</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/store/user'
import request from '@/utils/request'
import { ElMessage, ElMessageBox } from 'element-plus'
import { OfficeBuilding, User, Calendar } from '@element-plus/icons-vue'

const router = useRouter()
const userStore = useUserStore()

const baseAPI = import.meta.env.VITE_API_BASE_URL || '/api'
const getDoctorAvatarUrl = (avatar) => {
  if (!avatar) return ''
  if (avatar.startsWith('http')) return avatar
  return baseAPI + avatar
}

const departmentList = ref([])
const doctorList = ref([])
const departmentKeyword = ref('')
const selectedDepartmentId = ref(null)
const selectedDoctorId = ref(null)
const selectedAppointmentDate = ref('')
const selectedTimeSlot = ref('上午')

const appointmentDialogVisible = ref(false)
const appointmentFormRef = ref(null)
const appointmentForm = reactive({
  patientId: null,
  doctorId: null,
  scheduleId: 0,
  appointmentDate: '',
  timeSlot: '',
  symptoms: ''
})

const appointmentInfo = reactive({
  date: '',
  timeSlot: '',
  department: '',
  doctor: ''
})

const appointmentFormRules = {
  symptoms: [{ required: true, message: '请描述您的症状', trigger: 'blur' }]
}

onMounted(async () => {
  if (!userStore.isLoggedIn) {
    ElMessage.warning('请先登录')
    router.push('/login')
    return
  }
  await fetchDepartments()
})

const fetchDepartments = async () => {
  await request.get('/department/list', {}, {
    onSuccess: (res) => {
      departmentList.value = res || []
    }
  })
}

const handleDepartmentChange = async (deptId) => {
  selectedDepartmentId.value = deptId
  selectedDoctorId.value = null
  await fetchDoctorsByDepartment(deptId)
}

const fetchDoctorsByDepartment = async (deptId) => {
  await request.get(`/doctor/department/${deptId}`, {}, {
    onSuccess: (res) => {
      doctorList.value = res || []
    }
  })
}

const handleDoctorSelect = (doctor) => {
  selectedDoctorId.value = doctor.id
}

const handleAppointment = () => {
  if (!selectedDoctorId.value) {
    ElMessage.warning('请先选择医生')
    return
  }
  if (!selectedAppointmentDate.value) {
    ElMessage.warning('请选择就诊日期')
    return
  }
  if (!selectedTimeSlot.value) {
    ElMessage.warning('请选择时间段')
    return
  }
  if (!userStore.patientInfo) {
    ElMessageBox.confirm('您尚未完善患者信息，需要先完善患者信息才能预约', '提示', {
      confirmButtonText: '去完善',
      cancelButtonText: '取消',
      type: 'warning'
    }).then(() => router.push('/profile')).catch(() => {})
    return
  }

  const selectedDoctor = doctorList.value.find(d => d.id === selectedDoctorId.value)
  const selectedDepartment = departmentList.value.find(d => d.id === selectedDepartmentId.value)

  appointmentForm.patientId = userStore.patientInfo.id
  appointmentForm.doctorId = selectedDoctorId.value
  appointmentForm.scheduleId = 0
  appointmentForm.appointmentDate = selectedAppointmentDate.value
  appointmentForm.timeSlot = selectedTimeSlot.value
  appointmentForm.symptoms = ''

  appointmentInfo.date = selectedAppointmentDate.value
  appointmentInfo.timeSlot = selectedTimeSlot.value
  appointmentInfo.department = selectedDepartment ? selectedDepartment.deptName : ''
  appointmentInfo.doctor = selectedDoctor ? `${selectedDoctor.name} ${selectedDoctor.title || ''}` : ''

  appointmentDialogVisible.value = true
}

const submitAppointment = () => {
  appointmentFormRef.value.validate(async (valid) => {
    if (!valid) return
    await request.post('/appointment', appointmentForm, {
      successMsg: '预约成功',
      onSuccess: () => {
        appointmentDialogVisible.value = false
        ElMessageBox.confirm('预约成功，是否查看我的预约记录？', '提示', {
          confirmButtonText: '查看',
          cancelButtonText: '继续预约',
          type: 'success'
        }).then(() => router.push('/my-appointments')).catch(() => {})
      }
    })
  })
}

const disabledDate = (time) => time.getTime() < Date.now() - 8.64e7

const filteredDepartmentList = computed(() => {
  const keyword = (departmentKeyword.value || '').trim()
  if (!keyword) return departmentList.value
  return departmentList.value.filter(dept => (dept.deptName || '').includes(keyword))
})
</script>

<style scoped>
.appointment-page { padding: 20px; background-color: #f9f7f2; min-height: calc(100vh - 160px); }
.main-layout-row { align-items: stretch; }
.left-col, .right-col { display: flex; flex-direction: column; }
.right-col { gap: 20px; }
.left-col .selection-card { height: 100%; display: flex; flex-direction: column; }
.left-col .selection-card :deep(.el-card__body) { flex: 1; display: flex; flex-direction: column; min-height: 0; }
.selection-card, .schedule-card { border-radius: 15px; margin-bottom: 0; border: none; }
.card-header-custom { display: flex; align-items: center; margin-bottom: 20px; }
.header-icon { background-color: #a8d8ea; border-radius: 50%; width: 36px; height: 36px; display: flex; align-items: center; justify-content: center; margin-right: 12px; }
.header-icon .el-icon { color: #fff; }
.department-search { margin-bottom: 12px; }
.dept-list { display: flex; flex-direction: column; gap: 8px; padding-right: 6px; }
.department-list { min-height: 0; overflow: hidden; }
/* 固定一次展示10个科室，其余通过滚动查看 */
.department-scrollbar { height: 518px; }
.dept-item {
  border-radius: 10px;
  background-color: #f9f7f2;
  color: #5c7b8a;
  border: 1px solid #e8e1d0;
  padding: 10px 14px;
  height: 44px;
  box-sizing: border-box;
  line-height: 1.2;
  cursor: pointer;
  transition: all 0.2s ease;
}
.dept-item:hover { background-color: #f0eada; color: #3a5463; }
.dept-active { background-color: #c4e3b2; color: #3a5463; border-color: #c4e3b2; }
.doctor-cards { display: flex; flex-direction: column; gap: 15px; }
.doctor-card { display: flex; align-items: center; padding: 15px; border-radius: 12px; background-color: #f9f7f2; cursor: pointer; border: 1px solid transparent; }
.doctor-active { background-color: #e6f2e2; border-color: #c4e3b2; }
.doctor-avatar { margin-right: 15px; }
.doctor-title { display: inline-block; background-color: #a8d8ea; color: #fff; font-size: 12px; padding: 2px 8px; border-radius: 10px; }
.doctor-expertise { margin: 5px 0 0 0; font-size: 12px; color: #8a9ca7; }
.empty-prompt { display: flex; flex-direction: column; align-items: center; padding: 50px 0; color: #8a9ca7; }
.time-select-panel { padding: 10px 0; }
.custom-disabled-input :deep(.el-input__inner) { background-color: #f9f7f2; }
.dialog-footer { display: flex; justify-content: center; gap: 15px; }

@media (max-width: 991px) {
  .main-layout-row { align-items: flex-start; }
  .left-col .selection-card { height: auto; }
  .department-scrollbar { height: 430px; }
}
</style>
