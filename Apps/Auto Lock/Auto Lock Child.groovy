/* 
 *   Hubitat Import URL: https://raw.githubusercontent.com/heidrickla/Hubitat/Apps/Auto%20Lock/Auto%20Lock%20Child.groovy
 *
 *   Author Chris Sader, modified by Lewis Heidrick with permission from Chris to takeover the project.
 *   
 *   12/28/2020 - Project Published to GitHub
 */
import groovy.transform.Field
import hubitat.helper.RMUtils

def setVersion() {
    state.name = "Auto Lock"
	state.version = "1.1.24"
}

definition(
    name: "Auto Lock Child",
    namespace: "heidrickla",
    author: "Lewis Heidrick",
    description: "Automatically locks a specific door after X minutes/seconds when closed and unlocks it when open.",
    category: "Convenience",
    parent: "heidrickla:Auto Lock",
    iconUrl: "",
    iconX2Url: "",
    iconX3Url: "",
    importUrl: "https://raw.githubusercontent.com/heidrickla/Hubitat/Apps/Auto%20Lock/Auto%20Lock%20Child.groovy")

preferences {
    page(name: "mainPage")
    page(name: "timeIntervalInput", title: "Only during a certain time") {
		section {
			input "starting", "time", title: "Starting", required: false
			input "ending", "time", title: "Ending", required: false
        }
    }
}

