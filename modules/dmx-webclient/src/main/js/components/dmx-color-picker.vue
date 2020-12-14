<template>
  <div :class="['dmx-color-picker', mode]">
    <template v-if="infoMode">
      <div class="color-box" :style="{'background-color': object.value}"></div>
      <div class="color-value">{{object.value}}</div>
    </template>
    <template v-else>
      <el-color-picker v-model="object.value" :predefine="colors" size="medium" @input="input"></el-color-picker>
      <el-input class="color-value" v-model="object.value"></el-input>
    </template>
  </div>
</template>

<script>
export default {

  mixins: [
    require('./mixins/object').default,       // object to render
    require('./mixins/mode').default,
    require('./mixins/info-mode').default
  ],

  data: () => ({
    colors: [
      'hsl( 70, 60%, 53%)',   // light green
      'hsl(120, 50%, 53%)',   // green
      'hsl(180, 50%, 53%)',   // cyan
      'hsl(210, 50%, 53%)',   // blue         hue (210) matches Element UI --color-primary
      'hsl(255, 40%, 53%)',   // purple
      'hsl(300, 43%, 53%)',   // pink
      'hsl(  5, 50%, 53%)',   // red
      'hsl( 40, 70%, 53%)',   // orange
      'hsl( 60, 80%, 53%)',   // yellow
      'hsl(  0,  0%, 80%)',   // gray
      'hsl( 70, 80%, 96%)',
      'hsl(120, 80%, 96%)',
      'hsl(180, 80%, 96%)',
      'hsl(210, 80%, 96%)',
      'hsl(255, 80%, 96%)',
      'hsl(300, 80%, 96%)',
      'hsl(  5, 80%, 96%)',
      'hsl( 40, 80%, 96%)',
      'hsl( 60, 80%, 96%)',
      'hsl(  0,  0%, 97%)'
    ]
  }),

  methods: {
    input () {
      // Note: an <el-color-picker> represents a cleared value as null.
      // A serialized object sent to the server must not contain JSON null, but ''.
      if (this.object.value === null) {
        this.object.value = ''
      }
    }
  }
}
</script>

<style>
.dmx-color-picker {
  display: flex;
  align-items: center;
}

.dmx-color-picker .color-box {
  width: 24px;
  height: 24px;
  box-sizing: border-box;   /* copied from .el-color-picker__color */
  border: 1px solid #999;   /* copied from .el-color-picker__color */
  border-radius: 2px;       /* copied from .el-color-picker__color */
}

.dmx-color-picker .color-value {
  margin-left: 12px;
}

.dmx-color-picker.form .color-value {
  width: 12em;
  margin-left: 4px;
  margin-right: 4px;
}
</style>
