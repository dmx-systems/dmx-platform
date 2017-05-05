<template>
  <div class="toolbar">
    <component v-for="comp in components" :is="comp" :key="comp._dm5_id"></component>
  </div>
</template>

<script>
export default {
  computed: {
    components () {
      return this.$store.state.componentRegistry.components['dm5.webclient.toolbar']
    }
  }
}
</script>

<style>
.toolbar {
  display: flex;
  flex: none;
  position: absolute;
  width: 100%;
  z-index: 10;    /* Cytoscape canvases are at 1, 2, 3 */
  background-color: rgba(0, 0, 0, 0);
}
</style>