def mainPage() {
    dynamicPage(name: "mainPage", install: true, uninstall: true, refreshInterval:0) {
        ifTrace("mainPage")
        turnOffLoggingTogglesIn30()
        setPauseButtonName()
//      lock1LockHandler()
//      lock1UnlockHandler()
//      lock1BatteryHandler()
//      contactContactHandler()
//      contactBatteryHandler()
//      diagnosticHandler()

    section("") {
      input name: "Pause", type: "button", title: state.pauseButtonName, submitOnChange:true
      if (location?.name.contains("Home / Hubitat C7 - Dev")) {input "variableInfo", "bool", title: "Get Variable Info", submitOnChange: true, required: false, defaultValue: false
          log.info "${location.name}"
          getVariableInfo}
      app.updateSetting("variableInfo",[value:"false",type:"bool"])
      input "detailedInstructions", "bool", title: "Enable detailed instructions?", submitOnChange: true, required: false, defaultValue: false
      
    }
    section("") {
        if ((state.thisName == null) || (state.thisName == "null <span style=color:white> </span>")) {state.thisName = "Enter a name for this app."}
        input name: "thisName", type: "text", title: "", required:true, submitOnChange:true, defaultValue: "Enter a name for this app."
        state.thisName = thisName
        updateLabel()
    }
    section("") {
        if (detailedInstructions == true) {paragraph "This option performs an immediate update to the current status of the Lock, Contact Sensor, Presence Sensor, and Status of the application.  It will automatically reset back to off after activated."}
        input "refresh", "bool", title: "Click here to refresh the device status", submitOnChange: true, required: false
        app.updateSetting("refresh",[value:"false",type:"bool"])
        if (detailedInstructions == true) {paragraph "This is the lock that all actions will activate against. The app watches for locked or unlocked status sent from the device.  If it cannot determine the current status, the last known status of the lock will be used.  If there is not a last status available and State sync fix is enabled it will attempt to determine its' state, otherwise it will default to a space. Once a device is selected, the current status will appear on the device.  The status can be updated by refreshing the page or clicking the refresh status toggle."}
        input "lock1", "capability.lock", title: "Lock: ${state.lock1LockStatus} ${state.lock1BatteryStatus}", submitOnChange: true, required: true
        if (detailedInstructions == true) {paragraph "This is the contact sensor that will be used to determine if the door is open.  The lock will not lock while the door is open.  If it does become locked and Bolt/Frame strike protection is enabled, it will immediately try to unlock to keep from hitting the bolt against the frame. If you are having issues with your contact sensor or do not use one, it is recommended to disable Bolt/frame strike protection as it will interfere with the operation of the lock."}
        input "contact", "capability.contactSensor", title: "Door: ${state.contactContactStatus} ${state.contactBatteryStatus}", submitOnChange: true, required: false
    }
    section(title: "Locking Options:", hideable: true, hidden: hideLockOptionsSection()) {
        if (detailedInstructions == true) {paragraph "Use seconds instead changes the timer used in the application to determine if the delay before performing locking actions will be based on minutes or seconds.  This will update the label on the next option to show its' setting."}
        input "minSecLock", "bool", title: "Use seconds instead?", submitOnChange:true, required: true, defaultValue: false
        if (detailedInstructions == true) {paragraph "This value is used to determine the delay before locking actions occur. The minutes/seconds are determined by the Use seconds instead toggle."}
        if (minSecLock == false) {input "durationLock", "number", title: "Lock it how many minutes later?", required: true, submitOnChange: false, defaultValue: 10}
        if (minSecLock == true) {input "durationLock", "number", title: "Lock it how many seconds later?", required: true, submitOnChange: false, defaultValue: 10}
        if (detailedInstructions == true) {paragraph "Enable retries if lock fails to change state enables all actions that try to lock the door up to the maximum number of retries.  If all retry attempts fail, a failure notice will appear in the logs.  Turning this toggle off causes any value in the Maximum number of retries to be ignored."}
        input "retryLock", "bool", title: "Enable retries if lock fails to change state.", required: false, submitOnChange: false, defaultValue: true
        if (detailedInstructions == true) {paragraph "Maximum number of retries is used to determine the limit of times that a locking action can attempt to perform an action.  This option is to prevent the lock from attempting over and over until the batteries are drained."}
        input "maxRetriesLock", "number", title: "Maximum number of retries?", required: false, submitOnChange: false, defaultValue: 3
        if (detailedInstructions == true) {paragraph "Delay between retries in second(s) provides the lock enough time to perform the locking action.  If you set this too low  and it send commands to the lock before it completes its' action, the commands will be ignored.  Three to five seconds is usually enough time for the lock to perform any actions and report back its' status."}
        input "delayBetweenRetriesLock", "number", title: "Delay between retries in second(s)?", require: false, submitOnChange: false, defaultValue: 5
        if (enableHSMToggle == true) {input "hsmCommandsLock","enum", title: "Set HSM status when Locked?",required: false, multiple: false, submitOnChange: false, options: hsmCommandOptions}
    }
    section(title: "Unlocking Options:", hideable: true, hidden: hideUnlockOptionsSection()) {
        if (detailedInstructions == true) {if (settings.whenToUnlock?.contains("2")) {paragraph "This sensor is used for presence unlock triggers."}}
        if (settings.whenToUnlock?.contains("2")) {input "unlockPresence", "capability.presenceSensor", title: "Presence: ${state.unlockPresenceStatus} ${state.unlockPresenceBatteryStatus}", submitOnChange: true, required: false, multiple: false}
//wip        if ((settings.whenToUnlock?.contains("2")) && (unlockPresenceSensor)) {input "allUnlockPresenceSensor", "bool", title: "Present status requires all presence sensors to be present?", submitOnChange:true, required: false, defaultValue: false}
        if (settings.whenToUnlock?.contains("3")) {input "fireMedical", "capability.smokeDetector", title: "Fire/Medical: ${fireMedicalStatus} ${fireMedicalBatteryStatus}", submitOnChange: true, required: false, multiple: false}
        if (settings.whenToUnlock?.contains("4")) {input "deviceActivationSwitch", "capability.switch", title: "Switch Triggered Action: ${deviceActivationSwitchStatus}", submitOnChange: true, required: false, multiple: false}
        if (settings.whenToUnlock?.contains("4")) {input "deviceActivationToggle", "bool", title: "Invert Switch Triggered Action: ", submitOnChange: false, required: false, multiple: false, defaultValue: false}
        if (detailedInstructions == true) {paragraph "Bolt/Frame strike protection detects when the lock is locked and the door is open and immediately unlocks it to prevent it striking the frame.  This special case uses a modified delay timer that ignores the Unlock it how many minutes/seconds later and Delay between retries option.  It does obey the Maximum number of retries though."}
        if (detailedInstructions == true) {paragraph "Presence detection uses the selected presence device(s) and on arrival will unlock the door.  It is recommended to use a combined presence app to prevent false triggers.  I recommend Presence Plus and Life360 with States by BPTWorld, and the iPhone Presence driver (it works on android too).  You might need to mess around with battery optimization options to get presence apps to work reliably on your phone though."}
        if (detailedInstructions == true) {paragraph "Fire/Medical panic unlock will unlock the door whenever a specific sensor is opened.  I have zones on my alarm that trip open if one of these alarms are triggered and use an Envisalink 4 to bring over the zones into Hubitat. They show up as contact sensors.  If you have wired smoke detectors to your alarm panel, these are typically on zone 1.  You could use any sensor though to trigger."}
        if (detailedInstructions == true) {paragraph "Switch triggered unlock lets you trigger a lock or an unlock with a switch.  You can use the Invert Switch Triggered Action to flip the trigger logic to when the switch is on or off. This is different from the Switch to enable and disable option as it is used to lock and unlock the door."}
        if (detailedInstructions == true) {paragraph "State sync fix is used when the lock is locked but the door becomes opened.  Since this shouldn't happen it immediately unlocks the lock and tries to refresh the lock if successful it updates the app status.  If the unlock attempt fails, it then will attempt to retry and follows any unlock delays or retry restrictions.  This option allows you to use the lock and unlock functionality and still be able to use the app when you experience sensor problems by disabling this option."}
        if (detailedInstructions == true) {paragraph "Prevent unlocking under any circumstances is used when you want to disable all unlock functionality in the app. It overrides all unlock settings including Fire/Medical panic unlock."}
        input "whenToUnlock", "enum", title: "When to unlock?  Default: '(Prevent unlocking under any circumstances)'", options: whenToUnlockOptions, defaultValue: ["6"], required: true, multiple: true, submitOnChange:true
        if (!settings.whenToUnlock?.contains("6")) {
        if (detailedInstructions == true) {paragraph "Use seconds instead changes the timer used in the application to determine if the delay before performing unlocking actions will be based on minutes or seconds. This will update the label on the next option to show its' setting."}
        input "minSecUnlock", "bool", title: "Use seconds instead?", submitOnChange: true, required: true, defaultValue: true
        if (detailedInstructions == true) {paragraph "This value is used to determine the delay before unlocking actions occur. The minutes/seconds are determined by the Use seconds instead toggle."}
        if (minSecUnlock == false) {input "durationUnlock", "number", title: "Unlock it how many minutes later?", submitOnChange: false, required: true, defaultValue: 2}
        if (minSecUnlock == true) {input "durationUnlock", "number", title: "Unlock it how many seconds later?", submitOnChange: false, required: true, defaultValue: 2}
        if (detailedInstructions == true) {paragraph "Enable retries if unlock fails to change state enables all actions that try to unlock the door up to the maximum number of retries.  If all retry attempts fail, a failure notice will appear in the logs.  Turning this toggle off causes any value in the Maximum number of retries to be ignored."}
        input "retryUnlock", "bool", title: "Enable retries if unlock fails to change state.", submitOnChange: false, require: false, defaultValue: true
        if (detailedInstructions == true) {paragraph "Maximum number of retries is used to determine the limit of times that an unlocking action can attempt to perform an action.  This option is to prevent the lock from attempting over and over until the batteries are drained."}
        input "maxRetriesUnlock", "number", title: "Maximum number of retries? While door is open it will wait for it to close.", submitOnChange: false, required: false, defaultValue: 3
        if (detailedInstructions == true) {paragraph "Delay between retries in second(s) provides the lock enough time to perform the unlocking action.  If you set this too low and it send commands to the lock before it completes its' action, the commands will be ignored.  Three to five seconds is usually enough time for the lock to perform any actions and report back its' status."}
        input "delayBetweenRetriesUnlock", "number", title: "Delay between retries in second(s)?", submitOnChange: false, require: false, defaultValue: 3
        if (enableHSMToggle == true) {input "hsmCommandsUnlock","enum", title: "Set HSM when Unlocked?",required: false, multiple: false, submitOnChange: false, options: hsmCommandOptions}
        }
    }
    section (title: "Notification Options:", hideable: true, hidden: hideNotificationSection()) {
// Only low battery notifications are currently enabled.  The rest of the notifications will be coming soon.
//        input "notifyOnEvent", "bool", title: "Enable Event Notifications?", submitOnChange: true, required:false, defaultValue: false
//        if (notifyOnEvent == true) {input "eventNotificationDevices", "capability.notification", title: "Event Notification Devices:", submitOnChange: false, multiple: true, required: false}
//        if (notifyOnEvent == true) {input "eventsToNotifyFor", "enum", title: "Select the types of events that you want to get notifications for:", required: false, multiple: true, submitOnChange: false, options: eventNotificationOptions}
        input "notifyOnLowBattery", "bool", title: "Enable Low Battery Notifications?", submitOnChange: true, required:false, defaultValue: false
        if (notifyOnLowBattery == true) {input "lowBatteryNotificationDevices", "capability.notification", title: "Low Battery Notification Devices:", submitOnChange: false, multiple: true, required: false}
        if (notifyOnLowBattery == true) {input "lowBatteryDevicesToNotifyFor", "enum", title: "Select devices that you want to get notifications for:", required: false, multiple: true, submitOnChange: false, options: lowBatteryNotificationOptions}
        if (notifyOnLowBattery == true) {input "lowBatteryAlertThreshold", "number", title: "Below what percentage do you want to be notified?", submitOnChange: false, required:true, defaultValue: 30}
//        input "notifyOnFailure", "bool", title: "Enable Failure Notifications?", submitOnChange: true, required: false, defaultValue: false
//        if (notifyOnFailure == true) {input "failureNotificationDevices", "capability.notification", title: "Failure Notification Devices:", submitOnChange: false, multiple: true, required: false}
//        if (notifyOnFailure == true) {input "failureNotifications", "enum", title: "Failure Notifications:", required: false, multiple: true, submitOnChange: false, options: failureNotificationOptions}
    }
    section(title: "Logging Options:", hideable: true, hidden: hideLoggingSection()) {
        if (detailedInstructions == true) {paragraph "Enable Info logging for 30 minutes will enable info logs to show up in the Hubitat logs for 30 minutes after which it will turn them off. Useful for checking if the app is performing actions as expected."}
        input "isInfo", "bool", title: "Enable Info logging for 30 minutes", submitOnChange: false, required:false, defaultValue: false
        if (detailedInstructions == true) {paragraph "Enable Debug logging for 30 minutes will enable debug logs to show up in the Hubitat logs for 30 minutes after which it will turn them off. Useful for troubleshooting problems."}
        input "isDebug", "bool", title: "Enable debug logging for 30 minutes", submitOnChange: false, required:false, defaultValue: false
        if (detailedInstructions == true) {paragraph "Enable Trace logging for 30 minutes will enable trace logs to show up in the Hubitat logs for 30 minutes after which it will turn them off. Useful for following the logic inside the application but usually not neccesary."}
        input "isTrace", "bool", title: "Enable Trace logging for 30 minutes", submitOnChange: false, required:false, defaultValue: false
        if (detailedInstructions == true) {paragraph "Logging level is used to permanantly set your logging level for the application.  If it is set higher than any temporary logging options you enable, it will override them.  If it is set lower than temporary logging options, they will take priority until their timer expires.  This is useful if you prefer you logging set to a low level and then can use the logging toggles for specific use cases so you dont have to remember to go back in and change them later.  It's also useful if you are experiencing issues and need higher logging enabled for longer than 30 minutes."}
        input "ifLevel","enum", title: "Logging level", required: true, multiple: true, submitOnChange: false, options: logLevelOptions
        if (enableHSMToggle == true) {input "isHSM", "bool", title: "Enable HSM logging", submitOnChange: true, required:false, defaultValue: false}
        if ((enableHSMToggle == true) && (isHSM == true)) {input "hsmLogLevel","enum", title: "Show HSM Alerts in log as?", required: false, multiple: false, options: logLevelOptions}
    }
    section(title: "Only Run When:", hideable: true, hidden: hideOptionsSection()) {
        def timeLabel = timeIntervalLabel()
        if (detailedInstructions == true) {paragraph "Switch to Enable and Disable this app prevents the app from performing any actions other than status updates for the lock and contact sensor state and battery state on the app page."}
        input "disabledSwitch", "capability.switch", title: "Switch to Enable and Disable this app ${disabledSwitchStatus}", submitOnChange: false, required: false, multiple: false
        if (detailedInstructions == true) {paragraph "Only during a certain time is used to restrict the app to running outside of the assigned times. You can use this to prevent false presence triggers while your sleeping from unlocking the door."}
        href "timeIntervalInput", title: "Only during a certain time", description: timeLabel ?: "Tap to set", state: timeLabel ? "complete" : null
        if (detailedInstructions == true) {paragraph "Only on certain days of the week restricts the app from running outside of the assigned days. Useful if you work around the yard frequently on the weekends and want to keep your door unlocked and just want the app during the week."}
        input "days", "enum", title: "Only on certain days of the week", multiple: true, required: false, submitOnChange: false, options: daysOptions
        if (detailedInstructions == true) {paragraph "Only when mode is allows you to prevent the app from running outside of the specified modes. This is useful if you have a party mode and want the lock from re-locking on you while company is over.  This could also be used like the Only during a certain time mode to prevent faluse triggers at night for instance."}
        input "modes", "mode", title: "Only when mode is", multiple: true, required: false, submitOnChange:false
        input "enableHSMToggle", "bool", title: "Enable HSM Actions", required:false, submitOnChange: true, defaultValue: false
        input "enableHSMSwitch", "capability.switch", title: "Switch to Enable HSM Actions: (Optional) ${enableHSMSwitchStatus}", required: false, multiple: false, submitOnChange: true
        if (enableHSMToggle == true) {input "whenToLockHSM", "enum", title: "Only lock when HSM status is?", options: hsmStateOptions, required: false, multiple: true, submitOnChange:false}
        if (enableHSMToggle == true) {input "whenToUnlockHSM", "enum", title: "Only unlock when HSM status is?", options: hsmStateOptions, required: false, multiple: true, submitOnChange:false}
    }
    }
}

