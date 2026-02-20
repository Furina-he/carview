<template>
  <el-dialog
    :model-value="visible"
    :title="isEdit ? '编辑围栏' : '新增围栏'"
    width="480px"
    @close="emit('update:visible', false)"
    class="fence-dialog"
    append-to-body
  >
    <el-form ref="formRef" :model="form" :rules="rules" label-width="90px" size="default">
      <el-form-item label="围栏名称" prop="fenceName">
        <el-input v-model="form.fenceName" placeholder="请输入围栏名称" />
      </el-form-item>

      <el-form-item label="围栏类型" prop="fenceType">
        <el-radio-group v-model="form.fenceType">
          <el-radio value="RECTANGLE">矩形</el-radio>
          <el-radio value="CIRCLE">圆形</el-radio>
        </el-radio-group>
      </el-form-item>

      <template v-if="form.fenceType === 'RECTANGLE'">
        <el-form-item label="最小经度" prop="minLng">
          <el-input-number v-model="form.minLng" :precision="6" :step="0.001" :controls="false" style="width: 100%" />
        </el-form-item>
        <el-form-item label="最小纬度" prop="minLat">
          <el-input-number v-model="form.minLat" :precision="6" :step="0.001" :controls="false" style="width: 100%" />
        </el-form-item>
        <el-form-item label="最大经度" prop="maxLng">
          <el-input-number v-model="form.maxLng" :precision="6" :step="0.001" :controls="false" style="width: 100%" />
        </el-form-item>
        <el-form-item label="最大纬度" prop="maxLat">
          <el-input-number v-model="form.maxLat" :precision="6" :step="0.001" :controls="false" style="width: 100%" />
        </el-form-item>
      </template>

      <template v-if="form.fenceType === 'CIRCLE'">
        <el-form-item label="中心经度" prop="centerLng">
          <el-input-number v-model="form.centerLng" :precision="6" :step="0.001" :controls="false" style="width: 100%" />
        </el-form-item>
        <el-form-item label="中心纬度" prop="centerLat">
          <el-input-number v-model="form.centerLat" :precision="6" :step="0.001" :controls="false" style="width: 100%" />
        </el-form-item>
        <el-form-item label="半径(米)" prop="radius">
          <el-input-number v-model="form.radius" :min="1" :step="100" :controls="false" style="width: 100%" />
        </el-form-item>
      </template>

      <el-form-item label="启用状态">
        <el-switch v-model="enabledBool" active-text="启用" inactive-text="禁用" />
      </el-form-item>
    </el-form>

    <template #footer>
      <el-button @click="emit('update:visible', false)">取消</el-button>
      <el-button type="primary" @click="handleSubmit" :loading="submitting">确定</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { createFence, updateFence, type FenceRule } from '@/api/fence'

const props = defineProps<{
  visible: boolean
  editData?: FenceRule | null
}>()

const emit = defineEmits<{
  'update:visible': [val: boolean]
  'success': []
}>()

const isEdit = computed(() => !!props.editData?.id)

const formRef = ref<FormInstance>()
const submitting = ref(false)

const defaultForm = (): FenceRule => ({
  fenceName: '',
  fenceType: 'RECTANGLE',
  minLng: undefined,
  minLat: undefined,
  maxLng: undefined,
  maxLat: undefined,
  centerLng: undefined,
  centerLat: undefined,
  radius: undefined,
  enabled: 1
})

const form = ref<FenceRule>(defaultForm())

const enabledBool = computed({
  get: () => form.value.enabled === 1,
  set: (val: boolean) => { form.value.enabled = val ? 1 : 0 }
})

watch(() => props.visible, (val) => {
  if (val) {
    if (props.editData) {
      form.value = { ...props.editData }
    } else {
      form.value = defaultForm()
    }
    formRef.value?.clearValidate()
  }
})

const rules: FormRules = {
  fenceName: [{ required: true, message: '请输入围栏名称', trigger: 'blur' }],
  fenceType: [{ required: true, message: '请选择围栏类型', trigger: 'change' }],
  minLng: [{ required: true, message: '请输入最小经度', trigger: 'blur' }],
  minLat: [{ required: true, message: '请输入最小纬度', trigger: 'blur' }],
  maxLng: [{ required: true, message: '请输入最大经度', trigger: 'blur' }],
  maxLat: [{ required: true, message: '请输入最大纬度', trigger: 'blur' }],
  centerLng: [{ required: true, message: '请输入中心经度', trigger: 'blur' }],
  centerLat: [{ required: true, message: '请输入中心纬度', trigger: 'blur' }],
  radius: [{ required: true, message: '请输入半径', trigger: 'blur' }]
}

async function handleSubmit() {
  if (!formRef.value) return
  await formRef.value.validate()
  submitting.value = true
  try {
    if (isEdit.value) {
      await updateFence(form.value.id!, form.value)
      ElMessage.success('围栏更新成功')
    } else {
      await createFence(form.value)
      ElMessage.success('围栏创建成功')
    }
    emit('update:visible', false)
    emit('success')
  } catch {
    // error handled by interceptor
  } finally {
    submitting.value = false
  }
}
</script>

<style lang="scss">
.fence-dialog {
  .el-dialog {
    background: rgba(20, 30, 60, 0.95);
    border: 1px solid rgba(0, 242, 255, 0.2);
  }
  .el-dialog__header {
    border-bottom: 1px solid rgba(0, 242, 255, 0.1);
  }
  .el-dialog__title {
    color: #e6f7ff;
  }
}
</style>
