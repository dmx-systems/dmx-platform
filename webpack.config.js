const path = require('path')
const CleanWebpackPlugin = require('clean-webpack-plugin')
const UglifyJsPlugin = require('uglifyjs-webpack-plugin')

module.exports = {
  entry: './modules/dm4-webclient/src/main/js/main.js',
  output: {
    filename: '[name].bundle.js',
    path: path.resolve(__dirname, 'modules/dm4-webclient/src/main/resources/web/dist'),
    publicPath: 'dist/'
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
    new CleanWebpackPlugin(['modules/dm4-webclient/src/main/resources/web/dist'])
  ],
  devServer: {
    port: 8082,
    proxy: {
      '/': {
        target: 'http://localhost:8080',
        bypass: req => {
          if (req.url === '/') {
            return 'modules/dm4-webclient/src/main/resources/web/'
          }
        }
      }
    },
    noInfo: true,
    open: true
  },
  // When minifying ES6 code uglify-es 3.3.9 (as package locked by uglifyjs-webpack-plugin) generates code that throws
  // "TypeError: Assignment to constant variable". This is due to a bug in uglify-es 3.3.9 in conjunction with function
  // inlining.
  // https://github.com/mishoo/UglifyJS2/issues/2854
  // https://github.com/mishoo/UglifyJS2/issues/2842
  // The solution is to disable function inlining.
  // https://github.com/webpack/webpack/issues/6567
  optimization: {
    minimizer: [
      new UglifyJsPlugin({
        uglifyOptions: {
          compress: {
            inline: false
          }
        }
      })
    ]
  },
  performance: {
    hints: false
  }
}
