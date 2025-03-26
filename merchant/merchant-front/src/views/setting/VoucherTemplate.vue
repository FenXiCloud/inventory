<template>
  <div class="modal-column">
    <div>
      <vxe-button @click="showForm()" status="primary" class="float-right mt-4px mr-16px">新 增</vxe-button>
    </div>
    <div class="flex-1 h-0px p-16px">
      <div class="border p-8px text-12px mb-16px" v-for="(items,key) in dataList" :key="key">
        <div class="flex justify-between">
          <span class="font-bold">{{ key }}</span>
          <div class="actions">
            <span v-p="'cash-journalvouchertemplate-canedit'" @click="showForm(key)">编辑</span>
            <span v-p="'cash-journalvouchertemplate-candelete'" @click="doTrash(key)">删除</span>
          </div>
        </div>
        <div class="mt-16px">
          <div class="my-8px ml-16px" v-for="item in items" :key="item.id">
            {{ item.direction }}：{{ item.subjectTitle }}
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>

export default {
  name: "VoucherTemplate"
}
</script>

<script setup>


</script>
