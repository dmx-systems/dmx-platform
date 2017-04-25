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
import TopicmapPanel from 'modules-nodejs/dm5-topicmap-panel/src/main.js'
import DetailPanel   from 'modules-nodejs/dm5-detail-panel/src/main.js'
import pluginManager from '../plugin-manager'

export default {
  mounted () {
    pluginManager.loadPlugins()
  },
  components: {
    'toolbar':        require('./Toolbar.vue'),
    'topicmap-panel': TopicmapPanel.component,
    'detail-panel':   DetailPanel.component
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
