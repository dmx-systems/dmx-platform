import dm5 from 'dm5'

const state = {
  topicmap: undefined,      // selected topicmap (a Topicmap object)
  topicmapTopics: []
}

const actions = {

  selectTopicmap ({dispatch}, id) {
    dm5.restClient.getTopicmap(id).then(topicmap => {
      // update view model
      state.topicmap = topicmap
      // notify renderer
      dispatch('renderTopicmap', topicmap)
    }).catch(error => {
      console.error(error)
    })
  },

  onTopicDragged (_, {id, pos}) {
    // update view model
    state.topicmap.getTopic(id).setPosition(pos)
    // notify renderer (Note: the view is up-to-date already)
    // sync clients
    dm5.restClient.setTopicPosition(state.topicmap.id, id, pos)
  },

  revealTopic ({dispatch}, {topic, pos}) {
    const viewProps = {
      'dm4.topicmaps.x': pos.x,
      'dm4.topicmaps.y': pos.y,
      'dm4.topicmaps.visibility': true,
    }
    // update view model
    state.topicmap.addTopic(topic.newViewTopic(viewProps))
    // notify renderer
    dispatch('addTopic', topic.id)
    dispatch('select', topic.id)
    // sync clients
    dm5.restClient.addTopicToTopicmap(state.topicmap.id, topic.id, viewProps)
  },

  // WebSocket messages

  _addTopicToTopicmap ({dispatch}, {topicmapId, viewTopic}) {
    if (topicmapId === state.topicmap.id) {
      // update view model
      state.topicmap.addTopic(new dm5.ViewTopic(viewTopic))
      // notify renderer
      dispatch('addTopic', viewTopic.id)
    }
  },

  _setTopicPosition ({dispatch}, {topicmapId, topicId, pos}) {
    if (topicmapId === state.topicmap.id) {
      // update view model
      state.topicmap.getTopic(topicId).setPosition(pos)
      // notify renderer
      dispatch('setTopicPosition', topicId)
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
