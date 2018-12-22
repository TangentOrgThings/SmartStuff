// vim: set filetype=groovy tabstop=2 shiftwidth=2 sts=2 expandtab smarttab :

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
  return "v1.09"
}

def versionNum(){
  return getDriverVersion()
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

    if (parent) {
      parent.childLevel(device.deviceNetworkId)
    }
  }
}

def trueSetLevel(Integer cmd_value) {

  Integer val = Math.max(Math.min(cmd_value, 99), 0)

  if (val == 0) {
    sendEvent(name: "switch", value: "off")
    sendEvent(name: "level", value: val)
  } else {
    sendEvent(name: "switch", value: "on")
    sendEvent(name: "level", value: val)
  }
}

def ignoreDigital() {
  state.IgnoreDigital = state.IgnoreDigital ? false : true
  return
}

def installed() {
  log.info("$device.displayName installed()")

  sendEvent(name: "switch", value: "off")
  sendEvent(name: "level", value: 0)
  sendEvent(name: "numberOfButtons", value: 2, displayed: true, isStateChange: true)
  showVersion() 
}

def updated() {
  if (state.updatedDate && (Calendar.getInstance().getTimeInMillis() - state.updatedDate) < 5000 ) {
    return
  }
  log.info("$device.displayName updated() debug: ${settings.debugLevel}")

  sendEvent(name: "lastError", value: "", displayed: false)
  sendEvent(name: "logMessage", value: "", displayed: false)

  sendEvent(name: "numberOfButtons", value: 2, displayed: true, isStateChange: true)

  // Avoid calling updated() twice
  state.updatedDate = Calendar.getInstance().getTimeInMillis()
  showVersion() 
}

def showVersion() {
  def versionTxt = "${appName()}: ${versionNum()}\n"
  try {
    if (parent.getSwitchAbout()) {
      versionTxt += parent.getSwitchAbout()
    }
  }
  catch(e) {
    versionTxt += "Installed from the SmartThings IDE"
  }
  sendEvent (name: "about", value: "${versionTxt}") 
  sendEvent (name: "driverVersion", value: "${getDriverVersion()}") 
}

String appName() {
  return "Virtual Dimmer"
}

private logger(msg, level = "trace") {
  String device_name = "$device.displayName"
  String msg_text = (msg != null) ? "${msg}" : "<null>"

  Integer log_level = state.defaultLogLevel ?: settings.debugLevel

  switch(level) {
    case "warn":
    if (log_level >= 2) {
      log.warn "$device_name ${msg_text}"
    }
    sendEvent(name: "logMessage", value: "${msg_text}", isStateChange: true)
    break;

    case "info":
    if (log_level >= 3) {
      log.info "$device_name ${msg_text}"
    }
    break;

    case "debug":
    if (log_level >= 4) {
      log.debug "$device_name ${msg_text}"
    }
    break;

    case "trace":
    if (log_level >= 5) {
      log.trace "$device_name ${msg_text}"
    }
    break;

    case "error":
    default:
    log.error "$device_name ${msg_text}"
    sendEvent(name: "lastError", value: "${msg_text}", isStateChange: true)
    break;
  }
}
