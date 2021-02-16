<template>
    <pre>{{object.value}}</pre>
</template>

<script>
export default {
  mixins: [
    require('./mixins/object').default
  ]
}
</script>
