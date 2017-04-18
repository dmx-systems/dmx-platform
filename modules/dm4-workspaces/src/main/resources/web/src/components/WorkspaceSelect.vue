<template>
  <div>
    Workspace
    <select>
      <option v-for="topic in workspaceTopics" :value="topic.id">{{topic.value}}</option>
    </select>
  </div>
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
    }
  }
}
</script>
