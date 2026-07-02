import http from '@/api/request'

// 学生列表
export function getStudents(params) {
  return http.get('/students', { params })
}

// 学生详情
export function getStudent(id) {
  return http.get(`/students/${id}`)
}

// 新增学生
export function createStudent(data) {
  return http.post('/students', data)
}

// 更新学生
export function updateStudent(id, data) {
  return http.put(`/students/${id}`, data)
}

// 删除学生
export function deleteStudent(id) {
  return http.delete(`/students/${id}`)
}

// 课时加减
export function adjustHours(id, delta) {
  return http.patch(`/students/${id}/hours`, { delta })
}

// 日历查询
export function getCalendar(year, month) {
  return http.get('/students/calendar', { params: { year, month } })
}
