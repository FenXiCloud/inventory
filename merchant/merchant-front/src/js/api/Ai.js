import Ajax from "@common/Request";

export default {
	recognizeOrder(text) {
		return Ajax.post('/ai/recognize-order', {text});
	}
}
