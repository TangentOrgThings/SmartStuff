// vim: set filetype=groovy tabstop=2 shiftwidth=2 softtabstop=2 expandtab smarttab :

/**
 *
 *  Copyright 2018 Brian Aker <brian@tangent.org>
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

String getDriverVersion () {
  return "v0.11"
}

metadata {
  definition (name: "Virtual Fixture", namespace: "TangentOrgThings", author: "Brian Aker", vid: "generic-switch") {
    capability "Actuator"
    capability "Button"
    capability "Color Mode"
    capability "Contact Sensor"
    capability "Light"
    capability "Sensor"
    capability "Switch Level"
    capability "Switch"

    attribute "driverVersion", "string"
  }

  preferences {
    input name: "debugLevel", type: "number", title: "Debug Level", description: "Adjust debug level for log", range: "1..5", displayDuringSetup: false, defaultValue: 3
  }

  tiles {
    standardTile("contact", "device.contact", width: 2, height: 2) {
      state("open", label: 'on', action: "switch.off", icon: "st.motion.motion.active", backgroundColor: "#53a7c0")
      state("closed", label:'off', action: "switch.on", icon: "st.motion.motion.inactive", backgroundColor: "#ffffff")
    }

    valueTile("driverVersion", "device.driverVersion", width: 2, height: 2, decoration: "flat") {
      state "default", label: '${currentValue}', defaultState: true
    }

    main "contact"
    details(["contact", "on", "off", "driverVersion"])
  }
}

def parse(String description) {
  log.info "parse($description)"
}

void initialize() {
  log.info "initialize()"
  sendEvent(name: "contact", value: "closed", isStateChange: true, displayed: true)
  sendEvent(name: "numberOfButtons", value: 2, displayed: false)
  sendEvent(name: "driverVersion", value: getDriverVersion(), descriptionText: getDriverVersion(), isStateChange: true, displayed: true)
  sendEvent(name: "switch", value: "", isStateChange: true, displayed: true)
}

def installed() {
  log.info "installed()"
  initialize()
}

def updated() {
  log.info "updated()"
  initialize()
}

def followupStateCheck() {
  log.info "followupStateCheck()"
  off()
}

def push() {
  log.info "push()"

  on()
  runIn(360, followupStateCheck)
  sendEvent(name: "momentary", value: "pushed", isStateChange: true)
}

def on() {
  log.info "on()"
  // sendEvent(name: "switch", value: "on", isStateChange: true, displayed: true)
  sendEvent(name: "button", value: "pushed", data: [buttonNumber: 1], descriptionText: "$device.displayName button $button was pushed", isStateChange: true, type: "$buttonType")
  sendEvent(name: "contact", value: "open", isStateChange: true, displayed: true)
  sendEvent(name: "level", value: 100, isStateChange: true, displayed: true)
}

def setLevel (provided_value) {
  sendEvent(name: "contact", value: provided_value > 0 ? "open" : "closed", isStateChange: true, displayed: true)
  sendEvent(name: "level", value: provided_value, isStateChange: true, displayed: true)
}

def setLevel(value, duration) {
  setLevel(value)
}

def off() {
  log.info "off()"
  // sendEvent(name: "switch", value: "off", isStateChange: true, displayed: true)
  sendEvent(name: "button", value: "pushed", data: [buttonNumber: 2], descriptionText: "$device.displayName button $button was pushed", isStateChange: true, type: "$buttonType")
  sendEvent(name: "contact", value: "closed", isStateChange: true, displayed: true)
  sendEvent(name: "level", value: 0, isStateChange: true, displayed: true)
}
