<template>
  <div class="dmx-date-picker">
    <div class="field-label">{{fieldLabel}}</div>
    <div v-if="infoMode">{{dateString}}</div>
    <el-date-picker v-else v-model="date"></el-date-picker>
  </div>
</template>

<script>
export default {

  created () {
    // console.log('dmx-date-picker created', this.compDef)
  },

  mixins: [
    require('./mixins/object').default,       // object to render
    require('./mixins/info-mode').default,
    require('./mixins/comp-def').default,     // undefined for top-level object
    require('./mixins/path').default,
    require('./mixins/context').default
  ],

  computed: {

    type () {
      return this.object.type
    },

    // Custom label rule (compare to dmx-value-renderer.vue)
    fieldLabel () {
      let label = this.type.value
      if (this.path.length) {
        label += ` (${this.path.join(' / ')})`
      }
      return label
    },

    mode () {
      return this.context.mode
    },

    date: {

      get () {
        // console.log('date getter', this.object)
        const c = this.object.children
        const y = c['dmx.datetime.year'].value
        const m = c['dmx.datetime.month'].value
        const d = c['dmx.datetime.day'].value
        // Topics created through "filling" have empty string values. If any topic is empty we don't create a
        // Date object but return an empty string. The Element UI Date Picker interprets that as "not set".
        return y && m && d && new Date(y, m - 1, d)
      },

      set (date) {
        // console.log('date setter', date)
        // Note: if a date field is cleared in the GUI we receive null here. To clear a field at server-side an empty
        // string must be sent. null would deserialize as JSONObject$Null causing the SimpleValue constructor to fail.
        const c = this.object.children
        c['dmx.datetime.year'].value  = date && date.getFullYear()  || ''         /* eslint no-mixed-operators: "off" */
        c['dmx.datetime.month'].value = date && date.getMonth() + 1 || ''
        c['dmx.datetime.day'].value   = date && date.getDate()      || ''
      }
    },

    dateString () {
      // Note: after updating the server sends the Date topic without its children. This is a bug (#153).
      // Calculation of "this.date" would fail. As a workaround we display nothing.
      return this.object.children['dmx.datetime.year'] && this.date.toLocaleDateString()
    }
  },

  /* ### TODO: async loading
  components: {
    'el-date-picker': () => {
      import('element-plus/theme-chalk/el-date-picker.css')
      return import('element-plus/lib/components/date-picker/index.js')
    }
  } */
}
</script>
