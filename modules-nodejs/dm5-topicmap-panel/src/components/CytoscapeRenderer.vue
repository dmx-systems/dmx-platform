<template>
  <div id="cytoscape-renderer"></div>
</template>

<script>
export default {
  mounted () {
    this.$store.registerModule('cytoscapeRenderer', require('../cytoscape-renderer').default)
  }
}
</script>

<style>
#cytoscape-renderer {
  flex: auto;
  overflow: hidden;
}
</style>
