import Vue from 'vue'
import VueRouter from 'vue-router'
import Webclient from './components/Webclient'

Vue.use(VueRouter)

export default new VueRouter({
  routes: [
    {
        path: '/',
        component: Webclient
    },
    {
        path: '/topicmap/:topicmapId/topic/:topicId',
        name: 'webclient',
        component: Webclient
    }
  ]
})
