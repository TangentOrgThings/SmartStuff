// vim :set ts=2 sw=2 sts=2 expandtab smarttab :
/**
 *  Zooz ZSE02 Motion Sensor
 *
 *  Copyright 2016 Brian Aker
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
  return "v3.05"
}

def getAssociationGroup() {
  return 1
}

metadata {
  definition (name: "Zooz Motion Sensor", namespace: "TangentOrgThings", author: "Brian Aker") {
    capability "Battery"
    capability "Motion Sensor"
    capability "Refresh"
    capability "Sensor"
    capability "Tamper Alert"
    
    attribute "reset", "enum", ["false", "true"]
    attribute "driverVersion", "string"
    attribute "MSR", "string"
    attribute "Manufacturer", "string"
    attribute "ManufacturerCode", "string"
    attribute "ProduceTypeCode", "string"
    attribute "ProductCode", "string"
    attribute "WakeUp", "string"
    attribute "WirelessConfig", "string"
    attribute "firmwareVersion", "string"
    attribute "AlarmTypeSupportedReport", "string"
    attribute "SensorMultilevelSupportedScaleReport", "string"
    attribute "LastActive", "string"
    attribute "NIF", "string"
  }
  
  /*
    vendorId: 338 (2017-06-17)
    vendor: (2017-06-17)
    productId: 3 (2017-06-17)
    productType: 1280 (2017-06-17)
    */

  // zw:S type:0701 mfr:0152 prod:0500 model:0003 ver:0.01 zwv:3.95 lib:06 cc:5E,85,59,71,80,5A,73,84,72,86 role:06 ff:8C07 ui:8C07
  fingerprint type: "0701", mfr: "0152", prod: "0500", model: "0003", deviceJoinName: "Zooz Motion Sensor ZSE02"
  // fingerprint type: "0701", mfr: "027A", prod: "0500", model: "0003", deviceJoinName: "Zooz Motion Sensor ZSE02"
  // fingerprint type: "0701", mfr: "0152", prod: "0003", model: "0500", deviceJoinName: "Zooz Motion Sensor ZSE02"


  simulator 
  {
    // TODO: define status and reply messages here
  }

  tiles (scale: 2)
  {
    multiAttributeTile(name:"main", type: "generic", width: 6, height: 4)
    {
      tileAttribute ("device.motion", key: "PRIMARY_CONTROL") {
        attributeState "active", label:'motion', icon:"st.motion.motion.active", backgroundColor:"#53a7c0"
        attributeState "inactive", label:'no motion', icon:"st.motion.motion.inactive", backgroundColor:"#ffffff"
      }
    }
    valueTile("tamperAlert", "device.tamperAlert", inactiveLabel: false, decoration: "flat", width: 2, height: 2) {
      state "detected", backgroundColor:"#00FF00"
      state "clear", backgroundColor:"#e51426"
    }
    valueTile("battery", "device.battery", inactiveLabel: false, decoration: "flat", width: 2, height: 2) {
      state "battery", label:'${currentValue}', unit:"%"
    }
    valueTile("driverVersion", "device.driverVersion", width:2, height:2, inactiveLabel: true, decoration: "flat") {
      state "default", label: '${currentValue}'
    }
    standardTile("refresh", "device.switch", inactiveLabel: false, decoration: "flat", width: 2, height: 2) {
      state "default", label:'', action: "refresh.refresh", icon: "st.secondary.refresh"
    }
    standardTile("reset", "device.reset", inactiveLabel: false, decoration: "flat", width: 2, height: 2) {
      state "false", label:'', backgroundColor:"#ffffff"
      state "true", label:'reset', backgroundColor:"#e51426"
    }
    valueTile("lastActive", "device.LastActive", width:2, height:2, inactiveLabel: true, decoration: "flat") {
      state "default", label: '${currentValue}'
    }
    main(["main"])
    details(["main", "tamperAlert", "battery", "driverVersion", "refresh", "reset", "lastActive"])
  }
}

def prepDevice() {
  [
    zwave.manufacturerSpecificV2.manufacturerSpecificGet(),
    zwave.versionV1.versionGet(),
    zwave.associationV2.associationGroupingsGet(),
    zwave.zwaveCmdClassV1.requestNodeInfo(),
  ]
}

def installed() {
  log.debug "installed()"
  sendEvent(name: "driverVersion", value: getDriverVersion(), descriptionText: getDriverVersion(), isStateChange: true, displayed: true)
  sendCommands( prepDevice(), 2000 )
}

def updated() { 
  log.debug "updated()"
  
  sendEvent(name: "driverVersion", value: getDriverVersion(), descriptionText: getDriverVersion(), isStateChange: true, displayed: true)
//  sendEvent(name: "motion", value: "inactive", isStateChange: true)
  sendEvent(name: "motion", value: "inactive", descriptionText: "$device.displayName reset on update", isStateChange: true, displayed: true)
  
  sendCommands( prepDevice(), 2000 )
}

