import dm5 from '../rest-client'
import typeCache from '../type-cache'
import Vue from 'vue'
import Vuex from 'vuex'

// store modules
import componentRegistry from './modules/component-registry'

Vue.use(Vuex)

const state = {
  selectedObject: undefined,    // Topic or Association or undefined if nothing is selected
  topicTypes: undefined,
  assocTypes: undefined
}

const actions = {

  onSelectTopic ({dispatch}, id) {
    dm5.getTopic(id).then(topic => {
      state.selectedObject = topic
    })
  },

  onSelectAssoc ({dispatch}, id) {
    dm5.getAssoc(id).then(assoc => {
      state.selectedObject = assoc
    })
  }
}

const store = new Vuex.Store({
  state,
  actions,
  modules: {
    componentRegistry
  }
})

// init state

typeCache.init()

export default store
