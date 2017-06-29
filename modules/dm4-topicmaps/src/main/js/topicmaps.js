import dm5 from 'dm5'

const state = {
  topicmap: undefined,      // view model: the rendered topicmap (a Topicmap object)
  topicmapTopics: []
}

const actions = {

  renderTopicmap ({dispatch}, id) {
    dm5.restClient.getTopicmap(id).then(topicmap => {
      // update view model
      state.topicmap = topicmap
      // sync renderer
      dispatch('syncTopicmap', topicmap)
    }).catch(error => {
      console.error(error)
    })
  },

  revealTopic ({dispatch}, {topic, pos}) {
    const viewProps = {
      'dm4.topicmaps.x': pos.x,
      'dm4.topicmaps.y': pos.y,
      'dm4.topicmaps.visibility': true,
    }
    // update view model
    const added = state.topicmap.addTopic(topic.newViewTopic(viewProps))
    // sync renderer
    added && dispatch('syncAddTopic', topic.id)
    dispatch('syncSelect', topic.id)
    // sync clients
    added && dm5.restClient.addTopicToTopicmap(state.topicmap.id, topic.id, viewProps)
  },

  revealRelatedTopic ({dispatch}, {relTopic, pos}) {
    dispatch('revealTopic', {topic: relTopic, pos})
    //
    // update view model
    const added = state.topicmap.addAssoc(relTopic.assoc)
    // sync renderer
    added && dispatch('syncAddAssoc', relTopic.assoc.id)
    // sync clients
    added && dm5.restClient.addAssocToTopicmap(state.topicmap.id, relTopic.assoc.id)
  },

  onTopicDragged (_, {id, pos}) {
    // update view model
    state.topicmap.getTopic(id).setPosition(pos)
    // sync renderer (Note: the renderer is up-to-date already)
    // sync clients
    dm5.restClient.setTopicPosition(state.topicmap.id, id, pos)
  },

  onTopicDroppedOntoTopic (_, {topicId, droppedOntoTopicId}) {
    console.log(`Node ${topicId} dropped onto node ${droppedOntoTopicId}`)
    // TODO
  },

  // WebSocket messages

  _addTopicToTopicmap ({dispatch}, {topicmapId, viewTopic}) {
    if (topicmapId === state.topicmap.id) {
      // update view model
      const added = state.topicmap.addTopic(new dm5.ViewTopic(viewTopic))
      if (!added) {
        throw Error(`Topic ${viewTopic.id} already added to topimap ${topicmapId}`)
      }
      // sync renderer
      dispatch('syncAddTopic', viewTopic.id)
    }
  },

  _addAssocToTopicmap ({dispatch}, {topicmapId, assoc}) {
    if (topicmapId === state.topicmap.id) {
      // update view model
      const added = state.topicmap.addAssoc(new dm5.Assoc(assoc))
      if (!added) {
        throw Error(`Assoc ${assoc.id} already added to topimap ${topicmapId}`)
      }
      // sync renderer
      dispatch('syncAddAssoc', assoc.id)
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

// Process Directives

function updateTopic (topic, dispatch) {
  const _topic = state.topicmap.getTopicIfExists(topic.id)
  if (_topic) {
    // update view model
    _topic.value = topic.value
    // sync renderer
    dispatch('syncTopicLabel', topic.id)
  }
}
