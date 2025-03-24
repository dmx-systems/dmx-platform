<template>
  <el-button type="primary" link class="fa fa-arrows-alt" title="Zoom to Fit" @click="fitTopicmapViewport"></el-button>
</template>

<script>
export default {
  methods: {
    fitTopicmapViewport () {
      this.$store.dispatch('fitTopicmapViewport')
    }
  }
}
</script>
