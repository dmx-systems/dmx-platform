<template>
  <div id="detail-panel">
    <h3 v-if="object">{{object.value}}</h3>
  </div>
</template>

<script>
export default {
  computed: {
    object () {
      return this.$store.state.detailPanel.object
    }
  }
}
</script>

<style>
#detail-panel {
  flex-basis: 30%;    /* ### TODO: make independent from parent */
  background-color: #eff;
}
</style>
