<template>
  <div v-if="mode=='info'">
    {{object.value}}
  </div>
  <div v-else-if="mode=='form'">
    <input v-model="object.value">
  </div>
</template>

<script>
export default {
  props: ['object', 'mode']
}
</script>