// Application settings and startup
@Field static List<Map<String,String>> whenToUnlockOptions = [
    ["1": "Bolt/frame strike protection"],
    ["2": "Presence unlock"],
    ["3": "Fire/medical panic unlock"],
    ["4": "Switch triggered unlock"],
    ["5": "State sync fix"],
    ["6": "Prevent unlocking under any circumstances"]
]

@Field static List<Map<String>> daysOptions = ["Monday","Tuesday","Wednesday","Thursday","Friday","Saturday","Sunday"]

@Field static List<Map<String,String>> logLevelOptions = [
    ["0": "None"],
    ["1": "Info"],
    ["2": "Debug"],
    ["3": "Trace"]
]

@Field static List<Map<String,String>> hsmCommandOptions = [
    ["armAway": "Arm Away"],
    ["armHome": "Arm Home"],
    ["armNight": "Arm Night"],
    ["disarm": "Disarm"],
    ["armRules": "Arm Rules"],
    ["disarmRules": "Disarm Rules"],
    ["disarmAll": "Disarm All"],
    ["armAll": "Arm All"],
    ["cancelAlerts": "Cancel Alerts"]
]

@Field static List<Map<String,String>> hsmStateOptions = [
    ["armedAway": "Armed Away"],
    ["armedHome": "Armed Home"],
    ["armedNight": "Armed Night"],
    ["disarmed": "Disarmed"]
]

@Field static List<Map<String,String>> eventNotificationOptions = [
    ["1": "Lock Physical"],
    ["2": "Unlock Physical"],
    ["3": "Lock Digital"],
    ["4": "Unlock Digital"],
    ["5": "Fire/Medical Panic Triggered"],
    ["6": "Presence Arrival Unlock"],
    ["7": "Presence Departure Lock"],
    ["8": "Switch Triggered Lock"],
    ["9": "Switch Triggered Unlock"]
]

@Field static List<Map<String,String>> lowBatteryNotificationOptions = [
    ["1": "Lock"],
    ["2": "Contact Sensor"],
    ["3": "Smoke Detector"]
]

@Field static List<Map<String,String>> failureNotificationOptions = [
    ["1": "Max lock retries exceeded"],
    ["2": "Max unlock retries exceeded"],
    ["3": "Fire/Medical panic triggered unlock but application is paused or disabled"],
    ["4": "Switch triggered unlock but application is paused or disabled"],
    ["5": "State sync fix triggered"],
    ["6": "Unlock triggered while Preventing unlock under any circumstances was enabled"]
]

def installed() {
    ifTrace("installed")
    ifDebug("Auto Lock Door installed.")
    state.installed = true
    if (state.lock1LockStatus == null) {state.lock1LockStatus = " "}
    if (state.lock1BatteryStatus == null) {state.lock1BatteryStatus = " "}
    if (state.contactContactStatus == null) {state.contactContactStatus = " "}
    if (state.contactBatteryStatus == null) {state.contactBatteryStatus = " "}
    if (state.unlockPresenceStatus == null) {state.unlockPresenceStatus = " "}
    if (state.unlockPresenceBatteryStatus == null) {state.unlockPresenceBatteryStatus = " "}
    if (state.fireMedicalStatus == null) {state.fireMedicalStatus = " "}
    if (state.fireMedicalBatteryStatus == null) {state.fireMedicalBatteryStatus = " "}
    if (state.deviceActivationSwitchStatus == null) {state.deviceActivationSwitchStatus = " "}
    if (state.disabledSwitchStatus == null) {state.disabledSwitchStatus = " "}
    initialize()
}

def updated() {
    ifTrace("updated")
    ifDebug("Settings: ${settings}")
    if (state?.installed == null) {
		state.installed = true
	}
    if (state.lock1LockStatus == null) {state.lock1LockStatus = " "}
    if (state.lock1BatteryStatus == null) {state.lock1BatteryStatus = " "}
    if (state.contactContactStatus == null) {state.contactContactStatus = " "}
    if (state.contactBatteryStatus == null) {state.contactBatteryStatus = " "}
    if (state.unlockPresenceStatus == null) {state.unlockPresenceStatus = " "}
    if (state.unlockPresenceBatteryStatus == null) {state.unlockPresenceBatteryStatus = " "}
    if (state.fireMedicalStatus == null) {state.fireMedicalStatus = " "}
    if (state.fireMedicalBatteryStatus == null) {state.fireMedicalBatteryStatus = " "}
    if (state.deviceActivationSwitchStatus == null) {state.deviceActivationSwitchStatus = " "}
    if (state.disabledSwitchStatus == null) {state.disabledSwitchStatus = " "}
    unsubscribe()
    unschedule()
    initialize()
}

def initialize() {
    ifTrace("initialize")
    ifDebug("Settings: ${settings}")
    subscribe(disabledSwitch, "switch.on", disabledHandler)
    subscribe(disabledSwitch, "switch.off", disabledHandler)
    subscribe(deviceActivationSwitch, "switch.on", deviceActivationSwitchHandler)
    subscribe(deviceActivationSwitch, "switch.off", deviceActivationSwitchHandler)
    subscribe(deviceActivationToggle, "switch", deviceActivationToggleHandler)
    if (settings.whenToUnlock?.contains("3")) {subscribe(fireMedical, "contact.open", fireMedicalHandler)}
    if (settings.whenToUnlock?.contains("3")) {subscribe(fireMedical, "contact.closed", fireMedicalHandler)}
    if (settings.whenToUnlock?.contains("3")) {subscribe(fireMedical, "battery", fireMedicalBatteryHandler)}
    subscribe(lock1, "lock.locked", lock1LockHandler)
    subscribe(lock1, "lock.unlocked", lock1UnlockHandler)
    subscribe(lock1, "battery", lock1BatteryHandler)
    subscribe(contact, "contact.open", contactContactHandler)
    subscribe(contact, "contact.closed", contactContactHandler)
    subscribe(contact, "battery", contactBatteryHandler)
    if (settings.whenToUnlock?.contains("2")) {subscribe(unlockPresence, "presence", unlockPresenceHandler)}
    if (settings.whenToUnlock?.contains("2")) {subscribe(unlockPresence, "battery", unlockPresenceBatteryHandler)}
    subscribe(enableHSMSwitch, "switch", enableHSMHandler)
    if ((whenToLockHSM || whenToUnlockHSM) && (enableHSMActions)) {subscribe(location, "hsmStatus", hsmStatusHandler)}   //For app to subscribe to HSM status
    if ((whenToLockHSM || whenToUnlockHSM) && (enableHSMActions)) {subscribe(location, "hsmAlerts", hsmAlertHandler)}    //For app to subscribe to HSM alerts
    getAllOk()
}

