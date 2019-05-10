// vim: set filetype=groovy tabstop=2 shiftwidth=2 softtabstop=2 expandtab smarttab :
/*
    The MIT License (MIT)

    Copyright (c) 2018-2019 Brian Aker <brian@tangent.org>
    Copyright (c) 2015 Jesse Newland

    Permission is hereby granted, free of charge, to any person obtaining a copy
    of this software and associated documentation files (the "Software"), to deal
    in the Software without restriction, including without limitation the rights
    to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
    copies of the Software, and to permit persons to whom the Software is
    furnished to do so, subject to the following conditions:

    The above copyright notice and this permission notice shall be included in
    all copies or substantial portions of the Software.

    THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
    IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
    FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
    AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
    LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
    OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
    THE SOFTWARE.
 */


import groovy.json.JsonSlurper

/*
  Forked from https://github.com/jnewland/airfoil-api-smartthings
 */

definition(
  name: "Airfoil API Connect",
  namespace: "TangentOrgThings",
  author: "Jesse Newland",
  description: "Connect to a local copy of Airfoil API to add and control Airfoil connected Speakers",
  category: "SmartThings Labs",
  iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
  iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
  iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")

preferences {
  page(name: "config")
}

def config() {
  dynamicPage(name: "config", title: "Airfoil API", install: true, uninstall: true) {

    section("Please enter the details of the running copy of Airfoil API you want to control") {
      input(name: "ip", type: "text", title: "IP", description: "Airfoil API IP", required: true, submitOnChange: true)
      input(name: "port", type: "text", title: "Port", description: "Airfoil API port", required: true, submitOnChange: true)
      input(name: "name", type: "text", title: "Name", description: "Computer Name", required: true, submitOnChange: true)
    }

    if (ip && port) {
      int speakerRefreshCount = !state.speakerRefreshCount ? 0 : state.speakerRefreshCount as int
      state.speakerRefreshCount = speakerRefreshCount + 1
      doDeviceSync()

      def options = getSpeakers().collect { s ->
          if (s.id ==~ /com.rogueamoeba.airfoil.LocalSpeaker/) {
            next
          } else  if (s.id ==~ /com.rogueamoeba.group.*/) {
            next
          } else {
              s.name
          }
      }

      options.removeAll([null])
      def numFound = options.size() ?: 0

      if (name) {
        section("Please wait while we discover your speakers. Select your devices below once discovered.") {
          input name: "selectedSpeakers", type: "enum", required:false, title:"Select Speakers (${numFound} found)", multiple:true, options:options
        }
      }
    }
  }
}

def installed() {
  initialize()
}

def updated() {
  initialize()
}

def initialize() {
  if (selectedSpeakers) {
    addSpeakers()
  }

  if (ip) {
    doDeviceSync()
    runEvery5Minutes("doDeviceSync")
  }
}

def addSpeakers() {
  def speakers = getSpeakers()

  speakers.each { s ->
      if (s.id ==~ /com.rogueamoeba.airfoil.LocalSpeaker/) {
        next
      }
      
      if (s.id ==~ /com.rogueamoeba.group.*/) {
        next
      }
      
      def dni = app.id + "/" + s.id
      logger("DNI: ${dni}")

      def d = getChildDevice(dni)
      if (! d) {
        d = addChildDevice("tangentorgthings", "Airfoil Speaker", dni, null, ["label": "${s.name}@${name}"])
        d.refresh()
      } else {
        logger "found ${d.displayName} with id $dni already exists, type: '$d.typeName'"
      }
      s.dni = dni
      return s
    }

  atomicState.speakers = speakers
}

def setSpeaker( speaker ) {
  def dni = app.id + "/" + speaker.id
  def dev = getChildDevice(dni)
  if (dev) {
    if (speaker.connected == "true") {
      sendEvent(dev.deviceNetworkId, [name: "switch", value: "on"])
    } else {
      sendEvent(dev.deviceNetworkId, [name: "switch", value: "off"])
    }

    if (speaker.volume) {
      def level = Math.round(speaker.volume * 100.00)
      sendEvent(dni, [name: "level", value: level])
    }
  }
}

def locationHandler(evt) {
  def map = stringToMap(evt.stringValue)

  def body = getHttpBody(map.body);

  if (body) {
    if (body instanceof java.util.HashMap) {
        setSpeaker( body )
    } else if (body instanceof java.util.List) {
        atomicState.speakers = body
        body.each { s ->
                if (s instanceof java.util.HashMap) {
                } else {
                  logger("bad: ${s}")
                }
                setSpeaker( s )
        }
    } else {
        setSpeaker( body )
    }
  }
}

void stateHandler(physicalgraph.device.HubResponse resp) {
  def parsedEvent = parseLanMessage(resp.description)
  def body = new groovy.json.JsonSlurper().parseText(parsedEvent.body)

  if (body) {
    if (body instanceof java.util.HashMap) {
      setSpeaker( body )
    } else if (body instanceof java.util.List) {
      atomicState.speakers = body
      body.each { s ->
        if (s instanceof java.util.HashMap) {
        } else {
          logger("bad: ${s}")
        }
        setSpeaker( s )
      }
    } else {
      setSpeaker( body )
    }
  }
}

void speakerHandler(physicalgraph.device.HubResponse resp) {
  def parsedEvent = parseLanMessage(resp.description)
  def body = new groovy.json.JsonSlurper().parseText(parsedEvent.body)

  if (body) {
    setSpeaker( body )
  }
}

def getSpeakers() {
  atomicState.speakers ?: [:]
}

private def parseEventMessage(Map event) {
  return event
}

def doDeviceSync(){
  if (! state.isSubscribed) {
    subscribe(location, null, locationHandler, [filterEvents:false])
    state.isSubscribed = true
  }

  poll()
}

def on(childDevice) {
  logger("on()")
  post("/speakers/${getId(childDevice)}/connect", "", getId(childDevice))
}

def off(childDevice) {
  logger("off()")
  post("/speakers/${getId(childDevice)}/disconnect", "", getId(childDevice))
}

def setLevel(childDevice, level) {
  post("/speakers/${getId(childDevice)}/volume", "${level}", getId(childDevice))
}

private getId(childDevice) {
  return childDevice.device?.deviceNetworkId.split("/")[-1]
}

private poll() {
  sendHubCommand (
    new physicalgraph.device.HubAction([
      method: "GET",
      path: "/speakers",
      headers: [ 
                  HOST: "${ip}:${port}", 
                  Accept: "application/json"
      ]],
      "${ip}:${port}",
      [ callback: "stateHandler" ]
    ))
}

def pollCallback( result ) {
}

private post(path, text, dni) {
  def uri = path.replaceAll(' ', '%20')
  def length = text.getBytes().size().toString()

  if (length) {
      sendHubCommand (
        new physicalgraph.device.HubAction([
          method: "POST",
          path: "${uri}",
          headers: [ 
                      HOST: "${ip}:${port}", 
                      "Content-type": "text/plain",
          ],
          body: "${text}"
        ],
          "${dni}", // dni
          [ callback: "speakerHandler" ]
        )
      )
  } else {
      sendHubCommand (
        new physicalgraph.device.HubAction([
          method: "POST",
          path: "${uri}",
          headers: [ 
                      HOST: "${ip}:${port}", 
                      "Content-type": "text/plain",
                      "Content-length": "$length"
          ]
        ],
          "${dni}", // dni
          [ callback: "speakerHandler" ]
        )
      )
  }
}

private getHttpBody(body) {
  def obj = null;
  if (body) {
    obj = new JsonSlurper().parseText(new String(body.decodeBase64()))
  }
  return obj
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
  String device_name = " : "
  String msg_text = (msg != null) ? "${msg}" : "<null>"

  Integer log_level = 5// isTraceEnabled() ? 5 : 2

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

