<template>
  <iframe class="dmx-download-iframe" :src="url"></iframe>
</template>

<script>
export default {
  computed: {
    url () {
      return this.$store.state.files.url
    }
  }
}
</script>

<style>
.dmx-download-iframe {
  display: none;
}
</style>
