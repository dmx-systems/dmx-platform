import dm5 from 'dm5'

export default ({store}) => {
  return {

    storeModule: {
      name: 'search',
      module: require('./search').default
    },

    components: [
      {
        comp: require('dm5-search-widget').default,
        mount: 'webclient',
        props: {
          visible:        state => state.search.visible,
          pos:            state => state.search.pos,
          extraMenuItems: state => state.search.extraMenuItems,
          createEnabled:  state => state.workspaces.isWritable,
          markerIds:      (_, getters) => getters && getters.visibleTopicIds,
          menuTopicTypes: (_, getters) => getters && getters.menuTopicTypes  // TODO: why is getters undefined on start?
        },
        listeners: {
          'topic-reveal': revealTopic,
          'topic-create': createTopic,
          'extra-create': createExtra,
          close: _ => store.dispatch('closeSearchWidget')
        }
      }
    ]
  }

  function revealTopic (topic) {
    const state = store.state.search
    store.dispatch('revealTopic', {
      topic,
      pos: state.pos.model,
      select: !(state.options && state.options.noSelect)
    })
    state.options && state.options.topicHandler && state.options.topicHandler(topic)
  }

  function createTopic ({topicType, value}) {
    const topicModel = topicType.newTopicModel(value)
    // console.log('createTopic', topicModel)
    dm5.restClient.createTopic(topicModel).then(topic => {
      console.log('Created', topic)
      revealTopic(topic)
      store.dispatch('_processDirectives', topic.directives)
    })
  }

  function createExtra ({extraItem, value, optionsData}) {
    extraItem.create(value, optionsData, store.state.search.pos.model)
  }
}
