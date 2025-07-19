import {
  ElButton, ElInput, ElCheckbox, ElSelect, ElOption, ElOptionGroup, ElCollapse, ElCollapseItem,
  ElDialog, ElDropdown, ElDropdownMenu, ElDropdownItem, ElTabs, ElTabPane, ElLink, ElIcon, ElAutocomplete,
  ElMessageBox, ElNotification, ElLoading
} from 'element-plus'
import { ArrowDown } from '@element-plus/icons-vue'
import app from './app'

export default extraElementComponentsLoader

function extraElementComponentsLoader () {
  console.log('----> Load element-plus-extra')
  return import('./element-plus-extra')
}

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
