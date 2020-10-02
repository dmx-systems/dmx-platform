import dm5 from 'dmx-api'

export default ({store}) => {
  return {

    storeModule: {
      name: 'search',
      module: require('./search').default
    },

    components: [
      {
        comp: require('dmx-search-widget').default,
        mount: 'webclient',
        props: {
          visible:          state => state.search.visible,
          extraMenuItems:   state => state.search.extraMenuItems,
          createEnabled:    state => state.workspaces.isWritable,
          markerIds:        (_, getters) => getters && getters.visibleTopicIds,
          createTopicTypes: (_, getters) => getters && getters.createTopicTypes,  // TODO: getters is undefined on start
          topicmapTypes:    state => Object.values(state.topicmaps.topicmapTypes)
        },
        listeners: {
          'topic-click':     revealTopic,
          'icon-click':      revealTopicNoSelect,
          'assoc-click':     revealAssoc,
          'topic-create':    createTopic,
          'extra-create':    createExtra,
          'topicmap-create': createTopicmap,
          close: _ => store.dispatch('closeSearchWidget')
        }
      }
    ]
  }

  function revealTopicNoSelect (topic) {
    revealTopic(topic, true)    // noSelect=true
  }

  function revealTopic (topic, noSelect) {
    const state = store.state.search
    store.dispatch('revealTopic', {
      topic,
      pos: state.pos.model,
      noSelect: noSelect || state.options.noSelect
    })
    state.options.topicHandler && state.options.topicHandler(topic)
  }

  function revealAssoc (relTopic) {
    const pos = store.state.search.pos.model
    store.dispatch('revealTopicById', {
      id: relTopic.assoc.getOtherPlayerId(relTopic.id),
      pos: {x: pos.x + 340, y: pos.y - 40},
      noSelect: true
    }).then(() => {
      store.dispatch('revealRelatedTopic', {relTopic, pos, noSelect: true})
      store.dispatch('selectAssoc', relTopic.assoc.id)
    })
  }

  function createTopic ({topicType, value}) {
    // Note: for value integration to work at least all identity fields must be filled
    const topicModel = new dm5.Topic(topicType.newTopicModel(value)).fillChildren()
    // console.log('createTopic', topicModel)
    dm5.restClient.createTopic(topicModel).then(topic => {
      // console.log('Created', topic)
      revealTopic(topic)
      store.dispatch('_processDirectives', topic.directives)
    })
  }

  function createExtra ({extraItem, value, optionsData}) {
    extraItem.create(value, optionsData, store.state.search.pos.model)
  }

  function createTopicmap ({name, topicmapTypeUri, viewProps}) {
    store.dispatch('createTopicmap', {name, topicmapTypeUri, viewProps})
  }
}
