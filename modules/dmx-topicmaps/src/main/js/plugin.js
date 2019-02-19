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
          // TODO: static props? Note: contextCommands does not operate on state
          // TODO: make the commands extensible for 3rd-party plugins
          contextCommands: state => ({
            topic: [
              {label: 'Hide',            handler: idLists => store.dispatch('hideMulti',   idLists), multi: true},
              {label: 'Edit',            handler: id => store.dispatch('callTopicDetailRoute', {id, detail: 'edit'})},
              {label: "What's related?", handler: id => store.dispatch('callTopicDetailRoute', {id, detail: 'related'})}
            ],
            topic_danger: [
              {label: 'Delete',          handler: idLists => store.dispatch('deleteMulti', idLists), multi: true,
                                         disabled: deleteDisabled}
            ],
            assoc: [
              {label: 'Hide',            handler: idLists => store.dispatch('hideMulti',   idLists), multi: true},
              {label: 'Edit',            handler: id => store.dispatch('callAssocDetailRoute', {id, detail: 'edit'})},
              {label: "What's related?", handler: id => store.dispatch('callAssocDetailRoute', {id, detail: 'related'})}
            ],
            assoc_danger: [
              {label: 'Delete',          handler: idLists => store.dispatch('deleteMulti', idLists), multi: true}
            ]
          }),
          quillConfig: state => state.quillConfig
        },
        listeners: {
          'topic-select':         id          => store.dispatch('selectTopic', id),
          'topic-unselect':       id          => store.dispatch('unselectTopic', id),
          'topic-double-click':   topic       => selectTopicmapIf(topic),
          'topic-drag':           ({id, pos}) => store.dispatch('setTopicPosition', {id, pos}),
          'topics-drag':          coords      => store.dispatch('setTopicPositions', coords),
          'assoc-create':         playerIds   => store.dispatch('createAssoc', playerIds),
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
      renderer: () => import('dm5-cytoscape-renderer' /* webpackChunkName: "cytoscape" */)
    }
  }

  function deleteDisabled (idLists) {
    // only the selected topicmap is enabled for deletion
    return idLists.topicIds.some(id => {
      const topicmap = store.state.topicmaps.topicmap
      const topic = topicmap.getTopic(id)
      return topic.typeUri === 'dmx.topicmaps.topicmap' && topic.id !== topicmap.id
    })
  }

  function selectTopicmapIf (topic) {
    if (topic.typeUri === 'dmx.topicmaps.topicmap') {
      store.dispatch('selectTopicmap', topic.id)
    }
  }
}
