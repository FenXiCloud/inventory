/** vue配置 */
const path = require('path');

module.exports = {
    pages: {
        index: {
            title: "纷析云",
            entry: 'src/main.js',
            chunks: ['chunk-vendors', 'chunk-common', 'index']
        }
    },
    productionSourceMap: false,
    devServer: {
        port: 8411,
        client: {
            overlay: false
        },
        proxy: {
            '^/api': {
                target: 'http://localhost:8410',
                pathRewrite: {'^/api': ''}
            }
        }
    },
    configureWebpack: {
        resolve: {
            alias: {
                '@': path.resolve(__dirname, 'src/'),
                '@views': path.resolve(__dirname, 'src/views/'),
                '@common': path.resolve(__dirname, 'src/js/common/'),
                '@js': path.resolve(__dirname, 'src/js/'),
                // manba → dayjs 兼容实现
                'manba': path.resolve(__dirname, 'src/js/common/manba.js')
            },
            fallback: {
                fs: false,
                crypto: false
            }
        },
        externals: {
            './cptable': 'var cptable'
        }
    },
    css: {
        loaderOptions: {
            sass: {
                api: 'modern',
                sassOptions: {
                    quietDeps: true
                }
            }
        }
    },
    pluginOptions: {
        windicss: {}
    }
};
