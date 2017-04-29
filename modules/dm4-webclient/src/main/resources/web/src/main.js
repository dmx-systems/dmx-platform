import Vue from 'vue'
import App from './components/App.vue'
import './element-ui'

new Vue({
  el: '#app',
  store:  require('./store/webclient').default,
  router: require('./router').default,
  render: h => h(App)
})
