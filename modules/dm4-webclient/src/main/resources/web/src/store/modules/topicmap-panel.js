import http from 'axios'

const state = {
  topicmapTopics: []
}

const actions = {

  init () {
    http.get('/core/topic/by_type/dm4.topicmaps.topicmap').then(response => {
      state.topicmapTopics = response.data
      console.log('topicmapTopics', state.topicmapTopics)
    })
  }
}

export default {
  state,
  actions
}
