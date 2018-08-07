// vim :set tabstop=2 shiftwidth=2 sts=2 expandtab smarttab :

/**
 *  Flic Button Press
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

def deviceVersion() {
  def txt = "v1.07"
}

metadata {
  definition (name: "Flic Button Press", namespace: "TangentOrgThings", author: "Brian Aker") {
    capability "Actuator"
    capability "Momentary"
    capability "Switch"

    attribute "flicButtonType", "enum", ["Click", "Double Click", "Hold"]
    attribute "DeviceVersion", "string"
  }


  simulator {
    // TODO: define status and reply messages here
  }

  tiles {
    valueTile("buttonTile", "device.flicButtonType", inactiveLabel: true, decoration: "flat", width: 2, height: 2) {
			state "flicButtonType", label: '${currentValue}', action: "Momentary.push", backgroundColor: "#00a0dc"
		}

    main("buttonTile")
    details(["buttonTile"])
  }
}

// parse events into attributes
def parse(String description) {
  log.debug "Parsing '${description}'"
  // TODO: handle 'switch' attribute
  // TODO: handle 'about' attribute
  // TODO: handle 'version' attribute

}

// handle commands
def push() {
  log.debug "push()"
  if (state.pushDate && (Calendar.getInstance().getTimeInMillis() - state.pushDate) < 5000 ) {
    log.debug "push() skipping"
    return
  }
  state.pushDate= Calendar.getInstance().getTimeInMillis()

  parent.childPush(device.deviceNetworkId)
  sendEvent(name: "switch", value: "on", isStateChange: true, displayed: false)
  sendEvent(name: "switch", value: "off", isStateChange: true, displayed: false)
  sendEvent(name: "momentary", value: "pushed", isStateChange: true)
}

def on() {
  log.debug "Executing 'on'"
  push()
}

def off() {
  log.debug "Executing 'off'"
}

def installed() {
  log.info("installed()")
  sendEvent(name: "flicButtonType", value: "${device}", isStateChange: true)
  sendEvent(name: "DeviceVersion", value: deviceVersion(), isStateChange: true)
}

def updated() {
  log.info("updated()")
  sendEvent(name: "DeviceVersion", value: deviceVersion(), isStateChange: true)
}
