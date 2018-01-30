export default (store) => {

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
        created: comp => {
          console.log('comp created', comp.$props)
          // Note: manually mounted components needs to be manually updated
          store.watch(
            state => state.object,
            object => {
              console.log('object changed', object)
              comp.$props.object = object
            }
          )
        }
      },
      {
        comp: require('./components/TopicmapSelect'),
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
