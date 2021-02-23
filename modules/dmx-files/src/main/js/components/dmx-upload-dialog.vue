<template>
  <el-dialog custom-class="dmx-upload-dialog" :visible="visible" title="Upload" :modal="false" v-draggable
      @opened="opened" @close="close">
  </el-dialog>
</template>

<script>
export default {

  created () {
    // console.log('dmx-upload-dialog created')
  },

  mounted () {
    // console.log('dmx-upload-dialog mounted')
  },

  computed: {
    visible () {
      return this.$store.state.files.visible
    }
  },

  methods: {

    opened () {
    },

    close () {
      this.$store.dispatch('closeUploadDialog')
    }
  }
}
</script>

<style>
.dmx-upload-dialog .field + .field {
  margin-top: var(--field-spacing);
}
</style>
