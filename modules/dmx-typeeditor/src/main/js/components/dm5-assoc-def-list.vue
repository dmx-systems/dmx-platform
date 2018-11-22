<template>
  <div :class="['dm5-assoc-def-list', mode]">
    <div class="field-label">Child Types ({{size}})</div>
    <draggable :list="assocDefs" :options="options">
      <dm5-assoc-def v-for="assocDef in assocDefs" :assoc-def="assocDef" :marked="marked(assocDef)"
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
    assocDefs: {type: Array, required: true},
    markerIds: Array      // IDs of topics to render as "marked"
  },

  computed: {

    size () {
      return this.assocDefs.length
    },

    options () {
      return {
        disabled: this.infoMode
      }
    }
  },

  methods: {

    // TODO
    marked (topic) {
      return this.markerIds && this.markerIds.includes(topic.id)
    },

    // TODO
    click (topic) {
      this.$emit('topic-click', topic)
    }
  },

  components: {
    'dm5-assoc-def': require('./dm5-assoc-def').default,
    'draggable': require('vuedraggable')    // Note: no .default as it's a CommonJS export
  }
}
</script>

<style>
.dm5-assoc-def-list .dm5-assoc-def {
  border-bottom: 1px solid var(--border-color);
  border-left:   1px solid var(--border-color);
  background-color: white;
  transition: background-color 0.25s;
  padding: 8px;
}

.dm5-assoc-def-list .dm5-assoc-def:nth-child(1) {
  border-top: 1px solid var(--border-color);
}

.dm5-assoc-def-list.info .dm5-assoc-def:hover {
  background-color: var(--background-color-darker);
}

.dm5-assoc-def-list.form .dm5-assoc-def:hover {
  cursor: ns-resize;
}
</style>
