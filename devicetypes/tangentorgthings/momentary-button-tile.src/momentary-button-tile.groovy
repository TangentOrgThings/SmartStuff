// vim: set filetype=groovy tabstop=2 shiftwidth=2 softtabstop=2 expandtab smarttab :

/**
 *  Momentary Button Tile
 *
 *  Copyright 2017-2018 Brian Aker
 *  Copyright 2017-2018 Michael Struck
 *  Copyright 2016 Michael Struck
 *  Version 1.0.3 3/18/16
 *
 *  Version 1.0.0 Initial release
 *  Version 1.0.1 Reverted back to original icons for better GUI experience
 *  Version 1.0.2 Added dynamic feedback to user on code version of switch
 *  Version 1.0.3 Added PNG style icons to better differenciate the Alexa Helper created devices
 *
 *  Uses code from SmartThings
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

def versionNum(){
  def txt = "1.1.9 (10/15/18)"
}

metadata {
  definition (name: "Momentary Button Tile", namespace: "TangentOrgThings", author: "Brian Aker", ocfDeviceType: "x.com.st.momentary") {
    capability "Actuator"
    capability "Button"
    capability "Switch"
    capability "Momentary"
    capability "Sensor"

    attribute "about", "string"
  }

  // simulator metadata
  simulator {
  }
  // UI tile definitions
  tiles(scale: 2) {
    multiAttributeTile(name: "switch", type: "generic", width: 6, height: 4, canChangeIcon: false, canChangeBackground: true) {
      tileAttribute("device.switch", key: "PRIMARY_CONTROL") {
        attributeState "off", label: 'push', action: "momentary.push", backgroundColor: "#ffffff", nextState: "turningOn", defaultState: true
        attributeState "on", label: 'on', backgroundColor: "#00a0dc"
        attributeState "turningOn", label:'PUSHED', backgroundColor:"#00A0DC", nextState:"turningOn"
      }
    }

    valueTile("aboutTxt", "device.about", decoration: "flat", width: 6, height:2) {
      state "default", label:'${currentValue}'
    }

    main(["switch"])
    details (["switch", "aboutTxt"])
  }
}

def installed() {
  log.debug "installed()"
  sendEvent(name: "numberOfButtons", value: 1, isStateChange: true, displayed: false)
  off()
  showVersion() 
}

def updated() {
  log.debug "updated()"
  sendEvent(name: "numberOfButtons", value: 1, isStateChange: true, displayed: false)
  off()
  showVersion() 
}

def parse(String description) {
}

def push() {
  log.debug "push()"
  sendEvent(name: "switch", value: "on", isStateChange: true)
  sendEvent(name: "button", value: "pushed", data: [buttonNumber: 1], descriptionText: "$device.displayName button was pushed", isStateChange: true)
  
  runIn(30, followupOff)
}

def followupStateCheck() {
  log.info "followupStateCheck()"
  off()
}

def on() {
  log.debug "on()"
  push()
}

def off() {
  log.debug "off()"
  sendEvent(name: "switch", value: "off", isStateChange: true, displayed: true)
}

def showVersion(){
  def versionTxt = "${appName()}: ${versionNum()}\n"
  try {if (parent.getSwitchAbout()){versionTxt += parent.getSwitchAbout()}}
  catch(e){versionTxt +="Installed from the SmartThings IDE"}
  sendEvent (name: "about", value:versionTxt) 
}

def appName(){
  def txt="Momentary Button Tile"
}
