import {Notification} from 'element-ui'

export default function onHttpError (error) {
  const report = error.response.data
  if (!report.level) {
    report.level = 'ERROR'
  }
  Notification({
    title: report.level,
    type: report.level.toLowerCase(),
    message: '<p>' + report.error + '</p>' +
      (report.cause ? '<p>Cause: ' + report.cause + '</p>' : ''),
    dangerouslyUseHTMLString: true,
    duration: 0
  })
}
