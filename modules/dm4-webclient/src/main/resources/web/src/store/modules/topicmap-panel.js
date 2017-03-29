import http from 'axios'

const state = {
  topicmapTopics: []
}

const actions = {
  init () {
    console.log('TopicmapPanel state init() called!')
    http.get('/core/topic/by_type/dm4.topicmaps.topicmap').then(response => {
      state.topicmapTopics = response.data
    })
  }
}

export default {
  state,
  actions
}
