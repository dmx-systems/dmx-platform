import Vue from 'vue'
import { Button, Input, Select, Option, Checkbox, Dialog, Table, TableColumn, Tabs, TabPane } from 'element-ui'
import locale from 'element-ui/lib/locale'

// set locale
locale.use(require('element-ui/lib/locale/lang/en').default)

// global config
Vue.prototype.$ELEMENT = {
  size: 'mini'
}

// register components
Vue.use(Button)
Vue.use(Input)
Vue.use(Select)
Vue.use(Option)
Vue.use(Checkbox)
Vue.use(Dialog)
Vue.use(Table)
Vue.use(TableColumn)
Vue.use(Tabs)
Vue.use(TabPane)
