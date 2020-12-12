<template>
  <div class="dm5-webclient">
    <div v-for="compDef in compDefs" :id="mountId(compDef)" :key="compDef.id"></div>
  </div>
</template>

<script>
import dmx from 'dmx-api'
import axios from 'axios'
import Vue from 'vue'

Vue.component('dmx-object-renderer', require('dmx-object-renderer').default)
Vue.component('dmx-assoc',           require('dmx-object-renderer/src/components/dm5-assoc').default)
Vue.component('dmx-boolean-field',   require('dmx-object-renderer/src/components/dm5-boolean-field').default)
Vue.component('dmx-child-topic',     require('dmx-object-renderer/src/components/dm5-child-topic').default)
Vue.component('dmx-child-topics',    require('dmx-object-renderer/src/components/dm5-child-topics').default)
Vue.component('dmx-html-field',      require('dmx-object-renderer/src/components/dm5-html-field').default)
Vue.component('dmx-number-field',    require('dmx-object-renderer/src/components/dm5-number-field').default)
Vue.component('dmx-player',          require('dmx-object-renderer/src/components/dm5-player').default)
Vue.component('dmx-select-field',    require('dmx-object-renderer/src/components/dm5-select-field').default)
Vue.component('dmx-text-field',      require('dmx-object-renderer/src/components/dm5-text-field').default)
Vue.component('dmx-value-renderer',  require('dmx-object-renderer/src/components/dm5-value-renderer').default)

export default {

  provide: {
    dmx, axios, Vue
  },

  computed: {
    compDefs () {
      return this.$store.state.compDefs.webclient
    }
  },

  methods: {
    mountId (compDef) {
      return `mount-${compDef.id}`
    }
  }
}
</script>

<style>
.dm5-webclient {
  height: 100%;
  display: flex;
}

.dm5-webclient .dm5-topicmap-panel {
  flex-grow: 1;
  flex-basis: 70%;
  overflow: hidden;     /* leave place for the detail panel */
  position: relative;
}

.dm5-webclient .dm5-detail-panel {
  flex-grow: 1;
  flex-basis: 30%;
  box-sizing: border-box;
  background-color: var(--background-color);
  border-left: 1px solid var(--border-color);
  box-shadow: 5px 0 8px;
}
</style>
