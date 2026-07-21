/**
 * 命令式打开 TDesign Dialog / Drawer（带主应用 appContext，保证全局组件可用）
 *
 * openDialog({ header, body, width, closeOnOverlayClick, closeBtn })
 * openDrawer({ header, body, size, closeOnOverlayClick })
 * closeDialog(id)
 */
import {createVNode, defineComponent, render, ref, watch} from 'vue'
import {Dialog as TDialog, Drawer as TDrawer} from 'tdesign-vue-next'

let appContext = null
let seed = 1
const instances = new Map()

export function setupDialog(app) {
	appContext = app._context
}

function mountHost(Host, props) {
	const id = seed++
	const container = document.createElement('div')
	document.body.appendChild(container)

	const destroy = () => {
		render(null, container)
		if (container.parentNode) {
			container.parentNode.removeChild(container)
		}
		instances.delete(id)
	}

	const vnode = createVNode(Host, {
		...props,
		onClosed: destroy
	})
	vnode.appContext = appContext
	render(vnode, container)

	const api = {
		id,
		close: () => {
			if (vnode.component?.exposed?.close) {
				vnode.component.exposed.close()
			} else {
				destroy()
			}
		},
		destroy
	}
	instances.set(id, api)
	return id
}

const DialogHost = defineComponent({
	name: 'AppDialogHost',
	props: {
		header: String,
		body: {required: true},
		width: {type: [String, Number], default: '520px'},
		placement: {type: String, default: 'center'},
		closeOnOverlayClick: {type: Boolean, default: true},
		closeBtn: {type: [Boolean, Object], default: true}
	},
	emits: ['closed'],
	setup(props, {emit, expose}) {
		const visible = ref(true)
		const close = () => {
			visible.value = false
		}

		watch(visible, (v) => {
			if (!v) setTimeout(() => emit('closed'), 200)
		})

		expose({close})

		return () => createVNode(TDialog, {
			visible: visible.value,
			'onUpdate:visible': (v) => {
				visible.value = v
			},
			header: props.header,
			footer: false,
			width: props.width,
			placement: props.placement,
			closeOnOverlayClick: props.closeOnOverlayClick,
			closeBtn: props.closeBtn,
			destroyOnClose: true,
			attach: 'body',
			dialogClassName: 'app-dialog-flush',
			onClose: close
		}, {
			default: () => props.body
		})
	}
})

const DrawerHost = defineComponent({
	name: 'AppDrawerHost',
	props: {
		header: String,
		body: {required: true},
		size: {type: [String, Number], default: '40%'},
		closeOnOverlayClick: {type: Boolean, default: true}
	},
	emits: ['closed'],
	setup(props, {emit, expose}) {
		const visible = ref(true)
		const close = () => {
			visible.value = false
		}

		watch(visible, (v) => {
			if (!v) setTimeout(() => emit('closed'), 200)
		})

		expose({close})

		return () => createVNode(TDrawer, {
			visible: visible.value,
			'onUpdate:visible': (v) => {
				visible.value = v
			},
			header: props.header,
			footer: false,
			size: props.size,
			closeOnOverlayClick: props.closeOnOverlayClick,
			destroyOnClose: true,
			attach: 'body',
			onClose: close
		}, {
			default: () => props.body
		})
	}
})

export function openDialog(options = {}) {
	return mountHost(DialogHost, {
		header: options.header,
		body: options.body,
		width: options.width ?? '520px',
		placement: options.placement ?? 'center',
		closeOnOverlayClick: options.closeOnOverlayClick ?? true,
		closeBtn: options.closeBtn !== undefined ? options.closeBtn : true
	})
}

export function openDrawer(options = {}) {
	return mountHost(DrawerHost, {
		header: options.header,
		body: options.body,
		size: options.size ?? '40%',
		closeOnOverlayClick: options.closeOnOverlayClick ?? true
	})
}

export function closeDialog(id) {
	const inst = instances.get(id)
	if (inst) inst.close()
}

