/**
 * manba → dayjs 兼容层
 * 覆盖本项目实际用到的 API：format / startOf / endOf / add + MONTH/DAY 常量
 */
import dayjs from 'dayjs'

function normalizeFormat(fmt) {
	if (!fmt) return fmt
	// manba 常用 YYYY-MM-dd，dayjs 需要 YYYY-MM-DD
	return fmt.replace(/dd/g, 'DD').replace(/yyyy/g, 'YYYY')
}

function wrap(d) {
	return {
		format(fmt) {
			return d.format(normalizeFormat(fmt))
		},
		startOf(unit) {
			return wrap(d.startOf(resolveUnit(unit)))
		},
		endOf(unit) {
			return wrap(d.endOf(resolveUnit(unit)))
		},
		add(value, unit) {
			return wrap(d.add(value, resolveUnit(unit)))
		},
		valueOf() {
			return d.valueOf()
		},
		toDate() {
			return d.toDate()
		}
	}
}

function resolveUnit(unit) {
	if (unit === manba.DAY || unit === 'day' || unit === 'd') return 'day'
	if (unit === manba.MONTH || unit === 'month' || unit === 'M') return 'month'
	if (unit === manba.YEAR || unit === 'year' || unit === 'y') return 'year'
	return unit
}

function manba(input) {
	if (input === undefined || input === null || input === '') {
		return wrap(dayjs())
	}
	return wrap(dayjs(input))
}

manba.DAY = 'day'
manba.MONTH = 'month'
manba.YEAR = 'year'

export default manba
