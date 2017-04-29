<template>
  <div v-if="infoMode">{{value}}</div>
  <div v-else>
    <input :value="value" @input="updateValue($event)" type="number">
  </div>
</template>

<script>
export default {
  props: ['value', 'mode'],

  methods: {
    updateValue (event) {
      this.$emit("input", event.target.value)
    }
  },

  mixins: [
    require('./mixins/infoMode').default
  ]
}
</script>
