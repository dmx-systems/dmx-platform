<template>
  <div class="detail-panel">
    <div v-if="object">
      <h3>{{object.value}}</h3>
      <field-renderer :object="object" :mode="mode"></field-renderer>
      <!-- Button -->
      <el-button size="small" @click="buttonAction">{{buttonLabel}}</el-button>
    </div>
  </div>
</template>

<script>
export default {

  props: [
    'object',   // the Topic/Association to display; if undefined an empty detail panel is rendered
    'mode'      // 'info' or 'form'
  ],

  computed: {
    buttonLabel () {
      return this.infoMode ? 'Edit' : 'OK'
    }
  },

  methods: {
    buttonAction () {
      var action = this.infoMode ? 'edit' : 'submit'
      this.$store.dispatch(action)
    }
  },

  mixins: [
    require('./mixins/infoMode').default
  ],

  components: {
    'field-renderer': require('./FieldRenderer')
  }
}
</script>

<style>
.detail-panel {
  overflow: auto;
}
</style>
