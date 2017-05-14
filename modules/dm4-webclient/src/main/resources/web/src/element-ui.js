import Vue from 'vue'
import { Button, Input, Select, Option, Dialog, Table, TableColumn } from 'element-ui'
import locale from 'element-ui/lib/locale'

// set locale
locale.use(require('element-ui/lib/locale/lang/en').default)

// register components
Vue.use(Button)
Vue.use(Input)
Vue.use(Select)
Vue.use(Option)
Vue.use(Dialog)
Vue.use(Table)
Vue.use(TableColumn)
