<template>
  <el-button type="primary" link class="dmx-filebrowser-reveal fa fa-folder" title="Reveal File Browser"
    @click="reveal">
  </el-button>
</template>

<script>
export default {
  methods: {
    reveal () {
      this.$store.dispatch('revealFileBrowser')
    }
  }
}
</script>

<style>
.dmx-filebrowser-reveal {
  padding-left:  2px !important;
  padding-right: 2px !important;
  margin-right: 18px !important;
}
</style>
