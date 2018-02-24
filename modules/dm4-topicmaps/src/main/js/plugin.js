export default store => {
  return {

    storeModule: {
      name: 'topicmaps',
      module: require('./topicmaps').default
    },

    storeWatcher: [{
      getter: state => state.typeCache.assocTypes,
      callback: () => {
        store.dispatch('syncStyles', assocTypeColors())
      }
    }],

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
        },
        listeners: {
          'topic-select':         id          => store.dispatch('selectTopic', id),
          'topic-double-click':   id          => selectTopicmapIf(id),
          'topic-drag':           ({id, pos}) => store.dispatch('setTopicPosition', {id, pos}),
          'topic-drop-on-topic':  ids         => store.dispatch('createAssoc', ids),
          'assoc-select':         id          => store.dispatch('selectAssoc', id),
          'topicmap-click':       ()          => store.dispatch('unselect'),
          'topicmap-contextmenu': pos         => store.dispatch('openSearchWidget', {pos})
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

  function selectTopicmapIf (id) {
    if (store.state.topicmaps.topicmap.getTopic(id).typeUri === 'dm4.topicmaps.topicmap') {
      store.dispatch('selectTopicmap', id)
    }
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
