<template>
  <div id="app">
    <router-view></router-view>
  </div>
  <!--
    Note: the Vue template compiler is not available as we use the Vue runtime.
    So we can't put <router-view> in index.html but must render the root component
    (= this one) via render function (see main.js).
  -->
</template>

<script>
export default {}
</script>

<style></style>
