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
    }).catch(error => {
      console.error(error)
    })
  },

  onTopicDragged (_, {id, pos}) {
    dm5.restClient.setTopicPosition(state.topicmap.id, id, pos)
  },

  onTopicReveal ({dispatch}, {topic, pos}) {
    // update view model
    const viewProps = {
      'dm4.topicmaps.x': pos.x,
      'dm4.topicmaps.y': pos.y,
      'dm4.topicmaps.visibility': true,
    }
    topic.view_props = viewProps
    state.topicmap.addTopic(new dm5.TopicmapTopic(topic))
    // update view
    dispatch('addTopicAndSelect', topic.id)
    // update server
    dm5.restClient.addTopicToTopicmap(state.topicmap.id, topic.id, viewProps)
  },

  // WebSocket messages

  _addTopicToTopicmap (_, {topicmapId, topic}) {
    if (topicmapId === state.topicmap.id) {
      state.topicmap.addTopic(new dm5.TopicmapTopic(topic))   // TODO: let the websocket dispatcher do the instantiation
    }
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
