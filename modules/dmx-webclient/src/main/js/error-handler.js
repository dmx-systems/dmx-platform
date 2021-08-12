import {Notification} from 'element-ui'

export default function onHttpError (error) {
  const report = error.response.data
  const level = report.level || 'ERROR'
  Notification({
    title: level,
    type: level.toLowerCase(),
    message: '<p>' + report.error + '</p>' +
      (report.cause ? '<p>Cause: ' + report.cause + '</p>' : ''),
    dangerouslyUseHTMLString: true,
    duration: 0
  })
}
