// vim :set tabstop=2 shiftwidth=2 sts=2 expandtab smarttab :
/**
 *  Copyright 2017-2018 Brian Aker <brian@tangent.org>
 *  Copyright 2015 SmartThings
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

def getDriverVersion () {
  return "v0.09"
}

metadata {
  definition (name: "Z-Wave Device Multichannel Advanced", namespace: "smartthings", author: "SmartThings") {
    capability "Actuator"
    capability "Switch"
    capability "Refresh"
    capability "Configuration"
    capability "Sensor"

    attribute "DeviceReset", "enum", ["false", "true"]
    attribute "logMessage", "string"        // Important log messages.
    attribute "lastError", "string"        // Last error message
    attribute "parseErrorCount", "number"        // Last error message
    attribute "unknownCommandErrorCount", "number"        // Last error message

    attribute "driverVersion", "string"
    attribute "firmwareVersion", "string"
    attribute "zWaveProtocolVersion", "string"
    attribute "FirmwareMdReport", "string"
    attribute "ManufacturerCode", "string"
    attribute "NIF", "string"
    attribute "ProduceTypeCode", "string"
    attribute "ProductCode", "string"
    
    fingerprint inClusters: "0x60"
    fingerprint inClusters: "0x60, 0x25"
    fingerprint inClusters: "0x60, 0x26"
    fingerprint inClusters: "0x5E, 0x59, 0x60, 0x8E"
  }

  simulator {
    status "on":  "command: 2003, payload: FF"
    status "off": "command: 2003, payload: 00"
    reply "8E010101,delay 800,6007": "command: 6008, payload: 4004"
    reply "8505": "command: 8506, payload: 02"
    reply "59034002": "command: 5904, payload: 8102003101000000"
    reply "6007":  "command: 6008, payload: 0002"
    reply "600901": "command: 600A, payload: 10002532"
    reply "600902": "command: 600A, payload: 210031"
  }

  preferences {
    input name: "debugLevel", type: "number", title: "Debug Level", description: "Adjust debug level for log", range: "1..5", displayDuringSetup: false
  } 

  tiles(scale: 2) {
    multiAttributeTile(name:"switch", type: "lighting", width: 6, height: 4, canChangeIcon: true){
      tileAttribute ("device.switch", key: "PRIMARY_CONTROL") {
        attributeState "on", label:'${name}', action:"switch.off", icon:"st.switches.switch.on", backgroundColor:"#00a0dc", nextState:"turningOff"
        attributeState "off", label:'${name}', action:"switch.on", icon:"st.switches.switch.off", backgroundColor:"#ffffff", nextState:"turningOn"
        attributeState "turningOn", label:'${name}', action:"switch.off", icon:"st.switches.switch.on", backgroundColor:"#00a0dc", nextState:"turningOff"
        attributeState "turningOff", label:'${name}', action:"switch.on", icon:"st.switches.switch.off", backgroundColor:"#ffffff", nextState:"turningOn"
      }
    }
    childDeviceTiles("endpoints")

    valueTile("firmwareVersion", "device.firmwareVersion", width: 2, height: 2, decoration: "flat", inactiveLabel: false) {
      state "default", label: '${currentValue}'
    }

    valueTile("driverVersion", "device.driverVersion", width: 2, height: 2, inactiveLabel: true, decoration: "flat") {
      state "default", label: '${currentValue}'
    }

    standardTile("refresh", "device.switch", width: 2, height: 2, inactiveLabel: false, decoration: "flat") {
      state "default", label:'', action:"refresh.refresh", icon:"st.secondary.refresh"
    }

    main "switch"
    details(["switch", "firmwareVersion", "driverVersion", "refresh" ])
  }
}

private typeNameForDeviceClass(String deviceClass) {
  def typeName = null

  switch (deviceClass[0..1]) {
    case "10":
    case "31":
    typeName = "Switch Endpoint"
    break
    case "11":
    typeName = "Dimmer Endpoint"
    break
    case "08":
    //typeName = "Thermostat Endpoint"
    //break
    case "21":
    typeName = "Multi Sensor Endpoint"
    break
    case "20":
    case "A1":
    typeName = "Sensor Endpoint"
    break
  }
  return typeName
}

private queryCommandForCC(cc) {
  switch (cc) {
    case "30":
    return zwave.sensorBinaryV2.sensorBinaryGet(sensorType: 0xFF).format()
    case "71":
    return zwave.notificationV3.notificationSupportedGet().format()
    case "31":
    return zwave.sensorMultilevelV4.sensorMultilevelGet().format()
    case "32":
    return zwave.meterV1.meterGet().format()
    case "8E":
    return zwave.multiChannelAssociationV2.multiChannelAssociationGroupingsGet().format()
    case "85":
    return zwave.associationV2.associationGroupingsGet().format()
    default:
    return null
  }
}

def getCommandClassVersions() {
    [0x01: 1, 0x20: 1, 0x84: 1, 0x98: 1, 0x56: 1, 0x59: 1, 0x60: 3, 0x72: 2, 0x85: 2, 0x86: 1]
}

def parse(String description) {
  def result = []

  if (description && description.startsWith("Err")) {
    logger ( "parse error: ${description}", "error" )

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
      logger( "zwave.parse(getCommandClassVersions()) failed for: ${description}", "error" )
      // Try it without check for classes
      cmd = zwave.parse(description)

      if (cmd) {
        zwaveEvent(cmd, result)
      } else {
        logger( "zwave.parse() failed for: ${description}", "error" )
      }
    }
  }

  if (0) {
    def print_me =  zwave.zwaveCmdClassV1.cmdAutomaticControllerUpdateStart.format()
    logger("$print_me", "info");
  }


  return result
}

def uninstalled() {
  sendEvent(name: "epEvent", value: "delete all", isStateChange: true, displayed: false, descriptionText: "Delete endpoint devices")
}

def zwaveEvent(physicalgraph.zwave.commands.wakeupv1.WakeUpNotification cmd, result) {
  logger("$device.displayName $cmd")

  result << createEvent(descriptionText: "${device.displayName} woke up", isStateChange:true)
  result << sendCommands([zwave.wakeUpV1.wakeUpNoMoreInformation()])
}

def zwaveEvent(physicalgraph.zwave.commands.basicv1.BasicGet cmd, result) {
  logger("$device.displayName $cmd")
  result << sendCommands([zwave.basicV1.basicReport(value: 0xFF)])
}

def zwaveEvent(physicalgraph.zwave.commands.basicv1.BasicReport cmd, result) {
  logger("$device.displayName $cmd")

  if (cmd.value == 0) {
    result << createEvent(name: "switch", value: "off")
  } else if (cmd.value == 255) {
    result << createEvent(name: "switch", value: "on")
  } else {
    // This would be an error
  }
}

private List loadEndpointInfo() {
  if (state.endpointInfo) {
    state.endpointInfo
  } else if (device.currentValue("epInfo")) {
    util.parseJson((device.currentValue("epInfo")))
  } else {
    []
  }
}

def zwaveEvent(physicalgraph.zwave.commands.zwavecmdclassv1.AcceptLost cmd, result) {
  logger("$device.displayName $cmd")
}

def zwaveEvent(physicalgraph.zwave.commands.multichannelv3.MultiChannelEndPointReport cmd, result) {
  logger("$device.displayName $cmd")

  updateDataValue("endpoints", cmd.endPoints.toString())
  if (!state.endpointInfo) {
    state.endpointInfo = loadEndpointInfo()
  }
  if (state.endpointInfo.size() > cmd.endPoints) {
    cmd.endpointInfo
  }
  state.endpointInfo = [null] * cmd.endPoints
  //response(zwave.associationV2.associationGroupingsGet())
  result << createEvent(name: "epInfo", value: util.toJson(state.endpointInfo), displayed: false, descriptionText:"")
  result << sendCommands([zwave.multiChannelV3.multiChannelCapabilityGet(endPoint: 1)])
}

def zwaveEvent(physicalgraph.zwave.commands.multichannelv3.MultiChannelCapabilityReport cmd, result) {
  logger("$device.displayName $cmd")

  def cmds = []
  if(!state.endpointInfo) {
    state.endpointInfo = []
  }

  state.endpointInfo[cmd.endPoint - 1] = cmd.format()[6..-1]
  if (cmd.endPoint < getDataValue("endpoints").toInteger()) {
    cmds = zwave.multiChannelV3.multiChannelCapabilityGet(endPoint: cmd.endPoint + 1)
  } else {
    log.debug "endpointInfo: ${state.endpointInfo.inspect()}"
  }
  result << createEvent(name: "epInfo", value: util.toJson(state.endpointInfo), displayed: false, descriptionText:"")

  if(cmds) {
    result << sendCommands(cmds)
  }
}

def zwaveEvent(physicalgraph.zwave.commands.associationv2.AssociationGroupingsReport cmd, result) {
  logger("$device.displayName $cmd")

  state.groups = cmd.supportedGroupings

  if (cmd.supportedGroupings) {
    def cmds = []
    for (def x = 1; x <= cmd.supportedGroupings; x++) {
      cmds << zwave.associationGrpInfoV1.associationGroupNameGet(groupingIdentifier: x);
      cmds << zwave.associationGrpInfoV1.associationGroupInfoGet(groupingIdentifier: x, listMode: 1);
      cmds << zwave.associationGrpInfoV1.associationGroupCommandListGet(allowCache: true, groupingIdentifier: x);
    }

    sendCommands(cmds, 2000)
  } else {
    logger("$device.displayName reported no groups", "error")
  }
}

def zwaveEvent(physicalgraph.zwave.commands.associationv2.AssociationReport cmd, result) {
  logger("$device.displayName $cmd")
  
  def string_of_assoc = ""
  cmd.nodeId.each {
    string_of_assoc += "${it}, "
  }
  def lengthMinus2 = ( string_of_assoc.length() > 3 ) ? string_of_assoc.length() - 3 : 0
  def final_string = lengthMinus2 ? string_of_assoc.getAt(0..lengthMinus2) : string_of_assoc

  String group_name = getDataValue("Group #${cmd.groupingIdentifier}");
  
  if (cmd.groupingIdentifier == 1) {
    updateDataValue("Lifeline", "$final_string")
  } else {
    updateDataValue("group_name", "$final_string")
  }
}

def zwaveEvent(physicalgraph.zwave.commands.associationgrpinfov1.AssociationGroupInfoReport cmd, result) {
  logger("$device.displayName $cmd")

  def cmds = []
  /*for (def i = 0; i < cmd.groupCount; i++) {
    def prof = cmd.payload[5 + (i * 7)]
    def num = cmd.payload[3 + (i * 7)]
    if (prof == 0x20 || prof == 0x31 || prof == 0x71) {
    updateDataValue("agi$num", String.format("%02X%02X", *(cmd.payload[(7*i+5)..(7*i+6)])))
    cmds << response(zwave.multiChannelAssociationV2.multiChannelAssociationSet(groupingIdentifier:num, nodeId:zwaveHubNodeId))
    }
    }*/
  for (def i = 2; i <= state.groups; i++) {
    result << sendCommands([zwave.multiChannelAssociationV2.multiChannelAssociationSet(groupingIdentifier:i, nodeId:zwaveHubNodeId)])
  }
}

