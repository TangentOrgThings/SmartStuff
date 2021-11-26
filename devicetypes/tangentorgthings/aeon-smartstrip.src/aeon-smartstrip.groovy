/**
 *  Copyright Brian Aker
 *  Copyright 2017 Eric Maycock
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

import physicalgraph.*

metadata {
  definition (name: "Aeon SmartStrip", namespace: "tangentorgthings", author: "Brian Aker", 
              ocfDeviceType: "oic.d.switch",
              vid:"generic-switch-power-energy") {
		capability "Switch"
		capability "Energy Meter"
		capability "Power Meter"
		capability "Refresh"
		capability "Configuration"
		capability "Actuator"
		capability "Sensor"
    capability "Temperature Measurement"
    capability "Health Check"

		command "reset"

    fingerprint mfr: "0086", prod: "0003", model: "000B"
		fingerprint deviceId: "0x1001", inClusters: "0x25,0x32,0x27,0x70,0x85,0x72,0x86,0x60", outClusters: "0x82"
	}

	// simulator metadata
	simulator {
		status "on":  "command: 2003, payload: FF"
		status "off":  "command: 2003, payload: 00"
		status "power": new physicalgraph.zwave.Zwave().meterV1.meterReport(
		        scaledMeterValue: 30, precision: 3, meterType: 4, scale: 2, size: 4).incomingMessage()
		status "energy": new physicalgraph.zwave.Zwave().meterV1.meterReport(
		        scaledMeterValue: 200, precision: 3, meterType: 0, scale: 0, size: 4).incomingMessage()

		// reply messages
		reply "2001FF,delay 100,2502": "command: 2503, payload: FF"
		reply "200100,delay 100,2502": "command: 2503, payload: 00"
	}
    
  preferences {
    input("enableDebugging", "boolean", title:"Enable Debugging", value:false, required:false, displayDuringSetup:false)
  }

  // tile definitions
  tiles(scale: 2) {
    multiAttributeTile(name:"switch", type: "lighting", width: 6, height: 4, canChangeIcon: true){
      tileAttribute ("device.switch", key: "PRIMARY_CONTROL") {
        attributeState "on", label: '${name}', action: "switch.off", icon: "st.switches.switch.on", backgroundColor: "#00a0dc"
        attributeState "off", label: '${name}', action: "switch.on", icon: "st.switches.switch.off", backgroundColor: "#ffffff"
      }
    }

    valueTile("power", "device.power") {
      state "default", label:'${currentValue} W'
    }
    valueTile("energy", "device.energy") {
      state "default", label:'${currentValue} kWh'
    }
    standardTile("reset", "device.energy", inactiveLabel: false, decoration: "flat", width: 2, height: 2) {
      state "default", label:'reset kWh', action:"reset"
    }
    standardTile("refresh", "device.power", inactiveLabel: false, decoration: "flat", width: 2, height: 2) {
      state "default", label:'', action:"refresh.refresh", icon:"st.secondary.refresh"
    }
    standardTile("configure", "device.configure", inactiveLabel: false, width: 2, height: 2, decoration: "flat") {
      state "configure", label:'', action:"configuration.configure", icon:"st.secondary.configure"
    }
    valueTile("temperature", "device.temperature", inactiveLabel: false, width: 2, height: 2) {
      state "temperature", label:'${currentValue}',
      backgroundColors:
      [
      [value: 31, color: "#153591"],
      [value: 44, color: "#1e9cbb"],
      [value: 59, color: "#90d2a7"],
      [value: 74, color: "#44b621"],
      [value: 84, color: "#f1d801"],
      [value: 95, color: "#d04e00"],
      [value: 96, color: "#bc2323"]
      ]
    }

    main(["switch"])
    details(["switch",
    "temperature", "refresh","reset", "configure"])
  }
}

def parse(String description) {
  def result = []
  if (description.startsWith("Err")) {
    result = createEvent(descriptionText:description, isStateChange:true)
  } else if (description != "updated") {
    def cmd = zwave.parse(description, [0x20: 1, 0x21: 1, 0x25: 1, 0x27: 1, 0x30: 1, 0x31: 1, 0x32: 3, 0x33: 1, 0x60: 3, 0x70: 1, 0x72: 2, 0x82: 1, 0x85: 1, 0x86: 1])
    logging("Command: ${cmd}: ${description}")
    if (cmd) {
      result += zwaveEvent(cmd, null)
    }
  }

  //log.debug "parsed '${description}' to ${result.inspect()}"
  updateLastCheckIn()

  return result
}


void updateLastCheckIn() {
  if (!isDuplicateCommand(state.lastCheckInTime, 60000)) {
    state.lastCheckInTime = new Date().time

    sendEvent(name: "lastCheckIn", value: convertToLocalTimeString(new Date()), displayed: false)
  }
}

boolean isDuplicateCommand(lastExecuted, allowedMil) {
  !lastExecuted ? false : (lastExecuted + allowedMil > new Date().time)
}

String convertToLocalTimeString(dt) {
  try {
    def timeZoneId = location?.timeZone?.ID
    if (timeZoneId) {
      return dt.format("MM/dd/yyyy hh:mm:ss a", TimeZone.getTimeZone(timeZoneId))
    }
    else {
      return "$dt"
    }
  }
  catch (ex) {
    return "$dt"
  }
}

def endpointEvent(endpoint, map) {
  logging("endpointEvent($endpoint, $map)")
  if (endpoint) {
    // map.name = map.name + endpoint.toString()
    String childDni = "${device.deviceNetworkId}:${endpoint}"
    def child = childDevices.find { it.deviceNetworkId == childDni }
    child?.sendEvent(map)
  } else {   
    sendEvent(map)
  }

  return []
}

def zwaveEvent(physicalgraph.zwave.commands.multichannelv3.MultiChannelEndPointReport cmd, ep = null) {
  logging("MultiChannelEndPointReport() $cmd")
  def cmds = []

  if (!childDevices) {
    addChildSwitches(cmd.endPoints)

    // Undocumented Metered Outlets ( no switch control ) 
    addChildEnergyMeter(cmd.endPoints, 2)
  }

  return cmds
}

def zwaveEvent(physicalgraph.zwave.commands.multichannelv3.MultiChannelCapabilityReport cmd, ep) {
  logging("MultiChannelCapabilityReport() $cmd")
  def cmds = []

  return cmds
}

def zwaveEvent(physicalgraph.zwave.commands.multichannelv3.MultiChannelCmdEncap cmd, ep) {
  logging("MultiChannelCmdEncap() $cmd")
  if (cmd.commandClass == 0x6C && cmd.parameter.size >= 4) { // Supervision encapsulated Message
    // Supervision header is 4 bytes long, two bytes dropped here are the latter two bytes of the supervision header
    cmd.parameter = cmd.parameter.drop(2)
    // Updated Command Class/Command now with the remaining bytes
    cmd.commandClass = cmd.parameter[0]
    cmd.command = cmd.parameter[1]
    cmd.parameter = cmd.parameter.drop(2)
  }
  def encapsulatedCommand = cmd.encapsulatedCommand([0x32: 3, 0x31: 1, 0x25: 1, 0x20: 1])
  if (encapsulatedCommand) {
    Integer endpoint = cmd.sourceEndPoint
    logging("encapsulatedCommand($endpoint) $encapsulatedCommand")
    if (encapsulatedCommand.commandClassId == 0x32) {
      // Metered outlets are numbered differently than switches
      if (endpoint == 1 || endpoint == 2) {
        return zwaveEvent(encapsulatedCommand, endpoint + 4)
      } else if (endpoint > 2 && endpoint <= 6 ) {
        return zwaveEvent(encapsulatedCommand, endpoint - 2)
      } else if (endpoint == 0) {
        return zwaveEvent(encapsulatedCommand, 0)
      } else {
        log.warn "Ignoring metered outlet ${endpoint} msg: ${encapsulatedCommand}"
        return []
      }
    } else {
      if (endpoint >= 0 && endpoint <= 4 ) {
        return zwaveEvent(encapsulatedCommand, endpoint)
      } else {
        log.warn "Ignoring switch ${endpoint} msg: ${encapsulatedCommand}"
        return []
      }
    }
  } else {
    log.warn "Unknown encapsulatedCommand ${cmd}"
  }

  return []
}

def zwaveEvent(physicalgraph.zwave.commands.basicv1.BasicReport cmd, endpoint) {
  logging("BasicReport(${endpoint})")
  def cmds = []

  if (endpoint) {
    cmds << encap(zwave.switchBinaryV1.switchBinaryGet(), endpoint)
  } else {
    (1..4).each { n ->
      cmds << encap(zwave.switchBinaryV1.switchBinaryGet(), n)
      cmds << "delay 1000"
    }
  }

  return response(cmds)
}

def zwaveEvent(physicalgraph.zwave.commands.switchbinaryv1.SwitchBinaryReport cmd, endpoint) {
  logging("SwitchBinaryReport(${endpoint})")

  def map = [name: "switch", value: (cmd.value ? "on" : "off"), isStateChange: true]

  def events = [endpointEvent(endpoint, map)]
  def cmds = []
  // Was (!endpoint && events[0].isStateChange)
  if (!endpoint) {
    // events += (1..4).collect { ep -> endpointEvent(ep, map.clone()) }
    cmds << "delay 3000"
    cmds += delayBetween((1..6).collect { ep -> encap(zwave.meterV2.meterGet(scale: 2), ep) })
  } else {
    // Add 2 for outlet offset
    cmds << encap(zwave.meterV2.meterGet(scale: 2), endpoint +2)
    if (cmd.value) {
      events += [endpointEvent(null, [name: "switch", value: "on", isStateChange: true])]
    } else {
      def allOff = true
      if (0) { // Needs to be checked
        (1..4).each { n ->
          if (n != endpoint) {
            if (device.currentState("switch${n}").value != "off") allOff = false
          }
        }
      }
      if (allOff) {
        events += [endpointEvent(null, [name: "switch", value: "off", isStateChange: true])]
      }
    }

  }

  if (cmds) events << response(cmds)

    return events
}

def zwaveEvent(physicalgraph.zwave.commands.meterv3.MeterReport cmd, endpoint) {
  logging("MeterReport(${endpoint})")
  def event = [:]
  def cmds = []

  def val = Math.round(cmd.scaledMeterValue*100)/100
  switch (cmd.scale) {
    case 0x00:
    event = endpointEvent(endpoint, [name: "energy", value: val, unit: "kWh"])
    break;
    case 0x01:
    event = endpointEvent(endpoint, [name: "energy", value: val, unit: "kVAh"])
    break; 
    case 0x02:
    event = endpointEvent(endpoint, [name: "power", value: val, unit: "W"])
    break;
    default:
    break;
  }

  // check if we need to request temperature
  if (!state.lastTempReport || (now() - state.lastTempReport)/60000 >= 5) {
    cmds << zwave.configurationV1.configurationGet(parameterNumber: 90).format()
    cmds << "delay 400"
  }

  cmds ? [event, response(cmds)] : event
}

def zwaveEvent(physicalgraph.zwave.commands.manufacturerspecificv2.ManufacturerSpecificReport cmd, ep) {
  updateDataValue("MSR", String.format("%04X-%04X-%04X", cmd.manufacturerId, cmd.productTypeId, cmd.productId))
  return null
}

def zwaveEvent(physicalgraph.zwave.commands.configurationv1.ConfigurationReport cmd, ep) {
  def temperatureEvent
  if (cmd.parameterNumber == 90) { 
    def temperature = convertTemp(cmd.configurationValue)
    if(getTemperatureScale() == "C"){
      temperatureEvent = [name:"temperature", value: Math.round(temperature * 100) / 100]
    } else {
      temperatureEvent = [name:"temperature", value: Math.round(celsiusToFahrenheit(temperature) * 100) / 100]
    }
    state.lastTempReport = now()
  } else {
    //log.debug "${device.displayName} parameter '${cmd.parameterNumber}' with a byte size of '${cmd.size}' is set to '${cmd.configurationValue}'"
  }

  if (temperatureEvent) { 
    sendEvent(temperatureEvent) 
  }

  return null
}

def zwaveEvent(physicalgraph.zwave.Command cmd, ep) {
  logging("${device.displayName}: Unhandled ${cmd}" + (ep ? " from endpoint $ep" : ""))
}

private addChildSwitches(numberOfSwitches) {
  logging("${device.displayName} - Executing addChildSwitches()")
  for (def endpoint : 1..numberOfSwitches) {
    try {
      String childDni = "${device.deviceNetworkId}:$endpoint"
      def componentLabel = "${device.displayName} Outlet ${endpoint}"
      def childDthName = "Child Metering Switch"
      addChildDevice("smartthings", childDthName, childDni, device.getHub().getId(), [
        completedSetup  : true,
        label     : componentLabel,
        isComponent   : false
      ])
    } catch(Exception e) {
      log.warn "Exception: ${e}"
    }
  }
}

private addChildEnergyMeter(numberOfSwitches, numberOfMeters) {
  logging("${device.displayName} - Executing addChildSwitches()")

  def startNum = numberOfSwitches + 1
  def endNum = numberOfSwitches + numberOfMeters
  for (def endpoint : startNum..endNum) {
    try {
      String childDni = "${device.deviceNetworkId}:$endpoint"
      def componentLabel = "${device.displayName} Metered Outlet ${endpoint}"
      def childDthName = "Child Energy Meter"
      addChildDevice("smartthings", childDthName, childDni, device.getHub().getId(), [
        completedSetup  : true,
        label     : componentLabel,
        isComponent   : false
      ])
    } catch(Exception e) {
      log.warn "Exception: ${e}"
    }
  }
}

def getSwitchId(deviceNetworkId) {
  def split = deviceNetworkId?.split(":")
  return (split.length > 1) ? split[1] as Integer : null
}   

def childReset(deviceNetworkId) {
  def switchId = getSwitchId(deviceNetworkId)
  if (switchId != null) {
    logger("Child reset switchId: ${switchId}")
    sendHubCommand reset(switchId)
  }
}

def childOnOff(deviceNetworkId, value) {
  def switchId = getSwitchId(deviceNetworkId)
  if (switchId != null) sendHubCommand onOffCmd(value, switchId)
}

def childOn(deviceNetworkId) {
  childOnOff(deviceNetworkId, 0xFF)
}

def childOff(deviceNetworkId) {
  childOnOff(deviceNetworkId, 0x00)
}

private onOffCmd(value, endpoint = 0) {
  def cmds = []

  cmds += encap(zwave.basicV1.basicSet(value: value), endpoint)
  cmds += encap(zwave.basicV1.basicGet(), endpoint)

  if (0 && deviceIncludesMeter()) {
    cmds += "delay 3000"
    cmds += encap(zwave.meterV3.meterGet(scale: 0), endpoint)
    cmds += encap(zwave.meterV3.meterGet(scale: 2), endpoint)
  }

  delayBetween(cmds)
}

def on() {
  def cmds = []

  (1..4).each { endpoint ->
    cmds << encap(zwave.basicV1.basicSet(value: 255), endpoint)
  }

  (1..4).each { endpoint ->
    cmds << encap(zwave.switchBinaryV1.switchBinaryGet(), endpoint)
  }

  delayBetween(cmds, 1000)
}

def off() {
  def cmds = [
    encap(zwave.basicV1.basicSet(value: 0), 0),
    "delay 500",
    encap(zwave.switchBinaryV1.switchBinaryGet(), 0)
  ]

  delayBetween(cmds, 1000)
}

def refreshSyncStatus() {
  refresh()
}

def refresh(switchId = 0) {
  logging("refresh()")

  def cmds = [
  // zwave.basicV1.basicGet().format(),
  zwave.meterV2.meterGet(scale: 0).format(),
  zwave.meterV2.meterGet(scale: 2).format(),
  // encap(zwave.basicV1.basicGet(), 1)  // further gets are sent from the basic report handler
  ]
  // cmds << encap(zwave.switchBinaryV1.switchBinaryGet(), null)

  if (switchId) {
    if (switchId > 4) {
      cmds << encap(zwave.meterV2.meterGet(scale: 0), switchId -4)
      cmds << encap(zwave.meterV2.meterGet(scale: 2), switchId -4)
    } else {
      cmds << encap(zwave.switchBinaryV1.switchBinaryGet(), switchId)
      cmds << encap(zwave.meterV2.meterGet(scale: 0), switchId +2)
      cmds << encap(zwave.meterV2.meterGet(scale: 2), switchId +2)
    }
  } else {
    (1..4).each { endpoint ->
      cmds << encap(zwave.switchBinaryV1.switchBinaryGet(), endpoint)
    }

    if (1) {
      (1..6).each { endpoint ->
        cmds << encap(zwave.meterV2.meterGet(scale: 0), endpoint)
        cmds << encap(zwave.meterV2.meterGet(scale: 2), endpoint)
      }
      [90, 101, 102, 111, 112].each { p ->
        cmds << zwave.configurationV1.configurationGet(parameterNumber: p).format()
      }
    }
  }

  sendCommands(cmds, 1000)
  if (0) {
    delayBetween(cmds, 1000)
  }
}

def ping() {
  logging("ping")
  return encap(zwave.meterV2.meterGet(scale: 2), 0)
}

def resetCmd(endpoint = null) {
  logging("resetCmd($endpoint)")
  delayBetween([
    encap(zwave.meterV2.meterReset(), endpoint),
    encap(zwave.meterV2.meterGet(scale: 0), endpoint)
  ])
}

def reset() {
  logging("reset()")
  delayBetween([resetCmd(null), reset1(), reset2(), reset3(), reset4(), reset5(), reset6()])
}

def childRefresh(deviceNetworkId) {
  return refresh()

  def switchId = getSwitchId(deviceNetworkId)
  if (switchId != null) {
    sendHubCommand refresh(switchId)
  } 
}

def reset1() { resetCmd(1) }
def reset2() { resetCmd(2) }
def reset3() { resetCmd(3) }
def reset4() { resetCmd(4) }
def reset5() { resetCmd(5) }
def reset6() { resetCmd(6) }

def configure() {
  logging("configure()")

  state.resyncAll = true

  runIn(20, executeConfigureCmds)

  return []
}

void executeConfigureCmds() {
  state.enableDebugging = settings.enableDebugging
  logging("executeConfigureCmds()")

  runIn(6, refreshSyncStatus)

  sendEvent(name: "checkInterval", value: 2 * 60 * 60 + 2 * 60, displayed: false, data: [protocol: "zwave", hubHardwareId: device.hub.hardwareID])
  def cmds = [
  // Configuration of what to include in reports and how often to send them (if the below "change" conditions are met
  // Parameter 101 & 111: Send energy reports every 60 seconds (if conditions are met)
  // Parameter 102 & 112: Send power reports every 15 seconds (if conditions are met)
  zwave.configurationV1.configurationSet(parameterNumber: 101, size: 4, configurationValue: [0,0,0,127]).format(),
  zwave.configurationV1.configurationSet(parameterNumber: 102, size: 4, configurationValue: [0,0,127,0]).format(),
  zwave.configurationV1.configurationSet(parameterNumber: 111, size: 4, scaledConfigurationValue: 600).format(),
  zwave.configurationV1.configurationSet(parameterNumber: 112, size: 4, scaledConfigurationValue: 600).format(),
  ]
  [5, 6, 7, 8, 9, 10, 11].each { p ->
    // Send power reports at the time interval if they have changed by at least 1 watt
    cmds << zwave.configurationV1.configurationSet(parameterNumber: p, size: 2, scaledConfigurationValue: 25).format()
  }
  [12, 13, 14, 15, 16, 17, 18].each { p ->
    // Send energy reports at the time interval if they have changed by at least 5%
    cmds << zwave.configurationV1.configurationSet(parameterNumber: p, size: 1, scaledConfigurationValue: 5).format()
  }
  cmds += [
    // Parameter 4: Induce automatic reports at the time interval if the above conditions are met to reduce network traffic 
    zwave.configurationV1.configurationSet(parameterNumber: 4, size: 1, scaledConfigurationValue: 1).format(),
    // Parameter 80: Enable to send automatic reports to devices in association group 1
    zwave.configurationV1.configurationSet(parameterNumber: 80, size: 1, scaledConfigurationValue: 2).format(),
  ]

  cmds += [
  zwave.multiChannelV3.multiChannelEndPointGet().format(),
  // zwave.multiChannelV3.multiChannelCapabilityGet(endPoint: 1).format(),
  // zwave.multiChannelV3.multiChannelCapabilityGet(endPoint: 2).format(),
  // zwave.multiChannelV3.multiChannelCapabilityGet(endPoint: 3).format(),
  // zwave.multiChannelV3.multiChannelCapabilityGet(endPoint: 4).format(),    
  ]

  if (0) {
    delayBetween(cmds, 1000) + "delay 5000" + refresh()
  }
  sendCommands(cmds, 1000)
}

def installed() {
  logging("installed()")
  initialize()

  return []
}

void initialize() {
  if (!state.initialized) {
    logging("initialize()")

    if (0 && childDevices) {
      childDevices.each {
        try {
          deleteChildDevice(it.deviceNetworkId)
        }
        catch (e) {
          log.warn "Error deleting ${it.deviceNetworkId}: ${e}"
        }
      }
    }

    state.initialized = true
  }
}

def updated() {
  if (!isDuplicateCommand(state.lastUpdated, 2000)) {
    state.lastUpdated = new Date().time

    logging("updated()")

    initialize()

    runIn(2, executeConfigureCmds)
  }
  return []
}

void sendCommands(List<String> cmds, Integer delay=500) {
  if (cmds) {
    def actions = []
    cmds.each {
      actions << new physicalgraph.device.HubAction(it)
    }
    sendHubCommand(actions, delay)
  }
}

private encap(cmd, endpoint) {
  if (endpoint) {
    if (0 && cmd.commandClassId == 0x32) {
      // Metered outlets are numbered differently than switches
      if (endpoint == 5 || endpoint == 6) {
        endpoint -= 4
      }
      else if (endpoint < 0x80) {
        endpoint += 2
      } else {
        endpoint = ((endpoint & 0x7F) << 2) | 0x80
      }
    }
    zwave.multiChannelV3.multiChannelCmdEncap(destinationEndPoint:endpoint).encapsulate(cmd).format()
  } else {
    cmd.format()
  }
}

def convertTemp(value) {
  def highbit = value[0]
  def lowbit = value[1]

  if (highbit > 127) highbit = highbit - 128 - 128
    lowbit = lowbit * 0.00390625

    return highbit+lowbit
}

private def logging(message) {
  if (state.enableDebugging == "true") log.debug message
}
