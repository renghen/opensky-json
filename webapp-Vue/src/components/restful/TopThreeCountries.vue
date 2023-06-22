<script setup>
import axios from 'axios';
import { ref } from 'vue';

let topThreeCountries = [{ count: 0, name: '' }]
const isRendered = ref(true);

async function getTopThreeCountries() {
    isRendered.value = false;

    await axios.get('http://127.0.0.1:9000/top3Countries')
        .then(function (response) {
            // handle success
            console.log(response);
            topThreeCountries = response.data;
        })
        .catch(function (error) {
            // handle error
            console.log(error);
        })
        .finally(function () {
            isRendered.value = true;            
        });
}
getTopThreeCountries();
setInterval(getTopThreeCountries, 30000); // Will fetch data each 30 seconds
</script>

<template>
    <div class="pb-5 px-10" v-if="isRendered">
        <h1 class="text-slate-700 font-bold text-2xl border-solid border-b-4 border-indigo-400 pb-3">Top 3 countries of
            origin since the application is running</h1>
        <ol class="mt-5 ml-5 list-decimal">
            <li class="mb-2" v-for="country in topThreeCountries">{{ country.name }} : {{ country.count }}</li>
        </ol>
    </div>
</template>