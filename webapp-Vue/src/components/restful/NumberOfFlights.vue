<script setup>
import axios from 'axios';
import { ref } from 'vue';

let flights = { "count": 1 };
const isRendered = ref(true);

async function getFlightData() {
    isRendered.value = false;

    await axios.get('http://127.0.0.1:9000/overNetherlandsforlastHour')
        .then(function (response) {
            // handle success
            console.log(response);
            flights = response.data;
        })
        .catch(function (error) {
            // handle error
            console.log(error);
        })
        .finally(function () {
            isRendered.value = true;
        });

}

getFlightData();
setInterval(getFlightData, 30000); // Will fetch data each 30 seconds
</script>

<template>
    <div class="pb-5 px-10" v-if="isRendered">
        <h1 class="text-slate-700 font-bold text-2xl border-solid border-b-4 border-indigo-400 pb-3">Number of flights above Netherlands in last hour {{ flights.count }}</h1>        
    </div>
</template>