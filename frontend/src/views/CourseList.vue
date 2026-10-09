<template>
  <div class="course-page">
    <AppHeader active="courses" />

    <main class="ch-container main">
      <!-- 页头 -->
      <div class="page-head">
        <div>
          <h2>课程库</h2>
          <p>按学院与关键词查找课程，加入后即可查看课程资料</p>
        </div>
        <el-button
          v-if="userStore.isLoggedIn"
          type="primary"
          :icon="Plus"
          @click="openCreate"
        >
          新建课程
        </el-button>
      </div>

      <!-- 筛选栏 -->
      <div class="filter-bar ch-card">
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
      </div>

      <!-- 课程列表 -->
      <div v-loading="loading" class="course-list">
        <template v-if="courses.length">
          <div v-for="c in courses" :key="c.id" class="course-card ch-card">
            <div class="course-cover">{{ (c.name || '课').charAt(0) }}</div>
            <div class="course-body">
              <h3 class="ch-ellipsis">{{ c.name }}</h3>
              <p class="course-meta">
                <span v-if="c.collegeName">{{ c.collegeName }}</span>
                <span v-if="c.teacher">· {{ c.teacher }}</span>
                <span v-if="c.term">· {{ c.term }}</span>
                <span v-if="c.status === 'ARCHIVED'" class="tag-archived">已归档</span>
              </p>
              <p class="course-desc">{{ c.description || '暂无课程简介' }}</p>
              <div class="course-foot">
                <div class="stats">
                  <span>成员 {{ c.memberCount }}</span>
                  <span>资料 {{ c.resourceCount }}</span>
                </div>
                <el-button size="small" type="primary" plain @click="handleJoin(c)">
                  加入课程
                </el-button>
              </div>
            </div>
          </div>
        </template>

        <el-empty
          v-else-if="!loading"
          description="没有找到符合条件的课程"
        />
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

    <!-- ==================== 新建课程对话框 ==================== -->
    <el-dialog v-model="dialogVisible" title="新建课程" width="540px">
      <el-form
        ref="createFormRef"
        :model="createForm"
        :rules="createRules"
        label-width="80px"
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
import { Search, Plus } from '@element-plus/icons-vue'
import AppHeader from '@/components/AppHeader.vue'
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
  padding: 28px 20px 48px;
}

.page-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  margin-bottom: 18px;
}

.page-head h2 {
  margin: 0 0 6px;
  font-size: 22px;
  font-weight: 600;
}

.page-head p {
  margin: 0;
  font-size: 13px;
  color: var(--ch-text-secondary);
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
  max-width: 260px;
}

.filter-select {
  width: 180px;
}

/* ---------- 课程卡片 ---------- */
.course-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 16px;
  min-height: 200px;
}

.course-card {
  display: flex;
  gap: 14px;
  padding: 18px;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.course-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 20px rgba(0, 0, 0, 0.08);
}

.course-cover {
  width: 52px;
  height: 52px;
  flex-shrink: 0;
  border-radius: 12px;
  background: linear-gradient(135deg, #409eff, #337ecc);
  color: #fff;
  font-size: 22px;
  font-weight: 600;
  display: flex;
  align-items: center;
  justify-content: center;
}

.course-body {
  flex: 1;
  min-width: 0;
}

.course-body h3 {
  margin: 2px 0 6px;
  font-size: 16px;
  font-weight: 600;
}

.course-meta {
  margin: 0 0 8px;
  font-size: 12px;
  color: var(--ch-text-secondary);
}

.course-meta span {
  margin-right: 4px;
}

.tag-archived {
  color: var(--ch-warning);
  border: 1px solid var(--ch-warning);
  border-radius: 4px;
  padding: 0 4px;
  font-size: 11px;
}

.course-desc {
  margin: 0 0 12px;
  font-size: 13px;
  color: var(--ch-text-regular);
  line-height: 1.6;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  min-height: 42px;
}

.course-foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.stats {
  display: flex;
  gap: 12px;
  font-size: 12px;
  color: var(--ch-text-secondary);
}

/* ---------- 分页 ---------- */
.pager {
  display: flex;
  justify-content: center;
  margin-top: 28px;
}
</style>
