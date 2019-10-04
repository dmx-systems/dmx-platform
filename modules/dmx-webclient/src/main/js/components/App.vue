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
    Note 1: we use the native font of the respective platform.
      https://make.wordpress.org/core/2016/07/07/native-fonts-in-4-6/
      https://bitsofco.de/the-new-system-font-stack/
    Update: since Cytoscape 3.3 "BlinkMacSystemFont" lets crash Chrome on macOS.
      https://github.com/cytoscape/cytoscape.js/issues/2249
    As a workaround since DMX 5.0-beta-3 we replace "BlinkMacSystemFont" by "system-ui" which is supported by both,
    Safari and Chrome. Firefox (at least up to version 66) still needs "-apple-system".
      https://caniuse.com/#feat=font-family-system-ui

    Note 2: multiple-word font names like "Segoe UI" are not quoted.
    In various browsers DOM style.getPropertyValue() works differently (see https://jsfiddle.net/jri_/tt8o97yu/2/):
      Safari: converts " -> '
      Chrome: converts ' -> "
      Firefox: no conversion
    This affects at least 2 situations:
      1) When styling Cytoscape nodes/edges (style: {'font-family': ...}) Cytoscape expects " to be used when quoting
         multiple-word font names. Otherwise an error is reported along with stacktrace (but rendering works anyways).
      2) When rendering a SVG <text> element the font-family attribute value must be enclosed in the opposite quoting
         style (e.g. <text font-family='"Lucida Grande", sans-serif'>). Otherwise the SVG would be malformed.
    So, the different style.getPropertyValue() browser behavior creates quite a mess.
    All the mess vanish if multiple-word font names are not quoted at all in CSS. There are some debates whether this
    is valid CSS or not. Fact is multiple-word font names without quotes do work in all major browsers.
  */
  --main-font-family: -apple-system, system-ui, Segoe UI, Roboto, Oxygen-Sans, Ubuntu, Cantarell, Helvetica Neue,
      sans-serif;
  --main-font-size: 14px;
  --label-font-size: 12px;
  --label-color: #909399;   /* matches --color-text-secondary in element-ui/packages/theme-chalk/src/common/var.scss */
  --label-color-disabled: #c0c4cc;            /* matches --color-text-placeholder */
  --heading-font-size: 16px;
  --heading-spacing: 17px;                    /* vertical spacing before/after heading */
  --color-topic-icon:  hsl(210, 50%, 53%);    /* matches dm5-color-picker blue */
  --color-topic-hover: hsl(210, 50%, 53%);    /* matches dm5-color-picker blue */
  --line-height: 1.6em;
  --paragraph-spacing: 0.6em;
  --field-spacing: 1.2em;                     /* vertical spacing between data fields */
  --object-item-padding: 8px;                 /* padding for topic/assoc items e.g. in a topic list */
  --icon-spacing: 7px;                        /* horizontal spacing between icon and label */
  --detail-panel-padding: 12px;               /* left/right padding for detail panel tab panes */
  --detail-panel-padding-all: 29px 12px 12px 12px;
  --highlight-color: #409eff;                 /* matches --color-primary */
  --highlight-color-2: #66b1ff;               /* matches --color-primary-light-2 */
  --background-color: #f5f7fa;                /* matches --background-color-base (used for detail panel background) */
  --background-color-darker: #ebeef5;
  --color-danger: #f56c6c;                    /* matches --color-danger */
  --border-color: #dcdfe6;                    /* matches --border-color-base */
  --border-color-lighter: #ebeef5;            /* matches --border-color-lighter */
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

.label {
  font-size: var(--label-font-size);
  color:     var(--label-color);
}

.field-label {
  font-size: var(--label-font-size);
  color:     var(--label-color);
  margin-bottom: 0.2em;
}

/* Element UI */

/*
  Element UI font sizes     Default   small     mini
  el-input + el-select      14px      13px      12px
  e-button                  14px      12px      12px
*/

.el-button--mini {
  padding: 6px 13px !important;                   /* was 7px 15px */
}

.el-input__inner {
  padding: 0px 8px !important;                    /* was 0px 15px */
}

.el-radio, .el-link {
  font-weight: unset !important;                  /* was 500 */
}

.el-dialog__header {
  padding: 15px 15px 5px !important;              /* was 20px 20px 10px */
}

.el-dialog__title {
  font-size: var(--main-font-size) !important;    /* was 18px */
  line-height: unset !important;                  /* was 24px */
}

.el-dialog__headerbtn {
  top: 3px !important;                            /* was 20px */
  right: 5px !important;                          /* was 20px */
}

.el-dialog__body {
  padding: 15px !important;                       /* was 30px 20px */
  line-height: unset !important;                  /* was 24px */
}

.el-dialog__footer {
  padding: 5px 15px 15px !important;              /* was 10px 20px 20px */
}

/* Note: this is a global rule as el-selects are <body> mounted */
.el-select-dropdown__item .icon {
  color: var(--color-topic-icon);
  margin-right: var(--icon-spacing);
}

/* Quill editor */

button.ql-topic-link {
  font-size: 16px !important;
  color: #ccc;
}
</style>
