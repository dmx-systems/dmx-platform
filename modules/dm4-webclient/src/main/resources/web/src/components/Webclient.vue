<template>
  <q-layout>
    <toolbar slot="header"></toolbar>
    <topicmap-panel class="width-3of4"></topicmap-panel>
    <detail-panel class="width-1of4"></detail-panel>
  </q-layout>
</template>

<script>
import pluginManager from '../plugin-manager'

export default {

  mounted () {
    pluginManager.loadPlugins()
  },

  components: {
    'toolbar': require('./Toolbar.vue'),
    'topicmap-panel': require('./TopicmapPanel.vue'),
    'detail-panel': require('./DetailPanel.vue')
  }
}
</script>
