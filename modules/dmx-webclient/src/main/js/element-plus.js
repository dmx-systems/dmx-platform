import {
  ElButton, ElInput, ElCheckbox, ElSelect, ElOption, ElOptionGroup, ElCollapse, ElCollapseItem,
  ElDialog, ElDropdown, ElDropdownMenu, ElDropdownItem, ElTabs, ElTabPane, ElLink, ElIcon, ElAutocomplete,
  ElMessageBox, ElNotification, ElLoading
} from 'element-plus'
import { ArrowDown } from '@element-plus/icons-vue'
import app from './app'
// import DialogDraggable from 'vue-element-dialog-draggable'       // TODO?

export default () => undefined    // import('./element-plus-ext')   // TODO

// set locale       // TODO?
// import locale from 'element-plus/lib/locale'
// locale.use(require('element-plus/lib/locale/lang/en').default)

// global config    // TODO?
// Vue.prototype.$ELEMENT = {
//   size: 'mini'
// }

// register app assets

app.component('el-button', ElButton)
app.component('el-input', ElInput)
app.component('el-checkbox', ElCheckbox)
app.component('el-select', ElSelect)
app.component('el-option', ElOption)
app.component('el-option-group', ElOptionGroup)
app.component('el-collapse', ElCollapse)
app.component('el-collapse-item', ElCollapseItem)
app.component('el-dialog', ElDialog)
app.component('el-dropdown', ElDropdown)
app.component('el-dropdown-menu', ElDropdownMenu)
app.component('el-dropdown-item', ElDropdownItem)
app.component('el-tabs', ElTabs)
app.component('el-tab-pane', ElTabPane)
app.component('el-link', ElLink)
app.component('el-icon', ElIcon)
app.component('el-autocomplete', ElAutocomplete)

app.component('ArrowDown', ArrowDown)

app.use(ElMessageBox)
app.use(ElNotification)
app.use(ElLoading)

// Vue.use(DialogDraggable)     // TODO?
