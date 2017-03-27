import Vue from 'vue'

var plugin = 'dm4-workspaces'
var comp = 'WorkspaceSelector'

Vue.component('workspace-selector', require(
  '../../../../../../../modules/' + plugin + '/src/main/resources/web/components/' + comp + '.vue'))
