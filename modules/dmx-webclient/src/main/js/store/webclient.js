import { markRaw } from 'vue'
import { createStore } from 'vuex'
import dmx from 'dmx-api'

const RATIO = .7            // initial ratio of left/right panel width; must correspond with CSS variable
                            // --detail-panel-trans-x in dmx-detail-panel.vue

window.addEventListener('resize', e => {
  // read state from view
  const pos = store.state.details.visible ? document.querySelector('.dmx-topicmap-panel').clientWidth :
                                            RATIO * window.innerWidth
  store.dispatch('setResizerPos', pos)
})

const store = createStore({

  state: {

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
    },

    pluginsReady: undefined   // a promise resolved once all plugins are loaded and initialized       // ### TODO: drop?
  },

  actions: {

    displayObject ({state}, object) {
      // console.log('displayObject', object)
      state.object = object
      _initWritable(state)
    },

    emptyDisplay ({state}) {
      // console.log('emptyDisplay')
      state.object = undefined
    },

    submit ({dispatch}, object) {
      object.update().then(response => {
        dispatch('_processDirectives', response.directives)
      })
    },

    // ---

    registerComponent ({state}, compDef) {
      const compDefs = state.compDefs[compDef.mount] || (state.compDefs[compDef.mount] = [])
      compDefs.push(markRaw(compDef))       // Vue component internals are not made reactive (compDef.comp)
    },

    /**
     * @param   renderer    "object" or "value"
     */
    registerDetailRenderer ({state}, {renderer, typeUri, component}) {
      state.detailRenderers[renderer][typeUri] = markRaw(component)     // Vue component internals are not made reactive
    },

    registerIconRenderer ({state}, {typeUri, iconFunc}) {
      state.iconRenderers[typeUri] = iconFunc
    },

    registerContextCommands ({state}, commands) {
      ['topic', 'topic_danger', 'assoc', 'assoc_danger'].forEach(prop => {
        if (commands[prop]) {
          state.contextCommands[prop] = state.contextCommands[prop].concat(commands[prop])
        }
      })
    },

    registerDoubleClickHandlers ({state}, handlers) {
      Object.entries(handlers).forEach(([typeUri, handler]) => {
        const _handler = state.doubleClickHandlers[typeUri]
        if (_handler) {
          throw Error(`For type "${typeUri}" a double click handler is already registered`)
        }
        state.doubleClickHandlers[typeUri] = handler
      })
    },

    registerDropHandler ({state}, handler) {
      state.dropHandler.push(handler)
    },

    registerDetailPanelButtons ({state}, {typeUri, buttons}) {
      state.detailPanelButtons[typeUri] || (state.detailPanelButtons[typeUri] = [])
      state.detailPanelButtons[typeUri] = state.detailPanelButtons[typeUri].concat(buttons)
    },

    // Split Panel

    setResizerPos ({state}, pos) {
      state.resizerPos = pos
    },

    /**
     * Adapts left/right panel's width according to model ("resizerPos").
     * Dispatched when Detail Panel becomes visible.
     * Precondition: Detail Panel is in DOM already.
     */
    adaptPanelsWidth ({state}) {
      const container  = document.querySelector('.dmx-webclient')
      const leftPanel  = document.querySelector('.dmx-topicmap-panel')
      const rightPanel = document.querySelector('.dmx-detail-panel')
      const leftPanelWidth = state.resizerPos
      const rightPanelWidth = container.clientWidth - leftPanelWidth
      leftPanel.style.width  = `${leftPanelWidth}px`
      rightPanel.style.width = `${rightPanelWidth}px`
    },

    //

    loggedIn ({state}) {
      initWritable(state)
    },

    loggedOut ({state}) {
      initWritable(state)
    },

    // WebSocket messages

    _processDirectives ({state, dispatch}, directives) {
      // console.log(`Webclient: processing ${directives.length} directives`, directives)
      directives.forEach(dir => {
        switch (dir.type) {
        case 'UPDATE_TOPIC':
          displayObjectIf(state, new dmx.Topic(dir.arg))
          break
        case 'UPDATE_ASSOC':
          displayObjectIf(state, new dmx.Assoc(dir.arg))
          break
        }
      })
    }
  },

  getters: {

    // Recalculate "object" once the underlying type changes.
    // The detail panel updates when a type is renamed.
    object: state => {
      // console.log('object getter', state.object, state.object && state.typeCache.topicTypes[state.object.uri])
      // ### FIXME: the asCompDef() approach does not work at the moment. Editing an comp def would send an
      // update model with by-URI players while the server expects by-ID players.
      return state.object && (state.object.isType     ? state.object.asType() :
                              state.object.isCompDef  ? state.object.asCompDef() :
                              state.object.isRoleType ? state.object.asRoleType() :
                              state.object)                                                   /* eslint indent: "off" */
      // logical copy in createDetail()/updateDetail() (topicmap-model.js of dmx-cytoscape-renderer module)
    },

    showInmapDetails: state => !state.details.visible
  }
})

export default store

//

function displayObjectIf (state, object) {
  if (isSelected(state, object.id)) {
    store.dispatch('displayObject', object)
  }
}

/**
 * @return  true if the given object ID represents the current single-selection, if there is one, falsish otherwise
 */
function isSelected (state, id) {
  return state.object?.id === id
}

//

function initWritable (state) {
   state.object && _initWritable(state)
}

function _initWritable (state) {
  state.object.isWritable().then(writable => {
    state.writable = writable
  })
}
