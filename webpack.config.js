const HtmlWebpackPlugin = require('html-webpack-plugin')
const MiniCssExtractPlugin = require('mini-css-extract-plugin')
const { CleanWebpackPlugin } = require('clean-webpack-plugin')
const { VueLoaderPlugin } = require('vue-loader')
const { DefinePlugin } = require('webpack')

module.exports = (env = {}) => {

  const webpackConfig = {
    entry: './modules/dmx-webclient/src/main/js/main.js',
    output: {
      path: __dirname + '/modules/dmx-webclient/src/main/resources/web',
      filename: env.dev ? '[name].js' : '[chunkhash].[name].js'
    },
    resolve: {
      extensions: [".js", ".vue"],
      alias: {
        // used by plugin-manager.js
        'modules':          __dirname + '/modules',
        'modules-external': __dirname + '/modules-external'
      }
    },
    module: {
      rules: [
        {
          test: /\.vue$/,
          loader: 'vue-loader'
        },
        {
          test: /\.js$/,
          loader: 'babel-loader',
          exclude: /node_modules/
        },
        {
          test: /\.css$/,
          loader: [env.dev ? 'style-loader' : MiniCssExtractPlugin.loader, 'css-loader']
        },
        {
          test: /\.(png|jpg|jpeg|gif|eot|ttf|woff|woff2|svg|svgz)(\?.+)?$/,
          loader: 'file-loader'
        }
      ]
    },
    plugins: [
      new HtmlWebpackPlugin({
        template: 'modules/dmx-webclient/src/main/resources/index.html',
        favicon:  'modules/dmx-webclient/src/main/resources/favicon.png'
      }),
      new MiniCssExtractPlugin({
        filename: env.dev ? '[name].css' : '[contenthash].[name].css'
      }),
      new CleanWebpackPlugin(),
      new VueLoaderPlugin(),
      new DefinePlugin({
        DEV: env.dev
      })
    ],
    stats: {
      entrypoints: false,
      children: false,
      assetsSort: 'chunks'
    },
    performance: {
      hints: false
    }
  }

  if (env.dev) {
    webpackConfig.devServer = {
      port: 8082,
      proxy: {'/': 'http://localhost:8080'},
      noInfo: true,
      open: true
    }
  }

  return webpackConfig
}
