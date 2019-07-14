<template>
  <el-dialog custom-class="dm5-help-dialog" :visible="visible" @close="close">
    <div>
      <img src="../dmx-logo.svg" width="85%">
      <div style="display: flex">
        <div>
          <div class="field-label">Version</div>
          5.0-beta-4-SNAPSHOT
        </div>
        <div style="margin-left: 3em">
          <div class="field-label">Released</div>
          July 15, 2019
        </div>
      </div>
    </div>
    <div>
      <div class="field">
        <div class="field-label">License</div>
        GPL3 or later
      </div>
      <div class="field">
        <div class="field-label">Copyright</div>
        2019, <a href="https://dmx.systems">DMX Systems</a>
      </div>
      <div class="field">
        <div class="field-label">Documentation</div>
        <a href="https://docs.dmx.systems">docs.dmx.systems</a>
      </div>
      <div class="field">
        <div class="field-label">Support</div>
        <a href="https://forum.dmx.systems">forum.dmx.systems</a>
      </div>
    </div>
  </el-dialog>
</template>

<script>
export default {

  computed: {
    visible () {
      return this.$store.state.helpmenu.dialogVisibility
    }
  },

  methods: {
    close () {
      this.$store.dispatch('closeHelpDialog')
    }
  }
}
</script>

<style>
.dm5-help-dialog .el-dialog__body {
  display: flex;
}

.dm5-help-dialog .el-dialog__body > div:nth-child(1) {
  flex-basis: 60%;
}

.dm5-help-dialog img {
  position: relative;
  top: -10px;
  left: -15px;
}

.dm5-help-dialog .field + .field {
  margin-top: var(--field-spacing);
}
</style>
