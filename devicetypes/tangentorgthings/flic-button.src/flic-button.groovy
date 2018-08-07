// vim :set tabstop=2 shiftwidth=2 sts=2 expandtab smarttab :

/**
 *  Flic Button
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
  def txt = "v1.05"
}

metadata {
  definition (name: "Flic Button", namespace: "TangentOrgThings", author: "Brian Aker") {
    capability "Button"

    //command "childPush"
    attribute "status", "enum", ["default", "clicked", "double clicked", "held"]
    attribute "DeviceVersion", "string"
  }


  simulator {
    // TODO: define status and reply messages here
  }

  tiles(scale: 2) {
    standardTile("status", "device.status", width: 4, height: 4) {
      state "default", label: "", backgroundColor: "#ffffff"
      state "clicked", label: "clicked", backgroundColor: "#00a0dc"
      state "double clicked", label: "double", backgroundColor: "#00a0dc"
      state "held", label: "held", backgroundColor: "#00a0dc"
    }

    childDeviceTile("click", "click", childTileName: "buttonTile", height: 2, width: 2)
    childDeviceTile("double click", "doubleClick", childTileName: "buttonTile", height: 2, width: 2)
    childDeviceTile("hold", "hold", childTileName: "buttonTile", height: 2, width: 2)

    valueTile("version", "device.DeviceVersion", inactiveLabel: false, decoration: "flat") {
      state "DeviceVersion"
    }

    main("status")
    details(["status", "click", "double click", "hold", "version"])
  }
}

// parse events into attributes
def parse(String description) {
  log.debug "Parsing '${description}'"
  // TODO: handle 'button' attribute
  // TODO: handle 'numberOfButtons' attribute
  // TODO: handle 'supportedButtonValues' attribute

}

// handle commands
void childPush(String dni) {
  log.debug "Executing 'childPush': $dni"
  if (dni.endsWith("/double-click")) {
    sendEvent(name: "status", value: "double clicked", isStateChange: true)
    sendEvent(name: "button", value: "pushed", data: [buttonNumber: 2], descriptionText: "$device.displayName button $button was pushed", isStateChange: true, type: "physical")
  } else if (dni.endsWith("/click")) {
    sendEvent(name: "status", value: "clicked", isStateChange: true)
    sendEvent(name: "button", value: "pushed", data: [buttonNumber: 1], descriptionText: "$device.displayName button $button was pushed", isStateChange: true, type: "physical")
  } else if (dni.endsWith("/hold")) {
    sendEvent(name: "status", value: "held", isStateChange: true)
    sendEvent(name: "button", value: "held", data: [buttonNumber: 1], descriptionText: "$device.displayName button $button was pushed", isStateChange: true, type: "physicalbuttonType")
  } else {
    log.error("Unknown Child Device $dni")
    return
  }
  sendEvent(name: "status", value: "default", isStateChange: true)
}

def installed() {
  log.info("installed()")
  createChildDevices()
  sendEvent(name: "numberOfButtons", value: "2", isStateChange: true)
  sendEvent(name: "DeviceVersion", value: deviceVersion(), isStateChange: true)
}

def updated() {
  log.info("updated()")
  if (! childDevices) {
    createChildDevices()
  }

  sendEvent(name: "DeviceVersion", value: deviceVersion(), isStateChange: true)
}

private void createChildDevices() {
  // Save the device label for updates by updated()
  state.oldLabel = device.label

  // Add child devices for three button presses
  addChildDevice(
    "Flic Button Press",
    "${device.deviceNetworkId}/click",
    null,
    [
    completedSetup: true,
    label: "Click",
    isComponent: true,
    componentName: "click",
    componentLabel: "Click"
    ])

  addChildDevice(
    "Flic Button Press",
    "${device.deviceNetworkId}/double-click",
    null,
    [
    completedSetup: true,
    label: "Double Click",
    isComponent: true,
    componentName: "doubleClick",
    componentLabel: "Double Click"
    ])

  addChildDevice(
    "Flic Button Press",
    "${device.deviceNetworkId}/hold",
    null,
    [
    completedSetup: true,
    label: "Hold",
    isComponent: true,
    componentName: "hold",
    componentLabel: "Hold"
    ])
}
