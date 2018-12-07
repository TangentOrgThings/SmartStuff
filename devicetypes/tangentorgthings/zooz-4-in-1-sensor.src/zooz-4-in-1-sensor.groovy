// vim :set tabstop=2 shiftwidth=2 sts=2 expandtab smarttab :
/**
 *
 *  zooZ 4-in-1 Sensor
 *
 *  Copyright 2016-2018 Brian Aker
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

def getDriverVersion() {
  return "v1.61"
}

def getConfigurationOptions(Integer model) {
  return [ 1, 2, 3, 4, 5, 6, 7  ]
}

metadata {
  definition (name: "Zooz 4 in 1 Sensor", namespace: "TangentOrgThings", author: "Brian Aker", ocfDeviceType: "x.com.st.d.sensor.motion") {
    capability "Battery"
    capability "Illuminance Measurement"
    capability "Motion Sensor"
    capability "Relative Humidity Measurement"
    capability "Sensor"
    capability "Tamper Alert"
    capability "Temperature Measurement"

    command "resetBatteryRuntime"

    attribute "DeviceReset", "enum", ["false", "true"]
    attribute "logMessage", "string"        // Important log messages.
    attribute "lastError", "string"        // Last error message
    attribute "parseErrorCount", "number"        // Last error message
    attribute "unknownCommandErrorCount", "number"        // Last error message

    attribute "needUpdate", "enum", ["Synced", "Pending"]

    attribute "Lifeline", "string"

    attribute "driverVersion", "string"
    attribute "firmwareVersion", "string"
    attribute "FirmwareMdReport", "string"
    attribute "Manufacturer", "string"
    attribute "ManufacturerCode", "string"
    attribute "MSR", "string"
    attribute "NIF", "string"
    attribute "ProduceTypeCode", "string"
    attribute "ProductCode", "string"

    attribute "WakeUp", "string"
    attribute "WakeUpInterval", "number"
    attribute "WakeUpNode", "number"
    
    attribute "LastActive", "string"
    attribute "LastAwake", "string"
    attribute "MotionTimeout", "number"

    /*
      vendorId: 265 (00:15)
      vendor: Vision Security (00:15)
      productId: 8449 (00:15)
      productType: 8225 (00:15)
    */

    fingerprint mfr: "0109", prod: "2021", model: "2101", ui: "0C07", deviceJoinName: "Vision Security 4-in-1 Sensor ZP3111"
    fingerprint mfr: "027A", prod: "2021", model: "2101", ui: "0C07", deviceJoinName: "Zooz Z-Wave Plus 4-in-1 Sensor ZSE40"
    // fingerprint type: "0x0701", inClusters: "0x5E,0x86,0x72,0x59,0x85,0x73,0x71,0x84,0x80,0x31,0x70,0x5A,0x98,0x7A"
  }

  preferences {
    input name: "primaryDisplayType", type: "enum", options: ["Motion", "Temperature"], title: "Primary Display", defaultValue: "Motion", description: "Sensor to show in primary display", required: false, displayDuringSetup: true
    input name: "pirTimeout", type: "number", title: "Motion Sensor Idle Time (minutes)", defaultValue: 3, description: "Inactivity time before reporting no motion", required: false, displayDuringSetup: true, range: "1..255"
    input name: "pirSensitivity", type: "number", title: "Motion Sensor Sensitivity (1 high - 7 low)", defaultValue: 3, description: "1 is most sensitive, 7 is least sensitive", required: false, displayDuringSetup: true, range: "1..7"
    input name: "tempAlert", type: "number", title: "Temperature reporting level (1/10th °C)", defaultValue: 10, description: "Minimum temperature change to trigger temp updates", required: false, displayDuringSetup: true, range: "1..50"
    input name: "humidityAlert", type: "number", title: "Humidity reporting level", defaultValue: 50, description: "Minimum humidity level change to trigger updates", required: false, displayDuringSetup: true, range: "1..50"
    input name: "illumSensorAlerts", type: "enum", options: ["Enabled", "Disabled"], title: "Enable Illumination Sensor Updates", defaultValue: "Disabled", description: "Enables illumination update events", required: false, displayDuringSetup: true
    input name: "illumAlert", type: "number", title: "Illumination reporting level", defaultValue: 50, description: "Minumum illumination level change to trigger updates", required: false, displayDuringSetup: true, range: "5..50"
    input name: "ledMode", type: "number", title: "LED Mode", defaultValue: 3, description: "1 LED Off, 2 - Always on (drains battery), 3 - Blink LED", required: false, displayDuringSetup: true, range: "1..3"
    input name: "wakeInterval", type: "number", title: "Wake Interval (minutes)", defaultValue: 60, description: "Interval (in minutes) for polling configuration and sensor values, shorter interval reduces battery life.", required: false, displayDuringSetup: true, range: "10..10080"
    input name: "debugLevel", type: "number", title: "Debug Level", description: "Adjust debug level for log", range: "1..5", displayDuringSetup: false
  }

  simulator {
  }

  tiles (scale: 2) {
    multiAttributeTile(name:"temperature", type: "generic", width: 6, height: 4) {
      tileAttribute("device.temperature", key: "PRIMARY_CONTROL") {
        attributeState "temperature",label:'${currentValue}°', backgroundColors:[
          [value: 31, color: "#153591"],
          [value: 44, color: "#1e9cbb"],
          [value: 59, color: "#90d2a7"],
          [value: 74, color: "#44b621"],
          [value: 84, color: "#f1d801"],
          [value: 95, color: "#d04e00"],
          [value: 96, color: "#bc2323"]
        ]
      }
      tileAttribute("device.humidity", key: "SECONDARY_CONTROL") {
        attributeState("humidity", label:'${currentValue}%', unit:"%", defaultState: true)
      }
    }

    valueTile("motion", "device.motion", width: 2, height: 2) {
      state "inactive",label:'no motion',icon:"st.motion.motion.inactive", backgroundColor:"#ffffff"
      state "active",label:'motion',icon:"st.motion.motion.active", backgroundColor:"#00a0dc"
    }

    valueTile("illuminance", "device.illuminance", inactiveLabel: false, width: 2, height: 2) {
      state "luminosity", label:'${currentValue} lux', unit:"lux",
      backgroundColors:[
        [value: 0, color: "#000000"],
        [value: 1, color: "#060053"],
        [value: 3, color: "#3E3900"],
        [value: 12, color: "#8E8400"],
        [value: 24, color: "#C5C08B"],
        [value: 36, color: "#DAD7B6"],
        [value: 128, color: "#F3F2E9"],
        [value: 1000, color: "#FFFFFF"]
      ]
    }

    standardTile("tamper", "device.tamper", inactiveLabel: false, decoration: "flat", width: 2, height: 2) {
      state("clear", label:'clear', icon:"st.contact.contact.closed", backgroundColor:"#cccccc", action: "resetTamperAlert", defaultState: true)
      state("detected", label:'tamper', icon:"st.contact.contact.open", backgroundColor:"#e86d13", action: "resetTamperAlert")
    }

    valueTile("battery", "device.battery", decoration: "flat", width: 2, height: 2) {
      state "battery", label:'${currentValue}', unit:"%"
    }

    valueTile("firmwareVersion", "device.firmwareVersion", width:2, height: 2, decoration: "flat", inactiveLabel: false) {
      state "default", label: '${currentValue}'
    }

    valueTile("driverVersion", "device.driverVersion", width:2, height:2, inactiveLabel: true, decoration: "flat") {
      state "default", label: '${currentValue}'
    }

    standardTile("reset", "device.DeviceReset", inactiveLabel: false, decoration: "flat", width: 2, height: 2) {
      state "false", label:'', backgroundColor:"#ffffff", defaultState: true
      state "true", label:'reset', backgroundColor:"#e51426"
    }

    valueTile("configure", "device.needUpdate", decoration: "flat", width: 2, height: 2) {
      state("Synced" , label:'Synced', action:"configuration.configure", backgroundColor:"#8acb47")
      state("Pending", label:'Pending', action:"configuration.configure", backgroundColor:"#f39c12")
    }

    main("motion")
    details(["temperature", "motion", "illuminance", "tamper", "battery", "firmwareVersion", "driverVersion", "reset"])
   }
 }

 def getCommandClassVersions() { // cc:5E,98,86,72,5A,85,59,73,80,71,31,70,84,7A role:06 ff:8C07 ui:8C07
  [
    // 0x20: 1,  // Basic
    0x30: 2,  // Sensor Binary Command Class (V2)
    0x31: 5,  // Sensor Multilevel (V4)
    0x59: 1,  // Association Grp Info
    0x5A: 1,  // Device Reset Locally
    0x70: 1,  // Configuration
    0x71: 3,  // Notification
    0x72: 2,  // Manufacturer Specific
    // 0x73: 1, // Powerlevel
    0x7A: 2,  // Firmware Update Md
    0x80: 1,  // Battery
    0x84: 2,  // Wake Up
    0x85: 2,  // Association  0x85  V1 V2
    0x86: 1,  // Version 2?
    0x98: 1,  // Security
  ]
}

