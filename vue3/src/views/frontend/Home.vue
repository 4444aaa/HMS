<template>
  <div class="home-container">
    <!-- 快捷服务区域 -->
    <div class="quick-service-section">
      <div class="section-title">
        <h2>快捷服务</h2>
      </div>
      <div class="service-cards">
        <div
          v-for="service in quickServices"
          :key="service.id"
          class="service-card"
          @click="handleServiceClick(service.route)"
        >
          <el-icon
            class="service-icon"
            :size="40"
          >
            <component :is="service.icon" />
          </el-icon>
          <div class="service-title">
            {{ service.title }}
          </div>
          <div class="service-desc">
            {{ service.description }}
          </div>
        </div>
      </div>
    </div>

    <!-- 消息提示 -->
    <div class="home-block message-block">
      <div class="section-title section-title--accent-msg">
        <h2>消息提示</h2>
        <button
          v-if="isPatient && inboxItems.length"
          type="button"
          class="inbox-clear"
          @click="markAllInboxRead"
        >
          全部已读
        </button>
      </div>

      <div
        v-if="!isPatient"
        class="guest-hint"
      >
        登录患者账号后可在此查看病历、处方与缴费相关提醒。
      </div>

      <div
        v-else
        class="message-inbox"
      >
        <div
          v-if="inboxLoading"
          class="inbox-loading"
        >
          <el-icon class="is-loading">
            <Loading />
          </el-icon>
          <span>加载消息中…</span>
        </div>
        <template v-else>
          <div
            v-if="!inboxItems.length"
            class="inbox-empty"
          >
            暂无新消息
          </div>
          <div
            v-else
            class="inbox-list"
          >
            <button
              v-for="item in inboxItems"
              :key="item.key"
              type="button"
              class="inbox-row"
              @click="onInboxClick(item)"
            >
              <span
                class="inbox-dot"
                aria-hidden="true"
              />
              <div class="inbox-body">
                <div class="inbox-title">
                  {{ item.title }}
                </div>
                <div class="inbox-meta">
                  {{ item.subtitle }}
                </div>
              </div>
              <el-icon class="inbox-chevron">
                <ArrowRight />
              </el-icon>
            </button>
          </div>
        </template>
      </div>
    </div>

    <!-- 智能问诊 -->
    <div class="home-block consult-block">
      <div class="section-title section-title--accent-chat">
        <h2>智能问诊</h2>
      </div>

      <div
        v-if="!isPatient"
        class="guest-hint"
      >
        登录患者账号后可使用智能问诊进行症状咨询（结果仅供参考，不能替代线下就诊）。
      </div>

      <div
        v-else
        class="consult-panel"
      >
        <div
          ref="chatScrollRef"
          class="consult-messages"
        >
          <div
            v-if="!chatMessages.length"
            class="consult-welcome"
          >
            您好，我是智能问诊助手。我可以为您：推荐科室、医生；查询病历、处方；回答与就诊、服药相关常见问题。
          </div>
          <div
            v-for="m in chatMessages"
            :key="m.id"
            class="chat-row"
            :class="m.role === 'user' ? 'chat-row--user' : 'chat-row--bot'"
          >
            <div
              class="chat-bubble"
              :class="m.role === 'user' ? 'chat-bubble--user' : 'chat-bubble--bot'"
            >
              {{ m.text }}
            </div>
            <div class="chat-time">
              {{ formatChatTime(m.ts) }}
            </div>
          </div>
        </div>
        <div class="consult-input-bar">
          <el-input
            v-model="chatInput"
            type="textarea"
            :autosize="{ minRows: 1, maxRows: 4 }"
            placeholder="请输入您的问题…"
            maxlength="800"
            show-word-limit
            @keydown.enter.exact.prevent="sendChat"
          />
          <el-button
            type="primary"
            class="consult-send"
            :disabled="!chatInput.trim() || chatSending"
            :loading="chatSending"
            @click="sendChat"
          >
            发送
          </el-button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, nextTick, onUnmounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import {
  ArrowRight,
  Calendar,
  Loading,
  Money,
  Notification,
  Tickets
} from '@element-plus/icons-vue'
import { useUserStore } from '@/store/user'
import request from '@/utils/request'
import { prescriptionStatusLabel } from '@/utils/prescriptionStatus'

