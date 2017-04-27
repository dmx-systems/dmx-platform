<template>
  <div class="detail-panel">
    <div v-if="object">
      <h3>{{object.value}}</h3>
      <field-renderer :object="object" :mode="mode"></field-renderer>
    </div>
  </div>
</template>

<script>
export default {

  props: [
    'object',   // the Topic/Association to display; if undefined an empty detail panel is rendered
    'mode'      // 'info' or 'form'
  ],

  components: {
    'field-renderer': require('./FieldRenderer')
  }
}
</script>

<style>
.detail-panel {
  background-color: #f4f4f4;
}
</style>
