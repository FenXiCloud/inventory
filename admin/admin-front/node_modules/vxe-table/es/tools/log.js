import GlobalConfig from '../v-x-e-table/src/conf';
export function getLog(message, params) {
    return `[vxe-table v${"4.6.25"}] ${GlobalConfig.i18n(message, params)}`;
}
function outLog(type) {
    return function (message, params) {
        const msg = getLog(message, params);
        console[type](msg);
        return msg;
    };
}
export const warnLog = outLog('warn');
export const errLog = outLog('error');
