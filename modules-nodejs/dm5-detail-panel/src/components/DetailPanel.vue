<template>
  <div class="detail-panel">
    <div v-if="object">
      <h3>{{object.value}}</h3>
      <field-renderer :object="object" :type="type"></field-renderer>
    </div>
  </div>
</template>

<script>
export default {

  props: ['object'],    // the Topic/Association to display, may be undefined

  computed: {
    type () {
      return this.object.getType()
    }
  },

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
