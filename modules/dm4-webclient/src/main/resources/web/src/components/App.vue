<template>
  <router-view></router-view>
  <!--
    Note: the Vue template compiler is not available as we use the Vue runtime.
    So we can't put <router-view> in index.html but must render the root component
    (= this one) via render function (see main.js).
  -->
</template>

<script>
export default {}
</script>

<style>
html {
  height: 100%;
}
body {
  height: 100%;
  margin: 0;
}
</style>
