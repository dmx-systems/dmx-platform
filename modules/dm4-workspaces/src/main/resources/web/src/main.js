import storeModule from './workspaces'

export default {

  init ({store}) {
    // install component
    store.dispatch('addToToolbar', require('./components/WorkspaceSelect.vue'))
    // install store module
    store.registerModule('workspaces', storeModule)
  }
}
