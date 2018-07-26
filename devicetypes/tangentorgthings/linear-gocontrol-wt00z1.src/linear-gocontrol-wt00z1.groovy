// vim :set ts=2 sw=2 sts=2 expandtab smarttab :
/**
 *  Copyright 2017-2018 Brian Aker <brian@tangent.org>
 *  
 *  Linear GoControl WT00Z-1
 *  https://products.z-wavealliance.org/products/1028
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

def getDriverVersion() {
  return "v2.91"
}

metadata {
  definition (name: "Linear GoControl WT00Z-1", namespace: "TangentOrgThings", author: "brian@tangent.org", ocfDeviceType: "oic.d.light") {
    capability "Actuator"
    capability "Button"
    capability "Indicator"
    capability "Light"
    capability "Polling"
    capability "Refresh"
    capability "Sensor"
    capability "Switch Level"
    capability "Switch"

    attribute "DeviceReset", "enum", ["false", "true"]
    attribute "logMessage", "string"        // Important log messages.
    attribute "lastError", "string"        // Last error message
    attribute "parseErrorCount", "number"        // Last error message
    attribute "unknownCommandErrorCount", "number"        // Last error message

    attribute "MSR", "string"
    attribute "Manufacturer", "string"

    attribute "driverVersion", "string"
    attribute "firmwareVersion", "string"

    attribute "manufacturerId", "string"
    attribute "productId", "string"
    attribute "productType", "string"

    attribute "invertedStatus", "enum", ["false", "true"]

    attribute "NIF", "string"

    attribute "SwitchAll", "string"
    attribute "Power", "string"

    command "connect"
    command "disconnect"

    fingerprint mfr: "014f", prod: "5457", model: "3033", deviceJoinName: "Linear GoControl WT00Z-1"
  }

  simulator {
    status "on":  "command: 2003, payload: FF"
    status "off": "command: 2003, payload: 00"
    status "09%": "command: 2003, payload: 09"
    status "10%": "command: 2003, payload: 0A"
    status "33%": "command: 2003, payload: 21"
    status "66%": "command: 2003, payload: 42"
    status "99%": "command: 2003, payload: 63"

    // reply messages
    reply "2001FF,delay 5000,2602": "command: 2603, payload: FF"
    reply "200100,delay 5000,2602": "command: 2603, payload: 00"
    reply "200119,delay 5000,2602": "command: 2603, payload: 19"
    reply "200132,delay 5000,2602": "command: 2603, payload: 32"
    reply "20014B,delay 5000,2602": "command: 2603, payload: 4B"
    reply "200163,delay 5000,2602": "command: 2603, payload: 63"
  }

  preferences {
    input name: "DimStartLevel", type: "number", title: "Dim Start Level", description: "The WT00Z-1 can send Dim commands to Z-Wave enabled dimmers.The Dim command has a start level embedded in it. A dimmer receivingthis command will start dimming from that start level. However, thecommand can be sent so that the dimmer ignores the start level andinstead starts dimming from its current level. By default, the WT00Z-1sends the command so that the dimmer will start dimming from itscurrent dim level rather than the start level embedded in the command.To change this, simply set the conﬁguration parameter to 0.", range: "0..1", displayDuringSetup: false, defaultValue: 1
    input name: "SuspendGroup4", type: "number", title: "Suspend Group 4", description: "You may wish to disable transmitting commands to Z-Wave devices that are in Group 4 without 'un-associating' those devices from the group. Setting a value of 1 will stop the WT00Z-1 from transmitting to devices that are 'associated' into Group 4.", range: "0..1", displayDuringSetup: false, defaultValue: 0
    input name: "NightLight", type: "number", title: "Night Light", description: "Relationship between LED and status of devices in Group 1", range: "0..1", displayDuringSetup: false, defaultValue: 0
    input name: "EnableShadeControlGroup2", type: "number", title: "Enable Shade Control Group 2", description: "The switch can control shade control devices if this parameter is set to 1.", range: "0..1", displayDuringSetup: false, defaultValue: 1
    input name: "debugLevel", type: "number", title: "Debug Level", description: "Adjust debug level for log", range: "1..5", displayDuringSetup: false, defaultValue: 3 
  }

  tiles(scale: 2) {
    multiAttributeTile(name:"switch", type: "lighting", width: 6, height: 4, canChangeIcon: true){
      tileAttribute ("device.switch", key: "PRIMARY_CONTROL") {
        attributeState "on", label:'${name}', action:"disconnect", icon:"st.switches.switch.on", backgroundColor:"#00a0dc", nextState:"turningOff"
        attributeState "off", label:'${name}', action:"connect", icon:"st.switches.switch.off", backgroundColor:"#ffffff", nextState:"turningOn"
        attributeState "turningOn", label:'${name}', action:"disconnect", icon:"st.switches.switch.on", backgroundColor:"#00a0dc", nextState:"turningOff"
        attributeState "turningOff", label:'${name}', action:"connect", icon:"st.switches.switch.off", backgroundColor:"#ffffff", nextState:"turningOn"
      }
      tileAttribute ("device.level", key: "SLIDER_CONTROL") {
        attributeState "level", action:"switch level.setLevel"
      }
      tileAttribute("device.indicatorStatus", key: "SECONDARY_CONTROL") {
        attributeState("when off", label:'${currentValue}', icon:"st.indicators.lit-when-off")
        attributeState("when on", label:'${currentValue}', icon:"st.indicators.lit-when-on")
        attributeState("never", label:'${currentValue}', icon:"st.indicators.never-lit")
      }
    }

    standardTile("indicator", "device.indicatorStatus", width: 2, height: 2, inactiveLabel: false, decoration: "flat") {
      state "when off", action:"indicator.indicatorWhenOn", icon:"st.indicators.lit-when-off"
      state "when on", action:"indicator.indicatorNever", icon:"st.indicators.lit-when-on"
      state "never", action:"indicator.indicatorWhenOff", icon:"st.indicators.never-lit"
    }

    standardTile("refresh", "device.switch", width: 2, height: 2, inactiveLabel: false, decoration: "flat") {
      state "default", label:'', action:"refresh.refresh", icon:"st.secondary.refresh"
    }
    
    valueTile("driverVersion", "device.driverVersion", width: 2, height: 2, inactiveLabel: true, decoration: "flat") {
      state "default", label: '${currentValue}'
    }

    main "switch"
    details(["switch", "indicator", "driverVersion", "refresh"])
    // details(["switch", "level", "refresh"])
  }
}

def getCommandClassVersions() {
[
  0x20: 1,  // Basic
  0x25: 1,  // Switch Binary
  0x26: 1,  // SwitchMultilevel
  0x27: 1,  // Switch All
  0x70: 2,  // Configuration V1
  0x72: 2,  // Manufacturer Specific V1
  0x85: 2,  // Association  0x85  V1
  0x86: 1,  // Version
  0x87: 1,  // Indicator
]
}

def parse(String description) {
  def result = []

  if (description && description.startsWith("Err")) {
    log.error "parse error: ${description}"

    if (description.startsWith("Err 106")) {
      result << createEvent(
        descriptionText: "Security, possible key exchange error.",
        eventType: "ALERT",
        name: "secureInclusion",
        value: "failed",
        isStateChange: true,
      )
    }
  } else if (! description) {
    logger("$device.displayName parse() called with NULL description", "info")
  } else if (description != "updated") {
    def cmd = zwave.parse(description, getCommandClassVersions())

    if (cmd) {
      zwaveEvent(cmd, result)

    } else {
      logger("zwave.parse(getCommandClassVersions()) failed for: ${description}", "error")
      // Try it without check for classes
      cmd = zwave.parse(description)

      if (cmd) {
        zwaveEvent(cmd, result)
      } else {
        logger("zwave.parse(description) failed for: ${description}", "error")
      }
    }
  }

  return result
}

def zwaveEvent(physicalgraph.zwave.commands.basicv1.BasicReport cmd, result) {
  logger("$device.displayName $cmd")

  dimmerEvents(cmd.value, false, result);
}

// Physical press of the switch
def zwaveEvent(physicalgraph.zwave.commands.basicv1.BasicSet cmd, result) {
  logger("$device.displayName $cmd")

  dimmerEvents(cmd.value, true, result);

  if (cmd.value == 255) {
    buttonEvent("SceneActuatorConfGet()", 1, false, "physical")
  }
  else if (cmd.value == 0) {
    buttonEvent("SceneActuatorConfGet()", 2, false, "physical")
  }
}

def zwaveEvent(physicalgraph.zwave.commands.indicatorv1.IndicatorReport cmd, result) {
  logger("$device.displayName $cmd")
  result << createEvent(name: "switch", value: cmd.value ? "on" : "off", isStateChange: true, type: "digital")
}

def zwaveEvent(physicalgraph.zwave.commands.switchbinaryv1.SwitchBinaryReport cmd, result) {
  logger("$device.displayName $cmd")

  dimmerEvents(cmd.value, false, result);
}

def zwaveEvent(physicalgraph.zwave.commands.switchbinaryv1.SwitchBinarySet cmd, result) {
  logger("$device.displayName $cmd -- BEING CONTROLLED")

  dimmerEvents(cmd.switchValue, true, result);
}

def zwaveEvent(physicalgraph.zwave.commands.switchmultilevelv1.SwitchMultilevelReport cmd, result) {
  logger("$device.displayName $cmd")

  dimmerEvents(cmd.value, false, result)
}

def zwaveEvent(physicalgraph.zwave.commands.switchmultilevelv1.SwitchMultilevelSet cmd, result) {
  logger("$device.displayName $cmd")

  dimmerEvents(cmd.value, false, result)
}

def zwaveEvent(physicalgraph.zwave.commands.switchmultilevelv1.SwitchMultilevelStartLevelChange cmd, result) {
  logger("$device.displayName $cmd")
  dimmerEvents(cmd.startLevel, true, result);
}

def zwaveEvent(physicalgraph.zwave.commands.switchmultilevelv1.SwitchMultilevelStopLevelChange cmd, result) {
  logger("$device.displayName $cmd")
  result << response(zwave.switchMultilevelV1.switchMultilevelGet())
}

private dimmerEvents(Integer dimmer_level, boolean isPhysical, result) {
  def switch_value = (dimmer_level ? "on" : "off")
  result << createEvent(name: "switch", value: switch_value, type: isPhysical ? "physical" : "digital", isStateChange: true, displayed: true)

  if (dimmer_level && dimmer_level <= 100) {
    result << createEvent(name: "level", value: dimmer_level, unit: "%", isStateChange: true, displayed: true)
  }
}


def zwaveEvent(physicalgraph.zwave.commands.configurationv2.ConfigurationReport cmd, result) {
  logger("$device.displayName $cmd")

  def name = ""
  def value = ""
  def reportValue = cmd.configurationValue[0]
  switch (cmd.parameterNumber) {
    case 1:
    logger("Dim Start Level: ${reportValue}")
    if (settings.DimStartLevel != reportValue) {
      result << response( delayBetween([
        zwave.configurationV1.configurationSet(scaledConfigurationValue: settings.DimStartLevel, parameterNumber: cmd.parameterNumber, size: 1).format(),
        // zwave.configurationV1.configurationGet(parameterNumber: cmd.parameterNumber).format(),
      ], 2000) )
    }
    break
    case 2:
    logger("Suspend Group 4: ${reportValue}")
    if (settings.SuspendGroup4 != reportValue) {
      result << response( delayBetween([
        zwave.configurationV1.configurationSet(scaledConfigurationValue: settings.SuspendGroup4, parameterNumber: cmd.parameterNumber, size: 1).format(),
        // zwave.configurationV1.configurationGet(parameterNumber: cmd.parameterNumber).format(),
      ], 2000) )
    }
    break
    case 3:
    logger("Night Light: ${reportValue}")
    if (settings.NightLight != reportValue) {
      result << response( delayBetween([
        zwave.configurationV1.configurationSet(scaledConfigurationValue: settings.NightLight, parameterNumber: cmd.parameterNumber, size: 1).format(),
        // zwave.configurationV1.configurationGet(parameterNumber: cmd.parameterNumber).format(),
      ], 2000) )
    }
    break
    case 14:
    logger("Enable Shade Control Group 2: ${reportValue}")
    if (settings.EnableShadeControlGroup2 != reportValue) {
      result << response( delayBetween([
        zwave.configurationV1.configurationSet(scaledConfigurationValue: settings.EnableShadeControlGroup2, parameterNumber: cmd.parameterNumber, size: 1).format(),
        // zwave.configurationV1.configurationGet(parameterNumber: cmd.parameterNumber).format(),
      ], 2000) )
    }
    break
    default:
    break
  }

//  result <<  createEvent(name: name, value: value)
}

def zwaveEvent(physicalgraph.zwave.commands.switchallv1.SwitchAllReport cmd, result) {
  logger("$device.displayName $cmd")

  state.switchAllModeCache = cmd.mode

  def msg = ""
  switch (cmd.mode) {
    case 0:
    msg = "Device is excluded from the all on/all off functionality."
    break

    case 1:
    msg = "Device is excluded from the all on functionality but not all off."
    break

    case 2:
    msg = "Device is excluded from the all off functionality but not all on."
    break

    default:
    msg = "Device is included in the all on/all off functionality."
    break
  }
  logger("Switch All Mode: ${msg}","info")

  if (cmd.mode != 0) {
    sendCommands([
      zwave.switchAllV1.switchAllSet(mode: 0x00),
      zwave.switchAllV1.switchAllGet(),
    ])
  } else {
    result << createEvent(name: "SwitchAll", value: msg, isStateChange: true, displayed: true)
  }
}

def zwaveEvent(physicalgraph.zwave.commands.manufacturerspecificv2.ManufacturerSpecificReport cmd, result) {
  logger("$device.displayName $cmd")

  state.manufacturerId = cmd.manufacturerId
  state.productTypeId = cmd.productTypeId
  state.productId= cmd.productId
  state.manufacturer= cmd.manufacturerName ? cmd.manufacturerName : "Linear GoControl"

  String manufacturerId = String.format("%04X", cmd.manufacturerId)
  String productType = String.format("%04X", cmd.productTypeId)
  String productId = String.format("%04X", cmd.productId)

  def msr = String.format("%04X-%04X-%04X", cmd.manufacturerId, cmd.productTypeId, cmd.productId)
  updateDataValue("MSR", msr)
  updateDataValue("manufacturerName", state.manufacturer)
  state.MSR = "$msr"

  Integer[] parameters = [ 1, 2, 3, 4, 5, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 19, 20, 21 ]

  def cmds = []
  parameters.each {
    cmds << zwave.configurationV1.configurationGet(parameterNumber: it).format()
  }
  
  result << createEvent(name: "manufacturerId", value: manufacturerId)
  result << createEvent(name: "productType", value: productType)
  result << createEvent(name: "productId", value: productId)
  result << createEvent(name: "MSR", value: "$msr", descriptionText: "$device.displayName", isStateChange: false)
  result << createEvent(name: "Manufacturer", value: "${state.manufacturer}", descriptionText: "$device.displayName", isStateChange: false)
  result << response(delayBetween(cmds, 1000))
  result << response( zwave.versionV1.versionGet() )
}

def zwaveEvent(physicalgraph.zwave.commands.versionv1.VersionReport cmd, result) {
  logger("$device.displayName $cmd")

  def text = "$device.displayName: firmware version: ${cmd.applicationVersion}.${cmd.applicationSubVersion}, Z-Wave version: ${cmd.zWaveProtocolVersion}.${cmd.zWaveProtocolSubVersion}"
  def zWaveProtocolVersion = "${cmd.zWaveProtocolVersion}.${cmd.zWaveProtocolSubVersion}"
  state.firmwareVersion = cmd.applicationVersion+'.'+cmd.applicationSubVersion
  result << createEvent(name: "firmwareVersion", value: "V ${state.firmwareVersion}", descriptionText: "$text", isStateChange: true)
  result << createEvent(name: "zWaveProtocolVersion", value: "${zWaveProtocolVersion}", descriptionText: "${device.displayName} ${zWaveProtocolVersion}", isStateChange: true)
}

def buttonEvent(String exec_cmd, Integer button, held, buttonType = "physical") {
  logger("buttonEvent: $button  held: $held  type: $buttonType")

  String heldType = held ? "held" : "pushed"

  if (button > 0) {
    sendEvent(name: "button", value: "$heldType", data: [buttonNumber: button], descriptionText: "$device.displayName $exec_cmd button $button was pushed", isStateChange: true, type: "$buttonType")
  } else {
    sendEvent(name: "button", value: "default", descriptionText: "$device.displayName $exec_cmd button released", isStateChange: true, type: "$buttonType")
  }
}

def zwaveEvent(physicalgraph.zwave.commands.associationv2.AssociationGroupingsReport cmd, result) {
  logger("$device.displayName $cmd")

  state.groups = cmd.supportedGroupings

  if (cmd.supportedGroupings) {
    def cmds = []
    for (def x = 1; x <= cmd.supportedGroupings; x++) {
      cmds << zwave.associationV1.associationGet(groupingIdentifier: x).format()
    }

    result << response(delayBetween(cmds, 2000))

    return
  }
  
  logger("$device.displayName AssociationGroupingsReport: $cmd", "error")
}

def zwaveEvent(physicalgraph.zwave.commands.associationv2.AssociationReport cmd, result) {
  logger("$device.displayName $cmd")
}

def zwaveEvent(physicalgraph.zwave.Command cmd, result) {
  logger("$device.displayName command not implemented: $cmd", "error")
} 

def connect() {
  logger("$device.displayName connect()")  
  trueOn(true)
}

def on() {
  logger("$device.displayName on()")
  trueOn(false)
}

private trueOn(Boolean physical = true) {
  if (state.lastBounce && (Calendar.getInstance().getTimeInMillis() - state.lastBounce) < 2000 ) {
    logger("$device.displayName bounce", "warn")
    return
  }
  state.lastBounce = new Date().time

  if (physical) { // Add option to have digital commands execute buttons
    buttonEvent("on()", 1, false, "digital")
  }

  delayBetween([
      zwave.basicV1.basicSet(value: 0xFF).format(),
      zwave.switchMultilevelV1.switchMultilevelGet().format(),
  ], 5000)
}

def off() {
  logger("$device.displayName off()")
  
  if (settings.disbableDigitalOff) {
    logger("..off() disabled")
    return zwave.switchMultilevelV1.switchMultilevelGet().format();
  }
  
  trueOff(false)
}

def disconnect() {
  logger("$device.displayName disconnect()")
  trueOff(true)
}

private trueOff(Boolean physical = true) {   
  if (state.lastBounce && (Calendar.getInstance().getTimeInMillis() - state.lastBounce) < 2000 ) {
    logger("$device.displayName bounce", "warn")
    return
  }
  state.lastBounce = new Date().time

  if (physical) { // Add option to have digital commands execute buttons
    buttonEvent("off()", 2, false, "digital")
  }  
  sendEvent(name: "switch", value: "off");
    
  delayBetween([
    zwave.basicV1.basicSet(value: 0x00).format(),
    zwave.switchMultilevelV1.switchMultilevelGet().format(),
  ], 5000)
}

def setLevel (value) {
  logger ("setLevel >> value: $value", "debug")
  def valueaux = value as Integer
  def level = Math.max(Math.min(valueaux, 99), 0)

  delayBetween([
    zwave.switchMultilevelV1.switchMultilevelSet(value: level).format(),
    zwave.switchMultilevelV1.switchMultilevelGet().format(),
  ], 5000)
}


def setLevel(value, duration) {
  logger("setLevel >> value: $value, duration: $duration")

  def valueaux = value as Integer
  def level = Math.max(Math.min(valueaux, 99), 0)
  def dimmingDuration = duration < 128 ? duration : 128 + Math.round(duration / 60)
  def getStatusDelay = duration < 128 ? (duration*1000)+2000 : (Math.round(duration / 60)*60*1000)+2000

  delayBetween([
    zwave.switchMultilevelV1.switchMultilevelSet(value: level, dimmingDuration: dimmingDuration).format(),
    zwave.switchMultilevelV1.switchMultilevelGet().format(),
  ], getStatusDelay)
}

def poll() {
  logger("poll()")
  zwave.switchMultilevelV1.switchMultilevelGet().format()
}

/**
 * PING is used by Device-Watch in attempt to reach the Device
 * */
