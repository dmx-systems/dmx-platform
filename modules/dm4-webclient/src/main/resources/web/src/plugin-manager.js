import Vue from 'vue'
import store from './store'

// ### TODO: retrieve plugin list from server
var pluginUris = [
  'de.deepamehta.workspaces'
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
  loadPluginChunk(pluginUri, 'manifest')
  loadPluginChunk(pluginUri, 'vendor')
  loadPluginAppChunk(pluginUri, callback)
}

function loadPluginAppChunk (pluginUri, callback) {
  var pluginIdent = getPluginIdent(pluginUri)
  window[pluginIdent] = function (exports) {
    delete window[pluginIdent]
    removeScript()
    callback(exports)
  }
  var removeScript = loadPluginChunk(pluginUri, 'app')
}

function getPluginIdent (pluginUri) {
  return '_' + pluginUri.replace(/\./g, '_')
}

function loadPluginChunk (pluginUri, name) {
  return loadScript('/' + pluginUri + '/js/' + name + '.js')
}

function loadScript (url) {
  var script = document.createElement('script')
  script.type = 'text/javascript'
  script.charset = 'utf-8'
  script.src = url
  document.head.appendChild(script)
  return function remove () {
    document.head.removeChild(script)
  }
}
