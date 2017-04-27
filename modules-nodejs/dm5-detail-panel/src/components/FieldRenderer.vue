<template>
  <div class="field-renderer">
    <!-- simple -->
    <div v-if="isSimple" class="field">
      <div class="label">{{label}}</div>
      <component :is="simpleComp" v-if="object" :object="object" :mode="mode"></component>
    </div>
    <!-- composite -->
    <div v-else v-for="assocDef in assocDefs" :key="assocDef.id">
      <!-- single -->
      <field-renderer v-if="cardOne(assocDef)" :object="childObject(assocDef)" :type="childType(assocDef)" :mode="mode">
      </field-renderer>
      <!-- multi -->
      <field-renderer v-else v-for="object in childObject(assocDef)" :object="object" :type="childType(assocDef)"
        :mode="mode" :key="object.id">
      </field-renderer>
    </div>
  </div>
</template>

<script>
export default {

  name: 'field-renderer',

  props: [
    'object',   // the Topic/Assoc to render, may be undefined
    'type',     // a TopicType or AssocType, is not undefined
    'mode'      // 'info' or 'form'
  ],

  computed: {

    label () {
      return this.type.value
    },

    isSimple () {
      return this.type.isSimple()
    },

    assocDefs () {
      return this.type.assocDefs
    },

    simpleComp () {
      return this.type.dataType.substr('dm4.core.'.length) + '-field'
    }
  },

  methods: {

    childObject (assocDef) {
      return this.object && this.object.childs[assocDef.assocDefUri]
    },

    childType (assocDef) {
      return assocDef.getChildType()
    },

    cardOne (assocDef) {
      return assocDef.childCard === 'dm4.core.one'
    }
  },

  components: {
    'text-field':    require('./TextField'),
    'number-field':  require('./NumberField'),
    'boolean-field': require('./BooleanField'),
    'html-field':    require('./HtmlField')
  }
}
</script>

<style>
</style>