const router = useRouter()
const userStore = useUserStore()

const INBOX_STORAGE_KEY = 'patient_home_inbox_seen_v1'
const CHAT_HISTORY_KEY_PREFIX = 'ai_clinic_chat_history_v1'

const isPatient = computed(() => userStore.isPatient && !!userStore.patientInfo?.id)
const patientId = computed(() => userStore.patientInfo?.id)

const quickServices = ref([
  {
    id: 1,
    title: '预约挂号',
    description: '在线预约，避免排队等候，轻松安排就诊时间',
    icon: Calendar,
    route: '/appointment'
  },
  {
    id: 2,
    title: '我的预约',
    description: '查看预约状态与就诊时间安排，支持快速管理我的预约',
    icon: Tickets,
    route: '/my-appointments'
  },
  {
    id: 3,
    title: '就诊缴费',
    description: '查看待缴费订单并在线完成支付，实时同步缴费状态',
    icon: Money,
    route: '/outpatient-charge'
  },
  {
    id: 4,
    title: '病历处方',
    description: '随时查询历史病历与处方信息，健康档案一目了然',
    icon: Notification,
    route: '/medical-record'
  }
])

const handleServiceClick = (route) => {
  router.push(route)
}

function loadSeenState () {
  try {
    const raw = localStorage.getItem(INBOX_STORAGE_KEY)
    if (!raw) {
      return { recordIds: [], prescriptionIds: [], orderIds: [] }
    }
    const o = JSON.parse(raw)
    return {
      recordIds: Array.isArray(o.recordIds) ? o.recordIds.map(Number) : [],
      prescriptionIds: Array.isArray(o.prescriptionIds) ? o.prescriptionIds.map(Number) : [],
      orderIds: Array.isArray(o.orderIds) ? o.orderIds.map(Number) : []
    }
  } catch {
    return { recordIds: [], prescriptionIds: [], orderIds: [] }
  }
}

function saveSeenState (state) {
  localStorage.setItem(INBOX_STORAGE_KEY, JSON.stringify(state))
}

const inboxLoading = ref(false)
const inboxItems = ref([])
let pollTimer = null

const chatScrollRef = ref(null)
const chatMessages = ref([])
const chatInput = ref('')
const chatSending = ref(false)

const getChatHistoryStorageKey = () => {
  const pid = patientId.value
  const token = userStore.token || ''
  if (!pid || !token) return ''
  return `${CHAT_HISTORY_KEY_PREFIX}:${pid}:${token.slice(0, 16)}`
}

const loadChatHistory = () => {
  const key = getChatHistoryStorageKey()
  if (!key) {
    chatMessages.value = []
    return
  }
  try {
    const raw = localStorage.getItem(key)
    if (!raw) {
      chatMessages.value = []
      return
    }
    const parsed = JSON.parse(raw)
    chatMessages.value = Array.isArray(parsed) ? parsed : []
  } catch {
    chatMessages.value = []
  }
}

const saveChatHistory = () => {
  const key = getChatHistoryStorageKey()
  if (!key) return
  const lastMessages = chatMessages.value.slice(-100)
  localStorage.setItem(key, JSON.stringify(lastMessages))
}

const parseTime = (v) => {
  if (!v) return 0
  const t = new Date(v).getTime()
  return Number.isNaN(t) ? 0 : t
}

