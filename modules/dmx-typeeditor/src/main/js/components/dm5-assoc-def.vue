<template>
  <div class="dm5-assoc-def">
    <div class="card label">{{card}}</div>
    <span class="fa icon">{{icon}}</span><span class="type">{{childType.value}}</span><span class="info" v-if="info">{{info}}</span>
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
      return type && `(${type.value})`
    }
  }
}
</script>

<style>
.dm5-assoc-def .card {
  margin-bottom: 6px;
}

.dm5-assoc-def .icon {
  color: var(--color-topic-icon);
  margin-right: 7px;
}

.dm5-assoc-def .type {
  line-height: var(--line-height);
}

.dm5-assoc-def .info {
  color: var(--label-color);
  margin-left: 7px;
}
</style>
