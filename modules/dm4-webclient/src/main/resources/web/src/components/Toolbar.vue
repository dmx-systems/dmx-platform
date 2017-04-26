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
  align-items: baseline;
  background-color: #e8e8e8;
}
</style>
