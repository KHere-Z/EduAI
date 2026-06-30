<template>
  <div class="page-container">
    <div class="page-header">
      <div>
        <h2>👋 下午好，{{ auth.user?.real_name || '老师' }}</h2>
        <p>今天是 {{ today }}，共 {{ stats.todayClasses }} 节课待完成</p>
      </div>
      <el-button type="primary" size="large" @click="$router.push('/coach/classroom')">
        <el-icon style="margin-right:6px"><Reading /></el-icon>开始上课
      </el-button>
    </div>

    <el-row :gutter="20" class="stats-row">
      <el-col :span="6" v-for="s in statCards" :key="s.label">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-icon" :style="{ background: s.bg }">
            <el-icon :size="20"><component :is="s.icon" /></el-icon>
          </div>
          <div class="stat-value">{{ s.value }}</div>
          <div class="stat-label">{{ s.label }}</div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20">
      <!-- 快捷操作 -->
      <el-col :span="8">
        <el-card>
          <template #header><span class="card-title">⚡ 快捷操作</span></template>
          <div class="action-grid">
            <div v-for="a in actions" :key="a.text" class="action-item" @click="$router.push(a.path)">
              <div class="action-icon" :style="{ background: a.bg }">
                <el-icon :size="18"><component :is="a.icon" /></el-icon>
              </div>
              <span class="action-text">{{ a.text }}</span>
            </div>
          </div>
        </el-card>
      </el-col>

      <!-- 学科入口 -->
      <el-col :span="8">
        <el-card>
          <template #header><span class="card-title">📚 学科中心</span></template>
          <div class="subject-grid">
            <div
              v-for="s in subjects"
              :key="s.value"
              class="subject-chip"
              @click="$router.push(`/coach/subject/${s.value}/wrong-questions`)"
            >{{ s.icon }} {{ s.label }}</div>
          </div>
        </el-card>
      </el-col>

      <!-- 今日概览 -->
      <el-col :span="8">
        <el-card>
          <template #header><span class="card-title">📊 今日概览</span></template>
          <div class="today-item" v-for="t in todayStats" :key="t.label">
            <span>{{ t.label }}</span><strong>{{ t.value }}</strong>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useAuthStore } from '@/store/auth'
const auth = useAuthStore()

const today = new Date().toLocaleDateString('zh-CN', { year:'numeric', month:'long', day:'numeric', weekday:'long' })

const stats = {
  todayClasses: 4,
  weeklyHours: 18,
  newStudents: 3,
  completedTests: 12
}

const statCards = [
  { label:'我的学员', value:28, icon:'User',    bg:'#EEF2FF' },
  { label:'今日课堂', value:4,  icon:'Reading', bg:'#ECFDF5' },
  { label:'本周课时', value:18, icon:'Calendar',bg:'#FFF7ED' },
  { label:'待批作业', value:7,  icon:'Edit',    bg:'#FEF2F2' }
]

const subjectMeta = { chinese:{label:'语文',icon:'📝'}, math:{label:'数学',icon:'📐'}, english:{label:'英语',icon:'📖'}, physics:{label:'物理',icon:'⚛️'}, chemistry:{label:'化学',icon:'🧪'}, biology:{label:'生物',icon:'🧬'}, history:{label:'历史',icon:'📜'}, politics:{label:'政治',icon:'⚖️'}, geography:{label:'地理',icon:'🌍'} }

// 根据老师任教学科动态生成
const teacherSubjects = computed(() => auth.user?.subjects || [])
const hasEnglish = computed(() => teacherSubjects.value.includes('english'))

const subjects = computed(() =>
  teacherSubjects.value.filter(k => subjectMeta[k]).map(k => ({ value:k, ...subjectMeta[k] }))
)

const actions = computed(() => {
  const list = []
  if (hasEnglish.value) {
    list.push(
      { text:'英语课堂', icon:'Reading',    path:'/teacher/classroom',   bg:'#EEF2FF' },
      { text:'AI 阅读',  icon:'Notebook',   path:'/teacher/ai-reading',  bg:'#ECFDF5' },
      { text:'词汇测试', icon:'Document',   path:'/teacher/vocab-test',  bg:'#FFF7ED' },
      { text:'AI 口语',  icon:'MagicStick', path:'/teacher/ai-dialogue', bg:'#F5F3FF' }
    )
  }
  if (teacherSubjects.value.length > 0) {
    const first = teacherSubjects.value[0]
    list.push({ text:subjectMeta[first]?.label+'错题', icon:'Edit', path:`/teacher/subject/${first}/wrong-questions`, bg:'#FFF7ED' })
  }
  list.push({ text:'学习反馈', icon:'ChatLineSquare', path:'/teacher/feedback', bg:'#F0FDFA' })
  return list
})

const todayStats = [
  { label:'本月新增学员', value:'3 人' },
  { label:'本月课时完成', value:'42 节' },
  { label:'平均出勤率',   value:'96%' },
  { label:'学员满意度',   value:'4.8/5' }
]
</script>

<style scoped>
/* 统计卡片 */
.stat-card { text-align: center; padding: 8px 0; }
.stat-icon {
  width: 44px; height: 44px; border-radius: var(--radius-md);
  display: inline-flex; align-items: center; justify-content: center;
  margin-bottom: 12px; color: var(--color-primary);
}
.stat-value { font-size: 32px; font-weight: 700; line-height: 1.2; }
.stat-label { font-size: 13px; color: var(--text-muted); margin-top: 4px; }

.card-title { font-weight: 600; font-size: 15px; }

/* 快捷操作 */
.action-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 10px; }
.action-item {
  display: flex; align-items: center; gap: 10px;
  padding: 12px; border-radius: var(--radius-sm);
  cursor: pointer; transition: all var(--transition);
  border: 1px solid var(--color-border-light);
}
.action-item:hover { background: var(--color-bg-alt); border-color: var(--color-primary-light); }
.action-icon {
  width: 36px; height: 36px; border-radius: var(--radius-sm);
  display: flex; align-items: center; justify-content: center;
  color: var(--color-primary); flex-shrink: 0;
}
.action-text { font-size: 13px; font-weight: 500; color: var(--text-secondary); }

/* 学科网格 */
.subject-grid { display: grid; grid-template-columns: 1fr 1fr 1fr; gap: 6px; }
.subject-chip {
  padding: 8px 6px; font-size: 13px; text-align: center;
  border-radius: var(--radius-sm); cursor: pointer;
  background: var(--color-bg-alt); transition: all var(--transition);
}
.subject-chip:hover { background: var(--color-primary-bg); color: var(--color-primary); }

/* 今日概览 */
.today-item {
  display: flex; justify-content: space-between; align-items: center;
  padding: 10px 0; border-bottom: 1px solid var(--color-border-light);
  font-size: 14px; color: var(--text-secondary);
}
.today-item:last-child { border-bottom: none; }
.today-item strong { color: var(--text-primary); font-weight: 600; }
</style>
