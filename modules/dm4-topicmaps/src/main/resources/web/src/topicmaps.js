import http from 'axios'

const state = {
  topicmap: undefined,
  topicmapTopics: []
}

const actions = {
  selectTopicmap (_, topicmapId) {
    console.log('select topicmap', topicmapId)
    http.get('/topicmap/' + topicmapId).then(response => {
      state.topicmap = response.data
    })
  }
}

// init state
http.get('/core/topic/by_type/dm4.topicmaps.topicmap').then(response => {
  state.topicmapTopics = response.data
})

export default {
  state,
  actions
}