def parse(String description) {
  log.debug "parse() ${description}"

  def result = null

  if (description.startsWith("Err")) {
    if (description.startsWith("Err 106")) {
      if (state.sec) {
        log.debug description
        result = createEvent(
          descriptionText: "An error occured with a secure device.",
          eventType: "ALERT",
          name: "secureInclusion",
          value: description,
          isStateChange: true,
        )
      } else {
        result = createEvent(
          descriptionText: "This device failed to complete the network security key exchange. If you are unable to control it via SmartThings, you must remove it from your network and add it again.",
          eventType: "ALERT",
          name: "secureInclusion",
          value: "failed",
          isStateChange: true,
        )
      }
    } else {
      result = createEvent(value: description, descriptionText: description)
    }
  } else if (description != "updated") {
    def cmd = zwave.parse(description)// , [0x20: 1, 0x85: 2, 0x59: 1, 0x71: 3, 0x80: 1, 0x5A: 1, 0x84: 2, 0x72: 2, 0x86: 1, 0x31: 5])
	
    if (cmd) {
      result = zwaveEvent(cmd)
      
      if (!result) {
        log.warning "Parse Failed and returned ${result} for command ${cmd}"
        result = createEvent(value: description, descriptionText: description)
      }
    } else {
      log.info "Non-parsed event: ${description}"
      result = createEvent(value: description, descriptionText: description)
    }
  }
    
  return result
}

def zwaveEvent(physicalgraph.zwave.commands.wakeupv2.WakeUpNotification cmd) {
  log.debug("$device.displayName $cmd")
  def result = [createEvent(descriptionText: "${device.displayName} woke up", displayed: true)]
  def cmds = []
  
  /*
  if (state.configured) {
    if (zwaveHubNodeId == 1) { // Only check on battery if this controller is the main controller
      if (!state.lastbat || (new Date().time) - state.lastbat > 53*60*60*1000) {
        sendCommands( [ zwave.batteryV1.batteryGet() ] )
      }
    }
  }
  */
 
  return result
}

def zwaveEvent(physicalgraph.zwave.commands.deviceresetlocallyv1.DeviceResetLocallyNotification cmd) {
  state.reset = true
  [createEvent(name: "reset", value: state.reset, descriptionText: cmd.toString(), isStateChange: true, displayed: true)]
}

def zwaveEvent(physicalgraph.zwave.commands.manufacturerspecificv2.ManufacturerSpecificReport cmd) {
  log.debug("$device.displayName $cmd")

  state.manufacturer = cmd.manufacturerName ? cmd.manufacturerName : "Zooz"
  updateDataValue("manufacturer", state.manufacturer)

  def manufacturerCode = String.format("%04X", cmd.manufacturerId)
  def productTypeCode = String.format("%04X", cmd.productTypeId)
  def productCode = String.format("%04X", cmd.productId)
  def wirelessConfig = "ZWP"

  sendEvent(name: "ManufacturerCode", value: manufacturerCode)
  sendEvent(name: "ProduceTypeCode", value: productTypeCode)
  sendEvent(name: "ProductCode", value: productCode)
  sendEvent(name: "WirelessConfig", value: wirelessConfig)

  def msr = String.format("%04X-%04X-%04X", cmd.manufacturerId, cmd.productTypeId, cmd.productId)
  updateDataValue("MSR", msr)

  sendEvent(name: "MSR", value: "$msr", descriptionText: "$device.displayName", isStateChange: false)
  [createEvent(name: "Manufacturer", value: "${state.manufacturer}", descriptionText: "$device.displayName", isStateChange: false)]
}

def zwaveEvent(physicalgraph.zwave.commands.versionv1.VersionReport cmd) {
  def text = "$device.displayName: firmware version: ${cmd.applicationVersion}.${cmd.applicationSubVersion}, Z-Wave version: ${cmd.zWaveProtocolVersion}.${cmd.zWaveProtocolSubVersion}"
  state.firmwareVersion = cmd.applicationVersion+'.'+cmd.applicationSubVersion
  [createEvent(name: "firmwareVersion", value: "V ${state.firmwareVersion}", descriptionText: "$text", isStateChange: false)]
}

def zwaveEvent(physicalgraph.zwave.commands.batteryv1.BatteryReport cmd) {
  log.debug("$device.displayName $cmd")
  
  def results = []
 
  def map = [ name: "battery", unit: "%" ]
  if (cmd.batteryLevel == 0xFF) {
    map.value = 1
    map.descriptionText = "${device.displayName} has a low battery"
    map.isStateChange = true
  } else {
    map.value = cmd.batteryLevel
  }
  state.lastbat = new Date().time
  
  results << createEvent(map)
  
  return results
}