def zwaveEvent(physicalgraph.zwave.commands.associationgrpinfov1.AssociationGroupNameReport cmd, result) {
  logger("$device.displayName $cmd")

  def name = new String(cmd.name as byte[])
  logger("Association Group #${cmd.groupingIdentifier} has name: ${name}", "info")
  
  updateDataValue("Group #${cmd.groupingIdentifier}", "${name}")

  result << sendCommands([ zwave.associationV1.associationGet(groupingIdentifier: cmd.groupingIdentifier) ])
}

def zwaveEvent(physicalgraph.zwave.commands.associationgrpinfov1.AssociationGroupCommandListReport cmd, result) {
  logger("$device.displayName $cmd")
}

def zwaveEvent(physicalgraph.zwave.commands.multichannelv3.MultiChannelCmdEncap cmd, result) {
  logger("$device.displayName $cmd")

  def encapsulatedCommand = cmd.encapsulatedCommand([0x32: 3, 0x25: 1, 0x20: 1])
  if (encapsulatedCommand) {
    def formatCmd = ([cmd.commandClass, cmd.command] + cmd.parameter).collect{ String.format("%02X", it) }.join()
    if (state.enabledEndpoints.find { it == cmd.sourceEndPoint }) {
      result << createEvent(name: "epEvent", value: "$cmd.sourceEndPoint:$formatCmd", isStateChange: true, displayed: false, descriptionText: "(fwd to ep $cmd.sourceEndPoint)")
    }
    def childDevice = getChildDeviceForEndpoint(cmd.sourceEndPoint)
    if (childDevice) {
      log.debug "Got $formatCmd for ${childDevice.name}"
      childDevice.handleEvent(formatCmd)
    }
  }
}

