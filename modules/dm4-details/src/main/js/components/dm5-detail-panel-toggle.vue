<template>
  <el-button :class="['dm5-detail-panel-toggle', 'fa', icon]" :disabled="!object" type="text" @click="toggle">
  </el-button>
</template>

<script>
  export default {

    computed: {

      object () {
        return this.$store.state.object
      },

      visible () {
        return this.$store.state.details.visible
      },

      icon () {
        return this.visible ? 'fa-caret-square-o-right' : 'fa-caret-square-o-left'
      }
    },

    methods: {
      toggle () {
        this.$store.dispatch('toggleDetailPanelVisibility')
      }
    }
  }
</script>

<style>
.dm5-detail-panel-toggle {
  font-size: 18px !important;
}
</style>
