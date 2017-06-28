export default {

  storeModule: {
    name: 'workspaces',
    module: require('./workspaces')
  },

  components: [
    {
      extensionPoint: 'dm5.webclient.toolbar',
      component: require('./components/WorkspaceSelect')
    }
  ]
}
