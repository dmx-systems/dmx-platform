<template>
  <div class="dmx-icon-picker">
    <div v-if="infoMode" class="fa icon">{{object.value}}</div>
    <div v-else>
      <el-button class="fa icon" @click="open">{{object.value}}</el-button>
      <el-dialog title="Pick an Icon…" :visible.sync="visible" :modal="false" v-draggable>
        <fa-search @icon-select="select"></fa-search>
      </el-dialog>
    </div>
  </div>
</template>

<script>
export default {

  mixins: [
    require('./mixins/object').default,       // object to render
    require('./mixins/mode').default,
    require('./mixins/info-mode').default
  ],

  data () {
    return {
      visible: false      // dialog visibility
    }
  },

  methods: {

    open () {
      this.visible = true
    },

    close () {
      this.visible = false
    },

    select (icon) {
      // console.log('select icon', icon.id, icon.unicode)
      this.object.value = String.fromCharCode(parseInt(icon.unicode, 16))
      this.close()
    }
  },

  components: {
    'fa-search': () => ({
      component: import('vue-font-awesome-search'),
      loading: require('./dmx-spinner')
    })
  }
}
</script>

<style>
.dmx-icon-picker .icon {
  font-size: 24px !important;
  color: var(--color-topic-icon);
}
</style>