def ping() {
  logger("ping()")
  zwave.switchMultilevelV1.switchMultilevelGet().format()
}

def refresh() {
  logger("refresh()")

  def commands = []

  commands << zwave.switchMultilevelV1.switchMultilevelGet().format()

  delayBetween(commands)
}

void indicatorWhenOn() {
  sendEvent(name: "indicatorStatus", value: "when on", displayed: false)
  sendHubCommand(new physicalgraph.device.HubAction(zwave.configurationV1.configurationSet(configurationValue: [1], parameterNumber: 3, size: 1).format()))
}

void indicatorWhenOff() {
  sendEvent(name: "indicatorStatus", value: "when off", displayed: false)
  sendHubCommand(new physicalgraph.device.HubAction(zwave.configurationV1.configurationSet(configurationValue: [0], parameterNumber: 3, size: 1).format()))
}

void indicatorNever() {
  sendEvent(name: "indicatorStatus", value: "never", displayed: false)
  sendHubCommand(new physicalgraph.device.HubAction(zwave.configurationV1.configurationSet(configurationValue: [2], parameterNumber: 3, size: 1).format()))
}

def invertSwitch(invert=true) {
  if (invert) {
    zwave.configurationV1.configurationSet(configurationValue: [1], parameterNumber: 4, size: 1).format()
  } else {
    zwave.configurationV1.configurationSet(configurationValue: [0], parameterNumber: 4, size: 1).format()
  }
}

