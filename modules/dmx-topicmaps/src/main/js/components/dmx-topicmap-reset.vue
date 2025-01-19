<template>
  <el-button type="primary" link class="fa fa-compress" title="Reset Zoom and Pan" @click="resetTopicmapViewport">
  </el-button>
</template>

<script>
export default {
  methods: {
    resetTopicmapViewport () {
      this.$store.dispatch('resetTopicmapViewport')
    }
  }
}
</script>
