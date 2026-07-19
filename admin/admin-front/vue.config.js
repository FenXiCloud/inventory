/**
 * <p>****************************************************************************</p>
 * <p><b>Copyright © 2010-2019 soho team All Rights Reserved<b></p>
 * <ul style="margin:15px;">
 * <li>Description : </li>
 * <li>Version     : 1.0</li>
 * <li>Creation    : 2019年11月19日</li>
 * <li>@author     : ____′↘夏悸</li>
 * </ul>
 * <p>****************************************************************************</p>
 */
const path = require('path');

module.exports = {
	pages: {
		index: {
			title: "纷析云进销存系统",
			entry: 'src/main.js',
			chunks: ['chunk-vendors', 'chunk-common', 'index']
		}
	},
	productionSourceMap: false,
	devServer: {
		port: 8501,
		client: {
			overlay: false
		},
		proxy: {
			'^/api': {
				target: 'http://localhost:8500',
				pathRewrite: {'^/api': ''}
			}
		}
	},
	configureWebpack: {
		resolve: {
			alias: {
				'@': path.resolve(__dirname, 'src/'),
				'@common': path.resolve(__dirname, 'src/js/common/'),
				'@js': path.resolve(__dirname, 'src/js/')
			}
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
