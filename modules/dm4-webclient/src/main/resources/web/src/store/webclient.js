import dm5 from '../rest-client'
import typeCache from '../type-cache'
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
      dispatch("displayDetails", topic)
    })
  },
  onSelectAssoc ({dispatch}, id) {
    dm5.getAssoc(id).then(assoc => {
      dispatch("displayDetails", assoc)
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

typeCache.init()

export default store