def zwaveEvent(physicalgraph.zwave.commands.securityv1.SecurityMessageEncapsulation cmd, result) {
  logger("$device.displayName $cmd")

  def encapsulatedCommand = cmd.encapsulatedCommand([0x20: 1, 0x84: 1])
  if (encapsulatedCommand) {
    state.sec = 1

    zwaveEvent(encapsulatedCommand, result)

    result = result.collect {
      if (it instanceof physicalgraph.device.HubAction && !it.toString().startsWith("9881")) {
        response(cmd.CMD + "00" + it.toString())
      } else {
        it
      }
    }
    result
  }
}

def zwaveEvent(physicalgraph.zwave.commands.versionv1.VersionReport cmd, result) {
  logger("$device.displayName $cmd")

  def text = "$device.displayName: firmware version: ${cmd.applicationVersion}.${cmd.applicationSubVersion}, Z-Wave version: ${cmd.zWaveProtocolVersion}.${cmd.zWaveProtocolSubVersion}"
  def zWaveProtocolVersion = "${cmd.zWaveProtocolVersion}.${cmd.zWaveProtocolSubVersion}"
  state.firmwareVersion = cmd.applicationVersion+'.'+cmd.applicationSubVersion
  result << createEvent(name: "firmwareVersion", value: "V ${state.firmwareVersion}", descriptionText: "$text", isStateChange: true)
  result << createEvent(name: "zWaveProtocolVersion", value: "${zWaveProtocolVersion}", descriptionText: "${device.displayName} ${zWaveProtocolVersion}", isStateChange: true)
}