const buildInbox = async () => {
  if (!isPatient.value) {
    inboxItems.value = []
    return
  }
  const seen = loadSeenState()
  inboxLoading.value = true
  try {
    const [records, prescriptions, ordersPage] = await Promise.all([
      request.get('/medical-record/my', {}, { showDefaultMsg: false }).catch(() => []),
      request.get(`/prescription/patient/${patientId.value}`, {}, { showDefaultMsg: false }).catch(() => []),
      request.get('/finance/outpatient-orders/my/page', { currentPage: 1, size: 30 }, { showDefaultMsg: false }).catch(() => ({ records: [] }))
    ])

    const recList = Array.isArray(records) ? records : []
    const rxList = Array.isArray(prescriptions) ? prescriptions : []
    const ordList = ordersPage?.records || []

    const items = []

    recList.forEach((r) => {
      if (!r?.id || seen.recordIds.includes(Number(r.id))) return
      if (r.status !== 1) return
      items.push({
        key: `record-${r.id}`,
        kind: 'record',
        id: Number(r.id),
        title: '新的病历已生成',
        subtitle: `${r.recordNo || '病历'} · 就诊日期 ${r.recordDate || '-'}`,
        time: parseTime(r.createTime || r.updateTime || r.recordDate),
        route: '/medical-record'
      })
    })

    rxList.forEach((p) => {
      if (!p?.id || seen.prescriptionIds.includes(Number(p.id))) return
      if (p.status !== 1) return
      const st = prescriptionStatusLabel(p.status)
      items.push({
        key: `rx-${p.id}`,
        kind: 'prescription',
        id: Number(p.id),
        title: '处方待取药',
        subtitle: `${p.prescriptionNo || '处方'} · ${st}`,
        time: parseTime(p.updateTime || p.createTime || p.prescriptionDate),
        route: '/medical-record'
      })
    })

    ordList.forEach((o) => {
      if (!o?.id || seen.orderIds.includes(Number(o.id))) return
      const unpaid = o.status === 0
      if (!unpaid) return
      items.push({
        key: `order-${o.id}`,
        kind: 'order',
        id: Number(o.id),
        title: '待缴费提醒',
        subtitle: `${o.orderNo || '缴费单'} · 金额 ${o.totalAmount ?? '-'} 元`,
        time: parseTime(o.createTime || o.updateTime),
        route: '/outpatient-charge'
      })
    })

    items.sort((a, b) => b.time - a.time)
    inboxItems.value = items.slice(0, 30)
  } finally {
    inboxLoading.value = false
  }
}

const markSeen = (item) => {
  const seen = loadSeenState()
  if (item.kind === 'record' && !seen.recordIds.includes(item.id)) {
    seen.recordIds.push(item.id)
  }
  if (item.kind === 'prescription' && !seen.prescriptionIds.includes(item.id)) {
    seen.prescriptionIds.push(item.id)
  }
  if (item.kind === 'order' && !seen.orderIds.includes(item.id)) {
    seen.orderIds.push(item.id)
  }
  saveSeenState(seen)
}

const onInboxClick = (item) => {
  markSeen(item)
  inboxItems.value = inboxItems.value.filter((x) => x.key !== item.key)
  router.push(item.route)
}

const markAllInboxRead = () => {
  const seen = loadSeenState()
  inboxItems.value.forEach((item) => {
    if (item.kind === 'record' && !seen.recordIds.includes(item.id)) seen.recordIds.push(item.id)
    if (item.kind === 'prescription' && !seen.prescriptionIds.includes(item.id)) seen.prescriptionIds.push(item.id)
    if (item.kind === 'order' && !seen.orderIds.includes(item.id)) seen.orderIds.push(item.id)
  })
  saveSeenState(seen)
  inboxItems.value = []
}

const buildPatientInfoText = () => {
  const p = userStore.patientInfo || {}
  const chunks = []
  if (p.name) chunks.push(`姓名:${p.name}`)
  if (p.gender) chunks.push(`性别:${p.gender}`)
  if (p.age != null && p.age !== '') chunks.push(`年龄:${p.age}`)
  if (p.phone) chunks.push(`联系电话:${p.phone}`)
  return chunks.join('，')
}

const formatChatTime = (ts) => {
  const d = new Date(ts)
  if (Number.isNaN(d.getTime())) return ''
  const pad = (n) => String(n).padStart(2, '0')
  return `${pad(d.getHours())}:${pad(d.getMinutes())}`
}

const scrollChatToBottom = async () => {
  await nextTick()
  const el = chatScrollRef.value
  if (el) {
    el.scrollTop = el.scrollHeight
  }
}

const sendChat = async () => {
  const text = chatInput.value.trim()
  if (!text || chatSending.value) return
  chatSending.value = true
  const uid = `${Date.now()}-${Math.random().toString(16).slice(2)}`
  chatMessages.value.push({
    id: uid,
    role: 'user',
    text,
    ts: Date.now()
  })
  chatInput.value = ''
  await scrollChatToBottom()
  try {
    const res = await request.post('/ai/clinic/consult', {
      patientInfo: buildPatientInfoText(),
      question: text
    }, {
      showDefaultMsg: false
    })
    chatMessages.value.push({
      id: `${Date.now()}-bot`,
      role: 'system',
      text: res?.answer || '未获取到有效回复，请稍后重试。',
      ts: Date.now()
    })
  } catch (e) {
    chatMessages.value.push({
      id: `${Date.now()}-bot-err`,
      role: 'system',
      text: '问诊服务暂时不可用，请稍后重试。',
      ts: Date.now()
    })
  } finally {
    chatSending.value = false
    await scrollChatToBottom()
  }
}


