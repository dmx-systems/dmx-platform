<template>
  <div class="dmx-topicmap-commands">
    <el-select v-model="topicmapId">
      <el-option-group label="Topicmap">
        <el-option v-for="topic in topicmapTopics" :label="topic.value" :value="topic.id" :key="topic.id">
          <span class="fa icon">{{topic.icon}}</span><span>{{topic.value}}</span>
        </el-option>
      </el-option-group>
    </el-select>
    <component v-for="(command, i) in commands" :is="command" :key="i"></component>
  </div>
</template>

<script>
export default {

  computed: {

    topicmapId: {
      get () {
        return this.$store.getters.topicmapId
      },
      set (id) {
        this.$store.dispatch('selectTopicmap', id)
      }
    },

    workspaceId () {
      return this.$store.state.workspaces.workspaceId
    },

    topicmapTopics () {
      // Note 1: while initial rendering no workspace is selected yet
      // Note 2: when the workspace is switched its topicmap topics might not yet loaded
      const topics = this.$store.state.topicmaps.topicmapTopics[this.workspaceId]
      return topics && topics.sort((t1, t2) => t1.value.localeCompare(t2.value))
    },

    commands () {
      return this.$store.state.topicmaps.topicmapCommands[this.topicmapTypeUri]
    },

    topicmapTypeUri () {
      return this.$store.getters.topicmapTypeUri
    }
  }
}
</script>

<style>
.dmx-topicmap-commands {
  margin-left: 18px;
}

.dmx-topicmap-commands .el-button {
  padding-left:  2px !important;
  padding-right: 2px !important;
}

.dmx-topicmap-commands .el-button + .el-button {
  margin-left: 4px;     /* Element UI default is 10px */
}
</style>
