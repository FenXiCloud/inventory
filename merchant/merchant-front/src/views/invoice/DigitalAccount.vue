<template>
  <div class="digital-account">
    <div class="toolbar">
      <t-space break-line>
        <t-button style="border-radius:4px" :loading="loading" @click="refresh">刷新状态</t-button>
        <t-button theme="primary" style="border-radius:4px" :loading="loginLoading" :disabled="!hasAccount" @click="doLogin">登录</t-button>
        <t-button style="border-radius:4px" :disabled="!hasAccount" @click="openScan">扫码认证</t-button>
        <t-button style="border-radius:4px" :disabled="!hasAccount" @click="openDuration">扫脸时长设置</t-button>
      </t-space>
    </div>

    <!-- 未开通：引导订购 -->
    <div v-if="!hasAccount" class="setup-card">
      <div class="setup-title">开通数电账户</div>
      <div class="setup-form">
        <t-input v-model="setupForm.aggOrgName" clearable placeholder="企业名称" style="width:280px"/>
        <t-input v-model="setupForm.nsrsbh" clearable placeholder="纳税人识别号" style="width:280px"/>
        <t-button theme="primary" :loading="setupLoading" @click="doSetup">订购并开通</t-button>
      </div>
      <div class="setup-hint">开通后需依次完成「登录 → 扫码认证」才能开具发票。</div>
    </div>

    <!-- 已开通：状态展示 -->
    <div v-else class="state-card">
      <t-descriptions :column="2" bordered :items="stateItems"/>
    </div>

    <!-- 短信验证 -->
    <t-dialog v-model:visible="smsDialog.visible" header="短信验证" width="420px" :footer="false" :close-on-overlay-click="false">
      <div class="sms-hint">请输入手机收到的短信验证码</div>
      <t-input v-model="smsDialog.code" placeholder="短信验证码" @enter="doSmsSubmit"/>
      <div class="dialog-footer">
        <t-button @click="smsDialog.visible = false">取消</t-button>
        <t-button theme="warning" :loading="smsDialog.loading" @click="doSmsSubmit">提交验证码</t-button>
      </div>
    </t-dialog>

    <!-- 扫码认证 -->
    <t-dialog v-model:visible="scanDialog.visible" header="扫码认证" width="420px" :footer="false" :close-on-overlay-click="false" @close="closeScan">
      <div class="scan-body">
        <template v-if="!scanDialog.qrImageUrl">
          <t-button theme="primary" :loading="scanDialog.loading" @click="doGetQr">获取二维码</t-button>
        </template>
        <template v-else>
          <img :src="scanDialog.qrImageUrl" class="qr-img" alt="二维码"/>
          <p class="qr-hint">请用税务APP扫描二维码</p>
          <t-space>
            <t-button theme="success" size="small" @click="doCheckScan">检测扫脸结果</t-button>
            <t-button size="small" :loading="scanDialog.loading" @click="doGetQr">重新获取</t-button>
          </t-space>
          <p v-if="scanDialog.polling" class="polling-text">自动检测中...</p>
        </template>
      </div>
    </t-dialog>

    <!-- 时长设置 -->
    <t-dialog v-model:visible="durationDialog.visible" header="扫脸时长设置" width="420px" :footer="false">
      <t-form label-width="110px" :colon="false">
        <t-form-item label="当前间隔">{{ durationDialog.currentText }}</t-form-item>
        <t-form-item label="设置间隔(天)">
          <t-input-number v-model="durationDialog.days" :min="1" :max="365" theme="column" style="width:180px"/>
        </t-form-item>
      </t-form>
      <div class="dialog-footer">
        <t-button @click="durationDialog.visible = false">取消</t-button>
        <t-button theme="primary" :loading="durationDialog.loading" @click="doSetDuration">设置</t-button>
      </div>
    </t-dialog>
  </div>
</template>

<script>
import Auth from '@js/api/invoice/Auth';
import {MessagePlugin} from 'tdesign-vue-next';

