<template>
  <div class="dm5-time-picker">
    <div class="field-label">{{fieldLabel}}</div>
    <div v-if="infoMode">{{object.value}}</div>
    <el-time-picker v-else v-model="time"></el-time-picker>
  </div>
</template>

<script>
export default {

  created () {
    // console.log('dm5-time-picker created', this.assocDef)
  },

  mixins: [
    require('./mixins/object').default,       // object to render
    require('./mixins/info-mode').default,
    require('./mixins/assoc-def').default,    // undefined for top-level object
    require('./mixins/context').default
  ],

  computed: {

    type () {
      return this.object.getType()
    },

    // TODO: copy in dm5-value-renderer.vue
    fieldLabel () {
      const customAssocType = this.assocDef && this.assocDef.getCustomAssocType()
      return customAssocType && customAssocType.isSimple() ? customAssocType.value : this.type.value
    },

    mode () {
      return this.context.mode
    },

    time: {

      get () {
        console.log('time getter', this.object)
        const c = this.object.childs
        const h = c['dmx.datetime.hour'].value
        const m = c['dmx.datetime.minute'].value
        // Topics created through "filling" have empty string values. If any topic is empty we don't create a
        // Date object but return an empty string. The Element UI Time Picker interprets that as "not set".
        return h && m && new Date(0, 0, 0, h, m)
      },

      set (time) {
        console.log('time setter', time)
        // Note: if a time field is cleared in the GUI we receive null here. To clear a field at server-side an empty
        // string must be sent. null would deserialize as JSONObject$Null causing the SimpleValue constructor to fail.
        const c = this.object.childs
        c['dmx.datetime.hour'].value   = time && time.getHours()   || ''
        c['dmx.datetime.minute'].value = time && time.getMinutes() || ''
      }
    }
  },

  components: {
    'el-time-picker': () => {
      import('element-ui/lib/theme-chalk/time-picker.css' /* webpackChunkName: "el-time-picker" */)
      return import('element-ui/lib/time-picker.js'       /* webpackChunkName: "el-time-picker" */)
    }
  }
}
</script>

<style>
</style>
