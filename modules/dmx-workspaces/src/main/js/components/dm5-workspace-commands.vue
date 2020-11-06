<template>
  <div class="dm5-workspace-commands">
    <el-select v-model="workspaceId">
      <el-option-group label="Workspace">
        <el-option v-for="topic in workspaceTopics" :label="topic.value" :value="topic.id" :key="topic.id">
          <!-- Note: harcoding icon here; the Workspace type might not yet be present in type cache -->
          <span class="fa icon">&#xf005;</span><span>{{topic.value}}</span>
        </el-option>
      </el-option-group>
    </el-select>
    <component v-for="(command, i) in commands" :is="command" :key="i"></component>
  </div>
</template>

<script>
export default {

  computed: {

    workspaceId: {
      get () {
        return this.$store.state.workspaces.workspaceId
      },
      set (id) {
        this.$store.dispatch('selectWorkspace', id)
      }
    },

    workspaceTopics () {
      // Note: while initial rendering the workspace topics might not yet loaded
      const topics = this.$store.state.workspaces.workspaceTopics
      return topics && topics.sort((t1, t2) => t1.value.localeCompare(t2.value))
    },

    topicmapTypeUri () {
      const topicmapTopic = this.$store.getters.topicmapTopic
      return topicmapTopic && topicmapTopic.children['dmx.topicmaps.topicmap_type_uri'].value
    },

    commands () {
      return this.$store.state.workspaces.workspaceCommands[this.topicmapTypeUri]
    }
  }
}
</script>

<style>
.dm5-workspace-commands .el-button {
  padding-left:  2px !important;
  padding-right: 2px !important;
}
</style>
