import dm5 from 'dm5'
import Vue from 'vue'
import Vuex from 'vuex'

Vue.use(Vuex)

const state = {
  selectedObject: undefined,    // Topic or Association or undefined if nothing is selected
  detailPanelMode: undefined,   // 'info' or 'form'
}

const actions = {

  selectTopic (_, id) {
    dm5.restClient.getTopic(id, true).then(topic => {    // includeChilds=true
      state.selectedObject = topic
      state.detailPanelMode = 'info'
    })
  },

  selectAssoc (_, id) {
    dm5.restClient.getAssoc(id, true).then(assoc => {    // includeChilds=true
      state.selectedObject = assoc
      state.detailPanelMode = 'info'
    })
  },

  edit () {
    state.detailPanelMode = 'form'
  },

  submit () {
    state.detailPanelMode = 'info'
  }
}

const store = new Vuex.Store({
  state,
  actions,
  modules: {
    componentRegistry: require('./modules/component-registry').default
  }
})

dm5.typeCache.init(store)

export default store
