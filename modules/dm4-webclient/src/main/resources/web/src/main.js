import Vue from 'vue'
import {Select, Option} from 'element-ui'

import store from './store'
import router from './router'

Vue.use(Select)
Vue.use(Option)

new Vue({
  el: '#app',
  store,
  router,
  render: h => h(require('./components/App.vue'))
})
