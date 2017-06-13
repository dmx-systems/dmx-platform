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

  selectTopic (_, id) {
    dm5.restClient.getTopic(id, true).then(topic => {    // includeChilds=true
      state.selectedObject = topic
      state.detailPanel.mode = 'info'
    })
  },

  selectAssoc (_, id) {
    dm5.restClient.getAssoc(id, true).then(assoc => {    // includeChilds=true
      state.selectedObject = assoc
      state.detailPanel.mode = 'info'
    })
  },

  edit () {
    state.selectedObject.fillChilds()
    state.detailPanel.mode = 'form'
  },

  submit () {
    dm5.restClient.updateTopic(state.selectedObject)
    state.detailPanel.mode = 'info'
  },

  /**
   * @param   pos   `model` and `render` positions
   */
  onBackgroundTap ({dispatch}, pos) {
    dispatch('openSearchWidget', pos)
  },

  // WebSocket messages

  _processDirectives (_, directives) {
    console.log(`Processing ${directives.length} directives ...`)
    // TODO
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
