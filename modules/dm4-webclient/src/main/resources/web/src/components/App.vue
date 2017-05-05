<template>
  <router-view></router-view>
  <!--
    Note: the Vue template compiler is not available as we use the Vue runtime.
    So we can't put <router-view> in index.html but must render the root component
    (= this one) via render function (see main.js).
  -->
</template>

<style>
html {
  height: 100%;
}

body {
  height: 100%;
  margin: 0;
  overflow: hidden;   /* avoid window bounce when scrolling reaches top/bottom */
  font-family: "Avenir", Helvetica, Arial, sans-serif;
}

input {
  font-family: "Avenir", Helvetica, Arial, sans-serif;
  font-size: 16px !important;   /* fixed absolute size for all el-input sizes */
}

button {
  font-family: "Avenir", Helvetica, Arial, sans-serif;
  font-size: 14px !important;   /* fixed absolute size for all el-button sizes */
}
</style>
