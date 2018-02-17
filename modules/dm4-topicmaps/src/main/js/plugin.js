export default store => {
  return {

    storeModule: {
      name: 'topicmaps',
      module: require('./topicmaps')
    },

    storeWatcher: [
      {
        getter: state => state.typeCache.assocTypes,
        callback: assocTypes => {
          store.dispatch('syncStyles', assocTypeColors())
        }
      }
    ],

    components: [
      {
        comp: require('dm5-topicmap-panel'),
        mount: 'webclient',
        props: {
          object:          state => state.object,
          writable:        state => state.writable,
          objectRenderers: state => state.objectRenderers,
          toolbarCompDefs: state => ({
            left:  state.compDefs['toolbar-left'],
            right: state.compDefs['toolbar-right']
          })
        }
      },
      {
        comp: require('./components/dm5-topicmap-select'),
        mount: 'toolbar-left'
      }
    ],

    extraMenuItems: [{
      uri: 'dm4.topicmaps.topicmap',
      label: 'Topicmap',
      create: name => {
        store.dispatch('createTopicmap', name)
      }
    }]
  }

  function assocTypeColors() {
    return Object.values(store.state.typeCache.assocTypes).reduce((colors, assocType) => {
      const color = assocType.getColor()
      if (color) {
        colors[assocType.uri] = color
      }
      return colors
    }, {})
  }
}
