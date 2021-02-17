<template>
  <div class="dmx-file-renderer">
    <img v-if="isImage" :src="fileUrl">
    <embed v-if="isPDF" class="pdf" :src="fileUrl" :type="mediaType"></embed>
    <dmx-value-renderer :object="object" :level="0" :path="[]" :context="context" :no-heading="true">
    </dmx-value-renderer>
  </div>
</template>

<script>
export default {

  created () {
    // console.log('dmx-file-renderer created', this.object)
  },

  mixins: [
    require('./mixins/object').default,
    require('./mixins/context').default
  ],

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

    isImage () {
      return this.mediaType && this.mediaType.startsWith('image/')
    },

    isPDF () {
      return this.mediaType === 'application/pdf'
    }
  }
}
</script>

<style>
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
