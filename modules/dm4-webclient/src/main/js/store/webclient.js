import Vue from 'vue'
import Vuex from 'vuex'
import dm5 from 'dm5'

Vue.use(Vuex)

var compCount = 0

const state = {

  object: undefined,        // The selected Topic/Assoc/TopicType/AssocType.
                            // Undefined if nothing is selected.

  writable: undefined,      // True if the current user has WRITE permission for the selected object.

  objectRenderers: {},      // Registered page renderers:
                            //   {
                            //     typeUri: component
                            //   }

  quill: undefined,         // The Quill instance deployed in form mode.
                            // FIXME: support more than one Quill instance per form.

  compDefs: {}              // Registered components
}

const actions = {

  displayObject (_, object) {
    // console.log('displayObject')
    state.object = object.isType() ? object.asType() : object
    _initWritable()
  },

  emptyDisplay () {
    // console.log('emptyDisplay')
    state.object = undefined
  },

  // TODO: introduce edit buffer also for inline editing?
  submit ({dispatch}, object) {
    object.update().then(object => {
      dispatch('_processDirectives', object.directives)
    })
  },

  registerObjectRenderer (_, {typeUri, component}) {
    state.objectRenderers[typeUri] = component
  },

  setQuill (_, quill) {
    state.quill = quill
  },

  createTopicLink (_, topic) {
    console.log('createTopicLink', topic)
    state.quill.format('topic-link', {
      topicId: topic.id,
      linkId: undefined   // TODO
    })
  },

  unselectIf ({dispatch}, id) {
    // console.log('unselectIf', id, isSelected(id))
    if (isSelected(id)) {
      dispatch('stripSelectionFromRoute')
    }
  },

  // ---

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
      // loading plugins and does not change afterwards. The watcher (see step 4) would not fire as it is
      // registered *after* the plugins are loaded.
      // TODO: think about startup order: instantiating the Webclient component vs. loading the plugins.
      // Note 2: props must be inited *before* the component is instantiated (see step 3). While instantiation
      // the component receives the declared "default" value (plugin.js), if no value is set already.
      // The default value must not be overridden by an undefined init value.
      const propsData = {}
      for (let prop in compDef.props) {
        propsData[prop] = compDef.props[prop](store.state)    // call getter function
      }
      // 2) instantiate
      // Note: to manually mounted components the store must be passed explicitly
      // https://forum.vuejs.org/t/this-store-undefined-in-manually-mounted-vue-component/8756
      const Component = Vue.extend(compDef.comp)
      const comp = new Component({store, propsData}).$mount(`#mount-${compDef.id}`)
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
