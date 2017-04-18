<template>
  <div>
    <toolbar></toolbar>
    <topicmap-panel></topicmap-panel>
    <detail-panel></detail-panel>
  </div>
</template>

<script>
import pluginManager from '../plugin-manager'

export default {

  mounted () {
    pluginManager.loadPlugins()
  },

  components: {
    'toolbar':        require('./Toolbar.vue'),
    'topicmap-panel': require('./TopicmapPanel.vue'),
    'detail-panel':   require('./DetailPanel.vue')
  }
}
</script>
