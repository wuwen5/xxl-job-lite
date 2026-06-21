import { createRouter, createWebHistory } from 'vue-router'
import NProgress from 'nprogress'
import 'nprogress/nprogress.css'
import { useAuthStore } from '@/stores/auth'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/login',
      name: 'Login',
      component: () => import('@/views/login/index.vue'),
      meta: { requiresAuth: false }
    },
    {
      path: '/',
      component: () => import('@/components/Layout/index.vue'),
      redirect: '/dashboard',
      children: [
        {
          path: 'dashboard',
          name: 'Dashboard',
          component: () => import('@/views/dashboard/index.vue'),
          meta: { title: 'menu.dashboard' }
        },
        {
          path: 'jobinfo',
          name: 'JobInfo',
          component: () => import('@/views/jobinfo/index.vue'),
          meta: { title: 'menu.jobinfo' }
        },
        {
          path: 'jobgroup',
          name: 'JobGroup',
          component: () => import('@/views/jobgroup/index.vue'),
          meta: { title: 'menu.jobgroup', adminOnly: true }
        },
        {
          path: 'joblog',
          name: 'JobLog',
          component: () => import('@/views/joblog/index.vue'),
          meta: { title: 'menu.joblog' }
        },
        {
          path: 'joblog/detail/:id',
          name: 'JobLogDetail',
          component: () => import('@/views/joblog/detail.vue'),
          meta: { title: 'menu.joblog' }
        },
        {
          path: 'jobcode',
          name: 'JobCode',
          component: () => import('@/views/jobcode/index.vue'),
          meta: { title: 'menu.jobcode' }
        },
        {
          path: 'user',
          name: 'User',
          component: () => import('@/views/user/index.vue'),
          meta: { title: 'menu.user', adminOnly: true }
        },
        {
          path: 'help',
          name: 'Help',
          component: () => import('@/views/help/index.vue'),
          meta: { title: 'menu.help' }
        }
      ]
    }
  ]
})

NProgress.configure({ showSpinner: false })

router.beforeEach(async (to, _from, next) => {
  NProgress.start()
  
  const authStore = useAuthStore()
  
  if (to.meta.requiresAuth === false) {
    if (authStore.token && to.path === '/login') {
      next('/')
    } else {
      next()
    }
  } else {
    if (authStore.token) {
      if (!authStore.userInfo) {
        try {
          await authStore.getUserInfo()
        } catch {
          authStore.logout()
          next('/login')
          return
        }
      }
      if (to.meta.adminOnly && !authStore.isAdmin()) {
        next('/')
      } else {
        next()
      }
    } else {
      next('/login')
    }
  }
})

router.afterEach(() => {
  NProgress.done()
})

export default router
