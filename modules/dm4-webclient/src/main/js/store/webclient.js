import dm5 from 'dm5'
import router from '../router'
import Vue from 'vue'
import Vuex from 'vuex'

Vue.use(Vuex)

const state = {
  selectedObject: undefined,  // the Topic/Assoc to display in the detail panel; if undefined the detail panel is empty
  detailPanel: {
    mode: undefined           // 'info' or 'form'
  }
}

const actions = {

  selectTopic (_, id) {
    router.push({
      name: 'topic',
      params: {
        topicId: id
      }
    })
  },

  selectAssoc (_, id) {
    router.push({
      name: 'assoc',
      params: {
        assocId: id
      }
    })
  },

  // TODO: we need a general approach to unify both situations: when we have the real object at hand,
  // and when we only have its ID. The same object must not be retrieved twice.

  fetchTopicAndDisplayInDetailPanel (_, id) {
    dm5.restClient.getTopic(id, true).then(topic => {    // includeChilds=true
      state.selectedObject = topic
      state.detailPanel.mode = 'info'
    })
  },

  fetchAssocAndDisplayInDetailPanel (_, id) {
    dm5.restClient.getAssoc(id, true).then(assoc => {    // includeChilds=true
      state.selectedObject = assoc
      state.detailPanel.mode = 'info'
    })
  },

  unselect (_, id) {
    console.log('unselect', id, isSelected(id))
    if (isSelected(id)) {
      router.push({
        name: 'topicmap'
      })
    }
  },

  _unselect () {
    console.log('_unselect')
    state.selectedObject = undefined
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

  /**
   * @param   pos   `model` and `render` positions
   */
  onBackgroundRightClick ({dispatch}, pos) {
    dispatch('openSearchWidget', {pos})
  },

  // WebSocket message processing

  _processDirectives ({dispatch}, directives) {
    console.log(`Webclient: processing ${directives.length} directives`)
    directives.forEach(dir => {
      switch (dir.type) {
      case "UPDATE_TOPIC":
        setSelectedObject(new dm5.Topic(dir.arg))
        break
      case "DELETE_TOPIC":
        dispatch('unselect', dir.arg.id)
        break
      case "UPDATE_ASSOCIATION":
        setSelectedObject(new dm5.Assoc(dir.arg))
        break
      case "DELETE_ASSOCIATION":
        dispatch('unselect', dir.arg.id)
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

// Note: the dm5 library must be inited *before* the SearchWidget component is created.
// The SearchWidget relies on dm5's "menuTopicTypes" store getter.

dm5.init(store)

export default store

// ---

function setSelectedObject (object) {
  if (isSelected(object.id)) {
    state.selectedObject = object
  }
}

function isSelected (id) {
  return state.selectedObject && state.selectedObject.id === id
}
