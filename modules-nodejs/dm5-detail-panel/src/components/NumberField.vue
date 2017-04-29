<template>
  <div v-if="mode=='info'">
    {{value}}
  </div>
  <div v-else-if="mode=='form'">
    <input :value="value" @input="updateValue($event)" type="number">
  </div>
</template>

<script>
export default {
  props: ['value', 'mode'],

  methods: {
    updateValue (event) {
      console.log('input change!')
      this.$emit("input", event.target.value)
    }
  }
}
</script>
