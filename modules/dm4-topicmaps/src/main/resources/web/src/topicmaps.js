import dm5 from 'modules/dm4-webclient/src/main/resources/web/src/rest-client'

const state = {
  topicmap: undefined,      // selected topicmap
  topicmapTopics: []
}

const actions = {
  selectTopicmap (_, id) {
    dm5.getTopicmap(id).then(topicmap => {
      state.topicmap = topicmap
    })
  }
}

// init state
dm5.getTopicsByType('dm4.topicmaps.topicmap').then(topics => {
  state.topicmapTopics = topics
})

export default {
  state,
  actions
}