def zwaveEvent(physicalgraph.zwave.commands.manufacturerspecificv2.ManufacturerSpecificReport cmd, result) {
  logger("$device.displayName $cmd")

  if ( ! state.manufacturer ) {
    state.manufacturer= cmd.manufacturerName
  } else {
    state.manufacturer= "Unknown"
  }

  String manufacturerCode = String.format("%04X", cmd.manufacturerId)
  String productTypeCode = String.format("%04X", cmd.productTypeId)
  String productCode = String.format("%04X", cmd.productId)

  String msr = String.format("%04X-%04X-%04X", cmd.manufacturerId, cmd.productTypeId, cmd.productId)
  updateDataValue("MSR", "$msr")
  updateDataValue("manufacturer", "${state.manufacturer}")

  result << createEvent(name: "ManufacturerCode", value: manufacturerCode)
  result << createEvent(name: "ProduceTypeCode", value: productTypeCode)
  result << createEvent(name: "ProductCode", value: productCode)
  result << sendCommands(delayBetween(cmds, 1000))
  result << sendCommands([ zwave.versionV1.versionGet() ])
}

def zwaveEvent(physicalgraph.zwave.commands.crc16encapv1.Crc16Encap cmd, result) {
  logger("$device.displayName $cmd")

  def versions = [0x31: 2, 0x30: 1, 0x84: 1, 0x9C: 1, 0x70: 2]
  // def encapsulatedCommand = cmd.encapsulatedCommand(versions)
  def version = versions[cmd.commandClass as Integer]
  def ccObj = version ? zwave.commandClass(cmd.commandClass, version) : zwave.commandClass(cmd.commandClass)
  def encapsulatedCommand = ccObj?.command(cmd.command)?.parse(cmd.data)
  if (encapsulatedCommand) {
    zwaveEvent(encapsulatedCommand, result)
  }
}

