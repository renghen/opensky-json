<script>
import axios from 'axios';

export default {
    data() {        
        return {
            flight : { count: 1 }
        };
    },
    methods: {
        async getFlightData() {
            await axios.get('http://127.0.0.1:9000/overNetherlandsforlastHour')
                .then(response => {
                    // handle success
                    console.log(response);
                    this.flight = response.data;
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
        this.getFlightData();
        setInterval(this.getFlightData, 10000); // Will fetch data each 10 seconds
    }
}


</script>

<template>
    <div class="pb-5 px-10">
        <h1 :key="flight.count" class="text-slate-700 font-bold text-2xl border-solid border-b-4 border-indigo-400 pb-3">Number
            of flights above Netherlands in last hour {{ flight.count }}</h1>
    </div>
</template>