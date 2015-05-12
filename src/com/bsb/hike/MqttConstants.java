package com.bsb.hike;

import com.bsb.hike.utils.AccountUtils;

public class MqttConstants
{
	public static final String WAKE_LOCK_TAG = "MQTTWLock"; // Name of the MQTT Wake lock

	// constant used internally to schedule the next ping event
	public static final String MQTT_CONNECTION_CHECK_ACTION = "com.bsb.hike.PING";
	
	public static final String TCP_PROTOCOL = "tcp://";
	
	public static final String SSL_PROTOCOL = "ssl://";
	
	public static final String COLON = ":";

	public static final String PRODUCTION_BROKER_HOST_NAME = "mqtt.im.hike.in";

	public static final String STAGING_BROKER_HOST_NAME = AccountUtils.STAGING_HOST;

	public static final int PRODUCTION_BROKER_PORT_NUMBER = 8080;

	public static final int PRODUCTION_BROKER_PORT_NUMBER_SSL = 443;

	public static final int STAGING_BROKER_PORT_NUMBER = 1883;

	public static final int STAGING_BROKER_PORT_NUMBER_SSL = 8883;

	public static final int DEV_STAGING_BROKER_PORT_NUMBER = 1883;

	public static final int DEV_STAGING_BROKER_PORT_NUMBER_SSL = 8883;

	public static final int FALLBACK_BROKER_PORT_NUMBER_NON_SSL = 5222;

	public static final int FALLBACK_BROKER_PORT_NUMBER_SSL = 443;

	// this represents number of msgs published whose callback is not yet arrived
	public static final short MAX_INFLIGHT_MESSAGES_ALLOWED = 100;

	public static final short KEEP_ALIVE_SECONDS = HikeConstants.KEEP_ALIVE; // this is the time for which conn will remain open w/o messages

	public static final short CONNECTION_TIMEOUT_SECONDS = 60;

	/* Time after which a reconnect on mqtt thread is reattempted (Time in 'ms') */
	public static final short MQTT_WAIT_BEFORE_RECONNECT_TIME = 10;

	/*
	 * When disconnecting (forcibly) it might happen that some messages are waiting for acks or delivery. So before disconnecting,wait for this time to let mqtt finish the work and
	 * then disconnect w/o letting more msgs to come in.
	 */
	public static final short QUIESCE_TIME_MILLS = 500; // 500 milliseconds

	public static final short DISCONNECT_TIMEOUT = 1000; // 1 seconds (2 * QUIESCE_TIME_MILLS)

	public static final int MAX_RETRY_COUNT = 20;

	public static final String UNRESOLVED_EXCEPTION = "unresolved";

	/* publishes a message via mqtt to the server */
	public static int MQTT_QOS_ONE = 1;

	/* publishes a message via mqtt to the server with QoS 0 */
	public static int MQTT_QOS_ZERO = 0;

	/* represents max amount of time taken by message to process exceeding which we will send analytics to server */
	public static final long DEFAULT_MAX_MESSAGE_PROCESS_TIME = 1 * 1000l;

	// constants used to define MQTT connection status, this is used by external classes and hardly of any use internally
	public enum MQTTConnectionStatus
	{
		NOT_CONNECTED, // initial status
		CONNECTING, // attempting to connect
		CONNECTED, // connected
		NOT_CONNECTED_UNKNOWN_REASON // failed to connect for some reason
	}
	
	public enum HostState
	{
		STAGING_NON_SSL,
		STAGING_SSL,
		IP_NON_SSL,
		IP_SSL,
		FALLBACK_SSL,
		FALLBACK_NON_SSL,
		PRODUCTION_SSL,
		PRODUCTION_NON_SSL,
		CUSTOM_HOST
	}
}
