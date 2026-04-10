<template>
  <div class="stock-in-order">
    <el-card class="box-card">
      <template #header>
        <div class="card-header">
          <h3>入库单</h3>
          <el-button
            type="primary"
            @click="openCreate"
          >
            新增入库单
          </el-button>
        </div>
      </template>

      <el-form
        :model="searchForm"
        :inline="true"
        class="search-form"
      >
        <el-form-item label="入库单号">
          <el-input
            v-model="searchForm.stockInNo"
            placeholder="请输入入库单号"
            clearable
          />
        </el-form-item>
        <el-form-item label="验收单ID">
          <el-input
            v-model="searchForm.acceptanceId"
            placeholder="可选"
            clearable
          />
        </el-form-item>
        <el-form-item label="状态">
          <el-select
            v-model="searchForm.status"
            placeholder="请选择状态"
            clearable
            style="width: 140px"
          >
            <el-option
              label="草稿"
              :value="0"
            />
            <el-option
              label="已过账"
              :value="1"
            />
          </el-select>
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

      <!-- v-loading 放在表格外层，避免与表头/表体双表格布局冲突导致列错位；保留一列仅用 min-width 便于列宽与 colgroup 对齐 -->
      <div
        v-loading="loading"
        class="stock-in-order-table-wrap"
      >
        <el-table
          :data="tableData"
          border
          style="width: 100%"
          table-layout="fixed"
          :scrollbar-always-on="true"
        >
          <el-table-column
            label="入库单号"
            prop="stockInNo"
            min-width="170"
            show-overflow-tooltip
          >
            <template #default="scope">
              {{ scope.row.stockInNo ?? '-' }}
            </template>
          </el-table-column>
          <el-table-column
            label="验收单ID"
            min-width="160"
            show-overflow-tooltip
          >
            <template #default="scope">
              {{ formatAcceptanceIds(scope.row) }}
            </template>
          </el-table-column>
          <el-table-column
            label="状态"
            prop="status"
            width="110"
          >
            <template #default="scope">
              <el-tag :type="getStatusTagType(scope.row.status)">
                {{ getStatusText(scope.row.status) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column
            label="入库时间"
            prop="stockInTime"
            min-width="180"
          >
            <template #default="scope">
              {{ formatDate(scope.row.stockInTime) }}
            </template>
          </el-table-column>
          <el-table-column
            label="创建时间"
            prop="createTime"
            min-width="180"
          >
            <template #default="scope">
              {{ formatDate(scope.row.createTime) }}
            </template>
          </el-table-column>
          <el-table-column
            label="操作"
            min-width="260"
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
                @click="post(scope.row)"
              >
                过账
              </el-button>
              <el-button
                v-if="scope.row.status === 0"
                size="small"
                type="danger"
                @click="deleteStockIn(scope.row)"
              >
                删除
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>

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

    <!-- 创建入库单 -->
    <el-dialog
      v-model="createVisible"
      :title="isEdit ? '编辑入库单' : '新增入库单'"
      width="980px"
      @closed="resetCreate"
    >
      <el-form
        :model="createForm"
        label-width="90px"
      >
        <el-form-item label="验收单">
          <el-select
            v-model="createForm.acceptanceIds"
            multiple
            collapse-tags
            collapse-tags-tooltip
            placeholder="请选择已完成验收单（可多选合并入库）"
            filterable
            style="width: 420px"
            @change="onAcceptancesChange"
          >
            <el-option
              v-for="a in acceptanceOptions"
              :key="a.id"
              :label="`${a.acceptanceNo}（ID:${a.id}）`"
              :value="a.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="备注">
          <el-input
            v-model="createForm.remark"
            type="textarea"
            :rows="2"
            placeholder="可选"
          />
        </el-form-item>
      </el-form>

      <el-alert
        title="可多选核验单，明细合并为一张入库单；入库数量不得超过各验收明细合格数量（系统会做校验）。"
        type="info"
        :closable="false"
        show-icon
        style="margin-bottom: 10px"
      />
      <template
        v-for="src in sourceAcceptanceList"
        :key="src.id"
      >
        <div
          v-if="src?.id"
          class="doc-preview"
        >
          <div class="doc-preview-title">
            来源采购核验单
          </div>
          <div class="doc-preview-meta">
            <span>核验单号：{{ src.acceptanceNo || '-' }}</span>
            <span>采购单号：{{ src.purchaseOrder?.orderNo || src.purchaseOrderId || '-' }}</span>
            <span>状态：{{ src.status === 1 ? '已完成' : src.status }}</span>
          </div>
          <el-table
            :data="src.items || []"
            border
            size="small"
            style="width: 100%"
          >
            <el-table-column
              type="index"
              label="序号"
              width="60"
            />
            <el-table-column
              label="产品名称"
              min-width="180"
            >
              <template #default="scope">
                {{ scope.row.medicine?.medicineName || scope.row.medicineId }}
              </template>
            </el-table-column>
            <el-table-column
              prop="orderedQty"
              label="下单数量"
              width="100"
            />
            <el-table-column
              prop="receivedQty"
              label="到货数量"
              width="100"
            />
            <el-table-column
              prop="qualifiedQty"
              label="合格数量"
              width="100"
            />
            <el-table-column
              prop="remark"
              label="备注"
              min-width="140"
            />
          </el-table>
        </div>
      </template>

      <el-table
        :data="createForm.items"
        border
        style="width: 100%"
      >
        <el-table-column
          label="核验单"
          min-width="140"
          show-overflow-tooltip
        >
          <template #default="scope">
            {{ scope.row.acceptanceNo || scope.row.acceptanceId || '-' }}
          </template>
        </el-table-column>
        <el-table-column
          label="药品"
          min-width="200"
        >
          <template #default="scope">
            {{ scope.row.medicine?.medicineName || scope.row.medicineId }}
          </template>
        </el-table-column>
        <el-table-column
          prop="qualifiedQty"
          label="合格数量"
          width="120"
        />
        <el-table-column
          label="入库数量"
          width="160"
        >
          <template #default="scope">
            <el-input-number
              v-model="scope.row.stockInQty"
              :min="0"
              :max="scope.row.qualifiedQty"
              style="width: 100%"
            />
          </template>
        </el-table-column>
        <el-table-column
          label="单位成本"
          width="160"
        >
          <template #default="scope">
            <el-input-number
              v-model="scope.row.unitCost"
              :min="0"
              :precision="2"
              style="width: 100%"
            />
          </template>
        </el-table-column>
      </el-table>

      <template #footer>
        <el-button @click="createVisible = false">
          取消
        </el-button>
        <el-button
          type="primary"
          :loading="saving"
          @click="saveStockIn"
        >
          {{ isEdit ? '保存' : '创建' }}
        </el-button>
      </template>
    </el-dialog>

    <!-- 详情 -->
    <el-dialog
      v-model="detailVisible"
      title="入库单详情"
      width="980px"
    >
      <div class="doc-preview">
        <div class="doc-preview-title">
          采购入库单
        </div>
        <div class="doc-preview-meta">
          <span>入库时间：{{ formatDate(detail.stockInTime || detail.createTime) }}</span>
          <span>入库单号：{{ detail.stockInNo || '-' }}</span>
          <span>状态：{{ getStatusText(detail.status) }}</span>
        </div>
        <div class="doc-preview-meta">
          <span>核验单：{{ formatAcceptanceLine(detail) }}</span>
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
          min-width="220"
        >
          <template #default="scope">
            {{ scope.row.medicine?.medicineName || scope.row.medicineId }}
          </template>
        </el-table-column>
        <el-table-column
          prop="stockInQty"
          label="入库数量"
          width="120"
        />
        <el-table-column
          prop="unitCost"
          label="单位成本"
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
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import request from '@/utils/request'
import { ElMessageBox } from 'element-plus'
import { formatDate } from '@/utils/dateUtils'

const loading = ref(false)
const saving = ref(false)
const tableData = ref([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(10)

const searchForm = reactive({
  stockInNo: '',
  acceptanceId: '',
  status: undefined
})

const createVisible = ref(false)
const isEdit = ref(false)
const editingStockInId = ref(null)
const detailVisible = ref(false)
const detail = reactive({})
const sourceAcceptanceList = ref([])

const acceptanceOptions = ref([])
const createForm = reactive({
  acceptanceIds: [],
  remark: '',
  items: []
})

const formatAcceptanceIds = (row) => {
  if (Array.isArray(row?.acceptanceIds) && row.acceptanceIds.length > 0) {
    return row.acceptanceIds.join(', ')
  }
  return row?.acceptanceId ?? '-'
}

const formatAcceptanceLine = (d) => {
  if (d?.sourceAcceptances?.length) {
    return d.sourceAcceptances.map((a) => `${a.acceptanceNo || ''}(ID:${a.id})`).filter(Boolean).join('、') || '-'
  }
  if (Array.isArray(d?.acceptanceIds) && d.acceptanceIds.length > 0) {
    return d.acceptanceIds.join(', ')
  }
  if (d?.acceptance?.acceptanceNo) {
    return `${d.acceptance.acceptanceNo}(ID:${d.acceptance.id})`
  }
  return d?.acceptanceId ?? '-'
}

const fetchStockIns = async () => {
  loading.value = true
  try {
    await request.get('/stockInOrder/page', {
      stockInNo: searchForm.stockInNo,
      acceptanceId: searchForm.acceptanceId ? Number(searchForm.acceptanceId) : undefined,
      status: searchForm.status,
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
  fetchStockIns()
}
const resetSearch = () => {
  searchForm.stockInNo = ''
  searchForm.acceptanceId = ''
  searchForm.status = undefined
  handleSearch()
}
const handleSizeChange = (val) => {
  pageSize.value = val
  fetchStockIns()
}
const handleCurrentChange = (val) => {
  currentPage.value = val
  fetchStockIns()
}

const fetchAcceptanceOptions = async (alwaysIncludeAcceptanceIds = []) => {
  const include = (alwaysIncludeAcceptanceIds || []).filter((id) => id != null)
  await request.get('/purchaseAcceptance/page', {
    status: 1,
    currentPage: 1,
    size: 999,
    excludeFullyStockPosted: true,
    alwaysIncludeAcceptanceIds: include.length ? include.join(',') : undefined
  }, {
    showDefaultMsg: false,
    onSuccess: (res) => {
      acceptanceOptions.value = res.records || []
    }
  })
}

const openCreate = async () => {
  isEdit.value = false
  editingStockInId.value = null
  createVisible.value = true
  await fetchAcceptanceOptions()
}

const openEdit = async (row) => {
  isEdit.value = true
  editingStockInId.value = row.id
  createVisible.value = true
  await request.get(`/stockInOrder/${row.id}`, {}, {
    showDefaultMsg: false,
    onSuccess: async (res) => {
      const ids = (res?.acceptanceIds && res.acceptanceIds.length > 0)
        ? [...res.acceptanceIds]
        : (res?.acceptanceId ? [res.acceptanceId] : [])
      await fetchAcceptanceOptions(ids)
      createForm.acceptanceIds = ids
      createForm.remark = res?.remark || ''
      await onAcceptancesChange(ids)
      const itemMap = {}
      ;(res?.items || []).forEach(it => {
        itemMap[it.acceptanceItemId] = it
      })
      createForm.items = (createForm.items || []).map(it => {
        const saved = itemMap[it.acceptanceItemId]
        if (!saved) return it
        return {
          ...it,
          stockInQty: saved.stockInQty || 0,
          unitCost: Number(saved.unitCost || it.unitCost || 0)
        }
      })
    }
  })
}

const fetchOrderPriceMap = async (orderId) => {
  if (!orderId) return {}
  try {
    const orderRes = await request.get(`/purchaseOrder/${orderId}`, {}, { showDefaultMsg: false })
    const map = {}
    ;(orderRes?.items || []).forEach((oi) => {
      map[oi.medicineId] = Number(oi.unitPrice || 0)
    })
    return map
  } catch {
    return {}
  }
}

const onAcceptancesChange = async (acceptanceIds) => {
  if (!acceptanceIds || acceptanceIds.length === 0) {
    sourceAcceptanceList.value = []
    createForm.items = []
    return
  }
  try {
    const details = await Promise.all(
      acceptanceIds.map((id) =>
        request.get(`/purchaseAcceptance/${id}`, {}, { showDefaultMsg: false })
      )
    )
    sourceAcceptanceList.value = details.filter((d) => d && d.id)
    const orderPriceCache = {}
    const merged = []
    for (const res of sourceAcceptanceList.value) {
      const orderId = res?.purchaseOrder?.id || res?.purchaseOrderId
      let orderPriceMap = orderPriceCache[orderId]
      if (orderId && orderPriceMap === undefined) {
        orderPriceMap = await fetchOrderPriceMap(orderId)
        orderPriceCache[orderId] = orderPriceMap
      }
      orderPriceMap = orderPriceMap || {}
      for (const it of res.items || []) {
        merged.push({
          acceptanceId: res.id,
          acceptanceNo: res.acceptanceNo,
          acceptanceItemId: it.id,
          medicineId: it.medicineId,
          medicine: it.medicine,
          qualifiedQty: it.qualifiedQty || 0,
          stockInQty: it.qualifiedQty || 0,
          unitCost: Number(orderPriceMap[it.medicineId] || it.medicine?.price || 0)
        })
      }
    }
    createForm.items = merged
  } catch {
    sourceAcceptanceList.value = []
    createForm.items = []
  }
}

const resetCreate = () => {
  isEdit.value = false
  editingStockInId.value = null
  createForm.acceptanceIds = []
  createForm.remark = ''
  createForm.items = []
  sourceAcceptanceList.value = []
}

const saveStockIn = async () => {
  if (!createForm.acceptanceIds?.length) return
  const items = (createForm.items || [])
    .filter(i => i.stockInQty && i.stockInQty > 0)
    .map(i => ({
      acceptanceItemId: i.acceptanceItemId,
      stockInQty: i.stockInQty,
      unitCost: i.unitCost
    }))
  if (items.length === 0) return

  saving.value = true
  try {
    const payload = {
      remark: createForm.remark,
      items
    }
    if (isEdit.value && editingStockInId.value) {
      await request.put(`/stockInOrder/${editingStockInId.value}`, payload, {
        successMsg: '编辑入库单成功',
        onSuccess: () => {
          createVisible.value = false
          fetchStockIns()
        }
      })
    } else {
      await request.post('/stockInOrder', payload, {
        successMsg: '创建入库单成功',
        onSuccess: () => {
          createVisible.value = false
          fetchStockIns()
        }
      })
    }
  } finally {
    saving.value = false
  }
}

const openDetail = async (row) => {
  await request.get(`/stockInOrder/${row.id}`, {}, {
    showDefaultMsg: false,
    onSuccess: (res) => {
      Object.assign(detail, res || {})
      detailVisible.value = true
    }
  })
}

const post = async (row) => {
  await ElMessageBox.confirm(`确定过账入库单 ${row.stockInNo} 吗？过账将写入药品库存。`, '提示', { type: 'warning' })
  await request.put(`/stockInOrder/post/${row.id}`, {}, {
    successMsg: '过账成功',
    onSuccess: () => fetchStockIns()
  })
}

const deleteStockIn = async (row) => {
  await ElMessageBox.confirm(`确定删除入库单 ${row.stockInNo} 吗？`, '提示', { type: 'warning' })
  await request.delete(`/stockInOrder/${row.id}`, {
    successMsg: '删除成功',
    onSuccess: () => fetchStockIns()
  })
}

const getStatusText = (status) => {
  switch (status) {
    case 0: return '草稿'
    case 1: return '已过账'
    default: return status
  }
}
const getStatusTagType = (status) => {
  switch (status) {
    case 0: return 'info'
    case 1: return 'success'
    default: return ''
  }
}

onMounted(() => {
  fetchStockIns()
})
</script>

<style lang="scss" scoped>
.stock-in-order {
  .card-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
  }
  .search-form {
    margin-bottom: 16px;
  }
  .pagination-container {
    display: flex;
    justify-content: flex-end;
    margin-top: 16px;
  }
  .stock-in-order-table-wrap {
    min-height: 120px;
  }
  .doc-preview {
    border: 1px solid #dcdfe6;
    border-radius: 4px;
    padding: 10px 12px;
    margin-bottom: 12px;
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
}
</style>

