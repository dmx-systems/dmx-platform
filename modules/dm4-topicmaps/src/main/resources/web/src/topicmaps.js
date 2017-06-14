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
      // sync renderer
      dispatch('syncTopicmap', topicmap)
    }).catch(error => {
      console.error(error)
    })
  },

  onTopicDragged (_, {id, pos}) {
    // update view model
    state.topicmap.getTopic(id).setPosition(pos)
    // sync renderer (Note: the renderer is up-to-date already)
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
    // sync renderer
    dispatch('syncShowTopic', topic.id)
    dispatch('syncSelection', topic.id)
    // sync clients
    dm5.restClient.addTopicToTopicmap(state.topicmap.id, topic.id, viewProps)
  },

  // WebSocket messages

  _addTopicToTopicmap ({dispatch}, {topicmapId, viewTopic}) {
    if (topicmapId === state.topicmap.id) {
      // update view model
      state.topicmap.addTopic(new dm5.ViewTopic(viewTopic))
      // sync renderer
      dispatch('syncShowTopic', viewTopic.id)
    }
  },

  _setTopicPosition ({dispatch}, {topicmapId, topicId, pos}) {
    if (topicmapId === state.topicmap.id) {
      // update view model
      state.topicmap.getTopic(topicId).setPosition(pos)
      // sync renderer
      dispatch('syncTopicPosition', topicId)
    }
  },

  _processDirectives ({dispatch}, directives) {
    console.log(`Topicmaps: processing ${directives.length} directives ...`)
    directives.forEach(dir => {
      switch (dir.type) {
      case "UPDATE_TOPIC":
        updateTopic(dir.arg, dispatch)
        break
      case "DELETE_TOPIC":
        // TODO
        break
      case "UPDATE_ASSOCIATION":
        // TODO
        break
      case "DELETE_ASSOCIATION":
        // TODO
        break
      case "UPDATE_TOPIC_TYPE":
        // TODO
        break
      case "DELETE_TOPIC_TYPE":
        // TODO
        break
      case "UPDATE_ASSOCIATION_TYPE":
        // TODO
        break
      case "DELETE_ASSOCIATION_TYPE":
        // TODO
        break
      default:
        throw Error(`"${dir.type}" is an unsupported directive`)
      }
    })
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

// ---

function updateTopic (topic, dispatch) {
  const _topic = state.topicmap.getTopicIfExists(topic.id)
  if (_topic) {
    // update view model
    _topic.value = topic.value
    // sync renderer
    dispatch('syncTopicLabel', topic.id)
  }
}
