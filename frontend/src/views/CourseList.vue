<template>
  <div class="course-page">
    <AppHeader active="courses" />

    <main class="ch-container main">
      <!-- 页头 -->
      <div class="page-head ch-fade-up">
        <div class="ch-page-head">
          <h2>课程库</h2>
          <p>按学院与关键词查找课程，加入后即可查看课程资料</p>
        </div>
        <el-button
          v-if="userStore.isLoggedIn"
          type="primary"
          round
          :icon="Plus"
          @click="openCreate"
        >
          新建课程
        </el-button>
      </div>

      <!-- 筛选栏 -->
      <div class="filter-bar ch-card ch-fade-up ch-delay-1">
        <el-input
          v-model="query.keyword"
          placeholder="搜索课程名或教师"
          clearable
          class="filter-input"
          @keyup.enter="handleSearch"
          @clear="handleSearch"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>

        <el-select
          v-model="query.collegeId"
          placeholder="全部学院"
          clearable
          class="filter-select"
          @change="handleSearch"
        >
          <el-option
            v-for="c in colleges"
            :key="c.id"
            :label="c.name"
            :value="c.id"
          />
        </el-select>

        <el-button type="primary" @click="handleSearch">查询</el-button>
        <el-button @click="handleReset">重置</el-button>

        <span v-if="!loading" class="result-count">共 {{ total }} 门课程</span>
      </div>

      <!-- 课程列表 -->
      <div class="course-list">
        <!-- 骨架屏 -->
        <template v-if="loading && !courses.length">
          <div v-for="n in 6" :key="'sk' + n" class="course-card ch-card">
            <div class="ch-skeleton sk-cover"></div>
            <div class="sk-body">
              <div class="ch-skeleton sk-line" style="width: 60%"></div>
              <div class="ch-skeleton sk-line" style="width: 40%"></div>
              <div class="ch-skeleton sk-line" style="width: 90%"></div>
              <div class="ch-skeleton sk-line" style="width: 70%"></div>
            </div>
          </div>
        </template>

        <template v-else-if="courses.length">
          <div
            v-for="(c, i) in courses"
            :key="c.id"
            class="course-card ch-card ch-card--interactive ch-fade-up"
            :class="`ch-delay-${(i % 3) + 1}`"
            @click="goDetail(c)"
          >
            <div class="course-cover" :style="{ background: coverFor(c) }">
              {{ (c.name || '课').charAt(0) }}
            </div>
            <div class="course-body">
              <h3 class="ch-ellipsis">{{ c.name }}</h3>
              <p class="course-meta">
                <span v-if="c.collegeName" class="meta-chip">{{ c.collegeName }}</span>
                <span v-if="c.teacher">{{ c.teacher }}</span>
                <span v-if="c.term">{{ c.term }}</span>
                <span v-if="c.status === 'ARCHIVED'" class="tag-archived">已归档</span>
              </p>
              <p class="course-desc ch-clamp-2">{{ c.description || '暂无课程简介' }}</p>
              <div class="course-foot">
                <div class="stats">
                  <span><el-icon><User /></el-icon>{{ c.memberCount || 0 }}</span>
                  <span><el-icon><Files /></el-icon>{{ c.resourceCount || 0 }}</span>
                </div>
                <el-button
                  size="small"
                  type="primary"
                  plain
                  round
                  @click.stop="handleJoin(c)"
                >
                  加入课程
                </el-button>
              </div>
            </div>
          </div>
        </template>

        <div v-else class="empty-wrap">
          <el-empty description="没有找到符合条件的课程">
            <el-button @click="handleReset">清空筛选条件</el-button>
          </el-empty>
        </div>
      </div>

      <!-- 分页 -->
      <div v-if="total > 0" class="pager">
        <el-pagination
          background
          layout="prev, pager, next, total"
          :total="total"
          :current-page="query.page"
          :page-size="query.pageSize"
          @current-change="handlePageChange"
        />
      </div>
    </main>

    <AppFooter />

    <!-- ==================== 新建课程对话框 ==================== -->
    <el-dialog v-model="dialogVisible" title="新建课程" width="560px">
      <el-form
        ref="createFormRef"
        :model="createForm"
        :rules="createRules"
        label-width="84px"
      >
        <el-form-item label="课程名称" prop="name">
          <el-input v-model="createForm.name" placeholder="如：数据结构与算法" />
        </el-form-item>
        <el-form-item label="课程编号" prop="courseCode">
          <el-input v-model="createForm.courseCode" placeholder="如：DS-204" />
        </el-form-item>
        <el-form-item label="所属学院" prop="collegeId">
          <el-select
            v-model="createForm.collegeId"
            placeholder="请选择学院"
            style="width: 100%"
            clearable
          >
            <el-option
              v-for="c in colleges"
              :key="c.id"
              :label="c.name"
              :value="c.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="授课教师" prop="teacher">
          <el-input v-model="createForm.teacher" placeholder="如：张老师" />
        </el-form-item>
        <el-form-item label="开课学期" prop="term">
          <el-input v-model="createForm.term" placeholder="如：2026-2027-1" />
        </el-form-item>
        <el-form-item label="学分" prop="credit">
          <el-input-number
            v-model="createForm.credit"
            :min="0"
            :max="99"
            :step="0.5"
            :precision="1"
          />
        </el-form-item>
        <el-form-item label="课程简介" prop="description">
          <el-input
            v-model="createForm.description"
            type="textarea"
            :rows="3"
            placeholder="简单介绍这门课的内容（选填）"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitCreate">
          确定创建
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
/**
 * 课程列表页
 *
 * 这个页面同时演示了两条完整链路：
 *   1. 【读】进入页面 → 调用 GET /api/courses → 渲染卡片 → 分页/筛选再请求
 *   2. 【写】新建课程 → POST /api/courses → 成功后重新拉列表刷新
 *
 * 未登录用户可以浏览课程，但点「加入课程」会被引导去登录。
 */
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Search, Plus, User, Files } from '@element-plus/icons-vue'
import AppHeader from '@/components/AppHeader.vue'
import AppFooter from '@/components/AppFooter.vue'
import { getCourses, createCourse, joinCourse } from '@/api/course'
import { getColleges } from '@/api/college'
import { useUserStore } from '@/store/user'

