process.env.NODE_ENV = 'production'

require('colors')

var
  shell = require('shelljs'),
  path = require('path'),
  webpack = require('webpack'),
  webpackConfig = require('./webpack.config'),
  targetPath = path.join(__dirname, '../dist')

require('./script.clean.js')
console.log(('Building DeepaMehta frontend plugin ...\n').bold)

shell.mkdir('-p', targetPath)
shell.cp('-R', 'src/statics', targetPath)

webpack(webpackConfig, function (err, stats) {
  if (err) throw err
  process.stdout.write(stats.toString({
    colors: true,
    modules: false,
    children: false,
    chunks: false,
    chunkModules: false
  }) + '\n')

  console.log(('\nBuild complete in ' + '"/dist"'.bold + ' folder.\n').cyan)
})
