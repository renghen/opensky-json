<script>
import axios from 'axios';
import { ref } from 'vue';

export default {
    data() {
        return {
            flightData: [{ callsign: "", originCountry: "", time: 0, latitude: 0, baroAltitudeSlice: 0, baroAltitude: 0.0, verticalRate: 0.0, status: "Normal" }],
            listAltitudes: ref([
                { text: 'Select altitude', value: -1 },
                { text: '0 - 999m', value: 0 },
                { text: '1000 - 1999m', value: 1 },
                { text: '2000 - 2999m', value: 2 },
                { text: '3000 - 3999m', value: 3 },
                { text: '4000 - 4999m', value: 4 },
                { text: '5000 - 5999m', value: 5 }
            ]),
            selectedAltitude: null
        }
    },
    methods: {
        async getData() {
            await axios.get('http://127.0.0.1:9000/slice/' + this.selectedAltitude)
                .then((response) => {
                    // handle success
                    console.log(response);
                    this.flightData = response.data;
                })
                .catch(function (error) {
                    // handle error
                    console.log(error);
                })
                .finally(function () {
                });
        }
    },
    mounted() {
        this.selectedAltitude = ref(-1);
        this.getData();
        setInterval(this.getData, 10000); // Will fetch data each 30 seconds
    }
}
</script>



<template>
    <div class="py-0 px-10">
        <h1 class="text-slate-700 font-bold text-2xl border-solid border-b-4 border-indigo-400 pb-3">Flights in altitude
        </h1>

        <div class="flex w-full justify-left mt-5">
            <select v-model="selectedAltitude"
                class="bg-gray-50 border border-gray-300 text-gray-900 text-sm rounded-lg focus:ring-blue-500 focus:border-blue-500 block p-2.5 dark:bg-gray-700 dark:border-gray-600 dark:placeholder-gray-400 dark:text-white dark:focus:ring-blue-500 dark:focus:border-blue-500">
                <option v-for="option in listAltitudes" :value="option.value">{{ option.text }}</option>
            </select>
            <button @click="getData"
                class="ml-2 rounded-md bg-indigo-600 px-3.5 py-2.5 text-sm font-semibold text-white shadow-sm hover:bg-indigo-500 focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-indigo-600">Get
                data</button>
        </div>

        <table class="w-full text-sm text-left text-gray-500 dark:text-gray-400">
            <tr v-for="flight in flightData" class="bg-white border-b dark:bg-gray-800 dark:border-gray-700">
                <td scope="col" class="py-4 font-medium text-left text-gray-900 whitespace-nowrap dark:text-white">{{
                    flight.icao24 }}</td>
                <td scope="col" class="py-4 font-medium text-left text-gray-900 whitespace-nowrap dark:text-white">{{
                    flight.originCountry }}</td>
                <td scope="col" class="py-4 font-medium text-left text-gray-900 whitespace-nowrap dark:text-white">
                    <span v-if="flight.status == 'WARNING'" class="bg-red-800 text-white px-2 py-2 ">{{ flight.status }}</span>
                    <span v-if="flight.status == 'NORMAL'" class="bg-green-600 text-white px-2 py-2 ">{{ flight.status }}</span>
                </td>
                <td scope="col" class="py-4 font-medium text-left text-gray-900 whitespace-nowrap dark:text-white">{{
                    flight.baroAltitude }}</td>
            </tr>
        </table>
    </div>
</template>