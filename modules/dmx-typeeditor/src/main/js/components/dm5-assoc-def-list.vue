<template>
  <div :class="['dm5-assoc-def-list', mode]">
    <div class="field-label">Child Types ({{size}})</div>
    <template v-if="infoMode">
      <dm5-assoc-def v-for="assocDef in assocDefs" :assoc-def="assocDef" :class="{marked: marked(assocDef)}"
        :key="assocDef.assocDefUri" @click.native="click(assocDef)">
      </dm5-assoc-def>
    </template>
    <draggable v-else :list="assocDefs" :animation="300">
      <!-- 3 lines duplicated in favor of code splitting; TODO: avoid -->
      <dm5-assoc-def v-for="assocDef in assocDefs" :assoc-def="assocDef" :class="{marked: marked(assocDef)}"
        :key="assocDef.assocDefUri" @click.native="click(assocDef)">
      </dm5-assoc-def>
    </draggable>
  </div>
</template>

<script>
import dm5 from 'dm5'

export default {

  created () {
    // console.log('dm5-assoc-def-list created')
  },

  mixins: [
    require('./mixins/mode').default,
    require('./mixins/info-mode').default
  ],

  props: {
    assocDefs: {type: Array, required: true}
  },

  computed: {
    size () {
      return this.assocDefs.length
    }
  },

  methods: {

    marked (assocDef) {
      return this.$store.getters.visibleAssocIds.includes(assocDef.id)
    },

    click (assocDef) {
      this.$emit('assoc-def-click', assocDef)
    }
  },

  components: {
    'dm5-assoc-def': require('./dm5-assoc-def').default,
    draggable: () => ({
      component: import('vuedraggable' /* webpackChunkName: "vuedraggable" */),
      loading: require('modules/dmx-webclient/src/main/js/components/dm5-spinner')
    })
  }
}
</script>

<style>
.dm5-assoc-def-list .dm5-assoc-def {
  border-bottom: 1px solid var(--border-color);
  border-left:   1px solid var(--border-color);
  border-right:  3px solid var(--border-color);
  background-color: white;
  transition: background-color 0.25s;
  padding: 8px;
}

.dm5-assoc-def-list .dm5-assoc-def:nth-child(1) {
  border-top: 1px solid var(--border-color);
}

.dm5-assoc-def-list .dm5-assoc-def.marked {
  border-right-color: var(--color-topic-icon);
}

.dm5-assoc-def-list.info .dm5-assoc-def:hover {
  background-color: var(--background-color-darker);
}

.dm5-assoc-def-list.form .dm5-assoc-def:hover {
  cursor: ns-resize;
}
</style>
