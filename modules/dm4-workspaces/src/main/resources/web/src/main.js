import storeModule from './workspaces'

export default {

  init ({store}) {
    // install store module
    store.registerModule('workspaces', storeModule)
    // install component
    store.dispatch('registerComponent', {
      extensionPoint: 'dm5.webclient.toolbar',
      component: require('./components/WorkspaceSelect.vue')
    })
  }
}
