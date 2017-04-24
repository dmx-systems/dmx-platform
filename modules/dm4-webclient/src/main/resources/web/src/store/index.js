import http from 'axios'
import Vue from 'vue'
import Vuex from 'vuex'

// store modules
import toolbar from './modules/toolbar'
import TopicmapPanel from 'dm5-topicmap-panel'
import DetailPanel   from 'dm5-detail-panel'

Vue.use(Vuex)

const state = {
  topicTypes: undefined,
  assocTypes: undefined
}

const actions = {
  onSelectTopicmap ({dispatch}, topicmapId) {
    http.get('/topicmap/' + topicmapId).then(response => {
      dispatch("displayTopicmap", response.data)
    }).catch(response => {
      console.error(response)
    })
  },
  onSelectTopic ({dispatch}, topicId) {
    http.get('/core/topic/' + topicId).then(response => {
      dispatch("displayObject", response.data)
    }).catch(response => {
      console.error(response)
    })
  },
  onSelectAssoc ({dispatch}, assocId) {
    http.get('/core/association/' + assocId).then(response => {
      dispatch("displayObject", response.data)
    }).catch(response => {
      console.error(response)
    })
  }
}

const store = new Vuex.Store({
  state,
  actions,
  modules: {
    toolbar,
    topicmapPanel: TopicmapPanel.storeModule,
    detailPanel:   DetailPanel.storeModule
  }
})

// init state

http.get('/core/topictype/all').then(response => {
  state.topicTypes = hashByUri(response.data)
})
http.get('/core/assoctype/all').then(response => {
  state.assocTypes = hashByUri(response.data)
})

// utilities

function mapByUri (topics) {
  var map = {}
  topics.forEach(topic => map[topic.uri] = topic)
  return map
}

export default store
