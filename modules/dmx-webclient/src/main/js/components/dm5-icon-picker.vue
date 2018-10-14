<template>
  <div class="dm5-icon-field">
    <div v-if="infoMode" class="fa icon">{{object.value}}</div>
    <div v-else>
      <el-button @click="open" class="fa icon">{{object.value}}</el-button>
      <el-dialog :visible.sync="visible" :modal="false" :append-to-body="true">
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
      console.log('select icon', icon.id, icon.unicode)
      this.object.value = String.fromCharCode(parseInt(icon.unicode, 16))
      this.close()
    }
  },

  components: {
    'fa-search': () => ({
      component: import('vue-font-awesome-search' /* webpackChunkName: "fa-search" */),
      loading: require('./dm5-spinner')
    })
  }
}
</script>

<style>
.dm5-icon-field .icon {
  font-size: 24px !important;
  color: var(--color-topic-icon);
}
</style>
