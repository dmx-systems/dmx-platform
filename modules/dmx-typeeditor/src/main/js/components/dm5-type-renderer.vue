<template>
  <div class="dm5-type-renderer">
    <!-- Type Value -->
    <dm5-value-renderer :object="object" :level="0" :context="context"></dm5-value-renderer>
    <!-- Type URI -->
    <div class="field-label">Type URI</div>
    <div v-if="infoMode">{{object.uri}}</div>
    <el-input v-else v-model="object.uri"></el-input>
    <!-- Data Type -->
    <div class="field-label">Data Type</div>
    <div v-if="infoMode">{{dataType.value}}</div>
    <dm5-data-type-select v-else :type="type"></dm5-data-type-select>
    <!-- Assoc Defs -->
    <dm5-assoc-def-list :assoc-defs="assocDefs" :mode="mode" @assoc-def-click="click"></dm5-assoc-def-list>
  </div>
</template>

<script>
import dm5 from 'dm5'

export default {

  mixins: [
    require('./mixins/info-mode').default,
    require('./mixins/context').default
  ],

  props: {
    object: {   // the type to render
      type: dm5.Type,
      required: true
    }
  },

  computed: {

    type () {
      return this.object
    },

    mode () {
      return this.context.mode
    },

    dataType () {
      return this.type.getDataType()
    },

    assocDefs () {
      return this.type.assocDefs
    }
  },

  methods: {
    click (assocDef) {
      const childType = assocDef.getChildType()
      //
      childType.assoc = assocDef    // ugly type cache side effect
      //
      // Note: a Cytoscape edge can only be build on an assoc whose players are specified by-ID. But assoc def
      // players are specified by-URI. We set the IDs manually here. This is an ugly type cache side effect.
      // TODO: think about it.
      assocDef.getRole('dmx.core.parent_type').topicId = this.type.id
      assocDef.getRole('dmx.core.child_type').topicId = childType.id
      //
      this.$store.dispatch('revealRelatedTopic', childType)
    }
  },

  components: {
    'dm5-data-type-select': require('./dm5-data-type-select').default,
    'dm5-assoc-def-list':   require('./dm5-assoc-def-list').default,
    'dm5-value-renderer':   require('dm5-object-renderer/src/components/dm5-value-renderer').default
  }
}
</script>

<style>
.dm5-type-renderer .field-label {
  margin-top: var(--field-spacing);
}
</style>
