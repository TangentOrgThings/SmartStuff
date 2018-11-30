// vim: set filetype=groovy tabstop=2 shiftwidth=2 softtabstop=2 expandtab smarttab :

/**
 *
 *  Copyright 2018 Brian Aker <brian@tangent.org>
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

String getDriverVersion () {
  return "v0.01"
}

metadata {
  definition (name: "Virtual Contact Sensor", namespace: "tangentorgthings", author: "Brian Aker") {
    capability "Contact Sensor"
    capability "Momentary"
  }

    tiles {
        standardTile("momentary", "device.contact", width: 2, height: 2, canChangeIcon: true) {
            state "open", label: 'close me', action: "momentary.push", backgroundColor: "#00A0DC"
            state "closed", label: 'open me', action: "momentary.push", backgroundColor: "#FFFFFF"
        }
        main "momentary"
        details(["momentary"])
    }
}

def parse(String description) {
  log.info "parse($description)"
}

def installed() {
    log.info "installed()"
    sendEvent(name: "contact", value: "closed", isStateChange: true)
}

def updated() {
    log.info "updated()"
    
    if (! device.currentState("contact")) {
      sendEvent(name: "contact", value: "closed", isStateChange: true)
    }
}

def push() {
  log.info "push()"
  def toggleValue = "closed"
  
  if (device.currentState("contact") && device.currentState("contact").value.equals("closed")) {
    toggleValue = "open"
  }
    
  sendEvent(name: "contact", value: "$toggleValue", isStateChange: true)
  sendEvent(name: "momentary", value: "pushed", isStateChange: true)
}
