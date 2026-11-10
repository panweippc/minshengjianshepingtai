import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'
import { useUserStore } from '@/stores/user'

/** 静态路由（无需鉴权） */
const staticRoutes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue'),
    meta: { title: '登录' }
  }
]

/** 动态路由（需鉴权，根据菜单生成） */
export const dynamicRoutes: RouteRecordRaw[] = [
  {
    path: '/',
    component: () => import('@/layouts/BasicLayout.vue'),
    redirect: '/home',
    children: [
      {
        path: 'home',
        name: 'Home',
        component: () => import('@/views/Home.vue'),
        meta: { title: '首页', icon: 'HomeFilled' }
      },
      {
        path: 'base/village',
        name: 'BaseVillage',
        component: () => import('@/views/base/Village.vue'),
        meta: { title: '村委会信息', icon: 'OfficeBuilding' }
      },
      {
        path: 'base/household',
        name: 'BaseHousehold',
        component: () => import('@/views/base/Household.vue'),
        meta: { title: '户基础信息', icon: 'House' }
      },
      {
        path: 'base/facility',
        name: 'BaseFacility',
        component: () => import('@/views/base/Facility.vue'),
        meta: { title: '公共基础设施', icon: 'Connection' }
      },
      {
        path: 'system/user',
        name: 'SystemUser',
        component: () => import('@/views/system/User.vue'),
        meta: { title: '用户管理', icon: 'User' }
      },
      {
        path: 'system/role',
        name: 'SystemRole',
        component: () => import('@/views/system/Role.vue'),
        meta: { title: '角色管理', icon: 'Avatar' }
      },
      {
        path: 'system/menu',
        name: 'SystemMenu',
        component: () => import('@/views/system/Menu.vue'),
        meta: { title: '菜单管理', icon: 'Menu' }
      },
      {
        path: 'system/dict',
        name: 'SystemDict',
        component: () => import('@/views/system/Dict.vue'),
        meta: { title: '字典管理', icon: 'Collection' }
      },
      {
        path: 'system/log',
        name: 'SystemLog',
        component: () => import('@/views/system/Log.vue'),
        meta: { title: '操作日志', icon: 'Document' }
      },
      {
        path: 'project/approval',
        name: 'ProjectApproval',
        component: () => import('@/views/project/Approval.vue'),
        meta: { title: '立项管理', icon: 'Document' }
      },
      {
        path: 'project/fund',
        name: 'ProjectFund',
        component: () => import('@/views/project/Fund.vue'),
        meta: { title: '资金管理', icon: 'Money' }
      },
      {
        path: 'project/construction',
        name: 'ProjectConstruction',
        component: () => import('@/views/project/Construction.vue'),
        meta: { title: '施工管理', icon: 'Operation' }
      },
      {
        path: 'project/acceptance',
        name: 'ProjectAcceptance',
        component: () => import('@/views/project/Acceptance.vue'),
        meta: { title: '验收管理', icon: 'CircleCheck' }
      },
      {
        path: 'archive',
        name: 'Archive',
        component: () => import('@/views/archive/Archive.vue'),
        meta: { title: '档案管理', icon: 'Files' }
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes: staticRoutes
})

/** 路由守卫：未登录跳登录页，已登录访问登录页跳首页，动态注册路由 */
router.beforeEach(async (to, _from, next) => {
  const store = useUserStore()
  const hasToken = !!store.token

  if (to.path === '/login') {
    if (hasToken) {
      next('/')
    } else {
      next()
    }
    return
  }

  if (!hasToken) {
    next('/login')
    return
  }

  // 已登录但未加载用户信息：拉取并注册动态路由
  if (!store.userInfo) {
    try {
      await store.fetchUserInfo()
      // 注册动态路由
      dynamicRoutes.forEach((route) => {
        if (!router.hasRoute(route.name as string)) {
          router.addRoute(route)
        }
      })
      next({ ...to, replace: true })
    } catch {
      await store.logout()
      next('/login')
    }
    return
  }

  next()
})

router.afterEach((to) => {
  document.title = (to.meta.title as string) || '民生建设项目管理平台'
})

export default router