// Device Handlers
def hsmAlertHandler(evt) {
    // HSM Alert Handler Action
    if (evt.value != null) {ifTrace("hsmAlertHandler: ${evt.value}")}
    if (hsmLogLevel == "1") {log.info "HSM Alert: $evt.value" + (evt.value == "rule" ? ",  $evt.descriptionText" : "")}
    else if (hsmLogLevel == "2") {log.debug "HSM Alert: $evt.value" + (evt.value == "rule" ? ",  $evt.descriptionText" : "")}
    else if (hsmLogLevel == "3") {log.trace "HSM Alert: $evt.value" + (evt.value == "rule" ? ",  $evt.descriptionText" : "")}
    else {}
//    only has descriptionText for rule alert
}

def enableHSMHandler(evt) {
    // Device Status
    if (evt.value != null) {ifTrace("enableHSMHandler: ${evt.value}")}
    if (evt.value == "on") {app.updateSetting("enableHSMToggle",[value:"true",type:"bool"])
    } else if (evt.value == "off") {app.updateSetting("enableHSMToggle",[value:"false",type:"bool"])}
    updateLabel()
    
    // Device Handler Action
    
}

def lock1LockHandler(evt) {
    // Device Status
    if (evt.value != null) {
        ifTrace("lock1LockHandler: ${evt.value}")
        state.lock1LockStatus = "[${evt.value}]"
    } else if (evt.value == "locked") {state.lock1LockStatus = "[${evt.value}]"
    } else if (lock1?.currentValue("lock") != null) {state.lock1LockStatus = "[${lock1.currentValue("lock")}]"
    } else if (lock1?.latestValue("lock") != null) {state.lock1Lock1Status = "[${lock1.latestValue("lock")}]"
    } else {state.lock1LockStatus = " "}
    
    // Log Manual Locking
    ifDebug("${evt.name} : ${evt.descriptionText}")
    if (evt.type == 'physical' && evt.descriptionText.endsWith('locked by keypad')) {/* Do something here */
    } else if (evt.type == 'physical' && evt.descriptionText.contains('locked by code')) {/* Do something here */
    } else if (evt.type == 'physical' && evt.descriptionText.endsWith('locked by manual')) {/* Do something here */}
    updateLabel()
    
    // Device Handler Action
    if ((getAllOk() == false) || (state?.pausedOrDisabled == true)) {
        ifTrace("lock1LockHandler: Application is paused or disabled.")
    } else if (settings.whenToUnlock?.contains("6")) {
    // Unlocking is disabled. Doing nothing.
    } else if (settings.whenToUnlock?.contains("1") && (contact?.currentValue("contact") == "open")) {
        ifDebug("lock1LockHandler:  Lock was locked while Door was open. Performing a fast unlock to prevent hitting the bolt against the frame.")
        lock1.unlock()
        countUnlock = maxRetriesUnlock
        unschedule(unlockDoor)
        def delayUnlock = 1
        runIn(delayUnlock, unlockDoor)
    } else if (settings.whenToUnlock?.contains("3") && (fireMedical?.currentValue("smokeSensor") == "detected") && (lock1?.currentValue("lock") == "locked")) {
        ifDebug("lock1LockHandler: Lock was locked while the Fire/Medical Sensor detected smoke. Performing a fast unlock.")
        lock1.unlock()
        countUnlock = maxRetriesUnlock
        unschedule(unlockDoor)
        def delayUnlock = 1
        runIn(delayUnlock, unlockDoor)
    } else if ((lock1?.currentValue("lock") == "locked") && (contact?.currentValue("contact") == "closed") || (contact == null)) {
        ifDebug("Cancelling previous lock task...")
        unschedule(lockDoor)                  // ...we don't need to lock it later.
        state.status = "(Locked)"
    } else {
        countLocked = maxRetriesLock
        state.status = "(Unlocked)"
        if (minSecLock) {
            def delayLock = durationLock
            ifDebug("Re-arming lock in in ${durationLock} second(s)")
            runIn(delayLock, lockDoor)
        } else {
            def delayLock = (durationLock * 60)
            ifDebug("Re-arming lock in in ${durationLock} minute(s)")
            runIn(delayLock, lockDoor)
        }
    }
updateLabel()
}

def lock1UnlockHandler(evt) {
    // Device Status
    if (evt.value != null) {ifTrace("lock1UnlockHandler: ${evt.value}")
        state.lock1LockStatus = "[${evt.value}]"
    } else if (evt.value == "unlocked") {state.lock1LockStatus = "[${evt.value}]"
    } else if (lock1?.currentValue("lock") != null) {state.lock1LockStatus = "[${lock1.currentValue("lock")}]"
    } else if (lock1?.latestValue("lock") != null) {state.lock1Lock1Status = "[${lock1.latestValue("lock")}]"
    } else {state.lock1LockStatus = " "}
    
    // Log Manual Unlocking
    ifDebug("${evt.name} : ${evt.descriptionText}")
    if (evt.type == 'physical' && evt.descriptionText.endsWith('unlocked by keypad')) {/* Do something here */
	} else if (evt.type == 'physical' && evt.descriptionText.contains('unlocked by code')) {/* Do something here */
	} else if (evt.type == 'physical' && evt.descriptionText.endsWith('unlocked by manual')) {/* Do something here */}
    
    updateLabel()
    
    // Device Handler Action
    if ((getAllOk() == false) || (state?.pausedOrDisabled == true)) {
        ifTrace("lock1UnlockHandler: Application is paused or disabled.")
    } else if (settings.whenToUnlock?.contains("3") && (fireMedical?.currentValue("smokeSensor") == "detected") && (lock1?.currentValue("lock") == "locked")) {
        lock1.unlock()
        countUnlock = maxRetriesUnlock
        unschedule(unlockDoor)
        def delayUnlock = 1
        runIn(delayUnlock, unlockDoor)
    } else if (settings.whenToUnlock?.contains("3") && (fireMedical?.currentValue("smokeSensor") == "detected")) {
        // Keeping door unlocked until the sensor clears.
    } else {
        countLocked = maxRetriesLock
        state.status = "(Unlocked)"
        if (minSecLock) {
            def delayLock = durationLock
            ifDebug("Re-arming lock in in ${durationLock} second(s)")
            runIn(delayLock, lockDoor)
        } else {
            def delayLock = (durationLock * 60)
            ifDebug("Re-arming lock in in ${durationLock} minute(s)")
            runIn(delayLock, lockDoor)
        }
    }
    updateLabel()
}

def lock1BatteryHandler(evt) {
    // Device Status
    if (evt.value != null) {ifTrace("lock1BatteryHandler: ${evt.value}")
        state.lock1BatteryStatus = "Battery: [${evt.value}%]"
    } else if (lock1?.currentValue("battery") != null) {state.lock1BatteryStatus = "Battery: [${lock1.currentValue("battery")}%]"
    } else if (lock1?.latestValue("battery") != null) {state.lock1BatteryStatus = "Battery: [${lock1.latestValue("battery")}%]"
    } else {state.lock1BatteryStatus = " "
        if (evt.value != null) {log.warn "${evt.value}"}
    }
    if ((evt.value != null) && lowBatteryDevicesToNotifyFor?.contains("1") && (notifyOnLowBattery == true) && (lowBatteryAlertThreshold != null) && (lowBatteryAlertThreshold < 0) && (evt.value < ${lowBatteryAlertThreshold})) {
        lowBatteryDevicesToNotifyFor.deviceNotification("${lock1} battery is ${evt.value}.")}
    updateLabel()
    
    // Device Handler Action

}

