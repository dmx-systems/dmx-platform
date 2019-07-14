<template>
  <el-dialog custom-class="dm5-help-dialog" :visible="visible" title="Help" @close="close">
  </el-dialog>
</template>

<script>
export default {

  computed: {
    visible () {
      return this.$store.state.helpmenu.dialogVisibility
    }
  },

  methods: {
    close () {
      this.$store.dispatch('closeHelpDialog')
    }
  }
}
</script>

<style>
</style>