def zwaveEvent(physicalgraph.zwave.Command cmd, result) {
  logger("$device.displayName command not implemented: $cmd", "error")
}

def on() {
  sendCommands([zwave.basicV1.basicSet(value: 0xFF), zwave.basicV1.basicGet()])
}

def off() {
  sendCommands([zwave.basicV1.basicSet(value: 0x00), zwave.basicV1.basicGet()])
}

def refresh() {
  sendCommands([zwave.basicV1.basicGet()])
}

def configure() {
  sendCommands([
    zwave.multiChannelV3.multiChannelEndPointGet()
  ], 800)
}

// epCmd is part of the deprecated Zw Multichannel capability
def epCmd(Integer ep, String cmds) {
  def result
  if (cmds) {
    def header = state.sec ? "988100600D00" : "600D00"
    result = cmds.split(",").collect { cmd -> (cmd.startsWith("delay")) ? cmd : String.format("%s%02X%s", header, ep, cmd) }
  }
  result
}

// enableEpEvents is part of the deprecated Zw Multichannel capability
def enableEpEvents(enabledEndpoints) {
  state.enabledEndpoints = enabledEndpoints.split(",").findAll()*.toInteger()
  null
}

// sendCommand is called by endpoint child device handlers
def sendCommand(endpointDevice, commands) {
  def result
  if (commands instanceof String) {
    commands = commands.split(',') as List
  }
  def endpoint = deviceEndpointNumber(endpointDevice)
  if (endpoint) {
    log.debug "${endpointDevice.deviceNetworkId} cmd: ${commands}"
    result = commands.collect { cmd ->
      if (cmd.startsWith("delay")) {
        new physicalgraph.device.HubAction(cmd)
      } else {
        new physicalgraph.device.HubAction(encap(cmd, endpoint))
      }
    }
    sendHubCommand(result, 0)
  }
}

private deviceEndpointNumber(device) {
  String dni = device.deviceNetworkId
  if (dni.size() >= 5 && dni[2..3] == "ep") {
    // Old format: 01ep2
    return device.deviceNetworkId[4..-1].toInteger()
  } else if (dni.size() >= 6 && dni[2..4] == "-ep") {
    // New format: 01-ep2
    return device.deviceNetworkId[5..-1].toInteger()
  } else {
    log.warn "deviceEndpointNumber() expected 'XX-epN' format for dni of $device"
  }
}

private getChildDeviceForEndpoint(Integer endpoint) {
  def children = childDevices
  if (children && endpoint) {
    return children.find{ it.deviceNetworkId.endsWith("ep$endpoint") }
  }
}

private command(physicalgraph.zwave.Command cmd) {
  if (state.sec) {
    zwave.securityV1.securityMessageEncapsulation().encapsulate(cmd).format()
  } else {
    cmd.format()
  }
}

private commands(commands, delay=200) {
  delayBetween(commands.collect{ command(it) }, delay)
}

private encap(cmd, endpoint) {
  if (endpoint) {
    if (cmd instanceof physicalgraph.zwave.Command) {
      command(zwave.multiChannelV3.multiChannelCmdEncap(destinationEndPoint:endpoint).encapsulate(cmd))
    } else {
      // If command is already formatted, we can't use the multiChannelCmdEncap class
      def header = state.sec ? "988100600D00" : "600D00"
      String.format("%s%02X%s", header, endpoint, cmd)
    }
  } else {
    command(cmd)
  }
}

private encapWithDelay(commands, endpoint, delay=200) {
  delayBetween(commands.collect{ encap(it, endpoint) }, delay)
}

