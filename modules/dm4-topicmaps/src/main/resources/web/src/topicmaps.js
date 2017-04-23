import http from 'axios'

const state = {
  topicmap: undefined,
  topicmapTopics: []
}

const actions = {
  selectTopicmap (_, topicmapId) {
    state.topicmap = http.get('/topicmap/' + topicmapId)
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
