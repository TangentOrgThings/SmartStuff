// vim: set filetype=groovy tabstop=2 shiftwidth=2 softtabstop=2 expandtab smarttab :

/**
 *  Avatar
 *
 *  Copyright 2019 Brian Aker <brian@tangent.org>
 *
 *  For device parameter information and images, questions or to provide feedback on this device handler,
 *  please visit:
 *
 *      https://github.com/TangentOrgThings/SmartStuff/
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
 *  Author: Brian Aker <brian@tangent.org>
 *  Date: 2019
 *
 *  Changelog:
 *
 *
 *
 *
 */

import physicalgraph.*

String getDriverVersion () {
  return "v0.07"
}

metadata {
  definition (name: "Avatar", namespace: "tangentorgthings", author: "Brian Aker", cstHandler: true) {
    capability "Button"
    capability "Presence Sensor"
    capability "Switch"

    attribute "logMessage", "string"        // Important log messages.
    attribute "lastError", "string"        // Last error message

    attribute "lastSeen", "string"

    attribute "about", "string"
    attribute "driverVersion", "string"
  }


  simulator {
    // TODO: define status and reply messages here
  }

  preferences {
    input name: "normallyHome", type: "bool", title: "Normally Home", description: "If you oopsed the switch... ", required: false,  defaultValue: false
  }

  tiles {
    standardTile("presence", "device.presence", width: 2, height: 2, canChangeBackground: true) {
      state("not present", label:'not present', icon:"st.presence.tile.not-present", backgroundColor:"#ffffff", action:"arrived")
      state("present", label:'present', icon:"st.presence.tile.present", backgroundColor:"#00A0DC", action:"departed")
    }

    valueTile("lastSeen", "device.lastSeen", width: 2, height: 2, decoration: "flat") {
      state "default", label: '${currentValue}', defaultState: true
    }

    valueTile("driverVersion", "device.driverVersion", width: 2, height: 2, decoration: "flat") {
      state "default", label: '${currentValue}', defaultState: true
    }


    main("presence")
    details(["presence", "lastSeen", "driverVersion"])
  }

}

// parse events into attributes
def parse(String description) {
	log.debug "Parsing '${description}'"
	// TODO: handle 'button' attribute
	// TODO: handle 'numberOfButtons' attribute
	// TODO: handle 'supportedButtonValues' attribute
	// TODO: handle 'presence' attribute
	// TODO: handle 'switch' attribute

}

// handle commands
def on() {
  logger("on()")
  sendEvent(name: "presence", value: "present", displayed: true, isStateChange: true)
  sendEvent(name: "button", value: "pushed", data: [buttonNumber: 1], isStateChange: true, type: "digital")

  def lastSeen = new Date().time
  result << createEvent(name: "lastSeen", value: state.lastActive, isStateChange: false)
}

def off() {
  logger("off()")
  sendEvent(name: "presence", value: "not present", displayed: true, isStateChange: true)
  sendEvent(name: "button", value: "pushed", data: [buttonNumber: 2], isStateChange: true, type: "digital")
}

private initialized() {
  // Device Health
  sendEvent(name: "DeviceWatch-DeviceStatus", value: "online")
  sendEvent(name: "healthStatus", value: "online")
  sendEvent(name: "DeviceWatch-Enroll", value: [protocol: "cloud", scheme:"untracked"].encodeAsJson(), displayed: false)

  sendEvent(name: "numberOfButtons", value: 2, displayed: true, isStateChange: true)

  buildVersion()
  setTrace(true)
}

def installed() {
  logger("installed()")
  sendEvent(name: "presence", value: "not present", displayed: true, isStateChange: true)
  initialized()
}

def updated() {
  logger("updated()")
  initialized()
}

def buildVersion() {
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
  return "Avatar"
}

def versionNum(){
  return getDriverVersion()
}

def setTrace(Boolean enable) {
  state.isTrace = enable

  if ( enable ) {
    runIn(60*5, followupTraceDisable)
  }
}

Boolean isTraceEnabled() {
  Boolean is_trace = state.isTrace ?: false
  return is_trace
}

def followupTraceDisable() {
  logger("followupTraceDisable()")

  setTrace(false)
}

private logger(msg, level = "trace") {
  String device_name = "$device.displayName"
  String msg_text = (msg != null) ? "${msg}" : "<null>"

  Integer log_level =  isTraceEnabled() ? 5 : 2

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
