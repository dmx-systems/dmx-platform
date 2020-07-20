<template>
  <div :class="['dm5-comp-def-list', mode]">
    <div class="field-label">Composition Definitions ({{compDefs.length}})</div>
    <template v-if="infoMode">
      <dm5-comp-def v-for="compDef in compDefs" :comp-def="compDef" :class="{marked: marked(compDef)}"
        :key="compDef.compDefUri" @click.native="click(compDef)">
      </dm5-comp-def>
    </template>
    <draggable v-else :list="compDefs" :animation="300">
      <!-- 3 lines duplicated in favor of code splitting; TODO: avoid -->
      <dm5-comp-def v-for="compDef in compDefs" :comp-def="compDef" :class="{marked: marked(compDef)}"
        :key="compDef.compDefUri" @click.native="click(compDef)">
      </dm5-comp-def>
    </draggable>
  </div>
</template>

<script>
export default {

  created () {
    // console.log('dm5-comp-def-list created')
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
      return this.$store.getters.visibleAssocIds.includes(compDef.id)
    },

    click (compDef) {
      this.$emit('comp-def-click', compDef)
    }
  },

  components: {
    'dm5-comp-def': require('./dm5-comp-def').default,
    draggable: () => ({
      component: import('vuedraggable' /* webpackChunkName: "vuedraggable" */),
      loading: require('modules/dmx-webclient/src/main/js/components/dm5-spinner')
    })
  }
}
</script>

<style>
/* copy in dm5-topic-list.vue */
.dm5-comp-def-list .dm5-comp-def {
  border-bottom: 1px solid var(--border-color);
  border-left:   1px solid var(--border-color);
  border-right:  3px solid var(--border-color);
  background-color: white;
  transition: background-color 0.25s;
  padding: var(--object-item-padding);
}

.dm5-comp-def-list .dm5-comp-def:nth-child(1) {
  border-top: 1px solid var(--border-color);
}

.dm5-comp-def-list .dm5-comp-def.marked {
  border-right-color: var(--color-topic-icon);
}

.dm5-comp-def-list.info .dm5-comp-def:hover {
  background-color: var(--background-color-darker);
}

.dm5-comp-def-list.form .dm5-comp-def:hover {
  cursor: ns-resize;
}
</style>
