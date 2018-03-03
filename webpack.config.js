const path = require('path')

module.exports = (env = {}) => {

  const webpackConfig = {
    entry: './modules/dm4-webclient/src/main/js/main.js',
    output: {
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
          exclude: /node_modules\/(?!dm5)/
          // Note: at the moment the dm5 modules are distributed not in pre-compiled form.
          // For the production build we must include them in babel processing as UglifyJs accpets no ES6.
          // Note: regex x(?!y) matches x only if x is not followed by y. Only x is part of the match results.
          // https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/RegExp
          // TODO: distribute the dm5 modules in pre-compiled form.
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
    }
  }

  if (env.dev) {
    webpackConfig.devServer = {
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
    }
  }

  return webpackConfig
}
