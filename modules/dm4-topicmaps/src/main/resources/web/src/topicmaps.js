import http from 'axios'

const state = {
  topicmapTopics: []
}

const actions = {
  selectTopicmap ({dispatch}, topicmapId) {
    dispatch('onSelectTopicmap', topicmapId)
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
