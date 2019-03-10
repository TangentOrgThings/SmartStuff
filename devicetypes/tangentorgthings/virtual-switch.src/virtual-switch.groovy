// vim: set filetype=groovy tabstop=2 shiftwidth=2 softtabstop=2 expandtab smarttab :

/**
 *  Virtual Switch
 *
 *  Copyright 2018 Brian Aker
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
  return "v2.05"
}

metadata {
  definition (name: "Virtual Switch", namespace: "TangentOrgThings", author: "SmartThings", vid: "generic-switch") {
    capability "Actuator"
    capability "Button"
    capability "Sensor"
    capability "Switch Level"
    capability "Switch"

    attribute "driverVersion", "string"
  }

  // simulator metadata
  simulator {
  }

  // UI tile definitions
  tiles(scale: 2) {
    multiAttributeTile(name: "switch", type: "lighting", width: 6, height: 4, canChangeIcon: true, canChangeBackground: true) {
      tileAttribute("device.switch", key: "PRIMARY_CONTROL") {
        attributeState "on", label: '${name}', action: "switch.off", backgroundColor: "#79b821",icon: "st.switches.switch.on",  nextState: "turningOff"
        attributeState "off", label: '${name}', action: "switch.on", backgroundColor: "#ffffff",icon: "st.switches.switch.off", nextState: "turningOn", defaultState: true
        attributeState "turningOn", label: 'Turning On', action: "switch.off",backgroundColor: "#79b821", icon: "st.switches.switch.on", nextState: "turningOn"
        attributeState "turningOff", label: 'Turning Off', action: "switch.on",backgroundColor: "#ffffff", icon: "st.switches.switch.off",  nextState: "turningOff"
      }
      tileAttribute("device.level", key: "SLIDER_CONTROL") {
        attributeState "level", action:"switch level.setLevel"
      }
      tileAttribute("level", key: "SECONDARY_CONTROL") {
        attributeState "level", label: '${currentValue}%'
      }
    }

    valueTile("version", "device.getDriverVersion", inactiveLabel: false, decoration: "flat", width: 6, height:2) {
      state "default", label:'${currentValue}'
    }

    valueTile("level", "device.level", inactiveLabel: true, height:2, width:2, decoration: "flat") {
      state "levelValue", label:'${currentValue}%', unit:""
    }

    standardTile("on", "device.switch", inactiveLabel: false, decoration: "flat", width: 2, height: 2) {
      state "default", label:'on', action:"switch.on", icon:"st.switches.light.on"
    }

    standardTile("off", "device.switch", inactiveLabel: false, decoration: "flat", width: 2, height: 2) {
      state "default", label:'off', action:"switch.off", icon:"st.switches.light.off"
    }

    main "switch"
    details(["switch", "level", "on", "off", "version"])
  }
}

def installed() {
  log.debug "installed()"
  sendEvent(name: "numberOfButtons", value: 2, displayed: false)
  sendEvent(name: "driverVersion", value: getDriverVersion(), descriptionText: getDriverVersion(), isStateChange: true, displayed: true)
}

def updated() {
  log.debug "updated()"
  sendEvent(name: "numberOfButtons", value: 2, displayed: false)
  sendEvent(name: "driverVersion", value: getDriverVersion(), descriptionText: getDriverVersion(), isStateChange: true, displayed: true)
}

def parse(String description) {
  if (description) {
    log.debug("$device.displayName ${description}")
  }
}

def on() {
  log.info "on()"
  sendEvent(name: "switch", value: "on", isStateChange: true)
  sendEvent(name: "level", value: 100, isStateChange: true)
  sendEvent(name: "button", value: "pushed", data: [buttonNumber: 1], descriptionText: "$device.displayName button was pushed", isStateChange: true, type: "digital")
}

def off() {
  log.info "off()"

  sendEvent(name: "switch", value: "off", isStateChange: true)
  sendEvent(name: "level", value: 0, isStateChange: true)
  sendEvent(name: "button", value: "pushed", data: [buttonNumber: 2], descriptionText: "$device.displayName button was pushed", isStateChange: true, type: "digital")
}

def setLevel(value){
  log.info "setLevel($value)"

  if (value < 0) {
    value = 0
  }

  if (value > 100) {
    value = 100
  }

  sendEvent(name: "level", value: value, isStateChange: true)
  sendEvent(name: "switch", value: value == 0 ? "off" : "on", isStateChange: true)
}
