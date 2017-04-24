import http from 'axios'

const state = {
  topicmapTopics: []
}

const actions = {
  selectTopicmap ({dispatch}, topicmapId) {
    console.log('select topicmap', topicmapId)
    http.get('/topicmap/' + topicmapId).then(response => {
      dispatch("setTopicmapData", response.data)
    }).catch(response => {
      console.error(response)
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
