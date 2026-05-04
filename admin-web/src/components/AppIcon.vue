<script setup lang="ts">
/** 应用图标组件：根据图标名称从图标注册表中查找对应的 FontAwesome 图标定义并渲染 */
import { computed } from 'vue'
import { iconRegistry } from '@/config/icons'
import type { IconKey } from '@/types'

const props = defineProps<{
  name: IconKey
}>()

const icon = computed(() => iconRegistry[props.name])
</script>

<template>
  <FontAwesomeIcon :icon="icon" />
</template>
