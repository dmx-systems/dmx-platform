import Vue from 'vue'
import store from './store'
import router from './router'

new Vue({
  el: '#app',
  store,
  router,
  render: h => h(require('./components/App.vue'))
})