def parse(String description) {
  def result = null

  if (description && description.startsWith("Err")) {
    log.error "parse error: ${description}"
    result = []
    result << createEvent(name: "lastError", value: "Error parse() ${description}", descriptionText: description)

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
      result = zwaveEvent(cmd)

      if (! result) {
        logger("zwaveEvent() failed to return a value for command ${cmd}", "error")
      } else {
        // If we displayed the result
      }
    } else {
      logger("zwave.parse() failed for: ${description}", "error")
    }
  }

  return result
}

def zwaveEvent(physicalgraph.zwave.commands.securityv1.SecurityMessageEncapsulation cmd) {
  logger("$device.displayName $cmd")

  def encapsulatedCommand = cmd.encapsulatedCommand(getCommandClassVersions())
  state.sec = 1
  if (encapsulatedCommand) {
    zwaveEvent(encapsulatedCommand)
  } else {
    log.warn "SecurityMessageEncapsulation() Unable to extract encapsulated from $cmd"
    createEvent(descriptionText: "SecurityMessageEncapsulation() Unable to extract encapsulated from $cmd")
  }
}

def zwaveEvent(physicalgraph.zwave.commands.securityv1.SecurityCommandsSupportedReport cmd) {
  logger("$device.displayName $cmd")
  configure()
  [ createEvent(descriptionText: "SecurityCommandsSupportedReport() Unable to extract encapsulated from $cmd") ]
}

