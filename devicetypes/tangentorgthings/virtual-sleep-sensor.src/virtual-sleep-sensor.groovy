// vim :set tabstop=2 shiftwidth=2 sts=2 expandtab smarttab :
/**
 *  Virtual Sleep Sensor
 *
 *  Copyright 2018 Brian Aker <brian@tangent.org>
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
  return "v1.01"
}

String versionNum() {
  return getDriverVersion()
}

metadata {
  definition (name: "Virtual Sleep Sensor", namespace: "TangentOrgThings", author: "Brian Aker") {
    capability "Actuator"
    capability "Button"
    capability "Sensor"
    capability "Sleep Sensor"
    capability "Switch"

    attribute "about", "string"
  }


  simulator {
    // TODO: define status and reply messages here
  }

  tiles {
    standardTile("sleep", "device.motion", width: 2, height: 2, decoration: "flat") {
      state("sleeping", label:'Zzzz', backgroundColor: "#53a7c0")
      state("not sleeping", label:'(^・o・^)', backgroundColor: "#ffffff")
    }

    main "sleep"
    details(["sleep"])
  }
}

// parse events into attributes
def parse(String description) {
  log.debug "Parsing '${description}'"
  // TODO: handle 'presence' attribute
  // TODO: handle 'switch' attribute
}

// handle commands
def on() {
  log.debug "Executing 'on'"
  sendEvent(name: "button", value: "pushed", data: [buttonNumber: 1], descriptionText: "$device.displayName button was pushed", isStateChange: true, type: "digital")
  sendEvent(name: "switch", value: "on", isStateChange: true, type: "digital")
  sendEvent(name: "sleep", value: "sleeping", isStateChange: true, type: "digital")
}

def off() {
  log.debug "Executing 'off'"
  sendEvent(name: "button", value: "pushed", data: [buttonNumber: 2], descriptionText: "$device.displayName button was pushed", isStateChange: true, type: "digital")
  sendEvent(name: "switch", value: "off", isStateChange: true, type: "digital")
  sendEvent(name: "sleep", value: "not sleeping", isStateChange: true, type: "digital")
}

def showVersion() {
  def versionTxt = "${appName()}: ${versionNum()}\n"
  try {if (parent.getSwitchAbout()){versionTxt += parent.getSwitchAbout()}}
  catch(e){versionTxt +="Installed from the SmartThings IDE"}
  sendEvent (name: "about", value:versionTxt) 
}

def appName() {
  def txt = "Virtual Sleep Sensor"
}

def installed() {
  log.debug "installed()"
  sendEvent(name: "numberOfButtons", value: 2, displayed: false)
  showVersion()   
}

def updated() {
  log.debug "updated()"
  sendEvent(name: "numberOfButtons", value: 2, displayed: false)
  showVersion() 
}
