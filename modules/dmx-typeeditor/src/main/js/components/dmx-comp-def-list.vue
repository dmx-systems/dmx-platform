<template>
  <div :class="['dmx-comp-def-list', mode]">
    <div class="field-label">Composition Definitions ({{compDefs.length}})</div>
    <template v-if="infoMode">
      <dmx-comp-def v-for="compDef in compDefs" :comp-def="compDef" :class="{marked: marked(compDef)}"
        :key="compDef.compDefUri" @click.native="click(compDef)">
      </dmx-comp-def>
    </template>
    <draggable v-else :list="compDefs" :animation="300">
      <!-- 3 lines duplicated in favor of code splitting -->
      <dmx-comp-def v-for="compDef in compDefs" :comp-def="compDef" :class="{marked: marked(compDef)}"
        :key="compDef.compDefUri" @click.native="click(compDef)">
      </dmx-comp-def>
    </draggable>
  </div>
</template>

<script>
export default {

  created () {
    // console.log('dmx-comp-def-list created')
  },

  mixins: [
    require('./mixins/mode').default,
    require('./mixins/info-mode').default
  ],

  props: {
    compDefs: {type: Array, required: true}
  },

  methods: {

    marked (compDef) {
      return this.$store.getters.visibleAssocIds?.includes(compDef.id)
    },

    click (compDef) {
      this.$emit('comp-def-click', compDef)
    }
  },

  components: {
    'dmx-comp-def': require('./dmx-comp-def').default,
    draggable: () => ({
      component: import('vuedraggable' /* webpackChunkName: "vuedraggable" */),
      loading: require('modules/dmx-webclient/src/main/js/components/dmx-spinner')
    })
  }
}
</script>

<style>
/* principle copy in dmx-topic-list.vue */
.dmx-comp-def-list .dmx-comp-def {
  border-bottom: 1px solid var(--border-color);
  border-left:   1px solid var(--border-color);
  border-right:  3px solid var(--border-color);
  background-color: white;
  transition: background-color 0.25s;
  padding: var(--object-item-padding);
}

.dmx-comp-def-list .dmx-comp-def:nth-child(1) {
  border-top: 1px solid var(--border-color);
}

.dmx-comp-def-list .dmx-comp-def.marked {
  border-right-color: var(--color-topic-icon);
}

.dmx-comp-def-list.info .dmx-comp-def:hover {
  background-color: var(--background-color-darker);
}

.dmx-comp-def-list.form .dmx-comp-def:hover {
  cursor: ns-resize;
}
</style>
