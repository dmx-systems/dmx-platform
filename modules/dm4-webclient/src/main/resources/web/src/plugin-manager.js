import Vue from 'vue'
import store from './store'

export default {
  loadPlugins () {
    if (DEV) {
      console.info('DM5 development mode: plugins are loaded from file system and are hot replaced')
      initPlugin(require('../../../../../../dm4-workspaces/src/main/resources/web/src/main.js'))
      initPlugin(require('../../../../../../dm4-topicmaps/src/main/resources/web/src/main.js'))
    } else {
      console.info('DM5 production mode: plugins are loaded from server')
      loadPluginsFromServer()
    }
  }
}

// --- Load from file system ---

function initPlugin (plugin) {
  plugin.default.init({Vue, store})
}

// --- Load from server ---

function loadPluginsFromServer () {
  // ### TODO: retrieve plugin list from server
  var pluginUris = [
    'de.deepamehta.workspaces',
    'de.deepamehta.topicmaps'
  ]
  //
  pluginUris.forEach(pluginUri => loadPlugin(pluginUri, function (exports) {
    exports.default.init({Vue, store})
  }))
}

function loadPlugin (pluginUri, callback) {
  console.log('Loading plugin', pluginUri)
  installCallback(pluginUri, callback)
  loadScript(pluginURL(pluginUri))
}

function installCallback (pluginUri, callback) {
  var _pluginIdent = pluginIdent(pluginUri)
  window[_pluginIdent] = function (exports) {
    delete window[_pluginIdent]
    callback(exports)
  }
}

function pluginIdent (pluginUri) {
  return '_' + pluginUri.replace(/\./g, '_')
}

function pluginURL (pluginUri) {
  return '/' + pluginUri + '/plugin' + '.js'
}

function loadScript (url) {
  var script = document.createElement('script')
  script.src = url
  script.onload = function () {
    document.head.removeChild(script)
  }
  document.head.appendChild(script)
}