def prepDevice() {
  if (0) {
    [
    // zwave.configurationV1.configurationSet(configurationValue: [ledIndicator == "on" ? 1 : ledIndicator == "never" ? 2 : 0], parameterNumber: 3, size: 1),
    zwave.configurationV1.configurationSet(configurationValue: [invertSwitch == true ? 1 : 0], parameterNumber: 4, size: 1),
    zwave.configurationV1.configurationSet(configurationValue: [zwaveSteps], parameterNumber: 7, size: 1),
    zwave.configurationV1.configurationSet(configurationValue: [zwaveDelay], parameterNumber: 8, size: 1),
    zwave.configurationV1.configurationSet(configurationValue: [manualSteps], parameterNumber: 9, size: 1),
    zwave.configurationV1.configurationSet(configurationValue: [manualDelay], parameterNumber: 10, size: 1),
    zwave.configurationV1.configurationSet(configurationValue: [allonSteps], parameterNumber: 11, size: 1),
    zwave.configurationV1.configurationSet(configurationValue: [allonDelay], parameterNumber: 12, size: 1),
    zwave.configurationV1.configurationGet(parameterNumber: 3),
    zwave.configurationV1.configurationGet(parameterNumber: 4),
    zwave.configurationV1.configurationGet(parameterNumber: 7),
    zwave.configurationV1.configurationGet(parameterNumber: 8),
    zwave.configurationV1.configurationGet(parameterNumber: 9),
    zwave.configurationV1.configurationGet(parameterNumber: 10),
    zwave.configurationV1.configurationGet(parameterNumber: 11),
    zwave.configurationV1.configurationGet(parameterNumber: 12),
    ]
  }
  [
    // zwave.switchBinaryV1.switchBinaryGet(),
    zwave.manufacturerSpecificV1.manufacturerSpecificGet(),
    zwave.associationV1.associationGroupingsGet(),
    zwave.switchAllV1.switchAllGet(),
    zwave.switchMultilevelV1.switchMultilevelGet(),
    zwave.zwaveCmdClassV1.requestNodeInfo(),
  ]
}

