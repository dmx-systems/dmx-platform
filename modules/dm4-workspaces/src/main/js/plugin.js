export default (store) => ({

  storeModule: {
    name: 'workspaces',
    module: require('./workspaces').default
  },

  components: [{
    comp: require('./components/dm5-workspace-select'),
    mount: 'toolbar-left'
  }],

  extraMenuItems: [{
    uri: 'dm4.workspaces.workspace',
    label: 'Workspace',
    create: name => {
      store.dispatch('createWorkspace', name)
    }
  }]
})
