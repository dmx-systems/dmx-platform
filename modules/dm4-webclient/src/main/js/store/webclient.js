import Vue from 'vue'
import Vuex from 'vuex'
import dm5 from 'dm5'
import Selection from '../selection'

Vue.use(Vuex)

var compCount = 0

const state = {

  object: undefined,        // The selected Topic/Assoc/TopicType/AssocType.
                            // Undefined if nothing is selected.

  writable: undefined,      // True if the current user has WRITE permission for the selected object.

  selection: new Selection(handleSelection),

  objectRenderers: {},      // Registered object renderers:
                            //   {
                            //     typeUri: component
                            //   }

  compDefs: {},             // Registered webclient components

  quillConfig: {
    options: {
      theme: 'bubble',
      modules: {
        toolbar: {
          container: [
            ['bold', 'italic', 'code'],
            ['blockquote', 'code-block'],
            [{'list': 'ordered'}, {'list': 'bullet'}],
            [{'header': [1, 2, 3, false]}],
            ['topic-link', 'link', 'image', 'video']
          ]
        }
      }
    },
    // TODO: allow DM webclient plugins to provide Quill extensions
    extensions: [
      require('../topic-link').default
    ]
  }
}

const actions = {

  displayObject (_, object) {
    // console.log('displayObject', object)
    state.object = object.isType() ? object.asType() : object  // logical copy in createDetail() (cytoscape-renderer.js)
    _initWritable()
  },

  emptyDisplay () {
    // console.log('emptyDisplay')
    state.object = undefined
  },

  submit ({dispatch}, object) {
    object.update().then(object => {
      dispatch('_processDirectives', object.directives)
    })
  },

  // ---

  /**
   * Preconditions:
   * - the route is *not* yet set.
   */
  selectTopic (_, id) {
    console.log('selectTopic', id)
    state.selection.addTopic(id)
  },

  /**
   * Preconditions:
   * - the route is *not* yet set.
   */
  selectAssoc (_, id) {
    console.log('selectAssoc', id)
    state.selection.addAssoc(id)
  },

  /**
   * Preconditions:
   * - the route is *not* yet set.
   */
  unselectTopic (_, id) {
    console.log('unselectTopic', id)
    state.selection.removeTopic(id)
  },

  /**
   * Preconditions:
   * - the route is *not* yet set.
   */
  unselectAssoc (_, id) {
    console.log('unselectAssoc', id)
    state.selection.removeAssoc(id)
  },

  // ---

  /**
   * Preconditions:
   * - the route is set.
   */
  displayTopicmap (_, id) {
    state.selection.empty()   // TODO: have per-topicmap "selection" state?
  },

  // ---

  unselectIf ({dispatch}, id) {
    // console.log('unselectIf', id, isSelected(id))
    if (isSelected(id)) {
      dispatch('stripSelectionFromRoute')
    }
  },

  // ---

  registerObjectRenderer (_, {typeUri, component}) {
    state.objectRenderers[typeUri] = component
  },

  registerComponent (_, compDef) {
    const compDefs = state.compDefs[compDef.mount] || (state.compDefs[compDef.mount] = [])
    compDef.id = compCount++
    compDefs.push(compDef)
  },

  /**
   * Instantiates and mounts the registered components for mount point "webclient".
   */
  mountComponents () {
    state.compDefs.webclient.forEach(compDef => {
      // 1) init props
      // Note 1: props must be inited explicitly. E.g. the "objectRenderers" store state is populated while
      // loading plugins and does not change afterwards. The watcher (see step 3) would not fire as it is
      // registered *after* the plugins are loaded.
      // TODO: think about startup order: instantiating the Webclient component vs. loading the plugins.
      // Note 2: props must be inited *before* the component is instantiated (see step 2). While instantiation
      // the component receives the declared "default" value (plugin.js), if no value is set already.
      // The default value must not be overridden by an undefined init value.
      const propsData = {}
      for (let prop in compDef.props) {
        propsData[prop] = compDef.props[prop](store.state)    // call getter function
      }
      // 2) instantiate
      // Note: to manually mounted components the store must be passed explicitly
      // https://forum.vuejs.org/t/this-store-undefined-in-manually-mounted-vue-component/8756
      const comp = new Vue({store, propsData, ...compDef.comp}).$mount(`#mount-${compDef.id}`)
      // 3) make props reactive
      for (let prop in compDef.props) {
        registerPropWatcher(comp, prop, compDef.props[prop])
      }
      // 4) add event listeners
      for (let eventName in compDef.listeners) {
        comp.$on(eventName, compDef.listeners[eventName])
      }
      // TODO: unregister listeners?
    })
  },

  //

  loggedIn () {
    initWritable()
  },

  loggedOut () {
    initWritable()
  },

  // WebSocket messages

  _processDirectives ({dispatch}, directives) {
    console.log(`Webclient: processing ${directives.length} directives`, directives)
    directives.forEach(dir => {
      switch (dir.type) {
      case "UPDATE_TOPIC":
        displayObjectIf(new dm5.Topic(dir.arg))
        break
      case "DELETE_TOPIC":
        dispatch('unselectIf', dir.arg.id)
        break
      case "UPDATE_ASSOCIATION":
        displayObjectIf(new dm5.Assoc(dir.arg))
        break
      case "DELETE_ASSOCIATION":
        dispatch('unselectIf', dir.arg.id)
        break
      }
    })
  }
}

const store = new Vuex.Store({
  state,
  actions
})

export default store

//

function displayObjectIf (object) {
  if (isSelected(object.id)) {
    store.dispatch('displayObject', object)
  }
}

function handleSelection () {
  console.log('handleSelection', state.selection.topicIds, state.selection.assocIds)
  if (state.selection.isSingle()) {
    const topicId = state.selection.singleTopicId()
    const assocId = state.selection.singleAssocId()
    topicId && store.dispatch('callTopicRoute', topicId)
    assocId && store.dispatch('callAssocRoute', assocId)
  } else {
    store.dispatch('stripSelectionFromRoute')
  }
}

function isSelected (id) {
  const object = state.object
  return object && object.id === id
}

//

function initWritable () {
   state.object && _initWritable()
}

function _initWritable () {
  state.object.isWritable().then(writable => {
    state.writable = writable
  })
}

//

function registerPropWatcher (comp, prop, getter) {
  // console.log('registerPropWatcher', prop)
  store.watch(
    getter,
    val => {
      // console.log(`"${prop}" changed`, val)
      comp.$props[prop] = val
    }
  )
}
