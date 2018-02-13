export default (store) => ({

  storeModule: {
    name: 'workspaces',
    module: require('./workspaces')
  },

  components: [{
    comp: require('./components/dm5-workspace-select'),
    mount: 'toolbar'
  }],

  extraMenuItems: [{
    uri: 'dm4.workspaces.workspace',
    label: 'Workspace',
    create: name => {
      store.dispatch('createWorkspace', name)
    }
  }]
})
