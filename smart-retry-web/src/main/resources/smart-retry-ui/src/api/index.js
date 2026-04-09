import request from '@/utils/request'

// 获取仪表盘数据
export function getDashboardData() {
  return request({
    url: '/dashboard/data',
    method: 'get'
  })
}

// 查询实例列表
export function queryInstances(data) {
  return request({
    url: '/instance/query',
    method: 'post',
    data
  })
}

// 更新实例
export function updateInstance(data) {
  return request({
    url: '/instance/update',
    method: 'put',
    data
  })
}

// 删除实例
export function deleteInstance(id) {
  return request({
    url: `/instance/delete/${id}`,
    method: 'delete'
  })
}

// 查询任务列表
export function queryTasks(data) {
  return request({
    url: '/task/query',
    method: 'post',
    data
  })
}

// 创建任务
export function createTask(data) {
  return request({
    url: '/task/create',
    method: 'post',
    data
  })
}

// 更新任务
export function updateTask(data) {
  return request({
    url: '/task/update',
    method: 'put',
    data
  })
}

// 删除任务
export function deleteTask(id) {
  return request({
    url: `/task/delete/${id}`,
    method: 'delete'
  })
}

// 批量删除任务
export function batchDeleteTasks(ids) {
  return request({
    url: '/task/batch-delete',
    method: 'delete',
    data: ids
  })
}

// 获取分片选项
export function getShardingOptions() {
  return request({
    url: '/task/sharding-options',
    method: 'get'
  })
}
