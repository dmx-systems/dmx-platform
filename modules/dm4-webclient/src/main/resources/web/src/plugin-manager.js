import Vue from 'vue'
import store from './store'

export default {
  loadPlugins () {
    // ### TODO: load plugins dynamically
    require('../../../../../../dm4-workspaces/src/main/resources/web/src/main.js').default.init({Vue, store})
    require('../../../../../../dm4-topicmaps/src/main/resources/web/src/main.js').default.init({Vue, store})
  }
}

// doesn't work

/*
var plugins = [
  '/de.deepamehta.workspaces/js/manifest.js',
  '/de.deepamehta.workspaces/js/app.js'
]

plugins.forEach(url => loadScript(url))

function loadScript (url) {
  console.log('Loading script', url)
  var script = document.createElement('script')
  script.src = url
  document.head.appendChild(script)
}
*/
