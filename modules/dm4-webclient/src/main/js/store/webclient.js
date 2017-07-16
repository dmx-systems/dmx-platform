import dm5 from 'dm5'
import Vue from 'vue'
import Vuex from 'vuex'

Vue.use(Vuex)

const state = {
  selectedObject: undefined,  // Topic or Assoc or undefined if nothing is selected
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
    unsetSelectedObject(id)
  },

  edit () {
    state.selectedObject.fillChilds()
    state.detailPanel.mode = 'form'
  },

  submit ({dispatch}) {
    state.detailPanel.mode = 'info'
    state.selectedObject.update().then(object => {
      dispatch('_processDirectives', object.directives)
    })
  },

  onHideTopic (_, id) {
    unsetSelectedObject(id)
  },

  onHideAssoc (_, id) {
    unsetSelectedObject(id)
  },

  /**
   * @param   pos   `model` and `render` positions
   */
  onBackgroundRightClick ({dispatch}, pos) {
    dispatch('openSearchWidget', pos)
  },

  // WebSocket message processing

  _processDirectives (_, directives) {
    console.log(`Webclient: processing ${directives.length} directives ...`)
    directives.forEach(dir => {
      switch (dir.type) {
      case "UPDATE_TOPIC":
        setSelectedObject(new dm5.Topic(dir.arg))
        break
      case "DELETE_TOPIC":
        unsetSelectedObject(dir.arg.id)
        break
      case "UPDATE_ASSOCIATION":
        setSelectedObject(new dm5.Assoc(dir.arg))
        break
      case "DELETE_ASSOCIATION":
        unsetSelectedObject(dir.arg.id)
        break
      case "UPDATE_TOPIC_TYPE":
        // TODO
        console.warn('Directive UPDATE_TOPIC_TYPE not yet implemented')
        break
      case "DELETE_TOPIC_TYPE":
        // TODO
        console.warn('Directive DELETE_TOPIC_TYPE not yet implemented')
        break
      case "UPDATE_ASSOCIATION_TYPE":
        // TODO
        console.warn('Directive UPDATE_ASSOCIATION_TYPE not yet implemented')
        break
      case "DELETE_ASSOCIATION_TYPE":
        // TODO
        console.warn('Directive DELETE_ASSOCIATION_TYPE not yet implemented')
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

function setSelectedObject (object) {
  if (state.selectedObject && state.selectedObject.id === object.id) {
    state.selectedObject = object
  }
}

function unsetSelectedObject (id) {
  if (state.selectedObject && state.selectedObject.id === id) {
    state.selectedObject = undefined
  }
}
