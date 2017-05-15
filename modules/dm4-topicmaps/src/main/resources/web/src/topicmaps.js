import dm5 from 'dm5'

const state = {
  topicmap: undefined,      // selected topicmap (a Topicmap object)
  topicmapTopics: []
}

const actions = {

  onTopicmapSelect ({dispatch}, id) {
    dm5.restClient.getTopicmap(id).then(topicmap => {
      state.topicmap = topicmap
      dispatch('renderTopicmap', topicmap)   // get render libraries hold of the topicmap
    })
  },

  onTopicDragged (_, {id, pos}) {
    console.log('onTopicDragged', state.topicmap.id, id, pos)
    dm5.restClient.setTopicPosition(state.topicmap.id, id, pos)
  },

  onTopicReveal (_, id) {
    console.log('onTopicReveal', id)
    dm5.restClient.addTopicToTopicmap(state.topicmap.id, id, {
      'dm4.topicmaps.x': 100,   // TODO
      'dm4.topicmaps.y': 100,   // TODO
      'dm4.topicmaps.visibility': true,
    })
  },

  // WebSocket messages

  _addTopicToTopicmap (_, {topicmapId, topic, viewProps}) {
    // TODO
  },

  _setTopicPosition (_, {topicmapId, topicId, pos}) {
    if (topicmapId === state.topicmap.id) {
      state.topicmap.getTopic(topicId).setPosition(pos)
    }
  }
}

// init state
dm5.restClient.getTopicsByType('dm4.topicmaps.topicmap').then(topics => {
  state.topicmapTopics = topics
})

export default {
  state,
  actions
}
