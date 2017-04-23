<template>
  <div id="topicmap-panel">
    <cytoscape-renderer></cytoscape-renderer>
  </div>
</template>

<script>
export default {
  components: {
    'cytoscape-renderer': require('../../../../../../../dm4-topicmaps/src/main/resources/web/src/components/CytoscapeRenderer.vue')
  }
}
</script>

<style>
#topicmap-panel {
  flex-basis: 70%;
  background-color: #ffe;
}
</style>