def zwaveEvent(physicalgraph.zwave.commands.sensorbinaryv2.SensorBinaryReport cmd) {
  motionEvent((Boolean)cmd.sensorValue)
}

def zwaveEvent(physicalgraph.zwave.commands.basicv1.BasicSet cmd) {
  motionEvent((Boolean)cmd.value)
}

def zwaveEvent(physicalgraph.zwave.commands.configurationv2.ConfigurationReport cmd) {
  logger("$device.displayName $cmd")
  update_current_properties(cmd)
  log.debug "${device.displayName} parameter '${cmd.parameterNumber}' with a byte size of '${cmd.size}' is set to '${cmd2Integer(cmd.configurationValue)}'"
  [ createEvent(descriptionText: "ConfigurationReport() Unable to extract encapsulated from $cmd") ]
}

def zwaveEvent(physicalgraph.zwave.commands.batteryv1.BatteryReport cmd) {
  logger("$device.displayName $cmd")

  def map = [ name: "battery", unit: "%" ]
  if (cmd.batteryLevel == 0xFF) {
    map.value = 1
    map.descriptionText = "${device.displayName} battery is low"
    map.isStateChange = true
  } else {
    map.value = cmd.batteryLevel
  }
  state.lastBatteryReport = now()
  if (state.lastBatteryValue != map.value)
  {
    state.lastBatteryValue = map.value
    map.isStateChange = true
  }
  createEvent(map)
}