def contactContactHandler(evt) {
    // Device Status
    if (evt.value != null) {ifTrace("contactContactHandler: ${evt.value}")
        state.contactContactStatus = "[${evt.value}]"
    } else if (evt.value == "open") {state.contactContactStatus = "[${evt.value}]"
    } else if (evt.value == "closed") {state.contactContactStatus = "[${evt.value}]"
    } else if (contact?.currentValue("contact") != null) {state.contactContactStatus = "[${contact.currentValue("contact")}]"
    } else if (contact?.latestValue("contact") != null) {state.contactContactStatus = "[${contact.latestValue("contact")}]"
    } else {state.contactContactStatus = " "
        if (evt.value != null) {log.warn "${evt.value}"}
    }
    updateLabel()
    
    // Device Handler Action
    if ((getAllOk() == false) || (state?.pausedOrDisabled == true)) {
        ifTrace("doorHandler: Application is paused or disabled.")
    } else if (!settings.whenToUnlock?.contains("6") && settings.whenToUnlock?.contains("3") && (fireMedical?.currentValue("smokeSensor") == "detected") && (lock1?.currentValue("lock") == "locked")) {
        // Performing fast unlock if locked and unlocking door if locked
        countUnlock = maxRetriesUnlock
        lock1.unlock()
        unschedule(checkUnlockedStatus)
        def delayUnlock = 1
        runIn(delayUnlock, checkUnlockedStatus)
        lock1.refresh()
    } else if (!settings.whenToUnlock?.contains("6") && settings.whenToUnlock?.contains("3") && (fireMedical?.currentValue("smokeSensor") == "detected")) {
        // Keeping door unlocked until the sensor clears.
    } else if (!settings.whenToUnlock?.contains("6") && settings.whenToUnlock?.contains("5") && (contact.currentValue("contact") == "open") && (lock1.currentValue("lock") == "locked")) {
        // Unlock and refresh if known state is out of sync with reality.
        ifTrace("doorHandler: Door was opend while lock was locked. Performing a fast unlock in case and device refresh to get current state.")
        countUnlock = maxRetriesUnlock
        lock1.unlock()
        unschedule(checkUnlockedStatus)
        def delayUnlock = 1
        runIn(delayUnlock, checkUnlockedStatus)
        lock1.refresh()
    } else if ((contact?.currentValue("contact") == "closed") && (lock1?.currentValue("lock") == "unlocked")) {
        ifDebug("Door closed, locking door.")
        countLock = maxRetriesLock
        unschedule(lockDoor)
        if (minSecLock) {
            def delayLock = durationLock
            runIn(delayLock, lockDoor)
        } else {
            def delayLock = (durationLock * 60)
                runIn(delayLock, lockDoor)
        }
        
    }
    updateLabel()
}

def contactBatteryHandler(evt) {
    // Device Status
    if (evt.value != null) {ifTrace("contactBatterytHandler: ${evt.value}")
        state.contactBatteryStatus = "Battery: [${evt.value}]"
    } else if (contact?.currentValue("battery") != null) {state.contactBatteryStatus = "Battery: [${contact.currentValue("battery")}%]"
    } else if (contact?.latestValue("battery") != null) {state.contactBatteryStatus = "Battery: [${contact.latestValue("battery")}%]"
    } else {state.contactBatteryStatus = " "
        if (evt.value != null) {log.warn "${evt.value}"}
    }
    if ((evt.value != null) && lowBatteryDevicesToNotifyFor.contains("2") && (notifyOnLowBattery == true) && (lowBatteryAlertThreshold != null) && (lowBatteryAlertThreshold < 0) && (evt.value < ${lowBatteryAlertThreshold})) {
        lowBatteryDevicesToNotifyFor.deviceNotification("${contact} battery is ${evt.value}.")}
    updateLabel()
    
    // Device Handler Action
    
}

def unlockPresenceHandler(evt) {
    // Device Status
    if (evt.value != null) {ifTrace("unlockPresenceHandler: ${evt.value}")
        state.unlockPresenceStatus = "[${evt.value}]"
    } else if (evt.value == "arrived") {state.unlockPresenceStatus = "[${evt.value}]"
    } else if (evt.value == "departed") {state.unlockPresenceStatus = "[${evt.value}]"
    } else if (unlockPresence?.currentValue("presence") != null) {state.unlockPresenceStatus = "[${unlockPresence.currentValue("presence")}]"
    } else if (unlockPresence?.latestValue("presence") != null) {state.unlockPresenceStatus = "[${unlockPresence.latestValue("presence")}]"                                                                                                      
    } else {(state?.unlockPresenceStatus = " ")}
    updateLabel()
    
    // Device Handler Action
    if ((getAllOk() == false) || (state?.pausedOrDisabled == true)) {ifTrace("unlockPresenceHanlder: Application is paused or disabled.")
    } else if (!settings.whenToUnlock?.contains("6") && settings.whenToUnlock?.contains("2") && (unlockPresence?.currentValue("presence") == "present")) {unlockDoor()}
}

def unlockPresenceBatteryHandler(evt) {
    // Device Status
    if (evt.value != null) {ifTrace("unlockPresenceBatteryHandler: ${evt.value}")
        state.unlockPresenceBatteryStatus = "Battery: [${evt.value}]"
    } else if (unlockPresence?.currentValue("battery") != null) {state.unlockPresenceBatteryStatus = "Battery: [${unlockPresence.currentValue("battery")}]"
    } else if (unlockPresence?.latestValue("battery") != null) {state.unlockPresenceBatteryStatus = "Battery: [${unlockPresence.latestValue("battery")}]"                                                                                                      
    } else {(state.unlockPresenceBatteryStatus = " ")}
    updateLabel()
    
    // Device Handler Action
    
}

def fireMedicalHandler(evt) {
    // Device Status
    if (evt.value != null) {ifTrace("fireMedicalHandler: ${evt.value}")
        state.fireMedicalStatus = "[${fireMedical.currentValue("contact")}]"
    } else if (fireMedical?.currentValue("contact") != null) {state.fireMedicalStatus = "[${fireMedical.currentValue("contact")}]"
    } else if (fireMedical?.latestValue("contact") != null) {state.fireMedicalStatus = "[${fireMedical.latestValue("contact")}]"
    } else {(state.fireMedicalStatus = " ")}
    updateLabel()
    
    // Device Handler Action
    if (evt.value != null) {ifTrace("fireMedicalHandler: ${evt.value}")}
    updateLabel()
    if ((getAllOk() == false) || (state?.pausedOrDisabled == true)) {ifTrace("fireMedicalHandler: Application is paused or disabled.")
    } else if (settings.whenToUnlock?.contains("3") && !settings.whenToUnlock?.contains("6") && (fireMedical?.currentValue("switch") == "on")) {
        ifDebug("fireMedicalHandler:  Fast unlocking because of an emergency.")
        countUnlock = maxRetriesUnlock
        unlockDoor()
        unschedule(unlockDoor)
        def delayUnlock = 1
        runIn(delayUnlock, unlockDoor)
    } else if (lock1?.currentValue("lock") == "unlocked") {
        ifTrace("The door is open and the lock is unlocked. Nothing to do.")
    }
updateLabel()
}


def fireMedicalBatteryHandler(evt) {
    // Device Status
    if (evt.value != null) {ifTrace("fireMedicalBatteryHandler: ${evt.value}")
        state.fireMedicalBatteryStatus = "Battery: [${fireMedical.currentValue("battery")}]"
    } else if ((fireMedical?.currentValue("battery") != null)) {state.fireMedicalBatteryStatus = "Battery: [${fireMedical.currentValue("battery")}]"
    } else if (fireMedical?.latestValue("battery") != null) {state.fireMedicalBatteryStatus = "Battery: [${fireMedical.latestValue("battery")}]"
    } else {(state.fireMedicalBatteryStatus = " ")}
    if ((evt.value != null) && lowBatteryDevicesToNotifyFor.contains("3") && (notifyOnLowBattery == true) && (lowBatteryAlertThreshold != null) && (lowBatteryAlertThreshold < 0) && (evt.value < ${lowBatteryAlertThreshold})) {lowBatteryDevicesToNotifyFor.deviceNotification("${contact} battery is ${evt.value}.")}
    updateLabel()
    
    // Device Handler Action
    
}

