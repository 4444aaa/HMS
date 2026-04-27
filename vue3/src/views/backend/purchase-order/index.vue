<template>
  <div class="purchase-order">
    <el-card class="box-card">
      <template #header>
        <div class="card-header">
          <h3>采购单</h3>
          <div class="header-actions">
            <el-button
              type="primary"
              @click="openCreate"
            >
              新增采购单
            </el-button>
          </div>
        </div>
      </template>

      <el-form
        :model="searchForm"
        :inline="true"
        class="search-form"
      >
        <el-form-item label="采购单号">
          <el-input
            v-model="searchForm.orderNo"
            placeholder="请输入采购单号"
            clearable
          />
        </el-form-item>
        <el-form-item label="计划ID">
          <el-input
            v-model="searchForm.planId"
            placeholder="可选"
            clearable
          />
        </el-form-item>
        <el-form-item label="供应商名称">
          <el-input
            v-model="searchForm.supplierName"
            placeholder="供应商名称"
            clearable
            style="width: 160px"
          />
        </el-form-item>
        <el-form-item label="状态">
          <el-select
            v-model="searchForm.status"
            placeholder="请选择状态"
            style="width: 140px"
          >
            <el-option
              label="全部"
              :value="''"
            />
            <el-option
              label="草稿"
              :value="0"
            />
            <el-option
              label="已发送"
              :value="1"
            />
            <el-option
              label="已验收完成"
              :value="2"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="创建人">
          <el-input
            v-model="searchForm.creatorName"
            placeholder="姓名或用户名"
            clearable
            style="width: 160px"
          />
        </el-form-item>
        <el-form-item label="创建日期">
          <el-date-picker
            v-model="searchForm.createDateRange"
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
            @click="handleSearch"
          >
            查询
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
          prop="orderNo"
          label="采购单号"
          width="170"
        />
        <el-table-column
          label="计划ID"
          min-width="150"
        >
          <template #default="scope">
            {{ formatPlanIds(scope.row) }}
          </template>
        </el-table-column>
        <el-table-column
          label="供应商"
          min-width="180"
        >
          <template #default="scope">
            {{ supplierNameMap[scope.row.supplierId] || scope.row.supplierId }}
          </template>
        </el-table-column>
        <el-table-column
          prop="status"
          label="状态"
          width="120"
        >
          <template #default="scope">
            <el-tag :type="getStatusTagType(scope.row.status)">
              {{ getStatusText(scope.row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column
          prop="totalAmount"
          label="金额"
          width="120"
        />
        <el-table-column
          prop="createTime"
          label="创建时间"
          width="180"
        >
          <template #default="scope">
            {{ formatDate(scope.row.createTime) }}
          </template>
        </el-table-column>
        <el-table-column
          label="操作"
          width="380"
          fixed="right"
        >
          <template #default="scope">
            <el-button
              size="small"
              type="primary"
              @click="openDetail(scope.row)"
            >
              详情
            </el-button>
            <el-button
              v-if="scope.row.status === 0"
              size="small"
              type="warning"
              @click="openEdit(scope.row)"
            >
              编辑
            </el-button>
            <el-button
              v-if="scope.row.status === 0"
              size="small"
              type="success"
              @click="sendOrder(scope.row)"
            >
              发送
            </el-button>
            <el-button
              v-if="scope.row.status === 0"
              size="small"
              type="danger"
              @click="deleteOrder(scope.row)"
            >
              删除
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

    <!-- 创建采购单 -->
    <el-dialog
      v-model="createVisible"
      :title="isEdit ? '编辑采购单' : '新增采购单'"
      width="980px"
      @closed="resetCreate"
    >
      <div class="create-doc-layout">
        <div
          v-if="!isEdit"
          class="items-header create-basic-header-row"
        >
          <div class="items-title">
            基本信息
          </div>
        </div>
        <div
          v-if="!isEdit"
          class="create-meta-strip"
        >
          <div class="create-meta-line">
            <span class="create-meta-item">采购单号：{{ draftPreviewNo }}</span>
            <span class="create-meta-item">采购时间：{{ draftCreateTime }}</span>
            <span class="create-meta-item">创建人姓名：{{ currentUserDisplayName }}</span>
          </div>
        </div>
        <el-form
          :model="createForm"
          label-position="top"
          class="create-main-form create-main-form--top"
        >
          <el-row
            :gutter="16"
            class="create-form-row"
          >
            <el-col
              :xs="24"
              :sm="24"
              :md="12"
            >
              <el-form-item label="采购计划">
                <el-select
                  v-model="createForm.planIds"
                  multiple
                  collapse-tags
                  collapse-tags-tooltip
                  placeholder="请选择已提交计划"
                  filterable
                  class="create-field-full"
                  @change="onPlansChange"
                >
                  <el-option
                    v-for="p in planOptions"
                    :key="p.id"
                    :label="`${p.planNo} ${p.title ? ' - ' + p.title : ''}`"
                    :value="p.id"
                  />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col
              :xs="24"
              :sm="24"
              :md="12"
            >
              <el-form-item label="供应商">
                <el-select
                  v-model="createForm.supplierId"
                  placeholder="请选择供应商"
                  filterable
                  class="create-field-full"
                  @change="onSupplierChange"
                >
                  <el-option
                    v-for="s in supplierOptions"
                    :key="s.id"
                    :label="`${s.name}${s.supplierCode ? '（' + s.supplierCode + '）' : ''}`"
                    :value="s.id"
                  />
                </el-select>
              </el-form-item>
            </el-col>
          </el-row>
          <el-form-item label="备注">
            <el-input
              v-model="createForm.remark"
              type="textarea"
              :rows="2"
              placeholder="可选"
            />
          </el-form-item>
        </el-form>
      </div>

      <el-alert
        class="create-dialog-follow"
        title="下单明细必须来源采购计划明细。仅填写下单数量>0的行会被提交。"
        type="info"
        :closable="false"
        show-icon
        style="margin-bottom: 10px"
      />

      <div class="items-header create-purchase-detail-header">
        <div class="items-title">
          采购明细
        </div>
      </div>

      <div class="create-dialog-table-wrap">
        <el-table
          :data="createForm.items"
          border
          style="width: 100%"
        >
        <el-table-column
          label="药品"
          min-width="160"
        >
          <template #default="scope">
            {{ scope.row.medicine?.medicineName || scope.row.medicineId }}
          </template>
        </el-table-column>
        <el-table-column
          label="计划总量"
          width="100"
        >
          <template #default="scope">
            {{ scope.row.planQty }}
          </template>
        </el-table-column>
        <el-table-column
          label="已下单量"
          width="100"
        >
          <template #default="scope">
            {{ scope.row.purchasedQty }}
          </template>
        </el-table-column>
        <el-table-column
          label="计划剩余"
          width="100"
        >
          <template #default="scope">
            {{ scope.row.remainingQty }}
          </template>
        </el-table-column>
        <el-table-column
          label="下单数量"
          width="130"
        >
          <template #default="scope">
            <el-input-number
              v-model="scope.row.orderQty"
              :min="0"
              :max="scope.row.remainingQty"
              style="width: 100%"
            />
          </template>
        </el-table-column>
        <el-table-column
          label="单价"
          width="100"
        >
          <template #default="scope">
            {{ formatMoney(scope.row.unitPrice) }}
          </template>
        </el-table-column>
        <el-table-column
          label="总价"
          width="100"
        >
          <template #default="scope">
            {{ formatMoney(lineAmount(scope.row)) }}
          </template>
        </el-table-column>
        <el-table-column
          label="备注"
          min-width="140"
        >
          <template #default="scope">
            <el-input
              v-model="scope.row.remark"
              placeholder="可选"
            />
          </template>
        </el-table-column>
        </el-table>
      </div>

      <template #footer>
        <el-button @click="createVisible = false">
          取消
        </el-button>
        <el-button
          type="primary"
          :loading="saving"
          @click="saveOrder"
        >
          {{ isEdit ? '保存' : '创建' }}
        </el-button>
      </template>
    </el-dialog>

    <!-- 详情 -->
    <el-dialog
      v-model="detailVisible"
      title="采购单详情"
      width="980px"
    >
      <div class="doc-preview">
        <div class="doc-preview-title">
          采购订单单
        </div>
        <div class="doc-preview-meta">
          <span>采购单号：{{ detail.orderNo || '-' }}</span>
          <span>采购时间：{{ formatDate(detail.createTime) }}</span>
          <span>创建人姓名：{{ currentUserDisplayName }}</span>
        </div>
        <div class="doc-preview-meta">
          <span>采购计划：{{ formatPlanIds(detail) }}</span>
          <span>状态：{{ getStatusText(detail.status) }}</span>
        </div>
        <div class="doc-preview-meta">
          <span>供应商名称：{{ detail.supplier?.name || '-' }}</span>
          <span>联系人：{{ detail.supplier?.contactName || '-' }}</span>
          <span>联系电话：{{ detail.supplier?.contactPhone || '-' }}</span>
          <span>地址：{{ detail.supplier?.address || '-' }}</span>
        </div>
        <div class="doc-preview-meta">
          <span>备注：{{ detail.remark || '-' }}</span>
        </div>
      </div>
      <el-table
        :data="detail.items || []"
        border
        style="width: 100%"
      >
        <el-table-column
          label="药品"
          min-width="240"
        >
          <template #default="scope">
            {{ scope.row.medicine?.medicineName || scope.row.medicineId }}
          </template>
        </el-table-column>
        <el-table-column
          prop="orderQty"
          label="下单数量"
          width="120"
        />
        <el-table-column
          prop="unitPrice"
          label="单价"
          width="120"
        />
        <el-table-column
          prop="amount"
          label="金额"
          width="120"
        />
        <el-table-column
          prop="remark"
          label="备注"
          min-width="200"
        />
      </el-table>
      <div class="doc-preview-total">
        总金额：{{ detail.totalAmount || 0 }} 元
      </div>
      <template #footer>
        <el-button @click="detailVisible = false">
          关闭
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { useUserStore } from '@/store/user'
import request from '@/utils/request'
import { ElMessageBox } from 'element-plus'
import { formatDate } from '@/utils/dateUtils'
import { previewDocumentNo, formatDraftCreateTime } from '@/utils/draftDocumentPreview'

const userStore = useUserStore()
const currentUserDisplayName = computed(() => userStore.userInfo?.name || userStore.userInfo?.username || '-')

const loading = ref(false)
const saving = ref(false)
const tableData = ref([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(10)

const searchForm = reactive({
  orderNo: '',
  planId: '',
  supplierName: '',
  status: '',
  creatorName: '',
  createDateRange: null
})

const createVisible = ref(false)
const isEdit = ref(false)
const editingOrderId = ref(null)
const detailVisible = ref(false)
const detail = reactive({})

const planOptions = ref([])
const supplierOptions = ref([])
const supplierNameMap = reactive({})

const createForm = reactive({
  planIds: [],
  supplierId: null,
  remark: '',
  items: []
})
const supplierMedicineIdSet = ref(new Set())
const sourcePlanItems = ref([])

const draftPreviewNo = ref('')
const draftCreateTime = ref('')

const lineAmount = (row) => {
  const q = Number(row?.orderQty) || 0
  const p = Number(row?.unitPrice) || 0
  return q * p
}

const formatMoney = (n) => {
  const v = Number(n)
  if (Number.isNaN(v)) return '0.00'
  return v.toFixed(2)
}

const formatPlanIds = (row) => {
  if (Array.isArray(row?.planIds) && row.planIds.length > 0) {
    return row.planIds.join(', ')
  }
  return row?.planId ?? '-'
}

const fetchOrders = async () => {
  loading.value = true
  try {
    const params = {
      orderNo: searchForm.orderNo,
      planId: searchForm.planId ? Number(searchForm.planId) : undefined,
      currentPage: currentPage.value,
      size: pageSize.value
    }
    const sn = (searchForm.supplierName || '').trim()
    if (sn) {
      params.supplierName = sn
    }
    const cn = (searchForm.creatorName || '').trim()
    if (cn) {
      params.creatorName = cn
    }
    if (Array.isArray(searchForm.createDateRange) && searchForm.createDateRange.length === 2) {
      params.createDateStart = searchForm.createDateRange[0]
      params.createDateEnd = searchForm.createDateRange[1]
    }
    if (searchForm.status !== '') {
      params.status = searchForm.status
    }
    await request.get('/purchaseOrder/page', params, {
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
const resetSearch = () => {
  searchForm.orderNo = ''
  searchForm.planId = ''
  searchForm.supplierName = ''
  searchForm.status = ''
  searchForm.creatorName = ''
  searchForm.createDateRange = null
  handleSearch()
}
const handleSizeChange = (val) => {
  pageSize.value = val
  fetchOrders()
}
const handleCurrentChange = (val) => {
  currentPage.value = val
  fetchOrders()
}

const openCreate = async () => {
  isEdit.value = false
  editingOrderId.value = null
  draftPreviewNo.value = previewDocumentNo('PO')
  draftCreateTime.value = formatDraftCreateTime()
  createVisible.value = true
  await fetchPlanOptions()
  await fetchSupplierOptions()
}

const openEdit = async (row) => {
  isEdit.value = true
  editingOrderId.value = row.id
  createVisible.value = true
  await request.get(`/purchaseOrder/${row.id}`, {}, {
    showDefaultMsg: false,
    onSuccess: async (res) => {
      const planIds = (res?.planIds && res.planIds.length > 0) ? res.planIds : (res?.planId ? [res.planId] : [])
      await fetchPlanOptions(planIds)
      createForm.planIds = planIds
      createForm.supplierId = res?.supplierId || null
      createForm.remark = res?.remark || ''
      await onPlansChange(createForm.planIds, true)
      await onSupplierChange(createForm.supplierId)
      const itemMap = {}
      ;(res?.items || []).forEach(it => {
        itemMap[it.medicineId] = it
      })
      createForm.items = (createForm.items || []).map(it => {
        const saved = itemMap[it.medicineId]
        if (!saved) return it
        const allocationQtyMap = {}
        ;(saved.planAllocations || []).forEach(a => {
          allocationQtyMap[a.planItemId] = Number(a.allocatedQty || 0)
        })
        const planAllocations = (it.planAllocations || []).map(a => ({
          ...a,
          // 编辑时把“本单已占用”加回可分配额度，避免无法原样保存
          remainingQty: Number(a.remainingQty || 0) + Number(allocationQtyMap[a.planItemId] || 0)
        }))
        const rowRemaining = planAllocations.reduce((sum, a) => sum + Number(a.remainingQty || 0), 0)
        return {
          ...it,
          planAllocations,
          remainingQty: rowRemaining,
          purchasedQty: Math.max(0, Number(it.purchasedQty || 0) - Number(saved.orderQty || 0)),
          orderQty: saved.orderQty || 0,
          unitPrice: Number(saved.unitPrice || it.unitPrice || 0),
          remark: saved.remark || ''
        }
      })
    }
  })
}

const fetchPlanOptions = async (alwaysIncludePlanIds = []) => {
  const include = (alwaysIncludePlanIds || []).filter((id) => id != null)
  await request.get('/purchasePlan/page', {
    status: 1,
    currentPage: 1,
    size: 999,
    onlyWithPurchaseRemaining: true,
    alwaysIncludePlanIds: include.length ? include.join(',') : undefined
  }, {
    showDefaultMsg: false,
    onSuccess: (res) => {
      planOptions.value = res.records || []
    }
  })
}

const fetchSupplierOptions = async () => {
  supplierOptions.value = []
}

const fetchSupplierNameMap = async () => {
  await request.get('/supplier/page', {
    currentPage: 1,
    size: 999,
    status: 1
  }, {
    showDefaultMsg: false,
    onSuccess: (res) => {
      const records = res.records || []
      Object.keys(supplierNameMap).forEach(key => delete supplierNameMap[key])
      records.forEach(s => {
        supplierNameMap[s.id] = s.name
      })
    }
  })
}

const onPlansChange = async (planIds, preserveSupplier = false) => {
  if (!planIds || planIds.length === 0) {
    sourcePlanItems.value = []
    createForm.items = []
    supplierOptions.value = []
    return
  }
  if (!preserveSupplier) {
    createForm.supplierId = null
  }
  supplierMedicineIdSet.value = new Set()
  sourcePlanItems.value = []
  await request.get('/purchasePlan/suppliers', { planIds: planIds.join(',') }, {
    showDefaultMsg: false,
    onSuccess: (res) => {
      supplierOptions.value = res || []
    }
  })
  const planDetails = []
  for (const planId of planIds) {
    await request.get(`/purchasePlan/${planId}`, {}, {
      showDefaultMsg: false,
      onSuccess: (res) => planDetails.push(res)
    })
  }
  const merged = {}
  planDetails.forEach((plan) => {
    (plan?.items || []).forEach((it) => {
      const planQty = it.planQty || 0
      const purchasedQty = it.purchasedQty || 0
      const remainingQty = planQty - purchasedQty
      if (!merged[it.medicineId]) {
        merged[it.medicineId] = {
          medicineId: it.medicineId,
          medicine: it.medicine,
          planQty: 0,
          purchasedQty: 0,
          remainingQty: 0,
          orderQty: 0,
          unitPrice: Number(it.medicine?.purchasePrice || 0),
          remark: '',
          planAllocations: []
        }
      }
      merged[it.medicineId].planQty += planQty
      merged[it.medicineId].purchasedQty += purchasedQty
      merged[it.medicineId].remainingQty += remainingQty
      merged[it.medicineId].planAllocations.push({
        planItemId: it.id,
        remainingQty
      })
    })
  })
  Object.values(merged).forEach((row) => {
    row.orderQty = row.remainingQty
  })
  sourcePlanItems.value = Object.values(merged)
  createForm.items = filterItemsBySupplier(sourcePlanItems.value)
  if (preserveSupplier && createForm.supplierId) {
    const exists = (supplierOptions.value || []).some(s => s.id === createForm.supplierId)
    if (!exists) {
      createForm.supplierId = null
      createForm.items = sourcePlanItems.value
    }
  }
}

const onSupplierChange = async (supplierId) => {
  if (!supplierId) {
    supplierMedicineIdSet.value = new Set()
    return
  }
  await request.get('/medicine/list', {}, {
    showDefaultMsg: false,
    onSuccess: (res) => {
      const ids = (res || []).filter(m => m.supplierId === supplierId).map(m => m.id)
      supplierMedicineIdSet.value = new Set(ids)
      createForm.items = filterItemsBySupplier(sourcePlanItems.value || [])
    }
  })
}

const filterItemsBySupplier = (items) => {
  if (!createForm.supplierId) return items
  if (supplierMedicineIdSet.value.size === 0) return []
  return (items || []).filter(i => supplierMedicineIdSet.value.has(i.medicineId))
}

const resetCreate = () => {
  isEdit.value = false
  editingOrderId.value = null
  draftPreviewNo.value = ''
  draftCreateTime.value = ''
  createForm.planIds = []
  createForm.supplierId = null
  createForm.remark = ''
  createForm.items = []
  sourcePlanItems.value = []
}

const saveOrder = async () => {
  const buildAllocations = (row) => {
    let left = Number(row.orderQty || 0)
    const result = []
    ;(row.planAllocations || []).forEach((p) => {
      if (left <= 0) return
      const canUse = Number(p.remainingQty || 0)
      const allocatedQty = Math.min(left, canUse)
      if (allocatedQty > 0) {
        result.push({ planItemId: p.planItemId, allocatedQty })
        left -= allocatedQty
      }
    })
    return result
  }
  const items = (createForm.items || [])
    .filter(i => i.orderQty && i.orderQty > 0)
    .map(i => ({
      medicineId: i.medicineId,
      orderQty: i.orderQty,
      unitPrice: i.unitPrice,
      remark: i.remark,
      planAllocations: buildAllocations(i)
    }))
  if (!createForm.planIds?.length || !createForm.supplierId || items.length === 0) return

  saving.value = true
  try {
    const payload = {
      planIds: createForm.planIds,
      supplierId: createForm.supplierId,
      remark: createForm.remark,
      items
    }
    if (isEdit.value && editingOrderId.value) {
      await request.put(`/purchaseOrder/${editingOrderId.value}`, payload, {
        successMsg: '编辑采购单成功',
        onSuccess: () => {
          createVisible.value = false
          fetchOrders()
        }
      })
    } else {
      await request.post('/purchaseOrder', { ...payload, orderNo: draftPreviewNo.value }, {
        successMsg: '创建采购单成功',
        onSuccess: async (res) => {
          createVisible.value = false
          await fetchOrders()
          if (res?.id) {
            await openDetailById(res.id)
          }
        }
      })
    }
  } finally {
    saving.value = false
  }
}

const openDetailById = async (id) => {
  await request.get(`/purchaseOrder/${id}`, {}, {
    showDefaultMsg: false,
    onSuccess: (res) => {
      Object.assign(detail, res || {})
      detailVisible.value = true
    }
  })
}

const openDetail = async (row) => {
  await openDetailById(row.id)
}

const sendOrder = async (row) => {
  await ElMessageBox.confirm(`确定发送采购单 ${row.orderNo} 吗？发送后才能创建验收单。`, '提示', { type: 'warning' })
  await request.put(`/purchaseOrder/send/${row.id}`, {}, {
    successMsg: '发送成功',
    onSuccess: () => fetchOrders()
  })
}

const deleteOrder = async (row) => {
  await ElMessageBox.confirm(`确定删除采购单 ${row.orderNo} 吗？`, '提示', { type: 'warning' })
  await request.delete(`/purchaseOrder/${row.id}`, {
    successMsg: '删除成功',
    onSuccess: () => fetchOrders()
  })
}

const getStatusText = (status) => {
  switch (status) {
    case 0: return '草稿'
    case 1: return '已发送'
    case 2: return '已验收完成'
    default: return status
  }
}
const getStatusTagType = (status) => {
  switch (status) {
    case 0: return 'info'
    case 1: return 'success'
    case 2: return 'warning'
    default: return ''
  }
}

onMounted(() => {
  fetchSupplierNameMap()
  fetchOrders()
})
</script>

<style lang="scss" scoped>
.purchase-order {
  .card-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
  }
  .header-actions {
    display: flex;
    gap: 10px;
  }
  .search-form {
    margin-bottom: 16px;
  }
  .pagination-container {
    display: flex;
    justify-content: flex-end;
    margin-top: 16px;
  }
  .doc-preview {
    border: 1px solid #dcdfe6;
    border-radius: 4px;
    padding: 10px 12px;
    margin-bottom: 12px;
    background: #fafafa;
  }
  .create-doc-layout {
    width: 100%;
    box-sizing: border-box;
    padding: 0 12px;
  }
  .create-meta-strip {
    margin-bottom: 12px;
  }
  .create-meta-line {
    display: flex;
    flex-wrap: wrap;
    gap: 16px 24px;
    align-items: center;
  }
  .create-meta-item {
    font-size: var(--el-form-label-font-size, 14px);
    line-height: var(--el-form-line-height, 22px);
    color: var(--el-text-color-regular);
  }
  .items-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin: 10px 0;
    padding: 0 12px;
    box-sizing: border-box;
    .items-title {
      font-weight: 600;
      font-size: 14px;
      line-height: 22px;
      color: var(--el-text-color-primary);
      text-align: left;
    }
  }
  .create-doc-layout > .items-header.create-basic-header-row {
    padding-left: 0;
    padding-right: 0;
    margin-top: 0;
    margin-bottom: 8px;
    justify-content: flex-start;
  }
  .create-purchase-detail-header {
    justify-content: flex-start;
  }
  .create-dialog-table-wrap {
    padding: 0 12px;
    box-sizing: border-box;
  }
  .create-main-form {
    width: 100%;
    :deep(.el-form-item__content) {
      flex: 1;
      min-width: 0;
    }
    :deep(.el-input),
    :deep(.el-textarea) {
      width: 100%;
    }
    .create-field-full {
      width: 100%;
    }
    &.create-main-form--top {
      :deep(.el-form-item) {
        margin-bottom: 14px;
      }
      :deep(.el-form-item__label) {
        padding-bottom: 4px;
        line-height: 1.4;
      }
    }
  }
  .create-form-row {
    width: 100%;
  }
  .create-dialog-follow {
    margin-left: 12px;
    margin-right: 12px;
    box-sizing: border-box;
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
  .doc-preview-total {
    margin-top: 10px;
    text-align: right;
    font-weight: 600;
  }
}
</style>

