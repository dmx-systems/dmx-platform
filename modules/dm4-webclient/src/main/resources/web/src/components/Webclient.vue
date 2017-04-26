<template>
  <div class="webclient">
    <toolbar></toolbar>
    <div class="content">
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
.webclient {
  height: 100%;
  display: flex;
  flex-flow: column;
}

.webclient .content {
  display: flex;
  flex: auto;
}

.webclient .content .topicmap-panel {
  flex-basis: 70%;
}

.webclient .content .detail-panel {
  flex-basis: 30%;
}
</style>
