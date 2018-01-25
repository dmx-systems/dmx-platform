import Vue from 'vue'
import App from './components/App'
import dm5 from 'dm5'
import store from './store/webclient'
import router from './router'
import pluginManager from './plugin-manager'
import 'font-awesome/css/font-awesome.css'
import './element-ui'
import './websocket'

// The dm5 library must be inited *before* the SearchWidget component is created.
// The SearchWidget relies on dm5's "menuTopicTypes" store getter.
const ready = dm5.init(store)

new Vue({
  el: '#app',
  store,
  router,
  render: h => h(App)
})
// console.log('### Vue root instance created!')

// The vue component hierarchy must be instantiated *before* the Webclient plugins are
// loaded. Plugins that customize the detail panel rely on the "registerObjectRenderer"
// action, which is only registered in dm5-detail-panel's created() hook (see comment there). ### TODO: still true?
pluginManager.loadPlugins()
// console.log('### Plugins loaded!')

// TODO: synchronize initial navigation with loading the external plugins?
// (Note: the standard plugins are "loaded" synchronously anyways.)

store.dispatch('mountComponents')

// Initial navigation must take place *after* the Webclient plugins are loaded.
// The "workspaces" store module is registered by the Workspaces plugin.
Promise.all([
  // Both, the Topicmap Panel and the Detail Panel, rely on a populated type cache.
  // The type cache must be ready *before* "initialNavigation" is dispatched.
  ready,
  // Initial navigation might involve "select the 1st workspace", so the workspace
  // topics must be already loaded.
  store.state.workspaces.ready
]).then(() => {
  store.dispatch('initialNavigation')
})
