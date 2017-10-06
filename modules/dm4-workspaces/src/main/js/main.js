export default (store) => ({

  storeModule: {
    name: 'workspaces',
    module: require('./workspaces')
  },

  components: {
    toolbar: require('./components/WorkspaceSelect')
  },

  extraMenuItems: [{
    uri: 'dm4.workspaces.workspace',
    label: 'Workspace',
    create: name => {
      store.dispatch('createWorkspace', name)
    }
  }]
})
