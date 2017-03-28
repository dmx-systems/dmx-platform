<template>
  <q-select label="Workspace" :options="options" v-model="workspaceId"></q-select>
</template>

<script>
export default {

  data () {
    return {
      workspaceId: undefined,
      workspaceTopics: [
        {id: 1234, value: 'DeepaMehta'},
        {id: 2345, value: 'Infobits'}
      ]
    }
  },

  watch: {
    workspaceId: function (workspaceId) {
      console.log('select workspace', workspaceId)
    }
  },

  computed: {
    state () {
      return this.$store.state.workspace
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
