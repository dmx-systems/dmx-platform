import dm5 from 'dm5'

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
          contextCommands: state => ({
            topic: [
              {label: 'Hide', multi: true, handler: idLists => store.dispatch('hideMulti', idLists)},
              {
                label: 'Edit', handler: id => store.dispatch('callTopicDetailRoute', {id, detail: 'edit'}),
                disabled: isTopicEditDisabled
              },
              {label: 'Related', handler: id => store.dispatch('callTopicDetailRoute', {id, detail: 'related'})},
              {label: 'Details', handler: id => store.dispatch('callTopicDetailRoute', {id, detail: 'info'})}
            ],
            topic_danger: [{
              label: 'Delete', multi: true, handler: idLists => store.dispatch('deleteMulti', idLists),
              disabled: isTopicDeleteDisabled
            }],
            assoc: [
              {label: 'Hide', multi: true, handler: idLists => store.dispatch('hideMulti', idLists)},
              {
                label: 'Edit', handler: id => store.dispatch('callAssocDetailRoute', {id, detail: 'edit'}),
                disabled: isAssocEditDisabled
              },
              {label: 'Related', handler: id => store.dispatch('callAssocDetailRoute', {id, detail: 'related'})},
              {label: 'Details', handler: id => store.dispatch('callAssocDetailRoute', {id, detail: 'info'})}
            ],
            assoc_danger: [{
              label: 'Delete', multi: true, handler: idLists => store.dispatch('deleteMulti', idLists),
              disabled: isAssocDeleteDisabled
            }]
          }),
          quillConfig: state => state.quillConfig
        },
        listeners: {
          'topic-select':         id             => store.dispatch('selectTopic', id),
          'topic-unselect':       id             => store.dispatch('unselectTopic', id),
          'topic-double-click':   topic          => selectTopicmapIf(topic),
          'topic-drag':           ({id, pos})    => store.dispatch('setTopicPosition', {id, pos}),
          'topic-pin':            ({id, pinned}) => store.dispatch('setTopicPinned', {topicId: id, pinned}),
          'topics-drag':          topicCoords    => store.dispatch('setTopicPositions', topicCoords),
          'assoc-create':         playerIds      => store.dispatch('createAssoc', playerIds),
          'assoc-select':         id             => store.dispatch('selectAssoc', id),
          'assoc-unselect':       id             => store.dispatch('unselectAssoc', id),
          'assoc-pin':            ({id, pinned}) => store.dispatch('setAssocPinned', {assocId: id, pinned}),
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
          topicmapTypeUri: data.topicmapTypeUri,
          isPrivate: false      // TODO
        })
      }
    }],

    topicmapType: {
      uri: 'dmx.topicmaps.topicmap',
      name: 'Topicmap',
      renderer: () => import('dm5-cytoscape-renderer' /* webpackChunkName: "cytoscape" */)
    }
  }

  /**
   * @return    a promise for a boolean
   */
  function isTopicEditDisabled (id) {
    return isTopicWritable(id).then(writable => !writable)
  }

  /**
   * @return    a promise for a boolean
   */
  function isAssocEditDisabled (id) {
    return isAssocWritable(id).then(writable => !writable)
  }

  /**
   * @return    a boolean or a promise for a boolean
   */
  function isTopicDeleteDisabled (idLists) {
    return containUnselectedTopicmap(idLists) ||    // returns a boolean, so must be checked first
           containUnwritableObject(idLists)         // returns a promise
  }

  /**
   * @return    a promise for a boolean
   */
  function isAssocDeleteDisabled (idLists) {
    return containUnwritableObject(idLists)
  }

  /**
   * @return    a boolean
   */
  function containUnselectedTopicmap (idLists) {
    // only the selected topicmap is enabled for deletion
    const topicmap = store.state.topicmaps.topicmap
    return idLists.topicIds.some(id => {
      const topic = topicmap.getTopic(id)
      return topic.typeUri === 'dmx.topicmaps.topicmap' && topic.id !== topicmap.id
    })
  }

  /**
   * @return    a promise for a boolean
   */
  function containUnwritableObject (idLists) {
    return Promise.all([
      ...idLists.topicIds.map(isTopicWritable),
      ...idLists.assocIds.map(isAssocWritable)
    ]).then(writables =>
      writables.some(writable => !writable)
    )
  }

  function isTopicWritable (id) {
    return dm5.permCache.isTopicWritable(id)
  }

  function isAssocWritable (id) {
    return dm5.permCache.isAssocWritable(id)
  }

  function selectTopicmapIf (topic) {
    if (topic.typeUri === 'dmx.topicmaps.topicmap') {
      store.dispatch('selectTopicmap', topic.id)
    }
  }
}
