<script setup>
let topThreeCountries = [{name: 'America'}, { name: 'Mauritius' }, { name: 'Somalia' }];
const socketTopThreeCountries = new WebSocket('ws://localhost:3000/');

function getTopThreeCountries() {
    socketTopThreeCountries.onmessage = (event) => {
        this.topThreeCountries = event.data;
    }
}

getTopThreeCountries();
</script>

<template>
    <div class="pb-5 px-10">
        <h1 class="text-slate-700 font-bold text-2xl border-solid border-b-4 border-indigo-400 pb-3">Top 3 countries of origin since the application is running</h1>
        <ol class="mt-5 ml-5 list-decimal">
            <li class="mb-2" v-for="country in topThreeCountries">{{ country.name }}</li>
        </ol>
    </div>
</template>