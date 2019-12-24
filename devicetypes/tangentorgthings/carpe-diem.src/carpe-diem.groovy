// vim :set ts=2 sw=2 sts=2 expandtab smarttab :
/**
 *  Carpe Diem
 *
 *  Copyright 2017 Brian Aker
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

def getDriverVersion () {
  return "v0.19"
}

metadata {
  definition (name: "Carpe Diem", namespace: "TangentOrgThings", author: "Brian Aker") {
    capability "Button"
    capability "Illuminance Measurement"
    capability "Sensor"
    capability "Switch"

    attribute "driverVersion", "string"
    attribute "lastSunrise", "number"
    attribute "lastSunset", "number"

    command "noctem"
    command "diem"
  }


  simulator {
    // TODO: define status and reply messages here
  }

  tiles {
    multiAttributeTile(name:"switch", type: "generic", width: 6, height: 4, canChangeIcon: true) {
      tileAttribute ("device.switch", key: "PRIMARY_CONTROL") {
        state "off", label: "Night", icon: "st.Weather.weather4", backgroundColor: "#ffffff"
        state "on", label: "Day", icon: "st.Weather.weather14", backgroundColor: "#00a0dc"
      }
      tileAttribute ("device.illuminance", key: "SECONDARY_CONTROL") {
        attributeState "device.illuminance", value: '${currentValue}', unit:"lux"
      }
    }

    main "switch"
    details(["switch"])
  }
}

// parse events into attributes
def parse(String description) {
  log.debug "Parsing '${description}'"
}

// handle commands
def off() {
  log.debug "Executing 'off'"
  noctem()
}

def on() {
  log.debug "Executing 'on'"
  diem()
}

def setIlluminance(level) {
  if (level > 0) {
    sendEvent(name: "illuminance", value: 300, unit: "lux", isStateChange: true, displayed: true)
  } else {
    sendEvent(name: "illuminance", value: 8, unit: "lux", isStateChange: true, displayed: true)
  }
}

def noctem() {
  log.debug "noctem()"

  if (device.currentValue("switch") != "off") {
    setIlluminance(0x00)
    def sunset = new Date().time
    sendEvent(name: "lastSunset", value: "$sunset", isStateChange: true, displayed: true)
    sendEvent(name: "switch", value: "off", isStateChange: true, displayed: true)
    sendEvent(name: "button", value: "pushed", data: [buttonNumber: 2], descriptionText: "$device.displayName", isStateChange: true, type: "digital")
  }
}

def diem() {
  log.debug "diem()"

  if (device.currentValue("switch") != "on") {
    setIlluminance(0xFF)
    def sunrise = new Date().time
    sendEvent(name: "lastSunrise", value: "$sunrise", isStateChange: true, displayed: true)
    sendEvent(name: "switch", value: "on", isStateChange: true, displayed: true)
    sendEvent(name: "button", value: "pushed", data: [buttonNumber: 1], descriptionText: "$device.displayName", isStateChange: true, type: "digital")
  }
}


def installed() {
  log.debug ("installed()")

  sendEvent(name: "driverVersion", value: getDriverVersion(), descriptionText: getDriverVersion(), isStateChange: true, displayed: true)
  sendEvent(name: "numberOfButtons", value: 2, displayed: false)
}

def updated() {
  log.debug "updated()"

  sendEvent(name: "driverVersion", value: getDriverVersion(), descriptionText: getDriverVersion(), isStateChange: true, displayed: true)
  sendEvent(name: "numberOfButtons", value: 2, displayed: false)

  def sunrise = device.currentValue("lastSunrise")
  def sunset = device.currentValue("lastSunset")

  log.debug "sunset: $sunset"
  log.debug "sunrise: $sunrise"

  if (! sunset) {
    sunset = new Date().time
  }

  if (! sunrise) {
    sunrise = new Date().time
  }

  if ( sunset < sunrise ) {
    diem()
  } else {
    noctem()
  }
}
