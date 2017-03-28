<template>
  <!-- Don't drop "q-app" class -->
  <div id="q-app">
    <router-view></router-view>
  </div>
  <!--
    Note: the Vue template compiler is not available as we use the Vue runtime.
    So we can't put <router-view> in index.html but must render the root component
    (= this one) via render function (see main.js).
  -->
</template>

<script>
/*
 * Root component
 */
export default {}
</script>

<style></style>
