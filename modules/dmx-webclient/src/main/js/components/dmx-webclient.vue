<template>
  <div class="dmx-webclient" :style="{userSelect}">
    <dmx-resizer @resizeStart="resizeStart" @resizeStop="resizeStop"></dmx-resizer>
  </div>
</template>

<script>
import dmx from 'dmx-api'
import axios from 'axios'

export default {

  mounted () {
    // Note: in order to allow external plugins to provide Webclient toplevel components -- in particular el-dialog
    // boxes -- component mounting must perform not before all plugins are loaded.
    // Another approach would be not to collect the toplevel components and then mounting all at once but to mount a
    // plugin's components immediately while plugin initialization. However this would result in unresolved circular
    // dependencies, e.g. the Webclient plugin depends on Search plugin's `registerExtraMenuItems` action while
    // the Search plugin on the other hand depends on Workspaces `isWritable` state.
    this.$store.state.pluginsReady.then(() => {
      this.mountComponents()
    })
  },

  provide: {
    dmx, axios
  },

  data () {
    return {
      isResizing: false
    }
  },

  computed: {
    userSelect() {
      return this.isResizing ? 'none' : ''
    }
  },

  methods: {

    resizeStart () {
      this.isResizing = true
    },

    resizeStop () {
      this.isResizing = false
    },

    /**
     * Instantiates and mounts Webclient toplevel components provided by plugins (as registered for mount point
     * "webclient").
     */
    mountComponents () {
      const state = this.$store.state
      state.compDefs.webclient.forEach(compDef => {
        // 1) init props
        // Note 1: props must be inited explicitly. E.g. the "detailRenderers" store state is populated while
        // loading plugins and does not change afterwards. The watcher (see step 3) would not fire as it is
        // registered *after* the plugins are loaded. ### TODO: still true?
        // TODO: think about startup order: instantiating the Webclient component vs. loading the plugins.
        // Note 2: props must be inited *before* the component is instantiated (see step 2). While instantiation
        // the component receives the declared "default" value (plugin.js), if no value is set already.
        // The default value must not be overridden by an undefined init value. ### TODO: still true?
        const propsData = {}
        for (const prop in compDef.props) {
          propsData[prop] = compDef.props[prop](state)    // call getter function
        }
        /*** 2) instantiate & mount ### TODO
        // Note: to manually mounted components the store must be passed explicitly resp. "parent" must be set.
        // https://forum.vuejs.org/t/this-store-undefined-in-manually-mounted-vue-component/8756
        const comp = new Vue({parent: this, propsData, ...compDef.comp}).$mount()
        this.$el.appendChild(comp.$el)
        // 3) make props reactive
        for (const prop in compDef.props) {
          this.watchProp(comp, prop, compDef.props[prop])
        }
        // 4) add event listeners
        for (const eventName in compDef.listeners) {
          comp.$on(eventName, compDef.listeners[eventName])
        } ***/
        // TODO: unregister listeners?
      })
    },

    watchProp (comp, prop, getter) {
      // console.log('watchProp', prop)
      this.$store.watch(
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
  },

  components: {
    'dmx-resizer': require('./dmx-resizer').default
  }
}
</script>

<style>
.dmx-webclient {
  height: 100%;
  display: flex;
}

.dmx-webclient .dmx-topicmap-panel {
  flex-grow: 1;
  overflow: hidden;     /* leave place for the detail panel */
  position: relative;
}

.dmx-webclient .dmx-detail-panel {
  flex-grow: 1;
  box-sizing: border-box;
  background-color: var(--background-color);
  border-left: 1px solid var(--border-color);
  box-shadow: 5px 0 8px;
}
</style>
