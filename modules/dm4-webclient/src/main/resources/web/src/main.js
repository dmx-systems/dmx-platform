import Vue from 'vue'
import store from './store/webclient'
import router from './router'
import './element-ui'
import App from './components/App.vue'

new Vue({
  el: '#app',
  store,
  router,
  render: h => h(App)
})
