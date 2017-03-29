<template>
  <div>
    <div>DeepaMehta 5</div>
    <div>
      <component v-for="comp in components" :is="comp" :key="comp._dm4_id"></component>
    </div>
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
