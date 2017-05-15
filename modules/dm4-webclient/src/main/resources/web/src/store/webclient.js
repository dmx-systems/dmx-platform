import dm5 from 'dm5'
import Vue from 'vue'
import Vuex from 'vuex'

Vue.use(Vuex)

const state = {
  selectedObject: undefined,  // Topic or Association or undefined if nothing is selected
  detailPanel: {
    mode: undefined           // 'info' or 'form'
  }
}

const actions = {

  onTopicSelect (_, id) {
    dm5.restClient.getTopic(id, true).then(topic => {    // includeChilds=true
      state.selectedObject = topic
      state.detailPanel.mode = 'info'
    })
  },

  onAssocSelect (_, id) {
    dm5.restClient.getAssoc(id, true).then(assoc => {    // includeChilds=true
      state.selectedObject = assoc
      state.detailPanel.mode = 'info'
    })
  },

  onBackgroundTap ({dispatch}, pos) {
    dispatch('openSearchWidget', pos.rendered)
  },

  edit () {
    state.detailPanel.mode = 'form'
  },

  submit () {
    state.detailPanel.mode = 'info'
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
