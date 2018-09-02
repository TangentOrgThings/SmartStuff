// vim :set ts=2 sw=2 sts=2 expandtab smarttab :
/**
 *  Virtual Dimmer
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

String getDriverVersion () {
  return "v1.03"
}

metadata {
  definition (name: "Virtual Dimmer", namespace: "TangentOrgThings", author: "Brian Aker") {
    capability "Actuator"
    capability "Button"
    capability "Sensor"
    capability "Switch"
    capability "Switch Level"

    attribute "driverVersion", "string"

    attribute "logMessage", "string"        // Important log messages.
    attribute "lastError", "string"        // Last error message
    attribute "parseErrorCount", "number"        // Last error message
    attribute "unknownCommandErrorCount", "number"        // Last error message

    command "trueSetLevel", ["number"]
    command "ignoreDigital"
  }

  simulator {
  }

  preferences {
    input name: "debugLevel", type: "number", title: "Debug Level", description: "Adjust debug level for log", range: "1..5", displayDuringSetup: false
  }

  tiles {
    standardTile("switch", "device.switch", width: 2, height: 2, decoration: "flat") {
      state "on", label: "", backgroundColor: "#D3D3D3"
      state "off", label: "${currentValue}", backgroundColor: "#79b821"
    }

    valueTile("level", "device.level", width: 2, height: 2, decoration: "flat", inactiveLabel: false) {
      state "default", label: '${currentValue}'
    }

    main (["switch"])
    details(["switch", "level"])
  }
}

// parse events into attributes
def parse(String description) {
  logger("Parsing '${description}'")
}

// handle commands
def on() {
  logger("on()")

  if (! state.IgnoreDigital) {
    trueSetLevel(99)
    sendEvent(name: "button", value: "pushed", data: [buttonNumber: 1], isStateChange: true) //  type: "$buttonType"

    if (parent) {
      parent.childOn(device.deviceNetworkId)
    }
  }
}

def off() {
  logger("off()")

  if (! state.IgnoreDigital) {
    trueSetLevel(0)
    sendEvent(name: "button", value: "pushed", data: [buttonNumber: 2], isStateChange: true) //  type: "$buttonType"

    if (parent) {
      parent.childOff(device.deviceNetworkId)
    }
  }
}

def setLevel(val) {
  logger("setLevel()")

  if (! state.IgnoreDigital) {
    trueSetLevel(val)

    if (val == 0) {
      sendEvent(name: "button", value: "held", data: [buttonNumber: 1], isStateChange: true) //  type: "$buttonType"
    } else {
      sendEvent(name: "button", value: "held", data: [buttonNumber: 2], isStateChange: true) //  type: "$buttonType"
    }

    if (parent) {
      parent.childLevel(device.deviceNetworkId)
    }
  }
}

def trueSetLevel(Integer val) {
  if (val == 0) {
    sendEvent(name: "switch", value: "off")
    sendEvent(name: "level", value: val)
  } else {
    sendEvent(name: "switch", value: "on")
    sendEvent(name: "level", value: val == 255 ? 99 : val)
  }
}

def ignoreDigital() {
  state.IgnoreDigital = true
  return
}

def installed() {
  log.info("$device.displayName installed()")

  sendEvent(name: "driverVersion", value: getDriverVersion(), descriptionText: getDriverVersion(), isStateChange: true, displayed: true)

  sendEvent(name: "switch", value: "off")
  sendEvent(name: "level", value: 0)
}

def updated() {
  if (state.updatedDate && (Calendar.getInstance().getTimeInMillis() - state.updatedDate) < 5000 ) {
    return
  }
  log.info("$device.displayName updated() debug: ${settings.debugLevel}")

  sendEvent(name: "lastError", value: "", displayed: false)
  sendEvent(name: "logMessage", value: "", displayed: false)
  sendEvent(name: "parseErrorCount", value: 0, displayed: false)
  sendEvent(name: "unknownCommandErrorCount", value: 0, displayed: false)
  state.parseErrorCount = 0
  state.unknownCommandErrorCount = 0

  sendEvent(name: "driverVersion", value: getDriverVersion(), descriptionText: getDriverVersion(), isStateChange: true, displayed: true)

  // Avoid calling updated() twice
  state.updatedDate = Calendar.getInstance().getTimeInMillis()
}

/**
 *  logger()
 *
 *  Wrapper function for all logging:
 *    Logs messages to the IDE (Live Logging), and also keeps a historical log of critical error and warning
 *    messages by sending events for the device's logMessage attribute and lastError attribute.
 *    Configured using configLoggingLevelIDE and configLoggingLevelDevice preferences.
 **/
private logger(msg, level = "trace") {
  switch(level) {
    case "unknownCommand":
    state.unknownCommandErrorCount += 1
    sendEvent(name: "unknownCommandErrorCount", value: unknownCommandErrorCount, displayed: false, isStateChange: true)
    break

    case "parse":
    state.parseErrorCount += 1
    sendEvent(name: "parseErrorCount", value: parseErrorCount, displayed: false, isStateChange: true)
    break

    case "warn":
    if (settings.debugLevel >= 2) {
      log.warn msg
      sendEvent(name: "logMessage", value: "WARNING: ${msg}", displayed: false, isStateChange: true)
    }
    return

    case "info":
    if (settings.debugLevel >= 3) {
      log.info msg
    }
    return

    case "debug":
    if (settings.debugLevel >= 4) {
      log.debug msg
    }
    return

    case "trace":
    if (settings.debugLevel >= 5) {
      log.trace msg
    }
    return

    case "error":
    default:
    break
  }

  log.error msg
  sendEvent(name: "lastError", value: "ERROR: ${msg}", displayed: false, isStateChange: true)
}
