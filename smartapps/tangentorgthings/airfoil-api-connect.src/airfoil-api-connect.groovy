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
        if (s.name == name) {
          null
        } else if (s.name == "Computer") {
          null
        } else {
          s.name
        }
      }

      options.removeAll([null])
      log.trace "Speaker options: ${options}"
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
    log.debug "addSpeakers()"
    addSpeakers()
  }

  if (ip) {
    doDeviceSync()
    runEvery5Minutes("doDeviceSync")
  }
}

def addSpeakers() {
  def speakers = getSpeakers()

  log.debug("getSpeakers() ${speakers}")

  speakers.collect { s ->
    selectedSpeakers.findAll { selected ->
      selected == s.name
    }.first {
      def dni = app.id + "/" + s.id
      log.debug("DNI: ${dni}")

      def d = getChildDevice(dni)
      if(!d) {
        d = addChildDevice("tangentorgthings", "Airfoil Speaker", dni, null, ["label": "${s.name}@${name}"])
        log.debug "created ${d.displayName} with id $dni"
        d.refresh()
      } else {
        log.debug "found ${d.displayName} with id $dni already exists, type: '$d.typeName'"
      }
      s.dni = dni
      return s
    }
  }
  atomicState.speakers = speakers
  log.trace "Set atomicState.speakers to ${speakers}"
}

def locationHandler(evt) {
  def map = stringToMap(evt.stringValue)

  def body = getHttpBody(map.body);

  log.debug("DESCRIPTION: $body")
}

def getSpeakers() {
  atomicState.speakers ?: [:]
}

private def parseEventMessage(Map event) {
  return event
}

def doDeviceSync(){
  if (! state.subscribe) {
    subscribe(location, null, locationHandler, [filterEvents:false])
    state.subscribe = true
  }

  poll()
}

def on(childDevice) {
  log.debug "Executing 'on'"
  post("/speakers/${getId(childDevice)}/connect", "", getId(childDevice))
}

def off(childDevice) {
  log.debug "Executing 'off'"
  post("/speakers/${getId(childDevice)}/disconnect", "", getId(childDevice))
}

def setLevel(childDevice, level) {
  post("/speakers/${getId(childDevice)}/volume", "${level}", getId(childDevice))
}

private getId(childDevice) {
  return childDevice.device?.deviceNetworkId.split("/")[-1]
}

private poll() {
  def uri = "/speakers"
  log.debug "GET: ${uri} HOST: ${ip}:${port}";

  sendHubCommand (
    new physicalgraph.device.HubAction([
      method: "GET",
      path: "$uri",
      headers: [ 
                  HOST: "${ip}:${port}", 
                  Accept: "application/json"
      ]],
      "",
      ""//[ callback: pollCallback ]
    )
  )
}

def pollCallback( result ) {
}

private post(path, text, dni) {
  def uri = "$path"

  log.debug "POST:  $uri"

  sendHubCommand (
    new physicalgraph.device.HubAction([
      method: "POST",
      path: "${uri}",
      headers: [ 
                  HOST: "${ip}:${port}", 
                  Accept: "application/json"
      ],
      body: "${text}"
    ],
    "",//dni,
    ""//[ callback: pollCallback ]
    )
  )
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
