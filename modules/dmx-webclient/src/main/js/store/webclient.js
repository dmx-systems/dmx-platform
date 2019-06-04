import Vue from 'vue'
import Vuex from 'vuex'
import dm5 from 'dm5'

Vue.use(Vuex)

let compCount = 0

const state = {

  object: undefined,        // If there is a single-selection: the selected Topic/Assoc/TopicType/AssocType.
                            // This object is displayed in the detail panel. Its ID appears in the browser URL.
                            // Undefined if there is no selection or a multi-selection.

  writable: undefined,      // True if the current user has WRITE permission for the selected object.

  detailRenderers: {        // Registered detail renderers:
    object: {},             //   {
    value: {}               //     typeUri: component
  },                        //   }

  compDefs: {},             // Registered webclient components:
                            // {
                            //    mount: [compDef]
                            // }
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
    // TODO: allow DMX webclient plugins to provide Quill extensions
    extensions: [
      require('../topic-link').default
    ]
  }
}

const actions = {

  displayObject (_, object) {
    // console.log('displayObject', object)
    state.object = object
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

  deleteMulti ({dispatch}, idLists) {
    // console.log('deleteMulti', idLists.topicIds, idLists.assocIds)
    // update client state + sync view (for immediate visual feedback)
    idLists.topicIds.forEach(id => dispatch('_deleteTopic', id))
    idLists.assocIds.forEach(id => dispatch('_deleteAssoc', id))
    // update server state
    dm5.restClient.deleteMulti(idLists).then(object => {
      dispatch('_processDirectives', object.directives)
    })
  },

  // ---

  unselectIf ({dispatch}, id) {
    // console.log('unselectIf', id, isSelected(id))
    if (isSelected(id)) {
      dispatch('stripSelectionFromRoute')
    }
  },

  // ---

  /**
   * @param   render    "object" or "value"
   */
  registerDetailRenderer (_, {renderer, typeUri, component}) {
    state.detailRenderers[renderer][typeUri] = component
  },

  registerComponent (_, compDef) {
    const compDefs = state.compDefs[compDef.mount] || (state.compDefs[compDef.mount] = [])
    compDef.id = compCount++
    compDefs.push(compDef)
  },

  /**
   * Instantiates and mounts the registered components for mount point "webclient".
   */
  mountComponents (_, parent) {
    state.compDefs.webclient.forEach(compDef => {
      // 1) init props
      // Note 1: props must be inited explicitly. E.g. the "detailRenderers" store state is populated while
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
      // 2) instantiate & mount
      // Note: to manually mounted components the store must be passed explicitly resp. "parent" must be set.
      // https://forum.vuejs.org/t/this-store-undefined-in-manually-mounted-vue-component/8756
      const comp = new Vue({parent, propsData, ...compDef.comp}).$mount(`#mount-${compDef.id}`)
      // 3) make props reactive
      for (let prop in compDef.props) {
        watchProp(comp, prop, compDef.props[prop])
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

const getters = {

  // Recalculate "object" once the underlying type changes.
  // The detail panel updates when a type is renamed.
  object: state => {
    // console.log('object getter', state.object, state.object && state.typeCache.topicTypes[state.object.uri])
    // ### FIXME: the asCompDef() approach does not work at the moment. Editing an assoc def would send an
    // update model with by-URI players while the server expects by-ID players.
    return state.object && (state.object.isType()    ? state.object.asType() :
                            state.object.isCompDef() ? state.object.asCompDef() :
                            state.object)
    // logical copy in createDetail()/updateDetail() (topicmap-model.js of dm5-cytoscape-renderer module)
  },

  showInmapDetails: state => !state.details.visible
}

const store = new Vuex.Store({
  state,
  actions,
  getters
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

function watchProp (comp, prop, getter) {
  // console.log('watchProp', prop)
  store.watch(
    getter,
    val => {
      // console.log(`"${prop}" changed`, val)
      // Note: the top-level webclient components follow the convention of mirroring its "props" through "data".
      // To avoid the "Avoid mutating a prop directly" Vue warning here we update the data, not the props.
      // The components name the data like the prop but with an underscore appended.
      comp[prop + '_'] = val
    }
  )
}
