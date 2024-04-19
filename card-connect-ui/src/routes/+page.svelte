<script lang="ts">
	import { MQTT_HOST, MQTT_PASSWORD, MQTT_USERNAME, REST_URL, REST_USERNAME, REST_PASSWORD } from "$lib/config";
	import CardPointe from "$lib/terminal/CardPointe.svelte";

</script>
<div class="flex items-center justify-center">
    <img src="CardConnect-fiserv-logo.png" alt="terminal"  width="125px"  />
</div>

<div class="flex mt-3 items-center justify-center">
    <div >
        <CardPointe brokerLabel="1" mqttHost="{MQTT_HOST}" mqttUsername="{MQTT_USERNAME}" mqttPassword="{MQTT_PASSWORD}"
        restURL= {REST_URL} restUsername= {REST_USERNAME} restPassword= {REST_PASSWORD}
        />
    </div>
</div>
