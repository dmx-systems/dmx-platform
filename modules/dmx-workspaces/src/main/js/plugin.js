export default ({store}) => {
  return {

    init () {
      store.state.workspaces.ready = store.dispatch('fetchWorkspaceTopics')   // ### FIXME: init state too late?
      //
      window.addEventListener('focus', () => {
        store.dispatch('updateWorkspaceCookie')
      })
    },

    storeModule: {
      name: 'workspaces',
      module: require('./workspaces').default
    },

    storeWatcher: [
      {getter: state => state.workspaces.workspaceId, callback: initWritable},
      {getter: state => state.accesscontrol.username, callback: initWritable}
    ],

    components: [{
      comp: require('./components/dmx-workspace-commands').default,
      mount: 'toolbar-left'
    }],

    workspaceCommands: {
      'dmx.topicmaps.topicmap': [
        require('./components/dmx-workspace-info').default
      ]
    },

    extraMenuItems: [{
      uri: 'dmx.workspaces.workspace',
      optionsComp: require('./components/dmx-workspace-options').default,
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
