<template>
  <div class="collections-page">
    <AppHeader active="collections" />

    <main class="ch-container main">
      <div class="page-head ch-page-head ch-fade-up">
        <h2>我的收藏</h2>
        <p>你收藏的资料都会汇总在这里，方便随时回来看</p>
      </div>

      <div class="collection-list">
        <!-- 骨架屏 -->
        <template v-if="loading && !list.length">
          <div v-for="n in 3" :key="'sk' + n" class="col-card ch-card">
            <div class="ch-skeleton sk-line" style="width: 45%"></div>
            <div class="ch-skeleton sk-line" style="width: 70%"></div>
          </div>
        </template>

        <template v-else-if="list.length">
          <div
            v-for="(c, i) in list"
            :key="c.id"
            class="col-card ch-card ch-card--interactive ch-fade-up"
            :class="`ch-delay-${(i % 4) + 1}`"
            @click="goDetail(c.resourceId)"
          >
            <div class="col-icon" :class="'type-' + (c.type || 'OTHER')">
              {{ typeShort(c.type) }}
            </div>

            <div class="col-body">
              <h3 class="ch-ellipsis">{{ c.title || '（资料已删除）' }}</h3>
              <p class="col-meta">
                <span class="type-chip">{{ typeLabel(c.type) }}</span>
                <span v-if="c.courseName">{{ c.courseName }}</span>
                <span v-if="c.uploaderName">{{ c.uploaderName }}</span>
                <span v-if="c.readableSize">{{ c.readableSize }}</span>
                <span>下载 {{ c.downloadCount || 0 }}</span>
              </p>
              <p class="col-file ch-ellipsis">
                <el-icon><Document /></el-icon>{{ c.fileName || '无文件' }}
              </p>
            </div>

            <div class="col-side">
              <span class="col-time">{{ c.createdAt }}</span>
              <el-button
                link
                type="danger"
                size="small"
                :loading="removingId === c.id"
                @click.stop="handleRemove(c)"
              >
                取消收藏
              </el-button>
            </div>
          </div>
        </template>

        <el-empty
          v-else-if="!loading"
          description="还没有收藏任何资料"
        >
          <el-button type="primary" @click="$router.push('/resources')">
            去资料库逛逛
          </el-button>
        </el-empty>
      </div>

      <div v-if="total > 0" class="pager">
        <el-pagination
          background
          layout="prev, pager, next, total"
          :total="total"
          :current-page="page"
          :page-size="pageSize"
          @current-change="handlePageChange"
        />
      </div>
    </main>

    <AppFooter />
  </div>
</template>

<script setup>
/**
 * 我的收藏
 *
 * 数据来自 GET /api/collections —— 后端已经把资料标题、课程名、
 * 上传者、文件名一次性 JOIN 出来了，前端不用再逐条查资料（避免 N+1）。
 */
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Document } from '@element-plus/icons-vue'
import AppHeader from '@/components/AppHeader.vue'
import AppFooter from '@/components/AppFooter.vue'
import { getMyCollections, uncollectResource } from '@/api/collection'

const router = useRouter()

const loading = ref(false)
const list = ref([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(10)
const removingId = ref(null)

const TYPE_MAP = {
  TEXTBOOK: { short: '教', label: '教材' },
  LECTURE: { short: '讲', label: '讲义' },
  EXAM: { short: '题', label: '真题' },
  NOTES: { short: '记', label: '笔记' },
  ASSIGNMENT: { short: '业', label: '作业' },
  VIDEO: { short: '视', label: '视频' },
  OTHER: { short: '他', label: '其他' }
}
function typeShort(t) { return (TYPE_MAP[t] || TYPE_MAP.OTHER).short }
function typeLabel(t) { return (TYPE_MAP[t] || TYPE_MAP.OTHER).label }

async function fetchList() {
  loading.value = true
  try {
    const data = await getMyCollections({ page: page.value, pageSize: pageSize.value })
    list.value = data.list || []
    total.value = data.total || 0
  } catch (e) {
    list.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

async function handleRemove(c) {
  try {
    await ElMessageBox.confirm(
      `确定取消收藏《${c.title}》吗？`,
      '取消收藏',
      { type: 'warning', confirmButtonText: '确定', cancelButtonText: '再想想' }
    )
  } catch (e) {
    return
  }
  removingId.value = c.id
  try {
    await uncollectResource(c.resourceId)
    ElMessage.success('已取消收藏')
    // 当前页删空了就回退一页
    if (list.value.length === 1 && page.value > 1) {
      page.value -= 1
    }
    fetchList()
  } catch (e) {
    // 拦截器已提示
  } finally {
    removingId.value = null
  }
}

function handlePageChange(p) {
  page.value = p
  fetchList()
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

function goDetail(id) {
  if (id) router.push(`/resources/${id}`)
}

onMounted(fetchList)
</script>

<style scoped>
.main {
  padding: 30px 24px 48px;
}

.page-head {
  margin-bottom: 20px;
}

.collection-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
  min-height: 200px;
}

.col-card {
  display: flex;
  align-items: center;
  gap: 18px;
  padding: 20px;
}

.col-icon {
  width: 54px;
  height: 54px;
  flex-shrink: 0;
  border-radius: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 21px;
  font-weight: 700;
  color: #fff;
  background: linear-gradient(135deg, #4f46e5, #7c3aed);
  box-shadow: 0 10px 22px rgba(15, 23, 42, 0.16);
  transition: var(--ch-transition);
}

.col-card:hover .col-icon {
  transform: scale(1.06) rotate(-4deg);
}

.type-EXAM { background: linear-gradient(135deg, #f97316, #ef4444); }
.type-NOTES { background: linear-gradient(135deg, #10b981, #059669); }
.type-TEXTBOOK { background: linear-gradient(135deg, #8b5cf6, #6d28d9); }
.type-VIDEO { background: linear-gradient(135deg, #f59e0b, #f97316); }
.type-ASSIGNMENT { background: linear-gradient(135deg, #06b6d4, #0ea5e9); }

.col-body {
  flex: 1;
  min-width: 0;
}

.col-body h3 {
  margin: 0 0 8px;
  font-size: 16px;
  font-weight: 700;
}

.col-meta {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
  margin: 0 0 8px;
  font-size: 12.5px;
  color: var(--ch-text-secondary);
}

.type-chip {
  padding: 2px 10px;
  border-radius: 999px;
  font-weight: 500;
  color: var(--ch-primary-dark);
  background: rgba(79, 70, 229, 0.08);
}

.col-file {
  display: flex;
  align-items: center;
  gap: 6px;
  margin: 0;
  font-size: 12.5px;
  color: var(--ch-text-secondary);
  max-width: 460px;
}

.col-side {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 8px;
  flex-shrink: 0;
}

.col-time {
  font-size: 12px;
  color: var(--ch-text-placeholder);
}

.sk-line {
  height: 14px;
}

.pager {
  display: flex;
  justify-content: center;
  margin-top: 30px;
}

@media (max-width: 700px) {
  .main {
    padding: 20px 16px 40px;
  }

  .col-side {
    display: none;
  }
}
</style>
