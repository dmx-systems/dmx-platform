<template>
  <div :title="title">{{size}}</div>
</template>

<script>
export default {

  inject: ["dmx"],

  mixins: [
    require('./mixins/object').default
  ],

  computed: {

    size () {
      return this.dmx.utils.formatFileSize(this.object.value)
    },

    title () {
      return `${this.object.value} bytes`
    }
  }
}
</script>