export default {
  name: 'DigitalAccount',
  data() {
    return {
      loading: false,
      loginLoading: false,
      setupLoading: false,
      state: {},
      days: 0,
      setupForm: {aggOrgName: '', nsrsbh: ''},
      smsDialog: {visible: false, loading: false, code: '', taskId: ''},
      scanDialog: {visible: false, loading: false, qrImageUrl: '', polling: false},
      durationDialog: {visible: false, loading: false, days: 90, currentText: '—'}
    };
  },
  computed: {
    hasAccount() {
      return !!this.state.aggOrgId;
    },
    stateItems() {
      const s = this.state;
      return [
        {label: '登录身份', content: s.roleName || '—'},
        {label: '登录人姓名', content: s.realName || '—'},
        {label: '登录手机号', content: s.phone || '—'},
        {label: '纳税人识别号', content: s.nsrsbh || '—'},
        {label: '登录状态', content: s.loggedIn ? '已登录' : '未登录'},
        {label: '扫码验证状态', content: s.qrRzid ? '已验证' : '未验证'},
        {label: '扫脸间隔', content: this.days ? `${this.days} 天` : '—'},
        {label: '最后扫码时间', content: s.lastScanTime || '—'}
      ];
    }
  },
  methods: {
    refresh() {
      this.loadState();
      this.loadDuration();
    },
    loadState() {
      this.loading = true;
      Auth.state()
        .then(({data}) => {
          this.state = data || {};
        })
        .catch(() => {
          this.state = {};
        })
        .finally(() => (this.loading = false));
    },
    loadDuration() {
      Auth.scanDuration()
        .then(({data}) => {
          if (data && data.currentSeconds) {
            this.days = Math.round(data.currentSeconds / 86400);
          }
        })
        .catch(() => {});
    },
    doSetup() {
      if (!this.setupForm.aggOrgName.trim()) {
        MessagePlugin.warning('请输入企业名称');
        return;
      }
      if (!this.setupForm.nsrsbh.trim()) {
        MessagePlugin.warning('请输入纳税人识别号');
        return;
      }
      this.setupLoading = true;
      Auth.purchase({aggOrgName: this.setupForm.aggOrgName.trim(), nsrsbh: this.setupForm.nsrsbh.trim(), dq: '33'})
        .then(() => Auth.account())
        .then(() => {
          MessagePlugin.success('开通成功，请继续登录');
          this.refresh();
        })
        .finally(() => (this.setupLoading = false));
    },
    doLogin() {
      this.loginLoading = true;
      Auth.login()
        .then(({data}) => {
          if (data && data.success) {
            MessagePlugin.success(data.message || '登录成功');
            this.refresh();
          } else if (data && data.taskId) {
            this.smsDialog.taskId = data.taskId;
            this.smsDialog.code = '';
            this.smsDialog.visible = true;
            MessagePlugin.info('需要短信验证码');
          } else {
            MessagePlugin.warning((data && data.message) || '登录异常');
          }
        })
        .finally(() => (this.loginLoading = false));
    },
    doSmsSubmit() {
      if (!this.smsDialog.code.trim()) {
        MessagePlugin.warning('请输入短信验证码');
        return;
      }
      this.smsDialog.loading = true;
      Auth.sms(this.smsDialog.taskId, this.smsDialog.code.trim())
        .then(({data}) => {
          if (data && data.success) {
            MessagePlugin.success('短信验证成功');
            this.smsDialog.visible = false;
            this.refresh();
          } else {
            MessagePlugin.warning((data && data.message) || '验证失败');
          }
        })
        .finally(() => (this.smsDialog.loading = false));
    },
    openScan() {
      this.scanDialog.qrImageUrl = '';
      this.scanDialog.polling = false;
      this.scanDialog.visible = true;
    },
    closeScan() {
      if (this.scanPollTimer) {
        clearInterval(this.scanPollTimer);
        this.scanPollTimer = null;
      }
      this.scanDialog.polling = false;
    },
    doGetQr() {
      this.scanDialog.loading = true;
      Auth.qr()
        .then(() => {
          this.scanDialog.qrImageUrl = Auth.qrImageUrl();
          this.startPolling();
        })
        .finally(() => (this.scanDialog.loading = false));
    },
    startPolling() {
      if (this.scanPollTimer) clearInterval(this.scanPollTimer);
      this.scanDialog.polling = true;
      this.scanPollTimer = setInterval(() => {
        Auth.qrResult()
          .then(({data}) => {
            if (data && data.success) {
              this.stopPolling();
              MessagePlugin.success('人脸识别成功');
              this.scanDialog.visible = false;
              this.refresh();
            }
          })
          .catch(() => {});
      }, 3000);
    },
    stopPolling() {
      if (this.scanPollTimer) {
        clearInterval(this.scanPollTimer);
        this.scanPollTimer = null;
      }
      this.scanDialog.polling = false;
    },
    doCheckScan() {
      Auth.qrResult()
        .then(({data}) => {
          if (data && data.success) {
            this.stopPolling();
            MessagePlugin.success('人脸识别成功');
            this.scanDialog.visible = false;
            this.refresh();
          } else {
            MessagePlugin.info('尚未扫脸，轮询继续中...');
          }
        })
        .catch(() => {});
    },
    openDuration() {
      this.durationDialog.days = this.days || 90;
      this.durationDialog.currentText = `约${this.durationDialog.days}天`;
      this.durationDialog.visible = true;
    },
    doSetDuration() {
      this.durationDialog.loading = true;
      Auth.setScanDuration(this.durationDialog.days * 86400)
        .then(() => {
          MessagePlugin.success('设置成功');
          this.durationDialog.visible = false;
          this.loadDuration();
        })
        .finally(() => (this.durationDialog.loading = false));
    }
  },
  created() {
    this.refresh();
  },
  beforeUnmount() {
    if (this.scanPollTimer) clearInterval(this.scanPollTimer);
  }
};
</script>

<style scoped>
.digital-account {
  height: 100%;
  min-height: 0;
  background: #fff;
  border-radius: 4px;
  padding: 12px;
  box-sizing: border-box;
  overflow: auto;
}

.toolbar {
  margin-bottom: 12px;
}

.setup-card {
  border: 1px solid var(--td-component-border, #dcdcdc);
  border-radius: 6px;
  padding: 16px;
}

.setup-title {
  font-weight: 600;
  margin-bottom: 12px;
}

.setup-form {
  display: flex;
  gap: 8px;
  align-items: center;
  flex-wrap: wrap;
}

.setup-hint {
  margin-top: 10px;
  font-size: 12px;
  color: #909399;
}

.state-card {
  max-width: 720px;
}

.sms-hint {
  margin-bottom: 12px;
  color: #606266;
  font-size: 14px;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 16px;
}

.scan-body {
  text-align: center;
}

.qr-img {
  width: 200px;
  height: 200px;
  border: 1px solid #ebeef5;
  border-radius: 8px;
}

.qr-hint {
  font-size: 12px;
  color: #909399;
  margin: 8px 0;
}

.polling-text {
  color: #0052d9;
  font-size: 13px;
  margin-top: 6px;
}
</style>
