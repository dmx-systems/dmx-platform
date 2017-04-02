<template>
  <q-select label="Workspace" :options="options" v-model="workspaceId"></q-select>
</template>

<script>
export default {

  data () {
    return {
      workspaceId: undefined
    }
  },

  watch: {
    workspaceId: function (workspaceId) {
      console.log('select workspace', workspaceId)
    }
  },

  computed: {
    workspaceTopics () {
      return this.$store.state.workspaces.workspaceTopics
    },

    options () {
      return this.workspaceTopics.map(topic => ({
        label: topic.value,
        value: topic.id
      }))
    }
  }
}
</script>
