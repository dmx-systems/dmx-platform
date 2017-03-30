import Vue from 'vue'
import Vuex from 'vuex'

import toolbar from './modules/toolbar'
import topicmapPanel from './modules/topicmap-panel'
import detailPanel from './modules/detail-panel'

Vue.use(Vuex)

const store = new Vuex.Store({

  state: {
  },

  actions: {
  },

  modules: {
    toolbar,
    topicmapPanel,
    detailPanel
  }
})

export default store
