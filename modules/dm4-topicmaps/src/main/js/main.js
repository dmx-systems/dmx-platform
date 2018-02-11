export default store => {

  // TODO: declarative store watchers?
  store.watch(
    state => state.typeCache.assocTypes,
    assocTypes => {
      store.dispatch('syncStyles', assocTypeColors())
    }
  )

  return {

    storeModule: {
      name: 'topicmaps',
      module: require('./topicmaps')
    },

    components: [
      {
        comp: require('dm5-topicmap-panel'),
        mount: 'webclient',
        props: {
          object:   state => state.object,                // reactive (getter function)
          writable: state => state.writable,              // reactive (getter function)
          objectRenderers: store.state.objectRenderers    // static (value)
        }
      },
      {
        comp: require('./components/dm5-topicmap-select'),
        mount: 'toolbar'
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
