export default store => ({

  storeModule: {
    name: 'workspaces',
    module: require('./workspaces').default
  },

  components: [{
    comp: require('./components/dm5-workspace-select').default,
    mount: 'toolbar-left'
  }],

  extraMenuItems: [{
    uri: 'dmx.workspaces.workspace',
    label: 'Workspace',
    optionsComp: require('./components/dm5-workspace-options').default,
    create: (name, data) => {
      store.dispatch('createWorkspace', {
        name,
        sharingModeUri: data.sharingModeUri
      })
    }
  }]
})
