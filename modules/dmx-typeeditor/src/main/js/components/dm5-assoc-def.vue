<template>
  <div :class="['dm5-assoc-def', {marked}]">
    <div class="fa icon">{{icon}}</div>
    <div>
      <div>{{childType.value}}</div>
      <div class="info label">{{info}}</div>
    </div>
  </div>
</template>

<script>
import dm5 from 'dm5'

export default {

  props: {
    assocDef: {type: dm5.AssocDef, required: true},
    marked: Boolean   // optional: if truish the topic is rendered as "marked"
  },

  computed: {

    childType () {
      return this.assocDef.getChildType()
    },

    icon () {
      return this.childType.getIcon()
    },

    info () {
      return this.assocDef.value
    }
  }
}
</script>

<style>
.dm5-assoc-def {
  display: flex;
  align-items: baseline;
  border-right: 3px solid var(--border-color);
}

.dm5-assoc-def.marked {
  border-right-color: var(--color-topic-icon);
}

.dm5-assoc-def .icon {
  color: var(--color-topic-icon);
  margin-right: 7px;
}

.dm5-assoc-def .info {
  margin-top: 6px;
}
</style>
