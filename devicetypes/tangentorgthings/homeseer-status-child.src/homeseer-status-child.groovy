// vim :set tabstop=2 shiftwidth=2 sts=2 expandtab smarttab :

/**
 *  Homeseer Status Child
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

def getDriverVersion() {
  return "v0.01"
}

metadata {
	definition (name: "Homeseer Status Child", namespace: "TangentOrgThings", author: "Brian Aker") {
		capability "Actuator"
		capability "Sensor"
		capability "Switch"

		attribute "driverVersion", "string"

		command "getDriverVersion"
	}


	simulator {
		// TODO: define status and reply messages here
	}

  tiles(scale: 2) {
    multiAttributeTile(name: "switch", width: 6, height: 4, canChangeIcon: false) {
      tileAttribute("device.switch", key: "PRIMARY_CONTROL") {
        attributeState "on", label: '${name}', action: "switch.off", icon: "st.switches.light.on", backgroundColor: "#00a0dc"
        attributeState "off", label: '${name}', action: "switch.on", icon: "st.switches.light.off", backgroundColor: "#ffffff"
      }
    }

    valueTile("driverversion", "device.driverVersion", inactiveLabel: false, decoration: "flat", width: 6, height:2) {
      state "default", label:'${currentValue}'
    }

    main "switch"
    details (["switch", "driverversion"])
  }
}

// parse events into attributes
def parse(String description) {
	log.debug "Parsing '${description}'"
	// TODO: handle 'switch' attribute
	// TODO: handle 'driverVersion' attribute

}

// handle commands
def on() {
	log.debug "Executing 'on'"
	parent.childOn(device.deviceNetworkId)
}

def off() {
	log.debug "Executing 'off'"
	parent.childOff(device.deviceNetworkId)
}

def updated() {
	log.debug "Executing 'updated'"
  sendEvent(name: "driverVersion", value: getDriverVersion(), descriptionText: getDriverVersion(), isStateChange: true, displayed: true)
}

def installed() {
	log.debug "Executing 'installed'"
  sendEvent(name: "driverVersion", value: getDriverVersion(), descriptionText: getDriverVersion(), isStateChange: true, displayed: true)
}
