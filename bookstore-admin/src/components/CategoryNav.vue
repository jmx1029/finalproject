<template>
  <div class="category-nav">
    <!-- 主网格（默认只显示第一行） -->
    <div
      class="category-grid"
      :class="{ 'grid-expanded': isExpanded }"
    >
      <span
        class="category-nav-item"
        :class="{ active: isAllSelected }"
        @click="selectCategory(null)"
      >
        全部
      </span>
      <span
        v-for="cat in categories"
        :key="cat.id"
        class="category-nav-item"
        :class="{ active: isSelected(cat.id) }"
        @click="selectCategory(cat.id)"
      >
        {{ cat.name }}
      </span>
    </div>

    <!-- 展开/收起按钮（超过一行时显示） -->
    <div v-if="hasMore" class="toggle-row">
      <el-button link type="primary" size="small" @click="toggleExpand">
        {{ isExpanded ? '收起 ↑' : '展开全部 ↓' }}
      </el-button>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue'

const props = defineProps({
  categories: {
    type: Array,
    default: () => []
  },
  modelValue: {
    type: [Number, String],
    default: null
  }
})

const emit = defineEmits(['update:modelValue', 'change'])

// ========== 统一 ID 为 Number 或 null ==========
const normalizeId = (val) => {
  if (val === null || val === undefined || val === '') return null
  const n = Number(val)
  return isNaN(n) ? null : n
}

// 每行显示的列数
const COLS_PER_ROW = 8

// 内部状态
const selectedId = ref(normalizeId(props.modelValue))
const isExpanded = ref(false)

// 监听父组件传入变化
watch(
  () => props.modelValue,
  (newVal) => { selectedId.value = normalizeId(newVal) }
)

// 是否选中"全部"
const isAllSelected = computed(() => selectedId.value === null)

// 判断某个分类是否选中
const isSelected = (catId) => {
  return selectedId.value !== null && Number(catId) === Number(selectedId.value)
}

// 分类总数是否超过一行（有展开的必要）
const hasMore = computed(() => {
  // +1 是"全部"
  return (props.categories.length + 1) > COLS_PER_ROW
})

// 展开后如果之前选中的在第二行以下，自动展开
watch(() => props.modelValue, (val) => {
  const id = normalizeId(val)
  if (id !== null) {
    // 找到这个分类在哪个位置
    const idx = props.categories.findIndex(c => Number(c.id) === id)
    if (idx >= COLS_PER_ROW - 1) { // -1 因为"全部"占了第一个位置
      isExpanded.value = true
    }
  }
})

const selectCategory = (id) => {
  const normalized = normalizeId(id)
  selectedId.value = normalized
  emit('update:modelValue', normalized)
  emit('change', normalized)
}

const toggleExpand = () => {
  isExpanded.value = !isExpanded.value
}
</script>

<style scoped>
.category-nav {
  background: #fff;
  border-radius: 8px;
  padding: 12px 16px 8px;
  margin-bottom: 12px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06);
}

/* 网格布局：每行固定 COLS_PER_ROW 列，自动换行 */
.category-grid {
  display: grid;
  grid-template-columns: repeat(8, 1fr);
  gap: 8px;
}

/* 默认只显示第一行（隐藏超出的行） */
.category-grid:not(.grid-expanded) {
  max-height: 36px;
  overflow: hidden;
}

/* 展开后显示全部 */
.category-grid.grid-expanded {
  max-height: none;
}

/* 每个分类项固定高度，确保一行高度一致 */
.category-nav-item {
  text-align: center;
  padding: 0 12px;
  height: 36px;
  line-height: 36px;
  box-sizing: border-box;
  font-size: 14px;
  color: #606266;
  cursor: pointer;
  border-radius: 8px;
  transition: all 0.25s ease;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  user-select: none;
  min-width: 0;
}
.category-nav-item:hover {
  color: #409EFF;
  background: #ecf5ff;
}
.category-nav-item.active {
  color: #fff;
  background: linear-gradient(135deg, #409EFF 0%, #66b1ff 100%);
  box-shadow: 0 2px 8px rgba(64, 158, 255, 0.3);
  font-weight: 500;
}

/* 展开/收起按钮行 */
.toggle-row {
  text-align: center;
  padding-top: 6px;
}

@media (max-width: 1024px) {
  .category-grid { grid-template-columns: repeat(6, 1fr); }
}
@media (max-width: 768px) {
  .category-grid { grid-template-columns: repeat(4, 1fr); }
  .category-nav-item { font-size: 13px; height: 32px; line-height: 32px; }
}
@media (max-width: 480px) {
  .category-grid { grid-template-columns: repeat(3, 1fr); }
}
</style>
