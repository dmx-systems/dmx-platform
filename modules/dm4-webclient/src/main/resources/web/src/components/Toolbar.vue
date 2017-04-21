<template>
  <div id="toolbar">
    <div>DeepaMehta 5</div>
    <component v-for="comp in components" :is="comp" :key="comp._dm4_id"></component>
  </div>
</template>

<script>
export default {
  computed: {
    components () {
      return this.$store.state.toolbar.components
    }
  }
}
</script>

<style>
#toolbar {
  display: flex;
  align-items: baseline;
  background-color: #e8e8e8;
}
</style>