def zwaveEvent(physicalgraph.zwave.commands.sensormultilevelv5.SensorMultilevelReport cmd) {
  logger("$device.displayName $cmd")

  def result = []
  switch (cmd.sensorType) {
    case 1: //temp
      def cmdScale = cmd.scale == 1 ? "F" : "C"
      state.realTemperature = convertTemperatureIfNeeded(cmd.scaledSensorValue, cmdScale, cmd.precision)
      state.temp = getAdjustedTemp(state.realTemperature)
      result << createEvent(name: "temperature", value: state.temp, unit: "$cmdScale")
      break
    case 3: // light
      state.realLuminance = cmd.scaledSensorValue.toInteger()
      state.illuminance = getAdjustedLuminance(cmd.scaledSensorValue.toInteger())
      result << createEvent(name: "illuminance", value: state.illuminance, unit: "lux")
      break
    case 5: // humidity
      state.realHumidity = cmd.scaledSensorValue.toInteger()
      state.humidity = getAdjustedHumidity(cmd.scaledSensorValue.toInteger())
      result << createEvent(name: "humidity", value: state.humidity, unit: "%")
    break
  }

  return result
}

def motionEvent(boolean motion_value) {
  createEvent(name:"motion", value: motion_value ? "active" : "inactive", descriptionText: motion_value ? "detected motion" : "motion has stopped")
}

def zwaveEvent(physicalgraph.zwave.commands.notificationv3.NotificationReport cmd) {
  logger("$device.displayName $cmd")

  def result = []
  if (cmd.notificationType == 7) {
    switch (cmd.event) {
      case 0:
      result << motionEvent(false)
      if (cmd.eventParameter == [8]) {
        //
      } else if (cmd.eventParameter == [3]) {
        state.lastBatteryValue = 0
        state.configRequired = true
        result << createEvent(name:"needUpdate", value: "Pending")
        result << createEvent(name:"tamper", value: "clear")
      }
      break
      case 3:
      result << createEvent(name: "tamper", value: "detected", descriptionText: "$device.displayName was tampered")
      // Clear the tamper alert after 10s. This is a temporary fix for the tamper attribute until local execution handles it
      break
      case 1:
      case 2:
      case 7:
      result << motionEvent(true)
      break
    }
  } else {
    log.warn "Need to handle this cmd.notificationType: ${cmd.notificationType}"
      result << createEvent(descriptionText: cmd.toString(), isStateChange: false)
  }

  return result
}

def zwaveEvent(physicalgraph.zwave.commands.wakeupv2.WakeUpIntervalReport cmd) {
  logger("$device.displayName $cmd")
  
  sendCommands(configCmds());
  def result = []
  
  state.wakeInterval = cmd.seconds

  result << createEvent(name: "WakeUpNode", value: cmd.nodeid, isStateChange: true, displayed: true)
  result << createEvent(name: "WakeUpInterval", value: cmd.seconds, isStateChange: true, displayed: true)
}

def zwaveEvent(physicalgraph.zwave.commands.wakeupv2.WakeUpNotification cmd) {
  logger("$device.displayName $cmd")

  def result = []
  def cmds = []

  if (!state.lastbat || (new Date().time) - state.lastbat > 53*60*60*1000) {
    cmds << zwave.batteryV1.batteryGet().format()
  }

  state.lastActive = new Date().time
  result << createEvent(name: "LastAwake", value: state.lastActive, descriptionText: "${device.displayName} woke up", isStateChange: false)

  if (cmds) {
    result << response( delayBetween ( cmds ))
  }

  return result
}

