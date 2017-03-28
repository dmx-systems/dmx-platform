import Vue from 'vue'
// import state from './state'

console.log('Loading Workspaces main.js')

export default {
  init () {
    console.log('Workspaces init() called!')
    var WorkspaceSelect = Vue.extend(require('./components/WorkspaceSelector.vue'))
    document.getElementById('topicmap-panel').appendChild(new WorkspaceSelect().$mount().$el)
  }
}
