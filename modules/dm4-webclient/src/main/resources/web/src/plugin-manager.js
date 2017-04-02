import Vue from 'vue'
import store from './store'

export default {
  loadPlugins () {
    if (DEV) {
      console.log('DM5 development mode: plugins are loaded from file system and are hot replaced')
      initPlugin(require('../../../../../../dm4-workspaces/src/main/resources/web/src/main.js'))
      initPlugin(require('../../../../../../dm4-topicmaps/src/main/resources/web/src/main.js'))
    } else {
      console.log('DM5 production mode: plugins are loaded from server')
      loadPluginsFromServer()
    }
  }
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

function loadPlugin (pluginUri, mainCallback) {
  console.log('Loading plugin', pluginUri)
  installMainCallback(pluginUri, mainCallback)
  loadPluginChunk(pluginUri, 'manifest', () =>
    loadPluginChunk(pluginUri, 'vendor', () =>
      loadPluginChunk(pluginUri, 'main')
    )
  )
}

function installMainCallback (pluginUri, mainCallback) {
  var _pluginIdent = pluginIdent(pluginUri)
  window[_pluginIdent] = function (exports) {
    delete window[_pluginIdent]
    mainCallback(exports)
  }
}

function loadPluginChunk (pluginUri, name, callback) {
  return loadScript(pluginChunk(pluginUri, name), callback)
}

function pluginIdent (pluginUri) {
  return '_' + pluginUri.replace(/\./g, '_')
}

function pluginChunk (pluginUri, name) {
  return '/' + pluginUri + '/js/' + name + '.js'
}

function loadScript (url, callback) {
  var script = document.createElement('script')
  script.src = url
  script.onload = function () {
    document.head.removeChild(script)
    callback && callback()
  }
  document.head.appendChild(script)
}

// --- Load from file system ---

function initPlugin (plugin) {
  plugin.default.init({
    store
  })
}
