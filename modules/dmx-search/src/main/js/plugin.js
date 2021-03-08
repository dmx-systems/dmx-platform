import dmx from 'dmx-api'
import '../style/style.css'

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
          markerTopicIds:   (_, getters) => getters && getters.visibleTopicIds,
          markerAssocIds:   (_, getters) => getters && getters.visibleAssocIds,
          createTopicTypes: (_, getters) => getters && getters.createTopicTypes,  // TODO: getters is undefined on start
          searchAssocTypes: () => dmx.typeCache.getAllAssocTypes(),
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

  function revealAssoc (assoc) {
    const pos = store.state.search.pos.model
    store.dispatch('revealTopic', {topic: assoc.player1.topic, pos,                                  noSelect: true})
    store.dispatch('revealTopic', {topic: assoc.player2.topic, pos: {x: pos.x + 340, y: pos.y - 40}, noSelect: true})
    store.dispatch('revealAssoc', {assoc})
  }

  function createTopic ({topicType, value}) {
    const topicModel = topicType.newTopicModel(value)
    dmx.rpc.createTopic(topicModel).then(topic => {
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
