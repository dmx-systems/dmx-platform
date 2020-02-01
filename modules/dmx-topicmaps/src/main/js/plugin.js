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
          detailRenderers: state => state.detailRenderers,
          topicmapTypes:   state => state.topicmaps.topicmapTypes,
          toolbarCompDefs: state => ({
            left:  state.compDefs['toolbar-left'],
            right: state.compDefs['toolbar-right']
          }),
          // Note: command definitions are static; "contextCommands" does not operate on "state"
          // TODO: make the commands extensible for 3rd-party plugins
          contextCommands: state => state.topicmaps.contextCommands,
          quillConfig:     state => state.quillConfig
        },
        listeners: {
          'topic-select':         id             => store.dispatch('selectTopic', id),
          'topic-unselect':       id             => store.dispatch('unselectTopic', id),
          'topic-double-click':   topic          => selectTopicmapIf(topic),
          'topic-drag':           ({id, pos})    => store.dispatch('setTopicPosition', {id, pos}),
          'topic-pin':            ({id, pinned}) => store.dispatch('setTopicPinned', {topicId: id, pinned,
                                                                                      showDetails: showDetails()}),
          'topics-drag':          topicCoords    => store.dispatch('setTopicPositions', topicCoords),
          'assoc-create':         playerIds      => store.dispatch('createAssoc', playerIds),
          'assoc-select':         id             => store.dispatch('selectAssoc', id),
          'assoc-unselect':       id             => store.dispatch('unselectAssoc', id),
          'assoc-pin':            ({id, pinned}) => store.dispatch('setAssocPinned', {assocId: id, pinned,
                                                                                      showDetails: showDetails()}),
          'topicmap-contextmenu': pos            => store.dispatch('openSearchWidget', {pos}),
          'object-submit':        object         => store.dispatch('submit', object),
          'child-topic-reveal':   relTopic       => store.dispatch('revealRelatedTopic', {relTopic})
        }
      },
      {
        comp: require('./components/dm5-topicmap-select').default,
        mount: 'toolbar-left'
      }
    ],

    extraMenuItems: [{
      uri: 'dmx.topicmaps.topicmap',
      optionsComp: require('./components/dm5-topicmap-options').default,
      create: (name, data) => {
        store.dispatch('createTopicmap', {
          name,
          topicmapTypeUri: data.topicmapTypeUri
        })
      }
    }],

    topicmapType: {
      uri: 'dmx.topicmaps.topicmap',
      name: 'Topicmap',
      renderer: () => import('dm5-cytoscape-renderer' /* webpackChunkName: "cytoscape" */)
    }
  }

  function selectTopicmapIf (topic) {
    if (topic.typeUri === 'dmx.topicmaps.topicmap') {
      store.dispatch('selectTopicmap', topic.id)
    }
  }

  function showDetails () {
    return store.getters.showInmapDetails
  }
}