const startInboxPoll = () => {
  if (pollTimer) return
  pollTimer = setInterval(() => {
    buildInbox()
  }, 90000)
}

const stopInboxPoll = () => {
  if (pollTimer) {
    clearInterval(pollTimer)
    pollTimer = null
  }
}

watch(
  isPatient,
  (ok) => {
    if (ok) {
      loadChatHistory()
      buildInbox()
      startInboxPoll()
      nextTick(() => {
        scrollChatToBottom()
      })
    } else {
      inboxItems.value = []
      chatMessages.value = []
      stopInboxPoll()
    }
  },
  { immediate: true }
)

watch(
  chatMessages,
  () => {
    if (isPatient.value) {
      saveChatHistory()
    }
  },
  { deep: true }
)

onUnmounted(() => {
  stopInboxPoll()
})
</script>

<style lang="scss" scoped>
:root {
  --warm-white: #faf7f2;
  --light-beige: #f5efe2;
  --soft-wood: #e8dcc7;
  --soft-green: #a9c8a3;
  --soft-blue: #a3c6d3;
  --text-primary: #5a5a5a;
  --text-secondary: #7a7a7a;
  --shadow-soft: 0 4px 12px rgba(0, 0, 0, 0.05);
  --radius-large: 16px;
  --radius-medium: 12px;
  --radius-small: 8px;
}

.home-container {
  width: 100%;
  max-width: 1200px;
  margin: 0 auto;
  padding: 25px;
  background-color: var(--warm-white);
  font-family: 'Nunito', 'Helvetica Neue', Helvetica, 'PingFang SC', 'Hiragino Sans GB', 'Microsoft YaHei', Arial, sans-serif;
}

.section-title {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 25px;
  padding-bottom: 10px;
  border-bottom: 1px solid #e8dcc7;

  h2 {
    font-size: 24px;
    color: var(--text-primary);
    margin: 0;
    position: relative;
    padding-left: 15px;
    font-weight: 600;

    &::before {
      content: '';
      position: absolute;
      left: 0;
      top: 50%;
      transform: translateY(-50%);
      width: 5px;
      height: 24px;
      background-color: var(--soft-green);
      border-radius: 10px;
    }
  }

  &.section-title--accent-msg h2::before {
    background-color: var(--soft-blue);
  }

  &.section-title--accent-chat h2::before {
    background-color: #c4b5a0;
  }

  .view-more {
    color: var(--soft-blue);
    text-decoration: none;
    font-size: 15px;
    font-weight: 500;
    transition: all 0.3s ease;

    &:hover {
      color: darken(#a3c6d3, 15%);
    }
  }
}

.inbox-clear {
  border: none;
  background: transparent;
  color: var(--soft-blue);
  font-size: 15px;
  font-weight: 500;
  cursor: pointer;
  padding: 4px 8px;
  border-radius: 6px;

  &:hover {
    background: rgba(163, 198, 211, 0.15);
  }
}

.home-block {
  margin-bottom: 35px;
}

.guest-hint {
  background: #fff;
  border: 1px solid var(--light-beige);
  border-radius: var(--radius-medium);
  padding: 18px 20px;
  color: var(--text-secondary);
  font-size: 15px;
  line-height: 1.6;
  box-shadow: var(--shadow-soft);
}

.message-inbox {
  background: #fff;
  border: 1px solid var(--light-beige);
  border-radius: var(--radius-medium);
  box-shadow: var(--shadow-soft);
  min-height: 120px;
  overflow: hidden;
}

.inbox-loading,
.inbox-empty {
  padding: 28px 20px;
  text-align: center;
  color: var(--text-secondary);
  font-size: 15px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}

.inbox-list {
  display: flex;
  flex-direction: column;
}

.inbox-row {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  width: 100%;
  text-align: left;
  padding: 14px 18px;
  border: none;
  border-bottom: 1px solid #f0ebe3;
  background: #fff;
  cursor: pointer;
  transition: background 0.2s ease;

  &:last-child {
    border-bottom: none;
  }

  &:hover {
    background: #faf8f4;
  }
}

.inbox-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #f56c6c;
  margin-top: 7px;
  flex-shrink: 0;
}

