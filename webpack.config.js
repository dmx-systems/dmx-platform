const path = require('path')
const HtmlWebpackPlugin = require('html-webpack-plugin')
const CleanWebpackPlugin = require('clean-webpack-plugin')
const { VueLoaderPlugin } = require('vue-loader')

module.exports = (env = {}) => {

  const webpackConfig = {
    entry: './modules/dm4-webclient/src/main/js/main.js',
    output: {
      filename: '[name].bundle.js',
      path: path.resolve(__dirname, 'modules/dm4-webclient/src/main/resources/web')
    },
    resolve: {
      extensions: [".js", ".vue"],
      alias: {
        'modules': path.resolve(__dirname, 'modules')     // needed by plugin-manager.js
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
          loader: ['style-loader', 'css-loader']
        },
        {
          test: /\.(png|jpg|jpeg|gif|eot|ttf|woff|woff2|svg|svgz)(\?.+)?$/,
          loader: 'file-loader'
        }
      ]
    },
    plugins: [
      new HtmlWebpackPlugin({
        template: 'modules/dm4-webclient/src/main/resources/index.html'
      }),
      new CleanWebpackPlugin(['modules/dm4-webclient/src/main/resources/web']),
      new VueLoaderPlugin()
    ],
    performance: {
      hints: false
    }
  }

  if (env.dev) {
    webpackConfig.devServer = {
      port: 8082,
      proxy: {
        '/': 'http://localhost:8080'
      },
      noInfo: true,
      open: true
    }
  }

  return webpackConfig
}
