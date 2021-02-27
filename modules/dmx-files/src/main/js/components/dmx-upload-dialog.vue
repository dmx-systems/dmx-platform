<template>
  <el-dialog :visible="visible" :title="title" :modal="false" v-draggable @close="close">
    <el-upload :action="action" :on-success="onSuccess" ref="upload">
      <el-button slot="trigger" type="primary">Select File</el-button>
    </el-upload>
  </el-dialog>
</template>

<script>
export default {

  inject: ["dmx"],

  computed: {

    visible () {
      return this.$store.state.files.visible
    },

    folderName () {
      return this.$store.state.files.folderName
    },

    path () {
      return this.$store.state.files.path
    },

    title () {
      return `Upload to "${this.folderName}"`
    },

    action () {
      return '/files/' + encodeURIComponent(this.path)
    }
  },

  methods: {

    close () {
      this.$store.dispatch('closeUploadDialog')
    },

    onSuccess (response, file, fileList) {
      this.$store.dispatch('revealRelatedTopic', {relTopic: new this.dmx.RelatedTopic(response.topic)})
      this.$refs.upload.clearFiles()
      this.close()
    }
  }
}
</script>
