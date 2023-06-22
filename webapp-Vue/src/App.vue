<script setup>
   import HeaderComponent from './components/HeaderComponent.vue'
   import CalculationsComponentWebSocket from './components/CalculationsComponentWebSocket.vue'
   import CalculationsComponentREST from './components/CalculationsComponentREST.vue'
</script>

<template>
  <HeaderComponent />
  <!-- <CalculationsComponentWebSocket /> -->
  <CalculationsComponentREST />
</template>

<style scoped>

</style>
