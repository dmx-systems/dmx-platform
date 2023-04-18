import Vue from 'vue'
import Vuex from 'vuex'
import {MessageBox} from 'element-ui'
import dmx from 'dmx-api'

const RATIO = .7            // initial ratio of left/right panel width; must correspond with CSS variable
                            // --detail-panel-trans-x in dmx-detail-panel.vue

Vue.use(Vuex)

window.addEventListener('resize', e => {
  // read state from view
  const pos = store.state.details.visible ? document.querySelector('.dmx-topicmap-panel').clientWidth :
                                            RATIO * window.innerWidth
  store.dispatch('setResizerPos', pos)
})

const state = {

  object: undefined,        // If there is a single-selection: the selected Topic/Assoc/TopicType/AssocType.
                            // This object is displayed in detail panel or as in-map details. Its ID appears in the
                            // browser URL.
                            // Undefined if there is no selection or a multi-selection.

  writable: undefined,      // True if the current user has WRITE permission for the selected object.

  compDefs: {},             // Registered webclient components:
                            // {
                            //    mount: [compDef]
                            // }

  resizerPos: RATIO * window.innerWidth,    // x coordinate in pixel (number)

  detailRenderers: {        // Registered detail renderers; comprises object renderers and value renderers:
    object: {},             // {
    value: {}               //   typeUri: component
  },                        // }

  iconRenderers: {},        // Registered icon renderers:
                            // {
                            //   typeUri: function (topic) => icon
                            // }

  contextCommands: {
    topic: [],
    topic_danger: [],
    assoc: [],
    assoc_danger: []
  },

  doubleClickHandlers: {},  // Max one handler per type:
                            // {
                            //   typeUri: function
                            // }

  dropHandler: [],          // Array of drop handler objects:
                            // {
                            //   isDroppable (topic1, topic2) predicate returns boolean, called often while drag
                            //   handleDrop (topic1, topic2) handler for "topic 1 is dropped onto topic 2"
                            // }

  detailPanelButtons: {},   // Registered extra buttons being displayed in the detail panel:
                            // {
                            //   typeUri: [
                            //     {
                            //       label: button label (string)
                            //       handler: function
                            //     }
                            //   ]
                            // }

  quillConfig: {
    options: {
      theme: 'bubble',
      modules: {
        toolbar: {
          container: [
            ['bold', 'italic', 'code'],
            ['blockquote', 'code-block'],
            [{list: 'ordered'}, {list: 'bullet'}],
            [{header: [1, 2, 3, false]}],
            ['topic-link', 'link', 'image', 'video']
          ]
        }
      }
    },
    // TODO: allow DMX plugins to provide Quill extensions
    extensions: [
      require('../quill-extensions/topic-link').default,
      require('../quill-extensions/video').default
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
    object.update().then(response => {
      dispatch('_processDirectives', response.directives)
    })
  },

  deleteMulti ({dispatch}, idLists) {
    confirmDeletion(idLists).then(() => {
      // console.log('deleteMulti', idLists.topicIds, idLists.assocIds)
      // update client state + sync view (for immediate visual feedback)
      idLists.topicIds.forEach(id => dispatch('_deleteTopic', id))
      idLists.assocIds.forEach(id => dispatch('_deleteAssoc', id))
      // update server state
      dmx.rpc.deleteMulti(idLists).then(response => {
        dispatch('_processDirectives', response.directives)
      })
    }).catch(() => {})    // suppress unhandled rejection on cancel
  },

  // ---

  unselectIf ({dispatch}, id) {
    // console.log('unselectIf', id, isSelected(id))
    if (isSelected(id)) {
      dispatch('stripSelectionFromRoute')
    }
  },

  // ---

  registerComponent (_, compDef) {
    const compDefs = state.compDefs[compDef.mount] || (state.compDefs[compDef.mount] = [])
    compDefs.push(compDef)
  },

  /**
   * @param   renderer    "object" or "value"
   */
  registerDetailRenderer (_, {renderer, typeUri, component}) {
    state.detailRenderers[renderer][typeUri] = component
  },

  registerIconRenderer (_, {typeUri, iconFunc}) {
    state.iconRenderers[typeUri] = iconFunc
  },

  registerContextCommands (_, commands) {
    ['topic', 'topic_danger', 'assoc', 'assoc_danger'].forEach(prop => {
      if (commands[prop]) {
        state.contextCommands[prop] = state.contextCommands[prop].concat(commands[prop])
      }
    })
  },

  registerDoubleClickHandlers (_, handlers) {
    Object.entries(handlers).forEach(([typeUri, handler]) => {
      const _handler = state.doubleClickHandlers[typeUri]
      if (_handler) {
        throw Error(`For type "${typeUri}" a double click handler is already registered`)
      }
      state.doubleClickHandlers[typeUri] = handler
    })
  },

  registerDropHandler (_, handler) {
    state.dropHandler.push(handler)
  },

  registerDetailPanelButtons (_, {typeUri, buttons}) {
    state.detailPanelButtons[typeUri] || (state.detailPanelButtons[typeUri] = [])
    state.detailPanelButtons[typeUri] = state.detailPanelButtons[typeUri].concat(buttons)
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
      for (const prop in compDef.props) {
        propsData[prop] = compDef.props[prop](state)    // call getter function
      }
      // 2) instantiate & mount
      // Note: to manually mounted components the store must be passed explicitly resp. "parent" must be set.
      // https://forum.vuejs.org/t/this-store-undefined-in-manually-mounted-vue-component/8756
      const comp = new Vue({parent, propsData, ...compDef.comp}).$mount()
      parent.$el.appendChild(comp.$el)
      // 3) make props reactive
      for (const prop in compDef.props) {
        watchProp(comp, prop, compDef.props[prop])
      }
      // 4) add event listeners
      for (const eventName in compDef.listeners) {
        comp.$on(eventName, compDef.listeners[eventName])
      }
      // TODO: unregister listeners?
    })
  },

  // Resizer

  setResizerPos (_, pos) {
    state.resizerPos = pos
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
    // console.log(`Webclient: processing ${directives.length} directives`, directives)
    directives.forEach(dir => {
      switch (dir.type) {
      case 'UPDATE_TOPIC':
        displayObjectIf(new dmx.Topic(dir.arg))
        break
      case 'DELETE_TOPIC':
        dispatch('unselectIf', dir.arg.id)
        break
      case 'UPDATE_ASSOC':
        displayObjectIf(new dmx.Assoc(dir.arg))
        break
      case 'DELETE_ASSOC':
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
    // ### FIXME: the asCompDef() approach does not work at the moment. Editing an comp def would send an
    // update model with by-URI players while the server expects by-ID players.
    return state.object && (state.object.isType     ? state.object.asType() :
                            state.object.isCompDef  ? state.object.asCompDef() :
                            state.object.isRoleType ? state.object.asRoleType() :
                            state.object)                                                     /* eslint indent: "off" */
    // logical copy in createDetail()/updateDetail() (topicmap-model.js of dmx-cytoscape-renderer module)
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

function confirmDeletion (idLists) {
  const _size = size(idLists)
  if (!_size) {
    throw Error('confirmDeletion() called with empty idLists')
  }
  let message, buttonText
  if (_size > 1) {
    message = "You're about to delete multiple items!"
    buttonText = `Delete ${_size} items`
  } else {
    message = `You're about to delete a ${viewObject(idLists).typeName}!`
    buttonText = 'Delete'
  }
  return MessageBox.confirm(message, 'Warning', {
    type: 'warning',
    confirmButtonText: buttonText,
    confirmButtonClass: 'el-button--danger',
    showClose: false
  })
}

// copy in cytoscape-view.js (module dmx-cytoscape-renderer)
// TODO: unify selection models (see selection.js in dmx-topicmaps module)
function size (idLists) {
  return idLists.topicIds.length + idLists.assocIds.length
}

function viewObject (idLists) {
  const id = idLists.topicIds.length ? idLists.topicIds[0] : idLists.assocIds[0]
  return state.topicmaps.topicmap.getObject(id)
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
