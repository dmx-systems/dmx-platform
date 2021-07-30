export default ({store, dmx}) => {
  return {

    storeModule: {
      name: 'topicmaps',
      module: require('./topicmaps').default
    },

    components: [
      {
        comp: require('dmx-topicmap-panel').default,
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
          contextCommands: state => state.contextCommands,
          dropHandler:     state => state.dropHandler,
          quillConfig:     state => state.quillConfig
        },
        listeners: {
          'topic-select':         id             => store.dispatch('selectTopic', id),
          'topic-unselect':       id             => store.dispatch('unselectTopic', id),
          'topic-double-click':   topic          => invokeDoubleClickHandler(topic),
          'topic-dragged':        ({id, pos})    => store.dispatch('setTopicPosition', {id, pos}),
          'topics-dragged':       topicCoords    => store.dispatch('setTopicPositions', topicCoords),
          'topic-dropped':        ids            => store.dispatch('handleTopicDrop', ids),
          'topic-pin':            ({id, pinned}) => store.dispatch('setTopicPinned', {
                                                                                        topicId: id,
                                                                                        pinned,
                                                                                        showDetails: showDetails()
                                                                                     }),      /* eslint indent: "off" */
          'assoc-create':         playerIds      => store.dispatch('createAssoc', playerIds),
          'assoc-select':         id             => store.dispatch('selectAssoc', id),
          'assoc-unselect':       id             => store.dispatch('unselectAssoc', id),
          'assoc-pin':            ({id, pinned}) => store.dispatch('setAssocPinned', {
                                                                                        assocId: id,
                                                                                        pinned,
                                                                                        showDetails: showDetails()
                                                                                     }),
          'topicmap-contextmenu': pos            => store.dispatch('openSearchWidget', {pos}),
          'object-submit':        object         => store.dispatch('submit', object),
          'child-topic-reveal':   relTopic       => store.dispatch('revealRelatedTopic', {relTopic})
        }
      },
      {
        comp: require('./components/dmx-topicmap-commands').default,
        mount: 'toolbar-left'
      }
    ],

    topicmapType: {
      uri: 'dmx.topicmaps.topicmap',
      name: 'Topicmap',
      renderer: () => import('dmx-cytoscape-renderer' /* webpackChunkName: "dmx-cytoscape-renderer" */)
    },

    topicmapCommands: {
      'dmx.topicmaps.topicmap': [
        require('./components/dmx-topicmap-info').default,
        require('./components/dmx-topicmap-fit').default,
        require('./components/dmx-topicmap-reset').default
      ]
    },

    contextCommands: {
      topic: [
        {label: 'Hide', multi: true, handler: idLists => store.dispatch('hideMulti', idLists)},
        {
          label: 'Edit',
          handler: id => store.dispatch('callTopicDetailRoute', {id, detail: 'edit'}),
          disabled: isEditDisabled
        },
        {
          label: 'Delete',
          multi: true,
          handler: idLists => store.dispatch('deleteMulti', idLists),
          disabled: isTopicDeleteDisabled
        },
        {label: 'Related', handler: id => store.dispatch('callTopicDetailRoute', {id, detail: 'related'})},
        {label: 'Details', handler: id => store.dispatch('callTopicDetailRoute', {id, detail: 'info'})}
      ],
      assoc: [
        {label: 'Hide', multi: true, handler: idLists => store.dispatch('hideMulti', idLists)},
        {
          label: 'Edit',
          handler: id => store.dispatch('callAssocDetailRoute', {id, detail: 'edit'}),
          disabled: isEditDisabled
        },
        {
          label: 'Delete',
          multi: true,
          handler: idLists => store.dispatch('deleteMulti', idLists),
          disabled: isAssocDeleteDisabled
        },
        {label: 'Related', handler: id => store.dispatch('callAssocDetailRoute', {id, detail: 'related'})},
        {label: 'Details', handler: id => store.dispatch('callAssocDetailRoute', {id, detail: 'info'})}
      ]
    },

    iconRenderers: {
      'dmx.topicmaps.topicmap': topic => {
        const mapTypeUri = topic.children['dmx.topicmaps.topicmap_type_uri'].value
        return dmx.typeCache.getTopicType(mapTypeUri).getViewConfig('dmx.webclient.icon')
      }
    },

    doubleClickHandlers: {
      'dmx.topicmaps.topicmap': topicmap => store.dispatch('selectTopicmap', topicmap.id)
    }
  }

  function invokeDoubleClickHandler (topic) {
    const handler = store.state.doubleClickHandlers[topic.typeUri]
    handler && handler(topic)
  }

  function showDetails () {
    return store.getters.showInmapDetails
  }

  // ---

  /**
   * @param   id    a topic/assoc ID.
   *
   * @return  a promise for a boolean
   */
  function isEditDisabled (id) {
    const object = store.state.topicmaps.topicmap.getObject(id)
    return !object.isEditable ? Promise.resolve(true) : isWritable(id).then(writable => !writable)
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
    return Promise.all([...idLists.topicIds, ...idLists.assocIds].map(isWritable)).then(writables =>
      writables.some(writable => !writable)
    )
  }

  function isWritable (id) {
    return dmx.permCache.isWritable(id)
  }
}
