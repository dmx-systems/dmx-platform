import Vue from 'vue'
import Vuex from 'vuex'

import topicmapPanel from './modules/topicmap-panel'
import detailPanel from './modules/detail-panel'

Vue.use(Vuex)

const store = new Vuex.Store({

  state: {
  },

  actions: {
  },

  modules: {
    topicmapPanel,
    detailPanel
  }
})

store.dispatch('init')

export default store
