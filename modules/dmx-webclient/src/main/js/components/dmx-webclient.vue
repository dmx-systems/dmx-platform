<template>
  <div class="dmx-webclient" :style="{userSelect}">
    <dmx-resizer @resizeStart="resizeStart" @resizeStop="resizeStop"></dmx-resizer>
  </div>
</template>

<script>
import dmx from 'dmx-api'
import axios from 'axios'
import Vue from 'vue'

// Global component registrations
// Allow plugins to reuse Webclient components (instead of rebundle the component along with the plugin)

Vue.component('dmx-object-renderer', require('dmx-object-renderer').default)
Vue.component('dmx-assoc',           require('dmx-object-renderer/src/components/dmx-assoc').default)
Vue.component('dmx-boolean-field',   require('dmx-object-renderer/src/components/dmx-boolean-field').default)
Vue.component('dmx-child-topic',     require('dmx-object-renderer/src/components/dmx-child-topic').default)
Vue.component('dmx-child-topics',    require('dmx-object-renderer/src/components/dmx-child-topics').default)
Vue.component('dmx-html-field',      require('dmx-object-renderer/src/components/dmx-html-field').default)
Vue.component('dmx-number-field',    require('dmx-object-renderer/src/components/dmx-number-field').default)
Vue.component('dmx-player',          require('dmx-object-renderer/src/components/dmx-player').default)
Vue.component('dmx-select-field',    require('dmx-object-renderer/src/components/dmx-select-field').default)
Vue.component('dmx-text-field',      require('dmx-object-renderer/src/components/dmx-text-field').default)
Vue.component('dmx-value-renderer',  require('dmx-object-renderer/src/components/dmx-value-renderer').default)

Vue.component('dmx-topic-list', require('dmx-topic-list').default)    // Required e.g. by dmx-geomaps

export default {

  provide: {
    dmx, axios, Vue
  },

  data() {
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
  width: 70%;
  overflow: hidden;     /* leave place for the detail panel */
  position: relative;
}

.dmx-webclient .dmx-detail-panel {
  flex-grow: 1;
  width: 30%;
  box-sizing: border-box;
  background-color: var(--background-color);
  border-left: 1px solid var(--border-color);
  box-shadow: 5px 0 8px;
}
</style>
