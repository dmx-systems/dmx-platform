<template>
  <div class="dm5-assoc-def-list">
    <div class="field-label">Child Types ({{size}})</div>
    <div>
      <dm5-assoc-def v-for="assocDef in assocDefs" :assoc-def="assocDef" :marked="marked(assocDef)"
        :key="assocDef.assocDefUri" @click.native="click(assocDef)">
      </dm5-assoc-def>
    </div>
  </div>
</template>

<script>
import dm5 from 'dm5'

export default {

  created () {
    // console.log('dm5-assoc-def-list created', this.markerIds)
  },

  props: {
    assocDefs: {type: Array, required: true},
    markerIds: Array      // IDs of topics to render as "marked"
  },

  computed: {
    size () {
      return this.assocDefs.length
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
    'dm5-assoc-def': require('./dm5-assoc-def').default
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

.dm5-assoc-def-list .dm5-assoc-def:hover {
  background-color: var(--background-color-darker);
}
</style>
