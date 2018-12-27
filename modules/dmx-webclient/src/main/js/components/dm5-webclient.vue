<template>
  <div class="dm5-webclient">
    <div v-for="compDef in compDefs" :id="mountId(compDef)" :key="compDef.id"></div>
  </div>
</template>

<script>
import dm5 from 'dm5'
import axios from 'axios'
import Vue from 'vue'

Vue.component('dm5-assoc',           require('dm5-object-renderer/src/components/dm5-assoc').default)
Vue.component('dm5-boolean-field',   require('dm5-object-renderer/src/components/dm5-boolean-field').default)
Vue.component('dm5-child-topic',     require('dm5-object-renderer/src/components/dm5-child-topic').default)
Vue.component('dm5-child-topics',    require('dm5-object-renderer/src/components/dm5-child-topics').default)
Vue.component('dm5-html-field',      require('dm5-object-renderer/src/components/dm5-html-field').default)
Vue.component('dm5-number-field',    require('dm5-object-renderer/src/components/dm5-number-field').default)
Vue.component('dm5-object-renderer', require('dm5-object-renderer/src/components/dm5-object-renderer').default)
Vue.component('dm5-player',          require('dm5-object-renderer/src/components/dm5-player').default)
Vue.component('dm5-select-field',    require('dm5-object-renderer/src/components/dm5-select-field').default)
Vue.component('dm5-spinner',         require('dm5-object-renderer/src/components/dm5-spinner').default)
Vue.component('dm5-text-field',      require('dm5-object-renderer/src/components/dm5-text-field').default)
Vue.component('dm5-value-renderer',  require('dm5-object-renderer/src/components/dm5-value-renderer').default)

export default {

  provide: {
    dm5, axios, Vue
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
  overflow: auto;
  box-sizing: border-box;
  padding: 0 12px 12px 12px;
  background-color: var(--background-color);
  border-left: 1px solid var(--border-color);
}
</style>
