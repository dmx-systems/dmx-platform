<template>
  <q-layout>
    <toolbar slot="header"></toolbar>
    <topicmap-panel class="width-3of4"></topicmap-panel>
    <detail-panel class="width-1of4"></detail-panel>
  </q-layout>
</template>

<script>
import Vue from 'vue'
import store from '../store'

export default {

  mounted () {
    console.log('DOM ready!')
    this.initPlugins()
    console.log('dispatch "init"')
    this.$store.dispatch('init')
  },

  methods: {
    initPlugins () {
      // ### TODO: load plugins dynamically
      require('../../../../../../../dm4-workspaces/src/main/resources/web/src/main.js').default.init({Vue, store})
      require('../../../../../../../dm4-topicmaps/src/main/resources/web/src/main.js').default.init({Vue, store})
    }
  },

  components: {
    'toolbar': require('./Toolbar.vue'),
    'topicmap-panel': require('./TopicmapPanel.vue'),
    'detail-panel': require('./DetailPanel.vue')
  }
}
</script>
