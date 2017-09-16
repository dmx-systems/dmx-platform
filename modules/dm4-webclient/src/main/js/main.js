import Vue from 'vue'
import App from './components/App'
import dm5 from 'dm5'
import store from './store/webclient'
import router from './router'
import pluginManager from './plugin-manager'
import 'font-awesome/css/font-awesome.css'
import './element-ui'
import './websocket'

// Note: the dm5 library must be inited *before* the SearchWidget component is created.
// The SearchWidget relies on dm5's "menuTopicTypes" store getter.
dm5.init(store)

pluginManager.loadPlugins()

new Vue({
  el: '#app',
  store,
  router,
  render: h => h(App)
})
