<template>
  <div class="dmx-comp-def">
    <div class="card label">{{card}}</div>
    <span class="fa icon">{{childType.icon}}</span><span class="type">{{childType.value}}</span><span class="info" v-if="info">{{info}}</span>
  </div>
</template>

<script>
import dmx from 'dmx-api'

export default {

  props: {
    compDef: {type: dmx.CompDef, required: true}
  },

  computed: {

    card () {
      return this.compDef.isOne ? "One" : "Many"     // TODO: do not hardcode labels?
    },

    childType () {
      return this.compDef.childType
    },

    info () {
      const type = this.compDef.customAssocType
      return type && `(${type.value})`
    }
  }
}
</script>

<style>
.dmx-comp-def .card {
  margin-bottom: 6px;
}

.dmx-comp-def .icon {
  color: var(--color-topic-icon);
  margin-right: var(--icon-spacing);
}

.dmx-comp-def .type {
  line-height: var(--line-height);
}

.dmx-comp-def .info {
  color: var(--label-color);
  margin-left: 7px;
}
</style>
