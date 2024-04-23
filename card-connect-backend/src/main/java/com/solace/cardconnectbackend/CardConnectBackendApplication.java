package com.solace.cardconnectbackend;

import com.solacesystems.jcsmp.Topic;
import com.solacesystems.jcsmp.JCSMPFactory;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.stream.binder.BinderHeaders;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.context.annotation.Bean;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

import java.util.function.Consumer;
import java.util.function.Function;

@SpringBootApplication
public class CardConnectBackendApplication {
	private static final Logger logger = LoggerFactory.getLogger(CardConnectBackendApplication.class);

	@Value("process/${process.id}")
	private String replyTo;
	private static final String REPLYTO_DESTINATION_KEY = "solace_replyTo";

	public static void main(String[] args) {
		SpringApplication.run(CardConnectBackendApplication.class, args);
	}

	/*
	@Bean
	public Consumer<Message<String>> initializeTransaction(StreamBridge sb){
		return v -> {
			logger.info("Received: " + v.getPayload());
			logger.info("All Headers: " + v.getHeaders());
			JSONObject obj = new JSONObject(v.getPayload());

			String deviceid = obj.getString("terminalId");
			String type = obj.getString("type");
			int amount = obj.getInt("amount");

			String topic = "terminal/".concat(deviceid).concat("/tranaction/").concat(type);
			logger.info("Publishing to: " + topic);
			JSONObject respObj = new JSONObject();
			respObj.put("transactionId",java.util.UUID.randomUUID());
			respObj.put("amount", amount);

			sb.send(topic, respObj.toString());
		};
	}
	*/

	@Bean
	public Function<Message<String>, Message<String>> initializeTransaction() {
		return v -> {
			logger.info("Received: " + v.getPayload());
			logger.info("All Headers: " + v.getHeaders());
			JSONObject obj = new JSONObject(v.getPayload());

			String deviceid = obj.getString("terminalId");
			String transType = obj.getString("type");
			float amount = obj.getFloat("amount");

			String topic = "terminal/".concat(deviceid);
			Topic replyToTopic = JCSMPFactory.onlyInstance().createTopic(replyTo);
			logger.info("Publishing to: " + topic);
			JSONObject respObj = new JSONObject();
			respObj.put("transactionId", java.util.UUID.randomUUID());
			respObj.put("amount", amount);
			respObj.put("cardTransactionType", transType);
			//sb.send(topic, respObj.toString());
			MessageBuilder<String> builder;
			builder = (MessageBuilder<String>) MessageBuilder.withPayload(respObj.toString()).setHeader(BinderHeaders.TARGET_DESTINATION, topic)
					.setHeader(REPLYTO_DESTINATION_KEY, replyToTopic);
			return builder.build();
		};
	}

	@Bean
	public Function<Message<String>, Message<String>> processTransaction() {
		return v -> {
			JSONObject respObj = new JSONObject();
			Topic replyToTopic = JCSMPFactory.onlyInstance().createTopic(replyTo);
			Object destinationTopic = v.getHeaders().get(REPLYTO_DESTINATION_KEY);

				logger.info("Received: " + v.getPayload());
				logger.info("All Headers: " + v.getHeaders());

				JSONObject reqObj = new JSONObject(v.getPayload());
				String transType = reqObj.getString("cardTransactionType");

				respObj.put("transactionId", reqObj.get("transactionId"));
				respObj.put("cardTransactionType", transType);

				if ("AUTH_CAPTURE".equals(transType)) {
					logger.info("Message received is of type AUTH_CAPTURE");
					respObj.put("processingStatus", "000");

				} else if ("AUTH_ONLY".equals(transType)) {
					respObj.put("processingStatus", "000");
					respObj.put("amount", reqObj.get("amount"));
					JSONObject cardHolderInfo = new JSONObject();
					cardHolderInfo.put("firstName", reqObj.getJSONObject("cardHolderInfo").getString("firstName"));
					cardHolderInfo.put("lastName", reqObj.getJSONObject("cardHolderInfo").getString("lastName"));
					cardHolderInfo.put("zip", reqObj.getJSONObject("cardHolderInfo").getString("zip"));
					respObj.put("cardHolderInfo", cardHolderInfo);

				} else if ("CAPTURE".equals(transType)) {
					respObj.put("processingStatus", "000");
					respObj.put("amount", reqObj.get("amount"));
				} else {
					respObj.put("processingStatus", "001");
				}

			logger.info("Response Message will be sent to ", replyToTopic.toString());
			MessageBuilder<String> builder;
			builder = (MessageBuilder<String>) MessageBuilder.withPayload(respObj.toString()).setHeader(BinderHeaders.TARGET_DESTINATION, destinationTopic.toString()).
					setHeader(REPLYTO_DESTINATION_KEY, replyToTopic);
			return builder.build();
		};
	}

}
