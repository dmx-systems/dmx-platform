<template>
  <div class="field-renderer">
    <div v-if="isSimple" class="field">
      <div class="label">{{label}}</div>
      <div class="value" v-if="object">{{object.value}}</div>
      <!--component :is="simpleComp" ></component-->
    </div>
    <field-renderer v-else v-for="assocDef in assocDefs" :key="assocDef.assocDefUri"
      :object="childObject(assocDef)" :type="childType(assocDef)">
    </field-renderer>
  </div>
</template>

<script>
export default {
  name: 'field-renderer',
  props: ['object', 'type'],  // object may be null, type is not null
  computed: {
    label () {
      return this.type.value
    },
    isSimple () {
      return this.type.isSimple()
    },
    assocDefs () {
      return this.type.assocDefs
    }
  },
  methods: {
    childObject (assocDef) {
      return this.object && this.object.childs[assocDef.assocDefUri]
    },
    childType (assocDef) {
      return assocDef.getChildType()
    }
  }
}
</script>

<style>
</style>
