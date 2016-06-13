package edu.mit.appinventor.raspberrypi.gpio;

import java.nio.charset.StandardCharsets;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import com.pi4j.io.gpio.GpioController;
import com.pi4j.io.gpio.GpioFactory;
import com.pi4j.io.gpio.GpioPinDigitalOutput;
import com.pi4j.io.gpio.PinState;
import com.pi4j.io.gpio.RaspiPin;

public class DisplayOutput implements MqttCallback
{

  MqttClient client;

  final String TEMPERATURE = "temperature";
  
  // create gpio controller
  final GpioController gpio = GpioFactory.getInstance();

  // provision gpio pin #02 as an output pin and turn on
  final GpioPinDigitalOutput tempLowIndicator = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_02, "BlueLED", PinState.LOW);

  public DisplayOutput() {}

  public void messageArrived(String topic, MqttMessage message) throws Exception
  {
    if (topic.equals(TEMPERATURE)) {
      String temperatureString = new String(message.getPayload(), StandardCharsets.UTF_8);
      int temperature = Integer.parseInt(temperatureString);
      if (temperature > 80) {
	System.out.println ("FIRE FIRE!!!!!! " + new String (message.getPayload()));
      } else if (temperature < 40) {
	tempLowIndicator.high();
	System.out.println ("BRRRR Freezing !!!!!! " + new String (message.getPayload()));
      } else {
	System.out.println ("Temperature Normal. " + new String (message.getPayload()));
      }      
    } else {
      System.out.println (topic + " " + new String (message.getPayload()));
    }

  }

  public void connectionLost (Throwable cause) {}
  public void deliveryComplete(IMqttDeliveryToken token) {}

  public static void main (String[] args) {
    new DisplayOutput().doDemo();
  }

  public void doDemo() {
    try {
      client = new MqttClient("tcp://192.168.0.9:1883", MqttClient.generateClientId());
      client.connect();
      client.setCallback(this);

      client.subscribe(TEMPERATURE);
      
      // set shutdown state for this pin
      tempLowIndicator.setShutdownOptions(true, PinState.LOW);


      // We’ll now idle here sleeping, but your app can be busy
      // working here instead
      while (true) {
	try { Thread.sleep (1000); 
	} catch (InterruptedException e) {
	    // stop all GPIO activity/threads by shutting down the GPIO controller
	    // (this method will forcefully shutdown all GPIO monitoring threads and scheduled tasks)
	    gpio.shutdown();
	}
      }
    }
    catch (MqttException e) { e.printStackTrace (); }
  }
}