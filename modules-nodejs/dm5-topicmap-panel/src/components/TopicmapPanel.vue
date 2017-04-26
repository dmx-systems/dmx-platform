<template>
  <div class="topicmap-panel">
    <component :is="renderer"></component>
  </div>
</template>

<script>
export default {
  computed: {
    renderer () {
      return this.$store.state.topicmapPanel.renderer
    }
  }
}
</script>

<style>
.topicmap-panel {
  display: flex;
  overflow: hidden;
}
</style>
