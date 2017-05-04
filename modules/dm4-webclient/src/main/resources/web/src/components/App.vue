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

input, button {
  font-family: "Avenir", Helvetica, Arial, sans-serif;
  font-size: 16px !important;   /* fixed absolute size for all element-ui size */
}

.el-select-dropdown__item {
  font-size: 1em;
}
</style>
