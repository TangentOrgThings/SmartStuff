// vim :set tabstop=2 shiftwidth=2 sts=2 expandtab smarttab :
/**
 *  Yamaha Network Receiver Volume
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
  return "v0.57"
}

metadata {
  definition (name: "Yamaha Network Receiver Volume", namespace: "TangentOrgThings", author: "Brian Aker") {
    capability "Switch"
    capability "Switch Level"

    command "getDriverVersion"

    attribute "driverVersion", "string"

  }


  simulator {
    // TODO: define status and reply messages here
  }

  tiles {
    controlTile("volume", "device.level", "slider", height: 1, width: 2, inactiveLabel: false, range:"(-500..-250)") {
      state "level", action:"switch level.setLevel"
    }
  }
}

// parse events into attributes
def parse(String description) {
  logger("Parsing '${description}'")
}

// handle commands
def on() {
  logger("Executing 'on'")
  parent.unmute()
}

def off() {
  parent.mute()
  logger("Executing 'off'")
}

def setLevel(value) {
  logger("Executing 'setLevel'")
  parent.setLevel(value)
}

def installed() {
  logger("installed()")
  sendEvent(name: "driverVersion", value: getDriverVersion(), descriptionText: getDriverVersion(), isStateChange: true, displayed: true)
}

def updated() {
  if (state.updatedDate && (Calendar.getInstance().getTimeInMillis() - state.updatedDate) < 5000 ) {
    return
  }

  sendEvent(name: "driverVersion", value: getDriverVersion(), descriptionText: getDriverVersion(), isStateChange: true, displayed: true)

  // Avoid calling updated() twice
  state.updatedDate = Calendar.getInstance().getTimeInMillis()
}

/*

 */
private logger(msg, level = "trace") {
  parent.logger("$device.displayName ${msg}", level)
}
