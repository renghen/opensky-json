<script setup>
import NumberOfFlights from './websocket/NumberOfFlights.vue'
import TopThreeCountries from './websocket/TopThreeCountries.vue'
import FlightsInAltitude from './websocket/FlightsInAltitude.vue'
</script>

<template>
    <div class="max-2xl">
        <NumberOfFlights />
        <TopThreeCountries />
        <FlightsInAltitude />
    </div>
</template>

<style scoped lang="scss">

</style>
