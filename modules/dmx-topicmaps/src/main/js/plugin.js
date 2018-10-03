export default ({store}) => {
  return {

    storeModule: {
      name: 'topicmaps',
      module: require('./topicmaps').default
    },

    components: [
      {
        comp: require('dm5-topicmap-panel').default,
        mount: 'webclient',
        props: {
          object:          (_, getters) => getters && getters.object,   // TODO: why is getters undefined on 1st call?
          writable:        state => state.writable,
          objectRenderers: state => state.objectRenderers,
          topicmapTypes:   state => state.topicmaps.topicmapTypes,
          toolbarCompDefs: state => ({
            left:  state.compDefs['toolbar-left'],
            right: state.compDefs['toolbar-right']
          }),
          // TODO: static props? Note: contextCommands does not operate on state
          // TODO: make the commands extensible for 3rd-party plugins
          contextCommands: state => ({
            topic: [
              {label: 'Hide',            handler: idLists => store.dispatch('hideMulti',   idLists), multi: true},
              {label: 'Delete',          handler: idLists => store.dispatch('deleteMulti', idLists), multi: true},
              {label: 'Edit',            handler: id => store.dispatch('callTopicDetailRoute', {id, detail: 'edit'})},
              {label: "What's related?", handler: id => store.dispatch('callTopicDetailRoute', {id, detail: 'related'})}
            ],
            assoc: [
              {label: 'Hide',            handler: idLists => store.dispatch('hideMulti',   idLists), multi: true},
              {label: 'Delete',          handler: idLists => store.dispatch('deleteMulti', idLists), multi: true},
              {label: 'Edit',            handler: id => store.dispatch('callAssocDetailRoute', {id, detail: 'edit'})},
              {label: "What's related?", handler: id => store.dispatch('callAssocDetailRoute', {id, detail: 'related'})}
            ]
          }),
          quillConfig: state => state.quillConfig
        },
        listeners: {
          'topic-select':         id          => store.dispatch('selectTopic', id),
          'topic-unselect':       id          => store.dispatch('unselectTopic', id),
          'topic-double-click':   viewTopic   => selectTopicmapIf(viewTopic),
          'topic-drag':           ({id, pos}) => store.dispatch('setTopicPosition', {id, pos}),
          'topics-drag':          coords      => store.dispatch('setTopicPositions', coords),
          'topic-drop-on-topic':  ids         => store.dispatch('createAssoc', ids),
          'assoc-select':         id          => store.dispatch('selectAssoc', id),
          'assoc-unselect':       id          => store.dispatch('unselectAssoc', id),
          'topicmap-contextmenu': pos         => store.dispatch('openSearchWidget', {pos}),
          'object-submit':        object      => store.dispatch('submit', object),
          'child-topic-reveal':   relTopic    => store.dispatch('revealRelatedTopic', relTopic)
        }
      },
      {
        comp: require('./components/dm5-topicmap-select').default,
        mount: 'toolbar-left'
      }
    ],

    extraMenuItems: [{
      uri: 'dmx.topicmaps.topicmap',
      label: 'Topicmap',
      optionsComp: require('./components/dm5-topicmap-options').default,
      create: (name, data) => {
        store.dispatch('createTopicmap', {
          name,
          topicmapTypeUri: data.topicmapTypeUri,
          isPrivate: false      // TODO
        })
      }
    }],

    topicmapType: {
      uri: 'dmx.webclient.default_topicmap_renderer',
      name: "Topicmap",
      renderer: () => import('dm5-cytoscape-renderer' /* webpackChunkName: "cytoscape" */),
      mounted () {
        // TODO: sync styles dynamically when assoc type view config changes
        store.dispatch('syncStyles', assocTypeColors())
      }
    }
  }

  function selectTopicmapIf (viewTopic) {
    if (viewTopic.typeUri === 'dmx.topicmaps.topicmap') {
      store.dispatch('selectTopicmap', viewTopic.id)
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
