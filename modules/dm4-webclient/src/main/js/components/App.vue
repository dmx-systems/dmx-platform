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
  /*
    Note: font name "Lucida Grande" is not quoted.
    In various browsers DOM style.getPropertyValue() works differently (see https://jsfiddle.net/jri_/tt8o97yu/2/):
      Safari: converts " -> '
      Chrome: converts ' -> "
      Firefox: no conversion
    This affects at least 2 situations:
      1) When styling Cytoscape nodes/edges (style: {'font-family': ...}) Cytoscape expects " to be used when quoting
         multiple word font names. Otherwise an error is reported along with stacktrace (but rendering works anyways).
      2) When rendering a SVG <text> element the font-family attribute value must be enclosed in the opposite quoting
         style (e.g. <text font-family='"Lucida Grande", sans-serif'>). Otherwise the SVG would be malformed.
    So, the different style.getPropertyValue() browser behavior creates quite a mess.
    All the mess vanish if multiple word font names are not quoted at all in CSS. There are some debates whether this
    is valid CSS or not. Fact is multiple word font names without quotes do work in all major browsers.
  */
  --main-font-family: Lucida Grande, Verdana, sans-serif;
  --main-font-size: 14px;
  --label-font-size: 12px;
  --label-color: #8391a5;
  --highlight-color: #20a0ff;   /* see --color-primary in element-ui/packages/theme-default/src/common/var.css */
  --background-color: #f6f8fb;  /* brigher version of Element UI's table headers (#eef1f6) */
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
  font-size:   var(--main-font-size) !important;   /* fixed size for all el-input sizes */
}

button {
  font-family: var(--main-font-family);
  font-size:   var(--main-font-size) !important;   /* fixed size for all el-button sizes */
}

/* Element UI */

.el-dialog__title {
  font-size: var(--main-font-size);     /* was 16px */
}

.el-tabs__item {
  font-size: var(--label-font-size);    /* was 14px */
  height: 40px;                         /* was 42px */
  padding: 0 14px;                      /* was 0 16px */
}

.el-table th {
  height: 30px;                         /* was 40px */
}

.el-table th > .cell {
  font-size: var(--label-font-size);    /* was 14px */
  font-weight: normal;                  /* was bold */
}
</style>
