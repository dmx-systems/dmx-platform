<template>
  <div class="topicmap-panel">
    <slot name="dm5-topicmap-panel"></slot>
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
  position: relative;
  display: flex;
  overflow: hidden;
}
</style>
