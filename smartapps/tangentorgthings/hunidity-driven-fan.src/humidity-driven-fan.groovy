// vim :set tabstop=2 shiftwidth=2 sts=2 expandtab smarttab :

/**
 *  Logitech Harmony Activity
 *
 *  Copyright 2018 Brian Aker <brian@tangent.org>
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 */


// Automatically generated. Make future change here.
definition(
  name: "Humidity Driven Fan",
  namespace: "TangentOrgThings",
  author: "ross.peterson@gmail.com, brian@tangent.org",
  description: "When the humidity level goes above a certain value, turn on a switched fan. When that value drops below a separate threshold, turn off the fan.",
  category: "Convenience",
  iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
  iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience%402x.png")

preferences {
  section("Monitor the humidity..."){
    input "humiditySensor1", "capability.relativeHumidityMeasurement", title: "Humidity Sensor?", required: true
  }    
  section("Choose a Switch that controls a Fan..."){
    input "fanSwitch1", "capability.switch", title: "Fan Location?", required: true
  }
  section("Turn fan on when the humidity is above:") {
    input "humidityUP", "number", title: "Humidity Upper Threshold (%)?"
  }
}

def installed() {
  subscribe(humiditySensor1, "humidity", humidityHandler)
  log.debug "Installed with settings: ${settings}"
}

def updated() {
  unsubscribe()
  subscribe(humiditySensor1, "humidity", humidityHandler)
  log.debug "Updated with settings: ${settings}"
}

def followupOff() {
  log.debug("followupStateCheck")
  
  fanSwitch1.off()   
}

def humidityHandler(evt) {
  log.debug "Humidity: $evt.value, $evt.unit, $evt.name, $evt"

  //def humNum = evt.value.replace("%", "")
  def humNum = Double.parseDouble(evt.value.replace("%", ""))
  def tooHumidNum = humidityUP
  double tooHumid = tooHumidNum

  def mySwitch = settings.fanSwitch1

  log.debug "Current Humidity: $humNum, Humidity Setting: $tooHumid"

  if (humNum >= tooHumid) {
    if ( fanSwitch1.currentValue("switch") == "off" ) {
      // Turn on if the switch is off
      fanSwitch1.on()   
    }
  } else {

    if (humNum < tooHumid) { // Humidity level is okay...fan should be off

      log.debug "Humidity is Less than Setting"
      if ( fanSwitch1.currentValue("switch") == "on" ) {
        // Turn off if the switch is on
        fanSwitch1.off()   
      }
    }    
  }
}