def motionEvent(value) {
  def map = [name: "motion"]
  if (value != 0) {
    map.value = "active"
    map.descriptionText = "$device.displayName detected motion"
    state.lastActive = new Date().time
	sendEvent(name: "LastActive", value: state.lastActive, displayed: true)
  } else {
    map.value = "inactive"
    map.descriptionText = "$device.displayName motion has stopped"
  }
  createEvent(map)
}

def zwaveEvent(physicalgraph.zwave.commands.basicv1.BasicSet cmd) {
  log.debug("$device.displayName $cmd")
  
  motionEvent(cmd.value)
}

def zwaveEvent(physicalgraph.zwave.commands.basicv1.BasicReport cmd) {
  log.debug("$device.displayName $cmd")
  
  motionEvent(cmd.value)
}

def zwaveEvent(physicalgraph.zwave.commands.alarmv2.AlarmTypeSupportedReport cmd) {
  log.debug("$device.displayName $cmd")
  
  state.AlarmTypeSupportedReport= "$cmd"
  [ createEvent(name: "AlarmTypeSupportedReport", value: cmd.toString(), descriptionText: "$device.displayName recieved: $cmd", displayed: true) ]
}

def zwaveEvent(physicalgraph.zwave.commands.sensormultilevelv5.SensorMultilevelSupportedScaleReport cmd) {
  log.debug("$device.displayName $cmd")
  
  state.SensorMultilevelSupportedScaleReport= "${cmd}"
  [ createEvent(name: "SensorMultilevelSupportedScaleReport", value: cmd.toString(), descriptionText: "$device.displayName recieved: $cmd", displayed: true) ]
}

def zwaveEvent(physicalgraph.zwave.commands.sensormultilevelv5.SensorMultilevelReport cmd) {
  log.debug("$device.displayName $cmd")
  
  def result = []
  switch (cmd.sensorType) {
    case 27:
      result << motionEvent(cmd.scale)
      break;
  default:
      result << createEvent(descriptionText: "$device.displayName unsupported sensortype: $cmd.sensorType", displayed: true)
      break;
  }
  
  return result
}


//  payload: 00 00 00 FF 07 00 01 03
def zwaveEvent(physicalgraph.zwave.commands.notificationv3.NotificationReport cmd) {
  log.debug("$device.displayName $cmd")
  
  def event = null
  // if (cmd.notificationType == 0x07) {
  if (cmd.notificationType == NOTIFICATION_TYPE_BURGLAR) {
    if (cmd.event == 0x00) {
      if (cmd.eventParameter == [8]) {
        event = motionEvent(0)
      } else if (cmd.eventParameter == [3]) { // payload : 00 00 00 FF 07 00 01 03
        event = createEvent(name: "tamper", value: "detected", descriptionText: "$device.displayName covering was removed", isStateChange: true)
      } else {
        sendEvent(name: "tamper", value: "clear", descriptionText: "$device.displayName has been deactivated by the switch.")
        event = motionEvent(0)
      }
    } else if (cmd.event == 0x03) {
      event = createEvent(name: "tamper", value: "detected", descriptionText: "$device.displayName has been deactivated by the switch.", isStateChange: true)
    } else if (cmd.event == 0x08) {
      event = motionEvent(255)
    }
  } else {
    event = createEvent(descriptionText: cmd.toString(), isStateChange: true)
  }
  
  /*
  def cmds = [
    zwave.alarmV2.alarmTypeSupportedGet(),
    zwave.manufacturerSpecificV1.manufacturerSpecificGet(),
    // zwave.sensorMultilevelV4.sensorMultilevelSupportedGetSensor(),
    zwave.versionV1.versionGet()
  ]
  
  if (! state.AlarmTypeSupportedReport) {
    return [ event, sendCommands(cmds)]
  } else {
    return [ event ]
  }
  */
  
  return [ event ]
}

def zwaveEvent(physicalgraph.zwave.commands.associationv2.AssociationGroupingsReport cmd) {
  log.debug("$device.displayName $cmd")

  state.groups = cmd.supportedGroupings

  if (cmd.supportedGroupings) {
    def cmds = []

    for (def x = cmd.supportedGroupings; x <= cmd.supportedGroupings; x++) {
      cmds << zwave.associationGrpInfoV1.associationGroupInfoGet(groupingIdentifier: x, listMode: 0x01);
      cmds << zwave.associationGrpInfoV1.associationGroupNameGet(groupingIdentifier: x);
      cmds << zwave.associationV2.associationGet(groupingIdentifier: x);
    }

    sendCommands(cmds, 1000)
  } else {
    [createEvent(descriptionText: "$device.displayName reported no groups", isStateChange: true, displayed: true)]
  }
}

