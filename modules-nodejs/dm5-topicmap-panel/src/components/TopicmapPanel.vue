<template>
  <div class="topicmap-panel">
    <component :is="renderer" :topicmap="topicmap"></component>
  </div>
</template>

<script>
export default {

  props: ['topicmap'],

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