def deviceActivationSwitchHandler(evt) {
    // Device Status
    if (evt.value != null) {ifTrace("deviceActivationSwitchHandler: ${evt.value}")
        state.deviceActivationSwitchStatus = "[${deviceActivationSwitch.currentValue("contact")}]"
    } else if (deviceActivationSwitch?.currentValue("switch") != null) {state.deviceActivationSwitchStatus = "[${deviceActivationSwitch.currentValue("switch")}]"
    } else if (deviceActivationSwitch?.latestValue("switch") != null) {state.deviceActivationSwitchStatus = "[${deviceActivationSwitch.latestValue("switch")}]"
    } else {(state.deviceActivationSwitchStatus = " ")}
    updateLabel()

    // Device Handler Action
    if ((getAllOk() == false) || (state?.pausedOrDisabled == true)) {
    ifTrace("deviceActivationSwitchHandler: Application is paused or disabled.")
    } else if (deviceActivationSwitch) {
        deviceActivationSwitch.each { it ->
            state.deviceActivationSwitchState = it.currentValue("switch")
        }
        if (state.deviceActivationSwitchState == "on") {
            if ((deviceActivationToggle == true) && (!settings.whenToUnlock?.contains("6") == true) && (getHSMUnlockOk() == true)) {
                ifDebug("deviceActivationSwitchHandler: Unlocking the door now")
                countUnlock = maxRetriesUnlock
                state.status = "(Unlocked)"
                lock1.unlock()
                if (minSecUnlock) {
                    def delayUnlock = durationUnlock
                    runIn(delayUnlock, unlockDoor)
                } else {
                    def delayUnlock = (durationUnlock * 60)
                    runIn(delayUnlock, unlockDoor)
                }
            } else if (getHSMLockOk() == true) {
                ifDebug("deviceActivationSwitchHandler: Locking the door now")
                countLock = maxRetriesLock
                state.status = "(Locked)"
                lock1.lock()
                if (minSecLock) {
                    def delayLock = durationLock
                    runIn(delayLock, lockDoor)
                } else {
                    def delayLock = durationLock * 60
                    runIn(delayLock, lockDoor)
                }
            }
        } else if ((state.deviceActivationSwitchState == "off") && (!settings.whenToUnlock?.contains("6") == true)) {
            if ((deviceActivationToggle == true) && (getHSMLockOk() == true)){
                countLock = maxRetriesLock
                state.status = "(Locked)"
                lock1.lock()
                if (minSecLock) {
                    def delayLock = durationLock
                    runIn(delayLock, lockDoor)
                } else {
                    def delayLock = durationLock * 60
                    runIn(delayLock, lockDoor)
                }
            } else if (getHSMUnlockOk() == true) {
                ifDebug("deviceActivationSwitchHandler: Unlocking the door now")
                countUnlock = maxRetriesUnlock
                state.status = "(Unlocked)"
                lock1.unlock()
                if (minSecUnlock) {
                    def delayUnlock = durationUnlock
                    runIn(delayUnlock, unlockDoor)
                } else {
                    def delayUnlock = (durationUnlock * 60)
                    runIn(delayUnlock, unlockDoor)
                }
            }
        }
    }
    updateLabel()
}

def deviceActivationToggleHandler(evt) {
    // Toggle Status
    if (evt.value != null) {ifTrace("Action Toggled: ${evt.value}")}
    ifTrace("Action Toggled deviceActivationToggle = ${deviceActivationToggle}")
}

def disabledHandler(evt) {
    // Device Status
    if (evt?.value != null) {ifTrace("disabledHandler: ${evt.value}")}
    if (disabledSwitch?.currentValue("switch") != null) {state.disabledSwitchStatus = "[${disabledSwitch.currentValue("switch")}]"
    } else if (disabledSwitch?.latestValue("switch") != null) {state.disabledSwitchStatus = "[${disabledSwitch.latestValue("switch")}]"
    } else {(state.disabledSwitchStatus = " ")}
    updateLabel()
    
    // Device Handler Action
    if (getAllOk() == false) {ifTrace("TurnOffFanSwitchManual: getAllOk = ${getAllOk()} state?.pausedOrDisabled = ${state?.pausedOrDisabled}")
        } else if (disabledSwitch) {
            disabledSwitch.each { it ->
            state.disabledSwitchState = it.currentValue("switch")
            if (state.disabledSwitchState == "on") {
                ifTrace("disabledHandler: Disable switch turned on")
                state.disabled = false
                if (state?.paused == true) {
                    state.status = "(Paused)"
                    state.pausedOrDisabled = true
                } else {
                    state.paused = false
                    state.disabled = false
                    state.pausedOrDisabled = false
                    if ((lock1?.currentValue("lock") == "unlocked") && ((contact?.currentValue("contact") == "closed") || (contact == null))) {
                        if (minSecLock) {
                            def delayLock = durationLock
                            ifDebug("disabledHandler: App was enabled or unpaused and lock was unlocked. Locking door.")
                            runIn(delayLock, lockDoor)
                        } else {
                        def delayLock = durationLock * 60
                            ifDebug("disabledHandler: App was enabled or unpaused and lock was unlocked. Locking door.")
                            runIn(delayLock, lockDoor)
                        }
                    }
                }
            } else if (state.disabledSwitchState == "off") {
                state.pauseButtonName = "Disabled by Switch"
                state.status = "(Disabled)"
                ifTrace("disabledHandler: (Disabled)")
                state.disabled = true
            }
        }
    }
    updateLabel()
}
    
def enableHSMSwitchHandler(evt) {
    // Device Status
    if (evt?.value != null) {ifTrace("enableHSMSwitchHandler: ${evt.value}")}
    if (enableHSMSwitch?.currentValue("switch") != null) {state.enableHSMSwitchStatus = "[${enableHSMSwitch.currentValue("switch")}]"
    } else if (enableHSMSwitch?.latestValue("switch") != null) {state.enableHSMSwitchStatus = "[${enableHSMSwitch.latestValue("switch")}]"
    } else {(state.enableHSMSwitchStatus = " ")}
    updateLabel()
    
    // Device Handler Action
    
}

// Application Functions
def lockDoor() {
    ifTrace("lockDoor")
    if ((getAllOk() == false) || (state?.pausedOrDisabled == true)) {ifTrace("lockDoor: Application is paused or disabled.")
    } else if (getHSMLockOk() == false) {ifDebug("Unable to lock the door. HSM Status is ${location.HSMStatus}.")
    } else {
        updateLabel()
        ifTrace("lockDoor: contact = ${contact}")
        if ((contact?.currentValue("contact") == "closed") || (contact == null)) {
            ifDebug("Door is closed, locking door.")
            lock1.lock()
            unschedule(checkLockedStatus)
            countLock = maxRetriesLock
            if (minSecLock) {
                def delayLock = durationLock
                runIn(delayLock, checkLockedStatus)
            } else {
	            def delayLock = durationLock * 60
                runIn(delayLock, checkLockedStatus)
                }
        } else if (!settings.whenToUnlock?.contains("6") && settings.whenToUnlock?.contains("1") && (contact?.currentValue("contact") == "open") && (lock1?.currentValue("lock") == "locked")) {
            ifTrace("lockDoor: Lock was locked while Door was open. Performing a fast unlock to prevent hitting the bolt against the frame.")
            countUnlock = maxRetriesUnlock
            lock1.unlock()
            unschedule(unlockDoor)
            if (minSecUnlocked) {
                def delayUnlock = 1
                ifTrace("lockDoor: Performing a fast unlock to prevent hitting the bolt against the frame.")
                runIn(delayUnlock, checkUnlockedStatus)
                lock1.refresh()
            }
        } else {
            ifTrace("lockDoor: Unhandled exception")
        }
    updateLabel()
    }
}

def unlockDoor() {
    ifTrace("unlockDoor")
    if ((getAllOk() == false) || (state?.pausedOrDisabled == true) || settings.whenToUnlock?.contains("6")) {ifTrace("lockDoor: Application is paused or disabled.")
    } else if (getHSMUnlockOk() == false) {ifDebug("Unable to unlock the door. HSM Status is ${location.HSMStatus}.")
    } else {
        updateLabel()
        ifTrace("unlockDoor: Unlocking door.")
        lock1.unlock()
        countUnlock = maxRetriesUnlock
        unschedule(unlockDoor)
        if (minSecUnlock) {
            def delayUnlock = durationUnlock
            runIn(delayUnlock, checkUnlockedStatus)
        } else {
	        def delayUnlock = (durationUnlock * 60)
            runIn(delayUnlock, checkUnlockedStatus)
        }
    updateLabel()
    }
}

