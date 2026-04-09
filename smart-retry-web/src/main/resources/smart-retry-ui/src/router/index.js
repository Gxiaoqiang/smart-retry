import { createRouter, createWebHistory } from 'vue-router'
import Layout from '../layout/Index.vue'

const routes = [
  {
    path: '/',
    component: Layout,
    redirect: '/dashboard',
    children: [
      {
        path: '/dashboard',
        name: 'Dashboard',
        component: () => import('../views/Dashboard.vue'),
        meta: { title: '整体分布', icon: 'DataAnalysis' }
      },
      {
        path: '/instance',
        name: 'Instance',
        component: () => import('../views/Instance.vue'),
        meta: { title: '实例管理', icon: 'Monitor' }
      },
      {
        path: '/task',
        name: 'Task',
        component: () => import('../views/Task.vue'),
        meta: { title: '任务管理', icon: 'List' }
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router
