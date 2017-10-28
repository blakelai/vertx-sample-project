import Vue from 'vue'
import Router from 'vue-router'
import WikiPage from '@/components/WikiPage'

Vue.use(Router)

export default new Router({
  routes: [
    {
      path: '/',
      name: 'Vertx-Wiki',
      component: WikiPage
    }
  ]
})
