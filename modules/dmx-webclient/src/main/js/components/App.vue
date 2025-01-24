<template>
  <router-view></router-view>
</template>

<style>
@import url(../../resources-build/ubuntu-font/fonts.css);

:root {
  /*
    Note 1: we no longer use the platform's system font (font-family: system-ui) as Chrome on a Mac renders some emojis
      (e.g. U+263A White Smiling Face, U+2708 Airplane) as small black glyphs then while Firefox and Safari render all
      emojis nicely and in color. The origin is that the macOS system font ("SF Pro"), while being a text font, it also
      contains small black glyphs for some emoji code points and these compete with the dedicated "Apple Color Emoji"
      font. When SF Pro is selected Chrome renders all characters with this font and falls back to "Apple Color Emoji"
      only for glyphs not found in SF Pro. Firefox (as well as Safari) on the other hand always use the "Apple Color
      Emoji" font for rendering emoji characters.
        https://fullystacked.net/using-emoji-on-the-web/
        https://nolanlawson.com/2022/04/08/the-struggle-of-using-native-emoji-on-the-web/
      To have colorful emoji rendering in all browsers the solution is utilizing a font for primary text rendering which
      does not contain any emoji glphys, e.g. the "Ubuntu" web font.

    Note 2: multiple-word font names like "Apple Color Emoji" are not quoted.
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
  --main-font-family: Ubuntu, Twemoji Country Flags, Twemoji Mozilla, Apple Color Emoji, Segoe UI Emoji,
      Segoe UI Symbol, Noto Color Emoji, EmojiOne Color, Android Emoji, sans-serif;
  --main-font-size: 14px;
  --label-font-size: 12px;
  --label-color: #909399;   /* matches --color-text-secondary in element-ui/packages/theme-chalk/src/common/var.scss */
  --label-color-disabled: #c0c4cc;            /* matches --color-text-placeholder */
  --heading-font-size: 16px;
  --heading-spacing: 17px;                    /* vertical spacing before/after heading */
  --color-topic-icon:  hsl(210, 50%, 53%);    /* matches dmx-color-picker blue */
  --color-topic-hover: hsl(210, 50%, 53%);    /* matches dmx-color-picker blue */
  --line-height: 1.5;
  --paragraph-spacing: 0.5em;
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
  font-family: var(--main-font-family);            /* defaults: Firefox (Mac): "-apple-system", Chrome (Mac): "Arial" */
  font-size:   var(--main-font-size) !important;   /* fixed size for all el-input sizes */
}

button {
  font-family: var(--main-font-family);            /* defaults: Firefox (Mac): "-apple-system", Chrome (Mac): "Arial" */
  font-size:   var(--main-font-size) !important;   /* fixed size for all el-button sizes */
}

/* Reusable classes */

.label {
  font-size: var(--label-font-size) !important;
  color:     var(--label-color) !important;
}

.field-label {
  font-size: var(--label-font-size) !important;
  color:     var(--label-color) !important;
  margin-bottom: 0.2em !important;
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
  padding: 4px 15px 3px !important;               /* was 20px 20px 10px */
  background-image: url("../../resources-build/dots.png");
}

.el-dialog__title {
  font-size: var(--main-font-size) !important;    /* was 18px */
  line-height: unset !important;                  /* was 24px */
  color: var(--label-color) !important;
}

.el-dialog__headerbtn {
  top: 1px !important;                            /* was 20px */
  right: 2px !important;                          /* was 20px */
}

.el-dialog__body {
  padding: 15px !important;                       /* was 30px 20px */
  line-height: unset !important;                  /* was 24px */
  word-break: normal !important;                  /* was "break-all" */
  overflow-wrap: anywhere;
}

.el-dialog__footer {
  padding: 5px 15px 15px !important;              /* was 10px 20px 20px */
}

.el-select-dropdown__wrap {
  max-height: calc(100vh - 7px) !important;       /* Element Plus default is 274px */
}

.el-select-dropdown__item .icon {
  color: var(--color-topic-icon);
  margin-right: var(--icon-spacing);
}

.el-notification__content {
  text-align: unset !important;                   /* was "justify" */
  overflow-wrap: anywhere;
}

.el-notification__content p + p {
  margin-top: 1em !important;                     /* was 0 */
}

/* Quill editor */

button.ql-topic-link {
  font-size: 16px !important;
  color: #ccc;
}

video.ql-video {
  width: 100%;
}
</style>
