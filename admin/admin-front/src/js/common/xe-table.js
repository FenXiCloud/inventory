/**
 * @功能描述: vxetable
 * @创建时间: 2023年08月08日
 * @公司官网: www.fenxi365.com
 * @公司信息: 纷析云（杭州）科技有限公司
 * @公司介绍: 专注于财务相关软件开发, 企业会计自动化解决方案
 */
import XEUtils, {get, toFormatString} from 'xe-utils'
import {Colgroup, Column, Edit, Filter, Icon, Input, Keyboard, Option, Pager, Select, Table, Toolbar, Tooltip, Validator, VXETable} from 'vxe-table'
import zhCN from 'vxe-table/lib/locale/lang/zh-CN'
import  'vxe-table/styles/cssvar.scss'

VXETable.setup({
	size: "mini",
	i18n: (key, args) => toFormatString(get(zhCN, key), args),
})

VXETable.formats.mixin({
	formatDate({cellValue}, format) {
		return XEUtils.toDateString(cellValue, format || 'yyyy-MM-dd')
	},
	formatMonth({cellValue}, format) {
		return XEUtils.toDateString(cellValue, format || 'yyyy-MM')
	},
	// 四舍五入,6位数
	formatFixedNumber({cellValue}, digits = 6) {
		return XEUtils.toFixed(XEUtils.round(cellValue, digits), digits)
	},
})

export const useTable = (app) => {
	app.use(Table)
	app.use(Column)
	app.use(Colgroup)
	app.use(Tooltip)
	app.use(Filter)
	app.use(Icon)
	app.use(Validator)
	app.use(Keyboard)
	app.use(Edit)
	app.use(Select)
	app.use(Input)
	app.use(Option)
	app.use(Toolbar)
	app.use(Pager)
}
