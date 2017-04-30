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
  },

  setTopicPosition (_, {id, pos}) {
    console.log('moveTopic', state.topicmap.info.id, id, pos)
    dm5.restClient.setTopicPosition(state.topicmap.info.id, id, pos)
  },
}

// init state
dm5.restClient.getTopicsByType('dm4.topicmaps.topicmap').then(topics => {
  state.topicmapTopics = topics
})

export default {
  state,
  actions
}
