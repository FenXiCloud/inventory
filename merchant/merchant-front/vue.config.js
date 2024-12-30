/**
 * @功能描述: vue配置
 * @创建时间: 2023年08月08日
 * @公司官网: www.fenxi365.com
 * @公司信息: 纷析云（杭州）科技有限公司
 * @公司介绍: 专注于财务相关软件开发, 企业会计自动化解决方案
 */
const path = require('path');
const webpack = require('webpack');
const globalVars = require('./src/style/var.js');

module.exports = {
	pages: {
		index: {
			title: "纷析云",
			entry: 'src/main.js',
			chunks: ['chunk-vendors', 'chunk-common', 'index']
		}
	},
	css: {
		loaderOptions: {
			less: {
				lessOptions: {globalVars}
			}
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
				'@components': path.resolve(__dirname, 'src/components/'),
				'@views': path.resolve(__dirname, 'src/views/'),
				'@common': path.resolve(__dirname, 'src/js/common/'),
				'@js': path.resolve(__dirname, 'src/js/')
			}
		},
		plugins: [new webpack.ProvidePlugin({})]
	},
	pluginOptions: {
		windicss: {}
	}
};