def zwaveEvent(physicalgraph.zwave.commands.firmwareupdatemdv2.FirmwareMdReport cmd) {
  logger("$device.displayName $cmd")
  def firmware_report = String.format("%s-%s-%s", cmd.checksum, cmd.firmwareId, cmd.manufacturerId)
  updateDataValue("FirmwareMdReport", firmware_report)
  [ createEvent(name: "FirmwareMdReport", value: firmware_report, descriptionText: "$device.displayName FIRMWARE_REPORT: $firmware_report", displayed: true, isStateChange: true) ]
}

def zwaveEvent(zwave.commands.manufacturerspecificv2.DeviceSpecificReport cmd, result) {
  logger("$cmd")

  updateDataValue("deviceIdData", "${cmd.deviceIdData}")
  updateDataValue("deviceIdDataFormat", "${cmd.deviceIdDataFormat}")
  updateDataValue("deviceIdDataLengthIndicator", "${cmd.deviceIdDataLengthIndicator}")
  updateDataValue("deviceIdType", "${cmd.deviceIdType}")

  if (cmd.deviceIdType == 1 && cmd.deviceIdDataFormat == 1) {//serial number in binary format
    String serialNumber = "h'"

    cmd.deviceIdData.each{ data ->
      serialNumber += "${String.format("%02X", data)}"
    }

    updateDataValue("serialNumber", serialNumber)
  }
}

def zwaveEvent(physicalgraph.zwave.commands.manufacturerspecificv2.ManufacturerSpecificReport cmd) {
  logger("$device.displayName $cmd")
  def result = []

  if ( ! state.manufacturer ) {
    state.manufacturer= cmd.manufacturerName ? cmd.manufacturerName : "Zooz"
  }
  state.manufacturerId = cmd.manufacturerId
  state.productTypeId = cmd.productTypeId
  state.productId= cmd.productId

  String manufacturerCode = String.format("%04X", cmd.manufacturerId)
  String productTypeCode = String.format("%04X", cmd.productTypeId)
  String productCode = String.format("%04X", cmd.productId)
  String wirelessConfig = "ZWP"

  sendEvent(name: "ManufacturerCode", value: manufacturerCode)
  sendEvent(name: "ProduceTypeCode", value: productTypeCode)
  sendEvent(name: "ProductCode", value: productCode)
  sendEvent(name: "WirelessConfig", value: wirelessConfig)
  sendEvent(name: "MSR", value: "$msr", descriptionText: "$device.displayName", isStateChange: false)

  String msr = String.format("%04X-%04X-%04X", cmd.manufacturerId, cmd.productTypeId, cmd.productId)
  updateDataValue("MSR", msr)
  updateDataValue("manufacturer", "${state.manufacturer}")

  Integer[] parameters = getConfigurationOptions(cmd.productId)

  parameters.each {
    cmds << zwave.configurationV1.configurationGet(parameterNumber: it).format()
  }

  state.MSR = msr

  result << response(delayBetween(cmds, 1000))
  result << response( zwave.versionV1.versionGet() )
  result << createEvent(name: "Manufacturer", value: "${state.manufacturer}", descriptionText: "$device.displayName", isStateChange: true)

  return result
}

def zwaveEvent(physicalgraph.zwave.commands.versionv1.VersionReport cmd) {
  logger("$device.displayName $cmd")

  def text = "$device.displayName: firmware version: ${cmd.applicationVersion}.${cmd.applicationSubVersion}, Z-Wave version: ${cmd.zWaveProtocolVersion}.${cmd.zWaveProtocolSubVersion}"
  state.firmwareVersion = cmd.applicationVersion+'.'+cmd.applicationSubVersion
  [ createEvent(name: "firmwareVersion", value: "V ${state.firmwareVersion}", descriptionText: "$text", displayed: true, isStateChange: true) ]
}

def zwaveEvent(physicalgraph.zwave.commands.deviceresetlocallyv1.DeviceResetLocallyNotification cmd) {
  logger("$device.displayName $cmd")
  [ createEvent(name: "DeviceReset", value: "true", descriptionText: cmd.toString(), isStateChange: true, displayed: true) ]
}

