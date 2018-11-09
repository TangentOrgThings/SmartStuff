private logger(msg, level = "trace") {
  String device_name = "$device.displayName"

  switch(level) {
    case "warn":
    if (settings.debugLevel >= 2) {
      log.warn "$device_name ${msg}"
    }
    sendEvent(name: "logMessage", value: "${msg}", displayed: false, isStateChange: true)
    break;

    case "info":
    if (settings.debugLevel >= 3) {
      log.info "$device_name ${msg}"
    }
    break;

    case "debug":
    if (settings.debugLevel >= 4) {
      log.debug "$device_name ${msg}"
    }
    break;

    case "trace":
    if (settings.debugLevel >= 5) {
      log.trace "$device_name ${msg}"
    }
    break;

    case "error":
    default:
    log.error "$device_name ${msg}"
    sendEvent(name: "lastError", value: "${msg}", displayed: false, isStateChange: true)
    break;
  }
}
