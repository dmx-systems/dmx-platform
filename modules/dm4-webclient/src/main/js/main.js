import Vue from 'vue'
import App from './components/App'
import store from './store/webclient'
import router from './router'
import pluginManager from './plugin-manager'
import './element-ui'
import './websocket'

pluginManager.loadPlugins()

new Vue({
  el: '#app',
  store,
  router,
  render: h => h(App)
})
