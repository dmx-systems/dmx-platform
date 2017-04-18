console.log('Loading Workspaces main.js')

import storeModule from './workspaces'

export default {

  init ({store}) {
    // install component
    console.log('Workspaces init() called!!')
    store.dispatch('addToToolbar', require('./components/WorkspaceSelect.vue'))
    // install store module
    store.registerModule('workspaces', storeModule)
  }
}
