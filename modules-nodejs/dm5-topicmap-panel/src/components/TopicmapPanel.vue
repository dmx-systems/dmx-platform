<template>
  <div class="topicmap-panel">
    <component :is="renderer"></component>
  </div>
</template>

<script>
export default {

  computed: {
    renderer () {
      // ### TODO: renderer registry
      return require('./CytoscapeRenderer')
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