def zwaveEvent(physicalgraph.zwave.commands.zwavecmdclassv1.NodeInfo cmd) {
  logger("$device.displayName $cmd")
  [ createEvent(name: "NIF", value: "$cmd", descriptionText: "$cmd", isStateChange: true, displayed: true) ]
}

def zwaveEvent(physicalgraph.zwave.commands.associationv2.AssociationGroupingsReport cmd) {
  logger("$device.displayName $cmd")

  state.groups = cmd.supportedGroupings

  if (cmd.supportedGroupings) {
    def cmds = []
    for (def x = cmd.supportedGroupings; x <= cmd.supportedGroupings; x++) {
      cmds << zwave.associationGrpInfoV1.associationGroupInfoGet(groupingIdentifier: x, listMode: 0x01);
      cmds << zwave.associationGrpInfoV1.associationGroupNameGet(groupingIdentifier: x);
      cmds << zwave.associationV2.associationGet(groupingIdentifier: x);
    }

    sendCommands(cmds, 2000)
  } else {
    [ createEvent(descriptionText: "$device.displayName reported no groups", isStateChange: true, displayed: true) ]
  }
}

def zwaveEvent(physicalgraph.zwave.commands.associationgrpinfov1.AssociationGroupInfoReport cmd) {
  logger("$device.displayName $cmd")
  [ createEvent(descriptionText: "$device.displayName AssociationGroupInfoReport: $cmd", isStateChange: true, displayed: true) ]
}

def zwaveEvent(physicalgraph.zwave.commands.associationgrpinfov1.AssociationGroupNameReport cmd) {
  logger("$device.displayName $cmd")
  [ createEvent(descriptionText: "$device.displayName AssociationGroupNameReport: $cmd", displayed: true) ]
}
def zwaveEvent(physicalgraph.zwave.commands.associationv2.AssociationReport cmd) {
  logger("$device.displayName $cmd")

  Boolean isStateChange
  String event_value
  String event_descriptionText

  // Lifeline
  if (cmd.groupingIdentifier == 0x01) {
    def string_of_assoc = ""
    cmd.nodeId.each {
      string_of_assoc += "${it}, "
    }
    def lengthMinus2 = string_of_assoc.length() - 2
    String final_string = string_of_assoc.getAt(0..lengthMinus2)

    if (cmd.nodeId.any { it == zwaveHubNodeId }) {
      isStateChange = state.isAssociated ?: false
      event_value = "${final_string}"
      event_descriptionText = "${final_string}"
      state.isAssociated = true
    } else {
      isStateChange = state.isAssociated ? true : false
      event_value = ""
      event_descriptionText = "Hub was not found in lifeline: ${final_string}"
      state.isAssociated = false
    }
  } else {
    isStateChange = state.isAssociated ? true : false
    event_value = "misconfigured"
    event_descriptionText = "misconfigured group ${cmd.groupingIdentifier}"
  }

  if (state.isAssociated == false && cmd.groupingIdentifier == 0x01) {
    sendEvent(name: "Lifeline",
        value: "${event_value}",
        descriptionText: "${event_descriptionText}",
        displayed: true,
        isStateChange: isStateChange)
      sendCommands( [ zwave.associationV2.associationSet(groupingIdentifier: cmd.groupingIdentifier, nodeId: [zwaveHubNodeId]) ] )
  } else {
    [ createEvent(descriptionText: "$device.displayName is not associated to ${cmd.groupingIdentifier}", displayed: true) ]
  }
}

def zwaveEvent(physicalgraph.zwave.Command cmd) {
  logger("$device.displayName command not implemented: $cmd", "error")
  [ createEvent(descriptionText: "$device.displayName command not implemented: $cmd", displayed: true) ]
}

