<script lang="ts">
	import { json } from "@sveltejs/kit";
    import mqtt from "mqtt";
	import { onMount } from "svelte";
    import { v4 as uuidv4 } from 'uuid';

    export let brokerLabel: string;
    export let mqttHost: string;
    export let mqttUsername: string;
    export let mqttPassword: string;

    export let restURL: string;
    export let restUsername: string;
    export let restPassword: string;

    let deviceId =  uuidv4().toString(); 
    let terminalDisplayText = "";

    const terminalLog = (message:string) => {
        let now = new Date();
        terminalDisplayText = `[${now.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', second: '2-digit' })}] ${message}\n${terminalDisplayText}`;
    }


    const postRESTTransaction = () => {
        fetch(`${restURL}/cardpointe/terminal/${deviceId}/begin_tran`, {
            method: "POST",
            headers: {
            "Content-Type": "application/json",
            "Authorization": "Basic " + btoa(`${restUsername}:${restPassword}`),
            },
            body: JSON.stringify({
            terminalId: deviceId,
            type: "BEGIN_TRAN",
            amount: "100.00",
            })
        })
        .then(data => {
            terminalLog("Requesting start of transaction...");
        })
        .catch(error => {
            console.log("Error starting transaction: ", error);
        });
        }
   

    onMount(()=>{
        const client = mqtt.connect(mqttHost, {
            username: mqttUsername,
            password: mqttPassword,
            clientId: deviceId,
            protocolVersion: 5
        });

        client.on("connect", () => {
           terminalLog(`Connected to Solace Broker ${brokerLabel} over MQTT with device id ${deviceId}!`);

            client.subscribe("cardpointe/terminal/"+deviceId+"/transaction/#", err =>{
                // terminalLog(`Subscribed to topic terminal/${deviceId}`);
                   if(err) console.log("Error subscribing to topic: ", err);
            });
        });

        client.on("message", (topic, message, packet) => {
            //reply-to
            const responseTopic = packet.properties?.responseTopic;
            const jsonMsg = JSON.parse(message.toString());
            console.log(jsonMsg)
            
            
            if ("BEGIN_TRAN" == jsonMsg.cardTransactionType) {
                // The first message in the flow will be a response to the start transaction
                // Need to construct a AUTH_CAPTURE message + replyTo header.
                terminalLog("****** Transaction "+jsonMsg.transactionId+" started. ******");

                var options= {
                    qos: 1,
                    properties: {responseTopic: "cardpointe/terminal/"+deviceId+'/transaction/#'}
                };
                
                let msg = JSON.stringify(
                    {
                    terminalId: deviceId,
                    cardTransactionType: "AUTH_CAPTURE",
                    amount: "100.00",
                    tipAmount : "25.00",
                    transactionId : jsonMsg.transactionId
                    });
                terminalLog("Publish CC message to backend "+responseTopic+" "+msg);
                client.publish(responseTopic, msg, options);
            } else if ("AUTH_CAPTURE" == jsonMsg.cardTransactionType) {
                if ("000" == jsonMsg.processingStatus) {
                    terminalLog("Received processing success");
                    terminalLog(message.toString());
                    terminalLog("****** End of Transaction ******");
                } else {
                    terminalLog("Error in processing CC");
                }
            } else {
                terminalLog("Error: Unknown message from backend");
            }
        });

      
    });
</script>

<div class="flex justify-center">
    <button class="bg-green-500 hover:bg-green-700 text-white font-bold py-2 px-4 rounded-full" on:click={postRESTTransaction}>
        Start POS transaction for Terminal
    </button>
</div>
<div class="flex items-center justify-center relative">

    <img src="card-pointe-terminal.png" alt="terminal"  width="450px" height="500px" />
    <div class="absolute top-55 left-62 w-76 h-47 bg-white bg-opacity-50 terminalDisplay" style="overflow: scroll;">{terminalDisplayText}</div>

</div>


<style>
.top-55 {
    top: 13.5rem;
}

.w-76{
    width: 18.2rem;
}

.h-47 {
    height: 11.8rem;
}

.terminalDisplay {
    white-space: pre-wrap;
    border: none;
    font-family: Monospace;
    font-size: x-small;

    outline: none;
    -moz-outline-style: none;
    overflow-wrap: anywhere;
}
</style>