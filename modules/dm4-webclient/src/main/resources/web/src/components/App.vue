<template>
  <router-view></router-view>
  <!--
    Note: the Vue template compiler is not available as we use the Vue runtime.
    So we can't put <router-view> in index.html but must render the root component
    (= this one) via render function (see main.js).
  -->
</template>

<style>
:root {
  --main-font-family: "Lucida Grande", Verdana, sans-serif;
  --main-font-size: 14px;
  --label-font-size: 12px;
  --label-color: #8391a5;
  --highlight-color: #20a0ff;   /* see --color-primary in element-ui/packages/theme-default/src/common/var.css */
}

html {
  height: 100%;
}

body {
  height: 100%;
  margin: 0;
  overflow: hidden;   /* avoid window bounce when scrolling reaches top/bottom */
  font-family: var(--main-font-family);
  font-size:   var(--main-font-size);
}

input {
  font-family: var(--main-font-family);
  font-size:   var(--main-font-size) !important;   /* fixed absolute size for all el-input sizes */
}

button {
  font-family: var(--main-font-family);
  font-size:   var(--main-font-size) !important;   /* fixed absolute size for all el-button sizes */
}
</style>
