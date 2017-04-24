import Vue from 'vue'
import Vuex from 'vuex'

import toolbar from './modules/toolbar'
import TopicmapPanel from 'dm5-topicmap-panel'
import detailPanel from './modules/detail-panel'

Vue.use(Vuex)

const store = new Vuex.Store({

  state: {
  },

  actions: {
  },

  modules: {
    toolbar,
    topicmapPanel: TopicmapPanel.storeModule,
    detailPanel
  }
})

export default store
