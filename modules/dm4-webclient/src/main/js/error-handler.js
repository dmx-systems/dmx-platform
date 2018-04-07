import { Notification } from 'element-ui'

export default function onHttpError (error) {
  const response = error.response
  Notification.error({
    title: `${response.status} ${response.statusText}`,
    message: response.data,
    duration: 0
  })
}
