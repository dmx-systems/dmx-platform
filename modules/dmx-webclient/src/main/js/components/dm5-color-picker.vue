<template>
  <div :class="['dm5-color-picker', mode]">
    <template v-if="infoMode">
      <div class="color-box" :style="style"></div>
      <div class="color-value">{{object.value}}</div>
    </template>
    <template v-else>
      <el-color-picker v-model="object.value" size="medium"></el-color-picker>
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

  computed: {
    style () {
      return {'background-color': this.object.value}
    }
  }
}
</script>

<style>
.dm5-color-picker {
  display: flex;
  align-items: center;
}

.dm5-color-picker .color-box {
  width: 24px;
  height: 24px;
  box-sizing: border-box;   /* copied from .el-color-picker__color */
  border: 1px solid #999;   /* copied from .el-color-picker__color */
  border-radius: 2px;       /* copied from .el-color-picker__color */
}

.dm5-color-picker .color-value {
  margin-left: 12px;
}

.dm5-color-picker.form .color-value {
  width: 12em;
  margin-left: 4px;
  margin-right: 4px;
}
</style>
