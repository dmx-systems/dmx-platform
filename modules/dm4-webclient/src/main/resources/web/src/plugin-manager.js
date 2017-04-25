import Vue from 'vue'
import store from './store'
import http from 'axios'

export default {
  loadPlugins () {
    if (DEV) {
      console.info('You are running DM5 in development mode.\n' +
        'The standard plugins are loaded from file system and are hot replaced.')
    }
    initPlugin(require('modules/dm4-workspaces/src/main/resources/web/src/main.js'))
    initPlugin(require('modules/dm4-topicmaps/src/main/resources/web/src/main.js'))
    //
    loadPluginsFromServer()
  }
}

// --- Load from file system ---

function initPlugin (plugin) {
  plugin.default.init({Vue, store})
}

// --- Load from server ---

function loadPluginsFromServer () {
  http.get('/core/plugin').then(response => {
    response.data.forEach(pluginInfo => {
      if (pluginInfo.hasPluginFile) {
        loadPlugin(pluginInfo.pluginUri, function (plugin) {
          plugin.default.init({Vue, store})
        })
      }
    })
  })
}

function loadPlugin (pluginUri, callback) {
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
  return '_' + pluginUri.replace(/[.-]/g, '_')
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
