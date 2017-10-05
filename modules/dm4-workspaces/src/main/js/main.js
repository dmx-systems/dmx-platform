export default {

  storeModule: {
    name: 'workspaces',
    module: require('./workspaces')
  },

  components: {
    toolbar: require('./components/WorkspaceSelect')
  }
}
