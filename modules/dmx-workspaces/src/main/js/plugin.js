export default ({store}) => {
  return {

    storeModule: {
      name: 'workspaces',
      module: require('./workspaces').default
    },

    storeWatcher: [
      {getter: state => state.workspaces.workspaceId, callback: initWritable},
      {getter: state => state.accesscontrol.username, callback: initWritable}
    ],

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
  }

  function initWritable () {
    store.dispatch('_initWorkspaceIsWritable')
  }
}
