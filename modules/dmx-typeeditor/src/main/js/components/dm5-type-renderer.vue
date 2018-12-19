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

  created () {
    // console.log('dm5-type-renderer created', this.type)
  },

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
      childType.assoc = assocDef    // type cache side effect ### FIXME
      this.$store.dispatch('revealRelatedTopic', childType)
    }
  },

  components: {
    'dm5-data-type-select': require('./dm5-data-type-select').default,
    'dm5-assoc-def-list':   require('./dm5-assoc-def-list').default
  }
}
</script>

<style>
.dm5-type-renderer .field-label {
  margin-top: var(--field-spacing);
}
</style>
