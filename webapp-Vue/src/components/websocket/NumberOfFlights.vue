<script setup>
    let flights = [{ name: 'Flight 1' }, { name: 'Flight 2' }, { name: 'Flight 3' }];
    const socket = new WebSocket('ws://localhost:3000/');

    function getFlightData() {
        socket.onmessage = (event) => {
            this.flights = event.data;
        }
    }

    getFlightData();

</script>

<template>
    <div class="pb-5 px-10">
        <h1 class="text-slate-700 font-bold text-2xl border-solid border-b-4 border-indigo-400 pb-3">Number of flights above Netherlands/hour</h1>
        <ul class="mt-5">
            <li class="mb-2" v-for="flight in flights">{{ flight.name }}</li>
        </ul>
    </div>
</template>