def zwaveEvent(physicalgraph.zwave.commands.associationgrpinfov1.AssociationGroupInfoReport cmd) {
  log.debug("$device.displayName $cmd")
  [ createEvent(descriptionText: "$device.displayName AssociationGroupInfoReport: $cmd", isStateChange: true, displayed: true) ]
}

def zwaveEvent(physicalgraph.zwave.commands.associationgrpinfov1.AssociationGroupNameReport cmd) {
  log.debug("$device.displayName $cmd")
  [ createEvent(descriptionText: "$device.displayName AssociationGroupNameReport: $cmd", displayed: true) ]
}

def zwaveEvent(physicalgraph.zwave.commands.associationv2.AssociationReport cmd) {
  log.debug("$device.displayName $cmd")
  
  def result = []
  if (cmd.groupingIdentifier == 1)
  {
    if (cmd.nodeId.any { it == zwaveHubNodeId }) 
    {
      result << createEvent(descriptionText: "$device.displayName is associated in group ${cmd.groupingIdentifier}")
      result << createEvent(name: "configured", value: "true", descriptionText: "$device.displayName not associated with hub", isStateChange: true)
    } else {
      sendCommands(zwave.associationV2.associationSet(groupingIdentifier:cmd.groupingIdentifier, nodeId:zwaveHubNodeId))
      result << createEvent(name: "configured", value: "false", descriptionText: "$device.displayName not associated with hub", isStateChange: true)
    }
  } else {
    result << createEvent(descriptionText: "$device.displayName misconfigure(?) has no associations of ${cmd.groupingIdentifier} type")
  }
  
  return result
}

def zwaveEvent(physicalgraph.zwave.commands.controllerreplicationv1.CtrlReplicationTransferScene cmd) {
  log.debug("$device.displayName $cmd")
  [ createEvent(descriptionText: "$device.displayName told to CtrlReplicationTransferScene: $cmd", displayed: true) ]
}

def zwaveEvent(physicalgraph.zwave.commands.transportservicev1.CommandSubsequentFragment cmd) {
  log.debug("$device.displayName $cmd")
  [ createEvent(descriptionText: "$device.displayName told to CommandSubsequentFragment: $cmd", displayed: true) ]
}
 
def zwaveEvent(physicalgraph.zwave.Command cmd) {
  log.error "ERROR: $cmd"
  [ createEvent(descriptionText: "$device.displayName command not implemented: $cmd", displayed: true) ]
}

def zwaveEvent(physicalgraph.zwave.commands.zwavecmdclassv1.NodeInfo cmd) {
  log.debug("$device.displayName $cmd")
  [ createEvent(name: "NIF", value: "$cmd", descriptionText: "$cmd") ]
}

/*
private askIt()
{
  log.debug "askIt() is called"

  def cmds = []
  
  if (state.refresh) {
    cmds << zwave.notificationv3.NotificationGet()
    cmds << zwave.batteryV1.batteryGet()
    if (state.configure) {
      cmds << zwave.associationV2.associationSet(groupingIdentifier:cmd.groupingIdentifier, nodeId:zwaveHubNodeId)
    }
    cmds << zwave.associationV2.associationGet(groupingIdentifier:cmd.groupingIdentifier)

    if (getDataValue("MSR") == null) {
      cmds << zwave.manufacturerSpecificV2.manufacturerSpecificGet()
    }

    if (device.currentState('firmwareVersion') == null) {
      cmds << zwave.versionv1.VersionGet()
    }
  } else if (state.configure) {
    cmds << zwave.associationV2.associationSet(groupingIdentifier:cmd.groupingIdentifier, nodeId:zwaveHubNodeId)
    cmds << zwave.associationV2.associationGet(groupingIdentifier:cmd.groupingIdentifier)
  }

  
  return response(commands(cmds))
}
*/

def refresh() {
  log.debug "refresh() is called"
  state.refresh = false
  createEvent(descriptionText: "refresh will be called during next wakeup", displayed: true) 
}

def configure() {
  state.configured = false
}

private commands(sent_commands, delay=1000) {
  sendCommands(sent_commands, delay)
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
private prepCommands(cmds, delay=1000) {
  return response(delayBetween(cmds.collect{ (it instanceof physicalgraph.zwave.Command ) ? encapCommand(it).format() : it }, delay))
}

/**
 *  sendCommands(cmds, delay=200)
 *
 *  Sends a list of commands directly to the device using sendHubCommand.
 *  Uses encapCommand() to apply security or CRC16 encapsulation as needed.
 **/
private sendCommands(cmds, delay=1000) {
  sendHubCommand( cmds.collect{ (it instanceof physicalgraph.zwave.Command ) ? response(encapCommand(it)) : response(it) }, delay)
}
