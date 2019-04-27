// vim: set filetype=groovy tabstop=2 shiftwidth=2 softtabstop=2 expandtab smarttab :

/**
 *  Airfoil Speaker
 *
 *  Copyright 2017-2018 Brian Aker <brian@tangent.org>
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
 *  Date: 2017-2018
 *
 *  Changelog:
 *
 *
 *
 *
 */

import physicalgraph.*

String getDriverVersion () {
  return "v8.03"
}

metadata {
  definition (name: "Airfoil Speaker", namespace: "tangentorgthings", author: "Brian Aker") {
    capability "Actuator"
    capability "Audio Volume"
    capability "Refresh"
    capability "Sensor"
    capability "Switch level"
    capability "Switch"

    attribute "logMessage", "string"        // Important log messages.
    attribute "lastError", "string"        // Last error message
  }

  // UI tile definitions
  tiles {
    standardTile("switch", "device.switch", width: 2, height: 2, canChangeIcon: true) {
      state "off", label: '${name}', action: "switch.on", icon: "st.Electronics.electronics16", backgroundColor: "#ffffff"
      state "on", label: '${name}', action: "switch.off", icon: "st.Electronics.electronics16", backgroundColor: "#79b821"
    }
    standardTile("refresh", "device.switch", inactiveLabel: false, decoration: "flat") {
      state "default", label:"", action:"refresh.refresh", icon:"st.secondary.refresh"
    }
    controlTile("levelSliderControl", "device.level", "slider", height: 1, width: 2, inactiveLabel: false, range:"(0..100)") {
      state "level", action:"switch level.setLevel"
    }
    valueTile("level", "device.level", inactiveLabel: false, decoration: "flat") {
      state "level", label: 'Level ${currentValue}%'
    }

    main(["switch"])
    details(["switch", "levelSliderControl", "refresh"])  }
}

def parse(description) {
  log.debug "parse() - $description"
  def results = []

  def map = description
  if (description instanceof String)  {
    log.debug "stringToMap - ${map}"
    map = stringToMap(description)
  }

  if (map?.name && map?.value) {
    results << createEvent(name: "${map?.name}", value: "${map?.value}")
  }
  results
}

// handle commands
def on() {
  parent.on(this)
}

def off() {
  parent.off(this)
}

def setLevel(level) {
  log.debug "Executing 'setLevel'"
  log.debug "level=${level}"
  def percent = level / 100.00
  log.debug "percent=${percent}"
  parent.setLevel(this, percent)
}

def refresh() {
  log.debug "Executing 'refresh'"
  parent.poll()
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
