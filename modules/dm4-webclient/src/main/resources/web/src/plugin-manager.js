import Vue from 'vue'
import store from './store'

// ### TODO: retrieve plugin list from server
var pluginUris = [
  'de.deepamehta.workspaces',
  'de.deepamehta.topicmaps'
]

export default {
  loadPlugins () {
    pluginUris.forEach(pluginUri => loadPlugin(pluginUri, function (exports) {
      exports.default.init({Vue, store})
    }))
  }
}

function loadPlugin (pluginUri, callback) {
  console.log('Loading plugin', pluginUri)
  installAppCallback(pluginUri, callback)
  loadPluginChunk(pluginUri, 'manifest', () =>
    loadPluginChunk(pluginUri, 'vendor', () =>
      loadPluginChunk(pluginUri, 'main')
    )
  )
}

function installAppCallback (pluginUri, callback) {
  var _pluginIdent = pluginIdent(pluginUri)
  window[_pluginIdent] = function (exports) {
    delete window[_pluginIdent]
    callback(exports)
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
