import {Notification} from 'element-ui'

export default function onHttpError (error) {
  const report = error.response.data
  Notification.error({
    title: 'Error',
    message: `
      <p>${report.error}</p>
      <p>Cause: ${report.cause}</p>`,
    dangerouslyUseHTMLString: true,
    duration: 0
  })
}