def installed() {
  log.debug ("installed()")

  def zwInfo = getZwaveInfo()
  if ($zwInfo) {
    log.debug("$device.displayName $zwInfo")
    sendEvent(name: "NIF", value: "$zwInfo", isStateChange: true, displayed: true)
  } else {
    log.debug("$device.displayName has no ZwaveInfo")
  }


  // Device-Watch simply pings if no device events received for 86220 (one day minus 3 minutes)
  // sendEvent(name: "checkInterval", value: 2 * 15 * 60 + 2 * 60, displayed: false, data: [protocol: "zwave", hubHardwareId: device.hub.hardwareID, offlinePingable: "1"])

  sendEvent(name: "driverVersion", value: getDriverVersion(), isStateChange: true)
  sendCommands(prepDevice(), 3000)
}

def updated() {
  if ( state.updatedDate && ((Calendar.getInstance().getTimeInMillis() - state.updatedDate)) < 5000 ) {
    return
  }

  state.loggingLevelIDE = debugLevel ? debugLevel : 4
  log.info("$device.displayName updated() debug: ${state.loggingLevelIDE}")

  sendEvent(name: "lastError", value: "", displayed: false)
  sendEvent(name: "logMessage", value: "", displayed: false)
  sendEvent(name: "parseErrorCount", value: 0, displayed: false)
  sendEvent(name: "unknownCommandErrorCount", value: 0, displayed: false)

  /*
  def zwInfo = getZwaveInfo()
  if ($zwInfo) {
  log.debug("$device.displayName $zwInfo")
  sendEvent(name: "NIF", value: "$zwInfo", isStateChange: true, displayed: true)
  } else {
  log.debug("$device.displayName has no ZwaveInfo")
  }
   */

  // Device-Watch simply pings if no device events received for 86220 (one day minus 3 minutes)
  // sendEvent(name: "checkInterval", value: 2 * 15 * 60 + 2 * 60, displayed: false, data: [protocol: "zwave", hubHardwareId: device.hub.hardwareID, offlinePingable: "1"])

  sendEvent(name: "driverVersion", value: getDriverVersion(), isStateChange: true)
  sendEvent(name: "numberOfButtons", value: 4, displayed: false)
  sendCommands(prepDevice(), 3000)

  // Avoid calling updated() twice
  state.updatedDate = Calendar.getInstance().getTimeInMillis()
}