def prepDevice() {
  [
    zwave.basicV1.basicGet(),
    zwave.manufacturerSpecificV2.manufacturerSpecificGet(),
    zwave.associationV2.associationGroupingsGet(),
  ]
}

def installed() {
  def queryCmds = []
  def delay = 200
  def zwInfo = getZwaveInfo()
  def endpointCount = zwInfo.epc as Integer
  def endpointDescList = zwInfo.ep ?: []

  // This is needed until getZwaveInfo() parses the 'ep' field
  if (endpointCount && !zwInfo.ep && device.hasProperty("rawDescription")) {
    try {
      def matcher = (device.rawDescription =~ /ep:(\[.*?\])/)  // extract 'ep' field
      endpointDescList = util.parseJson(matcher[0][1].replaceAll("'", '"'))
    } catch (Exception e) {
      log.warn "couldn't extract ep from rawDescription"
    }
  }

  if (zwInfo.zw.contains("s")) {
    // device was included securely
    state.sec = true
  }

  if (endpointCount > 1 && endpointDescList.size() == 1) {
    // This means all endpoints are identical
    endpointDescList *= endpointCount
  }

  endpointDescList.eachWithIndex { desc, i ->
    def num = i + 1
    if (desc instanceof String && desc.size() >= 4) {
      // desc is in format "1001 AA,BB,CC" where 1001 is the device class and AA etc are the command classes
      // supported by this endpoint
      def parts = desc.split(' ')
      def deviceClass = parts[0]
      def cmdClasses = parts.size() > 1 ? parts[1].split(',') : []
      def typeName = typeNameForDeviceClass(deviceClass)
      def componentLabel = "${typeName} ${num}"
      log.debug "EP #$num d:$deviceClass, cc:$cmdClasses, t:$typeName"
      if (typeName) {
        try {
          String dni = "${device.deviceNetworkId}-ep${num}"
          addChildDevice(typeName, dni, device.hub.id,
          [completedSetup: true, label: "${device.displayName} ${componentLabel}",
          isComponent: true, componentName: "ch${num}", componentLabel: "${componentLabel}"])
          // enabledEndpoints << num.toString()
          log.debug "Endpoint $num ($desc) added as $componentLabel"
        } catch (e) {
          log.warn "Failed to add endpoint $num ($desc) as $typeName - $e"
        }
      } else {
        log.debug "Endpoint $num ($desc) ignored"
      }
      def cmds = cmdClasses.collect { cc -> queryCommandForCC(cc) }.findAll()
      if (cmds) {
        queryCmds += encapWithDelay(cmds, num) + ["delay 200"]
      }
    }
  }

  response(queryCmds)
}

def updated() {
  if (state.updatedDate && (Calendar.getInstance().getTimeInMillis() - state.updatedDate) < 5000 ) {
    return
  }
  log.info("$device.displayName updated() debug: ${settings.debugLevel}")
  
  if (state.sec) {
    updateDataValue( "isSecure", "true")
  } else {
    updateDataValue( "isSecure", "false")
  }

  sendEvent(name: "driverVersion", value: getDriverVersion(), descriptionText: getDriverVersion(), isStateChange: true, displayed: true)
  sendEvent(name: "lastError", value: "", isStateChange: true)
  sendCommands( prepDevice(), 2000 )

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
private prepCommands(cmds, delay) {
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
 *    messages by sending events for the device's logMessage attribute and lastError attribute.
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
    if (settings.debugLevel >= 2) {
      log.warn msg
      sendEvent(name: "logMessage", value: "WARNING: ${msg}", displayed: false, isStateChange: true)
    }
    return

    case "info":
    if (settings.debugLevel >= 3) {
      log.info msg
    }
    return

    case "debug":
    if (settings.debugLevel >= 4) {
      log.debug msg
    }
    return

    case "trace":
    if (settings.debugLevel >= 5) {
      log.trace msg
    }
    return

    case "error":
    default:
    break
  }

  log.error msg
  sendEvent(name: "lastError", value: "ERROR: ${msg}", isStateChange: true)
}
