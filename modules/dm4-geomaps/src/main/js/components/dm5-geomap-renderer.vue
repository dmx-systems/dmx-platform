<template>
  <div>
    <br><br>
    <h1>Geomap Renderer!</h1>
  </div>
</template>

<script>
export default {

  mounted () {
    console.log('dm5-geomap-renderer mounted')
    // TODO: allow different renderers for the same topicmap type.
    // At the moment we have a 1 to 1 relationship, so a renderer simply identifies themselves by topicmap type.
    this.$emit('renderer-mounted', 'dm4.geomaps.geomap_renderer')
  },

  destroyed () {
    console.log('dm5-geomap-renderer destroyed')
  },
}
</script>