.inbox-body {
  flex: 1;
  min-width: 0;
}

.inbox-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 4px;
}

.inbox-meta {
  font-size: 14px;
  color: var(--text-secondary);
  line-height: 1.45;
}

.inbox-chevron {
  color: #c0c4cc;
  font-size: 18px;
  margin-top: 4px;
  flex-shrink: 0;
}

.consult-panel {
  background: #fff;
  border: 1px solid var(--light-beige);
  border-radius: var(--radius-medium);
  box-shadow: var(--shadow-soft);
  display: flex;
  flex-direction: column;
  min-height: 360px;
  max-height: 520px;
}

.consult-messages {
  flex: 1;
  overflow-y: auto;
  padding: 16px 14px 12px;
  background: linear-gradient(180deg, #f7f5f0 0%, #faf9f7 40%, #fff 100%);
}

.consult-welcome {
  font-size: 14px;
  color: var(--text-secondary);
  line-height: 1.65;
  padding: 8px 10px 16px;
  max-width: 92%;
}

.chat-row {
  display: flex;
  flex-direction: column;
  margin-bottom: 14px;
  max-width: 85%;

  &--user {
    align-self: flex-end;
    align-items: flex-end;
  }

  &--bot {
    align-self: flex-start;
    align-items: flex-start;
  }
}

.chat-bubble {
  padding: 10px 14px;
  border-radius: 14px;
  font-size: 15px;
  line-height: 1.55;
  word-break: break-word;
  white-space: pre-wrap;
  text-align: left;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.06);

  &--user {
    background: linear-gradient(135deg, #95c090 0%, #7aab74 100%);
    color: #fff;
    border-bottom-right-radius: 4px;
  }

  &--bot {
    background: #fff;
    color: var(--text-primary);
    border: 1px solid #e8e4dc;
    border-bottom-left-radius: 4px;
  }
}

.chat-time {
  font-size: 11px;
  color: #b0b0b0;
  margin-top: 4px;
  padding: 0 4px;
}

.consult-input-bar {
  display: flex;
  gap: 10px;
  align-items: flex-end;
  padding: 12px 14px 14px;
  border-top: 1px solid #eee8df;
  background: #fff;
}


.consult-input-bar :deep(.el-textarea__inner) {
  border-radius: 10px;
  resize: none;
}

.consult-send {
  flex-shrink: 0;
  border-radius: 10px;
  padding: 10px 18px;
}

.quick-service-section {
  margin-top: 0;
  margin-bottom: 35px;

  .service-cards {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
    gap: 25px;
  }

  .service-card {
    background-color: #fff;
    border-radius: var(--radius-medium);
    padding: 25px;
    text-align: center;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    box-shadow: var(--shadow-soft);
    transition: all 0.3s ease;
    cursor: pointer;
    border: 1px solid var(--light-beige);

    &:hover {
      transform: translateY(-5px);
      box-shadow: 0 8px 20px rgba(0, 0, 0, 0.08);
    }

    .service-icon {
      color: var(--soft-green);
      margin-bottom: 18px;
      background-color: rgba(169, 200, 163, 0.1);
      width: 70px;
      height: 70px;
      border-radius: 50%;
      display: flex;
      align-items: center;
      justify-content: center;
    }

    .service-title {
      font-size: 19px;
      font-weight: 600;
      margin-bottom: 12px;
      color: var(--text-primary);
      width: 100%;
    }

    .service-desc {
      font-size: 15px;
      color: var(--text-secondary);
      line-height: 1.5;
      width: 100%;
    }
  }
}

@media (max-width: 768px) {
  .quick-service-section .service-cards {
    grid-template-columns: repeat(2, 1fr);
  }

  .consult-panel {
    max-height: 70vh;
  }

  .chat-row {
    max-width: 92%;
  }
}

@media (max-width: 576px) {
  .home-container {
    padding: 15px;
  }

  .quick-service-section .service-cards {
    grid-template-columns: 1fr;
  }
}
</style>
