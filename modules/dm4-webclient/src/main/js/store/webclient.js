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

  unselect (_, id) {
    // Note: selectedObject might be undefined if there was a multiple selection
    if (state.selectedObject && state.selectedObject.id === id) {
      state.selectedObject = undefined
    }
  },

  edit () {
    state.selectedObject.fillChilds()
    state.detailPanel.mode = 'form'
  },

  submit ({dispatch}) {
    state.detailPanel.mode = 'info'
    dm5.restClient.updateTopic(state.selectedObject).then(topic => {
      dispatch('_processDirectives', topic.directives)
    })
  },

  /**
   * @param   pos   `model` and `render` positions
   */
  onBackgroundRightClick ({dispatch}, pos) {
    dispatch('openSearchWidget', pos)
  },

  // WebSocket messages

  _processDirectives (_, directives) {
    console.log(`Webclient: processing ${directives.length} directives ...`)
    directives.forEach(dir => {
      switch (dir.type) {
      case "UPDATE_TOPIC":
        updateTopic(new dm5.Topic(dir.arg))
        break
      case "DELETE_TOPIC":
        // TODO
        break
      case "UPDATE_ASSOCIATION":
        // TODO
        break
      case "DELETE_ASSOCIATION":
        // TODO
        break
      case "UPDATE_TOPIC_TYPE":
        // TODO
        break
      case "DELETE_TOPIC_TYPE":
        // TODO
        break
      case "UPDATE_ASSOCIATION_TYPE":
        // TODO
        break
      case "DELETE_ASSOCIATION_TYPE":
        // TODO
        break
      default:
        throw Error(`"${dir.type}" is an unsupported directive`)
      }
    })
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

// ---

function updateTopic (topic) {
  if (state.selectedObject && state.selectedObject.id === topic.id) {
    state.selectedObject = topic
  }
}
