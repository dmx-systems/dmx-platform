<template>
  <div class="dmx-webclient" :style="{userSelect}">
    <dmx-resizer @resizeStart="resizeStart" @resizeStop="resizeStop"></dmx-resizer>
    <component v-for="compDef in compDefs" :is="compDef.comp" v-bind="props(compDef)" v-on="listeners(compDef)">
    </component>
  </div>
</template>

<script>
import dmx from 'dmx-api'
import axios from 'axios'

export default {

  provide: {
    dmx, axios
  },

  data () {
    return {
      isResizing: false
    }
  },

  computed: {

    compDefs () {
      return this.$store.state.compDefs.webclient
    },

    userSelect () {
      return this.isResizing ? 'none' : ''
    }
  },

  methods: {

    // ### TODO
    props (compDef) {
      const t = typeof compDef.props
      // console.log('# props', compDef, t)
      if (t === 'function') {
        return compDef.props()
      } else if (t === 'undefined') {
        return {}
      }
      throw Error(`Unexpected compDef props: ${t}`)
    },

    // ### TODO
    listeners (compDef) {
      const t = typeof compDef.listeners
      // console.log('# listeners', compDef, t)
      if (t === 'object') {
        return compDef.listeners
      } else if (t === 'undefined') {
        return {}
      }
      throw Error(`Unexpected compDef listeners: ${t}`)
    },

    resizeStart () {
      this.isResizing = true
    },

    resizeStop () {
      this.isResizing = false
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
