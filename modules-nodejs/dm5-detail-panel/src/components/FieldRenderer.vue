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
      <field-renderer v-if="cardOne(assocDef)" :object="childObject(assocDef)" :mode="mode">
      </field-renderer>
      <!-- multi -->
      <field-renderer v-else v-for="object in childObjects(assocDef)" :object="object" :mode="mode" :key="object.id">
      </field-renderer>
    </div>
  </div>
</template>

<script>
// TODO: publish model as package "dm5-model"
import { Topic } from 'modules/dm4-webclient/src/main/resources/web/src/model'

export default {

  name: 'field-renderer',

  props: [
    'object',   // the Topic/Assoc to render; is never undefined;
                // may be an "empty" topic/assoc, without ID, with just type set
    'mode'      // 'info' or 'form'
  ],

  computed: {

    type () {
      return this.object.getType()
    },

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

    // single
    childObject (assocDef) {
      return this.object.childs[assocDef.assocDefUri] || this.emptyTopic(assocDef)
    },

    // multi
    childObjects (assocDef) {
      return this.object.childs[assocDef.assocDefUri] || []
    },

    emptyTopic (assocDef) {
      return new Topic({
        type_uri: assocDef.childTypeUri
      })
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
