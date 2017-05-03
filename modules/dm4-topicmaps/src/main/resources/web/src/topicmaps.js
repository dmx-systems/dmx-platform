import dm5 from 'dm5'

const state = {
  topicmap: undefined,      // selected topicmap (a Topicmap object)
  topicmapTopics: []
}

const actions = {

  selectTopicmap ({dispatch}, id) {
    dm5.restClient.getTopicmap(id).then(topicmap => {
      dispatch('setTopicmap', topicmap)   // get render libraries hold of the topicmap
    })
  },

  setTopicmap (_, topicmap) {
    state.topicmap = topicmap
  },

  setTopicPosition (_, {id, pos}) {
    console.log('setTopicPosition', state.topicmap.id, id, pos)
    dm5.restClient.setTopicPosition(state.topicmap.id, id, pos)
  },

  // WebSocket messages

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