const router = useRouter()
const userStore = useUserStore()

const loading = ref(false)
const submitting = ref(false)
const courses = ref([])
const colleges = ref([])
const total = ref(0)

const query = reactive({
  page: 1,
  pageSize: 9,
  keyword: '',
  collegeId: null
})

/** 一组渐变色，按课程名做稳定映射，让每门课有自己固定的封面配色 */
const COVER_GRADIENTS = [
  'linear-gradient(135deg, #4f46e5, #7c3aed)',
  'linear-gradient(135deg, #06b6d4, #0ea5e9)',
  'linear-gradient(135deg, #f59e0b, #f97316)',
  'linear-gradient(135deg, #10b981, #059669)',
  'linear-gradient(135deg, #ec4899, #db2777)',
  'linear-gradient(135deg, #8b5cf6, #6d28d9)'
]

function coverFor(c) {
  const key = String(c.name || '')
  let sum = 0
  for (let i = 0; i < key.length; i++) sum += key.charCodeAt(i)
  return COVER_GRADIENTS[sum % COVER_GRADIENTS.length]
}

// ==================== 列表 ====================
async function fetchList() {
  loading.value = true
  try {
    const data = await getCourses({
      page: query.page,
      pageSize: query.pageSize,
      keyword: query.keyword || undefined,
      collegeId: query.collegeId || undefined
    })
    courses.value = data.list || []
    total.value = data.total || 0
  } catch (e) {
    courses.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  query.page = 1
  fetchList()
}

function handleReset() {
  query.keyword = ''
  query.collegeId = null
  query.page = 1
  fetchList()
}

function handlePageChange(page) {
  query.page = page
  fetchList()
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

/** 跳转到课程详情页 */
function goDetail(course) {
  router.push(`/courses/${course.id}`)
}

// ==================== 加入课程 ====================
async function handleJoin(course) {
  if (!userStore.isLoggedIn) {
    ElMessage.warning('请先登录后再加入课程')
    router.push({ path: '/login', query: { redirect: '/courses' } })
    return
  }
  try {
    await joinCourse(course.id)
    ElMessage.success(`已加入《${course.name}》`)
    fetchList() // 刷新，成员数会 +1
  } catch (e) {
    // 重复加入等错误已由拦截器提示
  }
}

// ==================== 新建课程 ====================
const dialogVisible = ref(false)
const createFormRef = ref(null)
const createForm = reactive({
  name: '',
  courseCode: '',
  collegeId: null,
  teacher: '',
  term: '',
  credit: 2,
  description: ''
})
const createRules = {
  name: [
    { required: true, message: '请输入课程名称', trigger: 'blur' },
    { max: 100, message: '课程名称最长 100 个字符', trigger: 'blur' }
  ]
}

function openCreate() {
  dialogVisible.value = true
}

async function submitCreate() {
  if (!createFormRef.value) return
  try {
    await createFormRef.value.validate()
  } catch (e) {
    return
  }
  submitting.value = true
  try {
    await createCourse({ ...createForm })
    ElMessage.success('课程创建成功')
    dialogVisible.value = false
    // 清空表单并回到第一页看新课程
    Object.assign(createForm, {
      name: '',
      courseCode: '',
      collegeId: null,
      teacher: '',
      term: '',
      credit: 2,
      description: ''
    })
    query.page = 1
    fetchList()
  } catch (e) {
    // 拦截器已提示
  } finally {
    submitting.value = false
  }
}

// ==================== 初始化 ====================
onMounted(async () => {
  fetchList()
  try {
    colleges.value = await getColleges()
  } catch (e) {
    colleges.value = []
  }
})
</script>

<style scoped>
.main {
  padding: 30px 24px 48px;
}

.page-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 20px;
  flex-wrap: wrap;
}

/* ---------- 筛选栏 ---------- */
.filter-bar {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
  padding: 16px 20px;
  margin-bottom: 20px;
}

.filter-input {
  max-width: 280px;
}

.filter-select {
  width: 180px;
}

.result-count {
  margin-left: auto;
  font-size: 13px;
  color: var(--ch-text-secondary);
}

/* ---------- 课程卡片 ---------- */
.course-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(340px, 1fr));
  gap: 18px;
  min-height: 200px;
}

