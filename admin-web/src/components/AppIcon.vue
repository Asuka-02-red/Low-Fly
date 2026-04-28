<script setup lang="ts">
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
