package edu.mit.appinventor.raspberrypi.gpio;

import java.nio.charset.StandardCharsets;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.pi4j.io.gpio.GpioController;
import com.pi4j.io.gpio.GpioFactory;
import com.pi4j.io.gpio.GpioPinDigitalOutput;
import com.pi4j.io.gpio.PinState;
import com.pi4j.io.gpio.RaspiPin;

import edu.mit.mqtt.raspberrypi.Messages.PinProperty;
import edu.mit.mqtt.raspberrypi.Messages.PinValue;
import edu.mit.mqtt.raspberrypi.Pin;
import edu.mit.mqtt.raspberrypi.Topics;

public class DisplayOutput implements MqttCallback {

  MqttClient mClient;

  final String TEMPERATURE = "temperature";

  // create gpio controller
  final GpioController gpio = GpioFactory.getInstance();

  // provision gpio pin #02 as an output pin and turn on
  final GpioPinDigitalOutput tempLowIndicator = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_02, "BlueLED",
      PinState.LOW);

  public DisplayOutput() {
  }

  public void messageArrived(String pTopic, MqttMessage pMessage) throws Exception {
    if (pTopic.equals(TEMPERATURE)) {
      String temperatureString = new String(pMessage.getPayload(), StandardCharsets.UTF_8);
      int temperature = Integer.parseInt(temperatureString);
      if (temperature > 80) {
	System.out.println("FIRE FIRE!!!!!! " + new String(pMessage.getPayload()));
      } else if (temperature < 40) {
	tempLowIndicator.high();
	System.out.println("BRRRR Freezing !!!!!! " + new String(pMessage.getPayload()));
      } else {
	System.out.println("Temperature Normal. " + new String(pMessage.getPayload()));
      }
    } else if (pTopic.equals(Topics.INTERNAL_TOPIC)) {
      GsonBuilder builder = new GsonBuilder();
      Gson gson = builder.create();
      Pin pinFromJson = gson.fromJson(new String(pMessage.getPayload()), Pin.class);
      if (pinFromJson.pinProperty.equals(PinProperty.PIN_STATE)) {
	if (pinFromJson.pinValue.equals(PinValue.HIGH)) {
	  tempLowIndicator.high();
	} else if (pinFromJson.pinValue.equals(PinValue.LOW)) {
	  tempLowIndicator.low();
	}
      }

    } else {
      System.out.println(pTopic + " " + new String(pMessage.getPayload()));
    }

  }

  public void connectionLost(Throwable cause) {
  }

  public void deliveryComplete(IMqttDeliveryToken token) {
  }

  public void doDemo() {
    try {
      mClient = new MqttClient("tcp://192.168.0.9:1883", MqttClient.generateClientId());
      mClient.connect();
      mClient.setCallback(this);

      mClient.subscribe(TEMPERATURE);
      mClient.subscribe(Topics.INTERNAL_TOPIC);

      // set shutdown state for this pin
      tempLowIndicator.setShutdownOptions(true, PinState.LOW);

      // We’ll now idle here sleeping, but your app can be busy
      // working here instead
      while (true) {
	try {
	  Thread.sleep(1000);
	} catch (InterruptedException e) {
	  // stop all GPIO activity/threads by shutting down the GPIO controller
	  // (this method will forcefully shutdown all GPIO monitoring threads
	  // and scheduled tasks)
	  gpio.shutdown();
	}
      }
    } catch (MqttException e) {
      e.printStackTrace();
    }
  }

  public static void main(String[] args) {
    new DisplayOutput().doDemo();
  }

}