.empty-wrap {
  grid-column: 1 / -1;
}

.course-card {
  display: flex;
  gap: 16px;
  padding: 20px;
}

.course-cover {
  width: 58px;
  height: 58px;
  flex-shrink: 0;
  border-radius: 16px;
  color: #fff;
  font-size: 24px;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 10px 22px rgba(15, 23, 42, 0.16);
  transition: var(--ch-transition);
}

.course-card:hover .course-cover {
  transform: scale(1.06) rotate(-4deg);
}

.course-body {
  flex: 1;
  min-width: 0;
}

.course-body h3 {
  margin: 2px 0 8px;
  font-size: 16.5px;
  font-weight: 700;
}

.course-meta {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  margin: 0 0 10px;
  font-size: 12.5px;
  color: var(--ch-text-secondary);
}

.meta-chip {
  padding: 2px 9px;
  border-radius: 999px;
  background: rgba(79, 70, 229, 0.08);
  color: var(--ch-primary-dark);
  font-weight: 500;
}

.tag-archived {
  color: var(--ch-warning);
  border: 1px solid rgba(245, 158, 11, 0.4);
  border-radius: 6px;
  padding: 0 6px;
  font-size: 11px;
}

.course-desc {
  margin: 0 0 14px;
  font-size: 13.5px;
  color: var(--ch-text-regular);
  line-height: 1.7;
  min-height: 46px;
}

.course-foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.stats {
  display: flex;
  gap: 14px;
  font-size: 12.5px;
  color: var(--ch-text-secondary);
}

.stats span {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

/* ---------- 骨架屏 ---------- */
.sk-cover {
  width: 58px;
  height: 58px;
  border-radius: 16px;
  flex-shrink: 0;
}

.sk-body {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding-top: 4px;
}

.sk-line {
  height: 12px;
}

/* ---------- 分页 ---------- */
.pager {
  display: flex;
  justify-content: center;
  margin-top: 30px;
}

@media (max-width: 600px) {
  .main {
    padding: 22px 16px 40px;
  }

  .filter-input,
  .filter-select {
    max-width: none;
    width: 100%;
  }

  .result-count {
    margin-left: 0;
  }
}
</style>