def refresh() {
  logger("$device.displayName - refresh()")

  def cmds = []

  if (state.lastRefresh != null && now() - state.lastRefresh < 5000) {
    logger("Refresh Double Press", "debug")

    cmds << zwave.wakeUpV1.wakeUpIntervalGet().format()
  }

  state.lastRefresh = now()
  cmds << zwave.batteryV1.batteryGet()
  cmds << zwave.sensorMultilevelV5.sensorMultilevelGet(sensorType:1, scale:1)
  cmds << zwave.sensorMultilevelV5.sensorMultilevelGet(sensorType:3, scale:1)
  cmds << zwave.sensorMultilevelV5.sensorMultilevelGet(sensorType:5, scale:1)
  cmds << zwave.associationV2.associationSet(groupingIdentifier:1, nodeId:[zwaveHubNodeId])

  sendCommands(cmds, 500)
}

/**
 * Convert 1 and 2 bytes values to integer
 */
def cmd2Integer(array) {
  switch(array.size()) {
    case 1:
    array[0]
    break
    case 2:
    ((array[0] & 0xFF) << 8) | (array[1] & 0xFF)
    break
    case 4:
    ((array[0] & 0xFF) << 24) | ((array[1] & 0xFF) << 16) | ((array[2] & 0xFF) << 8) | (array[3] & 0xFF)
    break
  }
}

def integer2Cmd(value, size) {
  switch(size) {
    case 1:
    [value]
    break
    case 2:
    def short value1   = value & 0xFF
    def short value2 = (value >> 8) & 0xFF
    [value2, value1]
    break
    case 4:
    def short value1 = value & 0xFF
    def short value2 = (value >> 8) & 0xFF
    def short value3 = (value >> 16) & 0xFF
    def short value4 = (value >> 24) & 0xFF
    [value4, value3, value2, value1]
    break
  }
}

private isConfigured() {
  getDataValue("configured") == "true"
}

private getBatteryRuntime() {
  def currentmillis = now() - state.batteryRuntimeStart
  def days=0
  def hours=0
  def mins=0
  def secs=0
  secs = (currentmillis/1000).toInteger()
  mins=(secs/60).toInteger()
  hours=(mins/60).toInteger()
  days=(hours/24).toInteger()
  secs=(secs-(mins*60)).toString().padLeft(2, '0')
  mins=(mins-(hours*60)).toString().padLeft(2, '0')
  hours=(hours-(days*24)).toString().padLeft(2, '0')


  if ( days>0) {
    return "$days days and $hours:$mins:$secs"
  } else {
    return "$hours:$mins:$secs"
  }
}

private getRoundedInterval(number) {
  double tempDouble = (number / 60)
  if (tempDouble == tempDouble.round()) {
    return (tempDouble * 60).toInteger()
  } else {
    return ((tempDouble.round() + 1) * 60).toInteger()
  }
}

private getAdjustedTemp(value) {

  value = Math.round((value as Double) * 100) / 100

  if (settings."302") {
    return value =  value + Math.round(settings."302" * 100) /100
  } else {
    return value
  }

}

private getAdjustedHumidity(value) {

  value = Math.round((value as Double) * 100) / 100

  if (settings."303") {
    return value =  value + Math.round(settings."303" * 100) /100
  } else {
    return value
  }

}

private getAdjustedLuminance(value) {

  value = Math.round((value as Double) * 100) / 100

  if (settings."304") {
    return value =  value + Math.round(settings."304" * 100) /100
  } else {
    return value
  }

}

