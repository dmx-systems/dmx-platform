import Vue from 'vue'
import ElementUI from 'element-ui'
import 'element-ui/lib/theme-default/index.css'

import store from './store'
import router from './router'

Vue.use(ElementUI)

new Vue({
  el: '#app',
  store,
  router,
  render: h => h(require('./components/App.vue'))
})