/*****************************************************************************************************************
 *  Private Helper Functions:
 *****************************************************************************************************************/

/**
 *  encapCommand(cmd)
 *
 *  Applies security or CRC16 encapsulation to a command as needed.
 *  Returns a physicalgraph.zwave.Command.
 **/
private encapCommand(physicalgraph.zwave.Command cmd) {
  if (state.sec) {
    return zwave.securityV1.securityMessageEncapsulation().encapsulate(cmd)
  }
  else if (state.useCrc16) {
    return zwave.crc16EncapV1.crc16Encap().encapsulate(cmd)
  }
  else {
    return cmd
  }
}

/**
 *  prepCommands(cmds, delay=200)
 *
 *  Converts a list of commands (and delays) into a HubMultiAction object, suitable for returning via parse().
 *  Uses encapCommand() to apply security or CRC16 encapsulation as needed.
 **/
private prepCommands(cmds, delay=200) {
  return response(delayBetween(cmds.collect{ (it instanceof physicalgraph.zwave.Command ) ? encapCommand(it).format() : it }, delay))
}

/**
 *  sendCommands(cmds, delay=200)
 *
 *  Sends a list of commands directly to the device using sendHubCommand.
 *  Uses encapCommand() to apply security or CRC16 encapsulation as needed.
 **/
private sendCommands(cmds, delay=200) {
  sendHubCommand( cmds.collect{ (it instanceof physicalgraph.zwave.Command ) ? response(encapCommand(it)) : response(it) }, delay)
}

/**
 *  logger()
 *
 *  Wrapper function for all logging:
 *    Logs messages to the IDE (Live Logging), and also keeps a historical log of critical error and warning
 *    messages by sending events for the device's logMessage attribute.
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
    if (state.loggingLevelIDE >= 2) {
      log.warn msg
      sendEvent(name: "logMessage", value: "WARNING: ${msg}", displayed: false, isStateChange: true)
    }
    return

    case "info":
    if (state.loggingLevelIDE >= 3) {
      log.info msg
    }
    return

    case "debug":
    if (state.loggingLevelIDE >= 4) {
      log.debug msg
    }
    return

    case "trace":
    if (state.loggingLevelIDE >= 5) {
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
