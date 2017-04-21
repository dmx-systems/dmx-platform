<template>
  <div id="webclient">
    <toolbar></toolbar>
    <div id="content">
      <topicmap-panel></topicmap-panel>
      <detail-panel></detail-panel>
    </div>
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

<style>
#webclient {
  height: 100%;
  display: flex;
  flex-flow: column;
}

#webclient #content {
  display: flex;
  flex: auto;
}
</style>
