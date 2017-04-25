import http from 'axios'
import dm5 from '../rest-client'
import Vue from 'vue'
import Vuex from 'vuex'

// store modules
import toolbar from './modules/toolbar'
import TopicmapPanel from 'modules-nodejs/dm5-topicmap-panel/src/main.js'
import DetailPanel   from 'modules-nodejs/dm5-detail-panel/src/main.js'

Vue.use(Vuex)

const state = {
  topicTypes: undefined,
  assocTypes: undefined
}

const actions = {
  onSelectTopic ({dispatch}, id) {
    dm5.getTopic(id).then(topic => {
      dispatch("displayObject", topic)
    })
  },
  onSelectAssoc ({dispatch}, id) {
    dm5.getAssoc(id).then(assoc => {
      dispatch("displayObject", assoc)
    })
  },
  onSelectTopicmap ({dispatch}, id) {
    dm5.getTopicmap(id).then(topicmap => {
      dispatch("displayTopicmap", topicmap)
    })
  }
}

const store = new Vuex.Store({
  state,
  actions,
  modules: {
    toolbar,
    topicmapPanel: TopicmapPanel.storeModule,
    detailPanel: DetailPanel.storeModule
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
