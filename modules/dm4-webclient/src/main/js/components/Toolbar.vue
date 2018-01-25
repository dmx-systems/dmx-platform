<template>
  <div class="dm5-toolbar">
    <component v-for="comp in components" :is="comp.comp" :key="comp.id"></component>
  </div>
</template>

<script>
export default {
  computed: {
    components () {
      return this.$store.state.components.toolbar
    }
  }
}
</script>

<style>
.dm5-toolbar {
  display: flex;
  flex: none;
  position: absolute;
  width: 100%;
  z-index: 10;    /* Cytoscape canvases are at 1, 2, 3 */
  background-color: rgba(0, 0, 0, 0);
}
</style>
