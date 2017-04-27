import dm5 from '../rest-client'
import typeCache from '../type-cache'
import Vue from 'vue'
import Vuex from 'vuex'

// store modules
import componentRegistry from './modules/component-registry'

Vue.use(Vuex)

const state = {
  selectedObject: undefined,    // Topic or Association or undefined if nothing is selected
  detailPanelMode: undefined,   // 'info' or 'form'
  // type cache
  topicTypes: undefined,
  assocTypes: undefined
}

const actions = {

  selectTopic (_, id) {
    dm5.getTopic(id, true).then(topic => {    // includeChilds=true
      state.selectedObject = topic
      state.detailPanelMode = 'info'
    })
  },

  selectAssoc (_, id) {
    dm5.getAssoc(id, true).then(assoc => {    // includeChilds=true
      state.selectedObject = assoc
      state.detailPanelMode = 'info'
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