def checkLockedStatus() {
    ifTrace("checkLockedStatus")
    if (getHSMLockOk() == false) {ifDebug("Unable to unlock the door. HSM Status is ${location.HSMStatus}.")
    } else if (lock1?.currentValue("lock") == "locked") {
        state.status = "(Locked)"
        ifTrace("checkLockedStatus: The lock was locked successfully")
        if (((enableHSMToggle == true) || (enableHSMSwitch?.currentValue("switch") == "on")) && (hsmCommandsLock != null)) {sendLocationEvent(name: "hsmSetArm", value: "${hsmCommandsLock}")}
        countLock = maxRetriesLock
    } else {
        state.status = "(Unlocked)"
        lock1.lock()
        countLock = (countLock - 1)
        if (countLock > -1) {
            runIn(delayBetweenRetriesLock, retryLockingCommand)
        } else {
            ifInfo("Maximum retries exceeded. Giving up on locking door.")
            countLock = maxRetriesLock
        }
    }
    updateLabel()
}

def checkUnlockedStatus() {
    ifTrace("checkUnlockedStatus")
    if (getHSMLockOk() == false) {ifDebug("Unable to lock the door. HSM Status is ${location.HSMStatus}.")
    } else if (lock1?.currentValue("lock") == "unlocked") {
        state.status = "(Unlocked)"
        ifTrace("checkUnlockedStatus: The lock was unlocked successfully")
        if (((enableHSMToggle == true) || (enableHSMSwitch?.currentValue("switch") == "on")) && (hsmCommandsUnlock != null)) {sendLocationEvent(name: "hsmSetArm", value: "${hsmCommandsUnlock}")}
        countUnlock = maxRetriesUnlock
    } else if (!settings.whenToUnlock?.contains("6")) {
        state.status = "(Locked)"
        lock1.unlock()
        countUnlock = (countUnlock - 1)
        if (countUnlock > -1) {
            checkLockedStatus
            runIn(delayBetweenRetriesUnlock, retryUnlockingCommand)
        } else {
            ifInfo("Maximum retries exceeded. Giving up on unlocking door.")
            countUnlock = maxRetriesUnlock
        }
    }
    updateLabel()
}

def retryLockingCommand() {
    ifTrace("retryLockingCommand")
    if (lock1?.currentValue("lock") == "locked") {
        state.status = "(Locked)"
        ifTrace("retryLockingCommand: The lock was locked successfully")
        if (((enableHSMToggle == true) || (enableHSMSwitch?.currentValue("switch") == "on")) && (hsmCommandsLock != null)) {sendLocationEvent(name: "hsmSetArm", value: "${hsmCommandsLock}")}
        countLock = maxRetriesLock
    } else if ((retryLock == true) && (lock1.currentValue("lock") != "locked")) {
        state.status = "(Unlocked)"
        lock1.lock()
        if (countUnlock > -1) {runIn(delayBetweenRetriesLock, retryLockingCommand)}
    } else {
        ifTrace("retryLockingCommand: retryLock = ${retryLock} - Doing nothing.")
    }
}

def retryUnlockingCommand() {
    ifTrace("retryUnlockingCommand")
    if (lock1?.currentValue("lock") == "unlocked") {
        state.status = "(Unlocked)"
        ifTrace("retryUnlockingCommand: The lock was unlocked successfully")
        if (((enableHSMToggle == true) || (enableHSMSwitch?.currentValue("switch") == "on")) && (hsmCommandsUnlock != null)) {sendLocationEvent(name: "hsmSetArm", value: "${hsmCommandsUnlock}")}
        countUnlock = maxRetriesUnlock
    } else if ((retryUnlock == true) && (lock1?.currentValue("lock") != "unlocked") && !settings.whenToUnlock?.contains("6")) {
        state.status = "(Locked)"
        lock1.unlock()
        countUnlock = (countUnlock - 1)
        if (countUnlock > -1) {runIn(delayBetweenRetriesUnlock, retryUnlockingCommand)}
    } else {
        ifTrace("retryUnlockingCommand: retryUnlock = ${retryUnlock} - Doing nothing.")
    }
}
    
//Label Updates
void updateLabel() {
    unschedule(updateLabel)
    runIn(1800, updateLabel)
    ifTrace("updateLabel")
//    getVariableInfo()
    if (getAllOk() == false) {
        if ((state?.paused == true) || (state?.disabled == true)) {state.pausedOrDisabled = true} else {state.pausedOrDisabled = false}
        state.status = "(Disabled by Time, Day, or Mode)"
        appStatus = "<span style=color:brown>(Disabled by Time, Day, or Mode)</span>"
    } else if (state?.disabled == true) {
        state.status = "(Disabled)"
        appStatus = "<span style=color:red>(Disabled)</span>"
    } else if (state?.paused == true) {
        state.status = "(Paused)"
        appStatus = "<span style=color:red>(Paused)</span>"
    } else if (lock1?.currentValue("lock") == "locked") {
        state.status = "(Locked)"
        appStatus = "<span style=color:green>(Locked)</span>"
    } else if (lock1?.currentValue("lock") == "unlocked") {
        state.status = "(Unlocked)"
        appStatus = "<span style=color:orange>(Unlocked)</span>"
    } else {
        initialize()
        state.pausedOrDisabled = false
        state.status = " "
        appStatus = "<span style=color:white> </span>"
    }
    if ((state?.paused == true) || (state?.disabled == true)) {state.pausedOrDisabled = true} else {state.pausedOrDisabled = false}
    app.updateLabel("${state.thisName} ${appStatus}")
}


//Enable, Resume, Pause button
def appButtonHandler(btn) {
    ifTrace("appButtonHandler")
    if (btn == "Disabled by Switch") {
        state.disabled = false
        subscribe(disabledSwitch, "switch", disabledHandler)
        subscribe(lock1, "lock.locked", lock1LockHandler)
        subscribe(lock1, "lock.unlocked", lock1UnlockHandler)
        subscribe(lock1, "battery", lock1BatteryHandler)
        subscribe(contact, "contact", contactContactHandler)
        subscribe(contact, "battery", contactBatteryHandler)
    } else if (btn == "Resume") {
        state.disabled = false
        state.paused = !state.paused
        initialize()
    } else if (btn == "Pause") {
        state.paused = !state.paused
        if (state?.paused) {
            unschedule()
            unsubscribe()
            subscribe(disabledSwitch, "switch", disabledHandler)
            subscribe(lock1, "lock.locked", lock1LockHandler)
            subscribe(lock1, "lock.unlocked", lock1UnlockHandler)
            subscribe(lock1, "battery", lock1BatteryHandler)
            subscribe(contact, "contact", contactContactHandler)
            subscribe(contact, "battery", contactBatteryHandler)
        } else {
            initialize()
            state.pausedOrDisabled = false
            if (lock1?.currentValue("lock") == "unlocked") {
                ifTrace("appButtonHandler: App was enabled or unpaused and lock was unlocked. Locking the door.")
                lockDoor()
            }
        }
    }
    updateLabel()
}

def setPauseButtonName() {
    if (state?.disabled == true) {
        state.pauseButtonName = "Disabled by Switch"
        unsubscribe()
        unschedule()
        subscribe(disabledSwitch, "switch", disabledHandler)
        subscribe(lock1, "lock.locked", lock1LockHandler)
        subscribe(lock1, "lock.unlocked", lock1UnlockHandler)
        subscribe(lock1, "battery", lock1BatteryHandler)
        subscribe(contact, "contact", contactContactHandler)
        subscribe(contact, "battery", contactBatteryHandler)
        updateLabel()
    } else if (state?.paused == true) {
        state.pauseButtonName = "Resume"
        updated()
        unschedule()
    } else {
        state.pauseButtonName = "Pause"
        updated()
    }
}