def updated() {
  if (state.updatedDate && (Calendar.getInstance().getTimeInMillis() - state.updatedDate) < 5000 ) {
    return
  }
  state.loggingLevelIDE = debugLevel ? debugLevel : 3
  log.info("$device.displayName updated() debug: ${state.loggingLevelIDE}")

  sendEvent(name: "lastError", value: "", displayed: false)
  sendEvent(name: "logMessage", value: "", displayed: false)
  sendEvent(name: "parseErrorCount", value: 0, displayed: false)
  sendEvent(name: "unknownCommandErrorCount", value: 0, displayed: false)

  state.lastBatteryReport = 0
  state.configRequired = true
  sendEvent(name:"needUpdate", value: "Pending")

  if (state.realTemperature != null) {
    sendEvent(name:"temperature", value: getAdjustedTemp(state.realTemperature))
  } else {
    state.realTemperature = 0
    state.temp = 0
  }

  if (state.realHumidity != null) {
    sendEvent(name:"humidity", value: getAdjustedHumidity(state.realHumidity))
  } else {
    state.realHumidity = 0
    state.humidity = 0
  }

  if (state.realLuminance != null) {
    sendEvent(name:"illuminance", value: getAdjustedLuminance(state.realLuminance))
  } else {
    state.realLuminance = 0
    state.illuminance = 0
  }

  // set default values so the display isn't messed up
  if (state.motion == null || state.motionText == null) {
      state.motion = "inactive"
      state.motionText = "No Motion"
  }

  sendEvent(name:"motion", value: state.motion, descriptionText: "$state.motionText")
  
  sendEvent(name: "sensorlevels", value: "")

  if (!device.currentState("ManufacturerCode")) {
    sendCommands([
      zwave.manufacturerSpecificV2.manufacturerSpecificGet()
    ])
  }

  // Avoid calling updated() twice
  state.updatedDate = Calendar.getInstance().getTimeInMillis()
}

def installed() {
  log.info("$device.displayName installed()")
  state.loggingLevelIDE = 4

  sendEvent(name: "driverVersion", value: getDriverVersion(), descriptionText: getDriverVersion(), isStateChange: true, displayed: true)
  sendEvent(name: "configured", value: "false", isStateChange: true)

  // called when the device is installed
  state.configRequired = true
  state.lastBatteryReport = 0

  state.realTemperature = 0
  state.temp = 0
  state.realHumidity = 0
  state.humidity = 0
  state.realLuminance = 0
  state.illuminance = 0

  sendEvent(name:"needUpdate", value: "Pending")
}

def configCmds() {
  log.debug "configure, tempAlertPref:${tempAlertPref} humidityAlertPref:${humidityAlertPref}"
  log.debug "configure, illumAlertPref:${illumAlertPref} pirTimeoutPref:${pirTimeoutPref}"
  log.debug "configure, pirSensitivityPref:${pirSensitivityPref} ledModePref:${ledModePref}"
  log.debug "configure, wakeIntervalPref:${wakeIntervalPref}"
  def cmds = [
    zwave.associationV2.associationSet(groupingIdentifier:1, nodeId:zwaveHubNodeId),
    zwave.configurationV1.configurationSet(scaledConfigurationValue: 1, parameterNumber: 1),
    zwave.configurationV1.configurationSet(scaledConfigurationValue: tempAlertPref, parameterNumber: 2),
    zwave.configurationV1.configurationSet(scaledConfigurationValue: humidityAlertPref, parameterNumber: 3),
    zwave.configurationV1.configurationSet(scaledConfigurationValue: illumAlertPref, parameterNumber: 4),
    zwave.configurationV1.configurationSet(scaledConfigurationValue: pirTimeoutPref, parameterNumber: 5),
    zwave.configurationV1.configurationSet(scaledConfigurationValue: pirSensitivityPref, parameterNumber: 6),
    zwave.configurationV1.configurationSet(scaledConfigurationValue: ledModePref, parameterNumber: 7),
    zwave.wakeUpV2.wakeUpIntervalSet(seconds: wakeIntervalPref, nodeid: 0x01),
    // zwave.wakeUpV2.wakeUpIntervalSet(seconds: wakeIntervalPref, nodeid:zwaveHubNodeId)
  ]
  return cmds
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

private logger(msg, level = "trace") {
  String device_name = "$device.displayName"
  String msg_text = (msg != null) ? "${msg}" : "<null>"

  Integer log_level = state.defaultLogLevel ?: settings.debugLevel

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
