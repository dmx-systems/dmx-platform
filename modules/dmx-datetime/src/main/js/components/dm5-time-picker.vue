<template>
  <div class="dm5-time-picker">
    <div class="field-label">{{fieldLabel}}</div>
    <div v-if="infoMode">{{timeString}}</div>
    <el-time-picker v-else v-model="time"></el-time-picker>
  </div>
</template>

<script>
export default {

  created () {
    // console.log('dm5-time-picker created', this.compDef)
  },

  mixins: [
    require('./mixins/object').default,       // object to render
    require('./mixins/info-mode').default,
    require('./mixins/comp-def').default,     // undefined for top-level object
    require('./mixins/context').default
  ],

  computed: {

    type () {
      return this.object.type
    },

    // TODO: copy in dm5-value-renderer.vue
    fieldLabel () {
      const customAssocType = this.compDef && this.compDef.getCustomAssocType()
      return customAssocType && customAssocType.isSimple() ? customAssocType.value : this.type.value
    },

    mode () {
      return this.context.mode
    },

    time: {

      get () {
        const c = this.object.children
        const h = c['dmx.datetime.hour'].value
        const m = c['dmx.datetime.minute'].value
        // console.log('time getter', this.object, h, m)
        // Topics created through "filling" have empty string values. If any topic is empty we don't create a
        // Date object but return false. The Element UI Time Picker interprets that as "not set" and shows an empty
        // field. Passing empty strings to Date() would result in a Date representing 0:00.
        return h !== '' && m !== '' && new Date(0, 0, 0, h, m)
      },

      set (time) {
        // console.log('time setter', time, time && time.getHours(), time && time.getMinutes())
        // Note: if a time field is cleared in the GUI we receive null here. To clear a field at server-side an empty
        // string must be sent. null would deserialize as JSONObject$Null causing the SimpleValue constructor to fail.
        const c = this.object.children
        c['dmx.datetime.hour'].value   = time === null ? '' : time.getHours()
        c['dmx.datetime.minute'].value = time === null ? '' : time.getMinutes()
      }
    },

    timeString () {
      // Note: after updating the server sends the Time topic without its children. This is a bug (#153).
      // Calculation of "this.time" would fail. As a workaround we display nothing.
      return this.object.children['dmx.datetime.hour'] && this.time.toLocaleTimeString()
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
