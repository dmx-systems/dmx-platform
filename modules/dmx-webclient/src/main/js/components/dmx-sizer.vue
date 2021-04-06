<template>
  <div class="dmx-sizer" :style="{left}" v-if="visible">
  </div>
</template>

<script>
export default {
  computed: {

    visible () {
      return this.$store.state.details && this.$store.state.details.visible
    },

    left () {
      return this.$store.state.topicmapPanelWidth + 'px'
    }
  }
}
</script>

<style>
.dmx-sizer {
  position: absolute;
  width: 5px;
  height: 100%;
  background-color: var(--background-color-darker);
  cursor: col-resize
}
</style>
