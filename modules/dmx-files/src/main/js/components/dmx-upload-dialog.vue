<template>
  <el-dialog custom-class="dmx-upload-dialog" :visible="visible" :title="title" :modal="false" v-draggable
      @open="clearError" @close="close">
    <el-upload :action="action" :on-success="onSuccess" :on-error="onError" ref="upload">
      <el-button slot="trigger" type="primary" @click="clearError">Select File</el-button>
    </el-upload>
    <div class="error">{{error}}</div>
  </el-dialog>
</template>

<script>
export default {

  inject: ["dmx"],

  data () {
    return {
      error: ''
    }
  },

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
    },

    onError (error, file, fileList) {
      this.error = `${error.name}: ${error.message}`
    },

    clearError () {
      this.error = ''
    }
  }
}
</script>

<style>
.dmx-upload-dialog .error {
  color: var(--color-danger);
  margin-top: var(--field-spacing);
}
</style>
