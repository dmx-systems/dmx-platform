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
  --label-color: #878d99;       /* see --color-text-secondary in element-ui/packages/theme-chalk/src/common/var.scss */
  --line-height: 1.5em;
  --paragraph-spacing: 1em;
  --highlight-color: #409eff;   /* see --color-primary */
  --background-color: #f5f7fa;  /* see --background-color-base */
  --background-dark-color: #eef1f6;
  --color-danger: #fa5555;      /* see --color-danger */
  --shadow-hover: inset 0px 0px 0px 1px;
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

/* Reusable classes */

.field-label {
  font-size: var(--label-font-size);
  color:     var(--label-color);
  margin-bottom: 0.2em;
}

/* Element UI Overrides */

/*
  Element UI font sizes     Default   small     mini
  el-input + el-select      14px      13px      12px
  e-button                  14px      12px      12px
*/

.el-button--small {
  padding: 8px 14px;                    /* was 9px 15px */
}

.el-button--mini {
  padding: 6px 13px;                    /* was 7px 15px */
}

.el-input__inner {
  padding: 0px 8px;                     /* was 0px 15px */
}

.el-dialog__title {
  font-size: var(--main-font-size);     /* was 18px */
  line-height: unset;                   /* was 24px */
}

.el-dialog__body {
  padding: 15px;                        /* was 30px 20px */
  line-height: unset;                   /* was 24px */
}

.el-tabs__item {
  font-size: var(--label-font-size);    /* was 14px */
  color: var(--label-color);            /* was #2d2f33 (Element UI --color-text-primary) */
  height: 36px;                         /* was 40px */
  line-height: 36px;                    /* was 40px */
  padding: 0 12px;                      /* was 0 20px */
}

.el-table {
  font-size: var(--main-font-size);     /* was 12px (.el-table--mini) */
}

.el-table th {
  font-size: var(--label-font-size);
  font-weight: normal;                  /* User agent stylesheet sets bold */
  padding: 0;                           /* was 12px 0 (.el-table th) or 6px 0 (.el-table--mini th) */
}

.el-table .cell {
  word-break: normal;                   /* was break-all which breaks words instead of line wrap */
}
</style>