// Application Page settings
def sendHsmCommandsLock() {sendLocationEvent(name: "hsmSetArm", value: "${hsmCommandsLock.collect { it.keySet()[0] }}")}    //Send HSM Commands When Locking
def sendHsmCommandsUnlock() {sendLocationEvent(name: "hsmSetArm", value: "${hsmCommandsLock.collect { it.keySet()[0] }}")}    //Send HSM Commands When Unlocking
private hideLockOptionsSection() {(minSecLock || durationLock || retryLock || maxRetriesLock || delayBetweenRetriesLock) ? false : true}
private hideUnlockOptionsSection() {(minSecUnlock || durationUnlock || retryUnlock || maxRetriesUnlock || delayBetweenRetriesUnlock) ? false : true}
private hideHSMSection() {(whenToLockHSM || whenToUnlockHSM || isHSM || hsmLogLevel) ? false : true}
private hideNotificationSection() {(notifyOnLowBattery || lowBatteryNotificationDevices || lowBatteryDevicesToNotifyFor || lowBatteryAlertThreshold || notifyOnFailure || failureNotificationDevices || failureNotifications) ? false : true}
private hideLoggingSection() {(isInfo || isDebug || isTrace || ifLevel) ? false : true}
private hideOptionsSection() {(starting || ending || days || modes || manualCount) ? false : true}
def getAllOk() {if ((modeOk && daysOk && timeOk) == true) {return true} else {return false}}

private getHSMLockOk() {
    def result = (!whenToLockHSM || whenToUnlockHSM.contains(location.hsmStatus))
    result
}

private getHSMUnlockOk() {
    def result = (!whenToUnlockHSM || whenToUnlockHSM.contains(location.hsmStatus))
    result
}

private getModeOk() {
	def result = (!modes || modes.contains(location.mode))
	result
}

private getDaysOk() {
	def result = true
	if (days) {
		def df = new java.text.SimpleDateFormat("EEEE")
		if (location.timeZone) {
			df.setTimeZone(location.timeZone)
		}
		else {
			df.setTimeZone(TimeZone.getTimeZone("America/New_York"))
		}
		def day = df.format(new Date())
		result = days.contains(day)
	}
	result
}

private getTimeOk() {
	def result = true
	if ((starting != null) && (ending != null)) {
		def currTime = now()
		def start = timeToday(starting).time
		def stop = timeToday(ending).time
		result = start < stop ? currTime >= start && currTime <= stop : currTime <= stop || currTime >= start
	}
	result
}

private hhmm(time, fmt = "h:mm a") {
	def t = timeToday(time, location.timeZone)
	def f = new java.text.SimpleDateFormat(fmt)
	f.setTimeZone(location.timeZone ?: timeZone(time))
	f.format(t)
}

private timeIntervalLabel() {(starting && ending) ? hhmm(starting) + "-" + hhmm(ending, "h:mm a z") : ""}

def turnOffLoggingTogglesIn30() {
    if (!isInfo) {app.updateSetting("isInfo",[value:"false",type:"bool"])}
    if (!isDebug) {app.updateSetting("isDebug",[value:"false",type:"bool"])}
    if (!isTrace) {app.updateSetting("isTrace",[value:"false",type:"bool"])}
    if (isTrace == true) {runIn(1800, traceOff)}
    if (isDebug == true) {runIn(1800, debugOff)}
    if (isTrace == true) {runIn(1800, traceOff)}
}

def infoOff() {
    log.info "${state.thisName}: Info logging disabled."
    app.updateSetting("isInfo",[value:"false",type:"bool"])
}

def debugOff() {
    log.info "${state.thisName}: Debug logging disabled."
    app.updateSetting("isDebug",[value:"false",type:"bool"])
}

def traceOff() {
    log.trace "${state.thisName}: Trace logging disabled."
    app.updateSetting("isTrace",[value:"false",type:"bool"])
}

def hsmOff() {
    log.info "${state.thisName}: HSM logging disabled."
    app.updateSetting("isHSM",[value:"false",type:"bool"])
}

def disableInfoIn30() {
    if (isInfo == true) {
        runIn(1800, infoOff)
        log.info "Info logging disabling in 30 minutes."
    }
}

def disableDebugIn30() {
    if (isDebug == true) {
        runIn(1800, debugOff)
        log.debug "Debug logging disabling in 30 minutes."
    }
}

def disableTraceIn30() {
    if (isTrace == true) {
        runIn(1800, traceOff)
        log.trace "Trace logging disabling in 30 minutes."
    }
}

def disableHSMIn30() {
    if (isHSM == true) {
        runIn(1800, hsmOff)
        log.info "HSM logging disabling in 30 minutes."
    }
}

def ifWarn(msg) {log.warn "${state.thisName}: ${msg}"}

def ifInfo(msg) {
    if (!settings.ifLevel?.contains("1") && (isInfo != true)) {return}//bail
    else if (settings.ifLevel?.contains("1") || (isInfo == true)) {log.info "${state.thisName}: ${msg}"}
}

def ifDebug(msg) {
    if (!settings.ifLevel?.contains("2") && (isDebug != true)) {return}//bail
    else if (settings.ifLevel?.contains("2") || (isDebug == true)) {log.debug "${state.thisName}: ${msg}"}
}

def ifTrace(msg) {
    if (!settings.ifLevel?.contains("3") && (isTrace != true)) {return}//bail
    else if (settings.ifLevel?.contains("3") || (isTrace != true)) {log.trace "${state.thisName}: ${msg}"}
}

def getVariableInfo() {
    if (variableInfo == true) {
        log.info "state.thisName = ${state.thisName}"
        log.info "state.status = ${state.status}"
        log.info "getAllOk = ${getAllOk()}"
        log.info "getModeOk = ${getModeOk()}"
        log.info "getDaysOk = ${getDaysOk()}"
        log.info "getTimeOk = ${getTimeOk()}"
        log.info "getHSMLockOk = ${getHSMLockOk()}"
        log.info "getHSMUnlockOk = ${getHSMUnlockOk()}"
        log.info "days = ${days}"
        log.info "daysOptions = ${daysOptions}"
        log.info "pausedOrDisabled = ${state.pausedOrDisabled}"
        log.info "state.disabled = ${state.disabled}"
        log.info "state.paused = ${state.paused}"
        log.info "isInfo = ${isInfo}"
        log.info "isDebug = ${isDebug}"
        log.info "isTrace = ${isTrace}"
        log.info "isHSM = ${isHSM}"
        log.info "settings.ifLevel?.contains(1) = ${settings.ifLevel.contains("1")}"
        log.info "settings.ifLevel?.contains(2) = ${settings.ifLevel.contains("2")}"
        log.info "settings.ifLevel?.contains(3) = ${settings.ifLevel.contains("3")}"
        log.info "settings.whenToUnlock?.contains(1) = ${(settings.whenToUnlock?.contains("1") == true)}"
        log.info "settings.whenToUnlock?.contains(2) = ${(settings.whenToUnlock?.contains("2") == true)}"
        log.info "settings.whenToUnlock?.contains(3) = ${(settings.whenToUnlock?.contains("3") == true)}"
        log.info "settings.whenToUnlock?.contains(4) = ${(settings.whenToUnlock?.contains("4") == true)}"
        log.info "settings.whenToUnlock?.contains(5) = ${(settings.whenToUnlock?.contains("5") == true)}"
        log.info "settings.whenToUnlock?.contains(6) = ${(settings.whenToUnlock?.contains("6") == true)}"
        log.info "delayBetweenRetriesLock = ${delayBetweenRetriesLock}"
        log.info "enableHSMToggle = ${enableHSMToggle}"
        log.info "hsmCommandsLock = ${hsmCommandsLock}"
        log.info "location.hsmStatus = ${location.hsmStatus}"
        log.info "location.hsmAlerts = ${location.hsmAlerts}"
        log.info "hsmLogLevel = ${hsmLogLevel}"
        log.info "state.lock1LockStatus = ${state.lock1LockStatus}"
        log.info "state.lock1BatteryStatus = ${state.lock1BatteryStatus}"
        log.info "state.contactContactStatus = ${state.contactContactStatus}"
        log.info "state.contactBatteryStatus = ${state.contactBatteryStatus}"
        log.info "state.unlockPresenceStatus = ${state.unlockPresenceStatus}"
        log.info "state.unlockPresenceBatteryStatus = ${state.unlockPresenceBatteryStatus}"
    }
}
