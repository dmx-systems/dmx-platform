<template>
  <div class="dm5-assoc-def">
    <div class="card label">{{card}}</div>
    <div class="types">
      <div class="fa icon">{{icon}}</div>
      <div>
        <div>{{childType.value}}</div>
        <div class="info label" v-if="info">{{info}}</div>
      </div>
    </div>
  </div>
</template>

<script>
import dm5 from 'dm5'

export default {

  props: {
    assocDef: {type: dm5.AssocDef, required: true}
  },

  computed: {

    card () {
      return this.assocDef.isOne() ? "One" : "Many"     // TODO: do not hardcode?
    },

    childType () {
      return this.assocDef.getChildType()
    },

    icon () {
      return this.childType.getIcon()
    },

    info () {
      const type = this.assocDef.getCustomAssocType()
      return type && type.value
    }
  }
}
</script>

<style>
.dm5-assoc-def .card {
  margin-bottom: 6px;
}

.dm5-assoc-def .types {
  display: flex;
  align-items: baseline;
}

.dm5-assoc-def .icon {
  color: var(--color-topic-icon);
  margin-right: 7px;
}

.dm5-assoc-def .info {
  margin-top: 6px;
}
</style>
