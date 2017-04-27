import dm5 from 'dm5'

const state = {
  topicmap: undefined,      // selected topicmap
  topicmapTopics: []
}

const actions = {
  selectTopicmap (_, id) {
    dm5.restClient.getTopicmap(id).then(topicmap => {
      state.topicmap = topicmap
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
