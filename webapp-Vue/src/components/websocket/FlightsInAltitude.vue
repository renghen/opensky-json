<script setup>
import { ref } from 'vue';

let selectedAltitude = ref(-1);
let listAltitudes = ref([
    { text: 'Select altitude', value: -1 },
    { text: '0 - 999m', value: 0 },
    { text: '1000 - 1999m', value: 1 },
    { text: '2000 - 2999m', value: 2 },
    { text: '3000 - 3999m', value: 3 },
    { text: '4000 - 4999m', value: 4 },
    { text: '5000 - 5999m', value: 5 }
]);

let flightData = [{name: "Flight 1"}, { name: "Flight 2" }, { name: "Flight 3" }];

const socketFlight = new WebSocket('ws://localhost:3000/');

function getData() {
    console.log("getData triggered");

    socketFlight.onmessage = (event) => {
        flightData = event.data;
    }
}

getData();

</script>

<template>
    <div class="py-0 px-10">
        <h1 class="text-slate-700 font-bold text-2xl border-solid border-b-4 border-indigo-400 pb-3">Flights in altitude</h1>
        
        <div class="flex w-full justify-left mt-5">
            <select v-model="selectedAltitude" id="countries" class="bg-gray-50 border border-gray-300 text-gray-900 text-sm rounded-lg focus:ring-blue-500 focus:border-blue-500 block p-2.5 dark:bg-gray-700 dark:border-gray-600 dark:placeholder-gray-400 dark:text-white dark:focus:ring-blue-500 dark:focus:border-blue-500">
                <option v-for="option in listAltitudes" :value="option.value">{{ option.text }}</option>
            </select>
            <button @click="getData" class="ml-2 rounded-md bg-indigo-600 px-3.5 py-2.5 text-sm font-semibold text-white shadow-sm hover:bg-indigo-500 focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-indigo-600">Get data</button>
        </div>
        
        <ul class="mt-5">
            <li v-for="flight in flightData">{{ flight.name }}</li>
        </ul>
    </div>
</template>