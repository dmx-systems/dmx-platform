<template>
  <div class="dmx-file-renderer">
    <pre v-if="isText">{{text}}</pre>
    <img v-if="isImage" :src="fileUrl" @load="update">
    <audio v-if="isAudio" :src="fileUrl" controls></audio>
    <video v-if="isVideo" :src="fileUrl" controls></video>
    <embed v-if="isPDF" :src="fileUrl" :type="mediaType" class="pdf" @load="update"></embed>
    <dmx-value-renderer :object="object" :level="0" :path="[]" :context="context" :no-heading="true">
    </dmx-value-renderer>
  </div>
</template>

<script>
export default {

  created () {
    // console.log('dmx-file-renderer created', this.object)
    this.initText()
  },

  mixins: [
    require('./mixins/object').default,
    require('./mixins/context').default
  ],

  data () {
    return {
      text: ''
    }
  },

  computed: {

    path () {
      return this.object.children['dmx.files.path'].value
    },

    mediaType () {
      const mediaType = this.object.children['dmx.files.media_type']
      return mediaType && mediaType.value
    },

    fileUrl () {
      return '/filerepo/' + encodeURIComponent(this.path)
    },

    isText () {
      return this.mediaType && this.mediaType.startsWith('text/')
    },

    isImage () {
      return this.mediaType && this.mediaType.startsWith('image/')
    },

    isAudio () {
      return this.mediaType && this.mediaType.startsWith('audio/')
    },

    isVideo () {
      return this.mediaType && this.mediaType.startsWith('video/')
    },

    isPDF () {
      return this.mediaType === 'application/pdf'
    }
  },

  watch: {
    object () {
      this.initText()
    }
  },

  methods: {

    update() {
      // console.log("update")
      this.context.updated()
    },

    initText () {
      if (this.isText) {
        this.$store.dispatch('getFileContent', this.path).then(content => {
          this.text = content
        })
      }
    }
  }
}
</script>

<style>
.dmx-file-renderer > pre {
  line-height: 1.4em;
  white-space: pre-wrap;
}

.dmx-file-renderer > img {
  max-width: 100%;
}

.dmx-file-renderer > .pdf {
  width: 100%;
  height: 100vh;
}

.dmx-file-renderer > .dmx-value-renderer {
  margin-top: var(--field-spacing);
}
</style>
