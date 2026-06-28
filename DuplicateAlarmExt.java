/*
 * Niagara Program Object slots required:
 *
 *   parentFolderOrd  BOrd    - folder/component to search recursively
 *   newExtensionName String  - optional copied-extension name
 *
 * Critical, High, and Warning alarm extensions are copied and assigned the
 * source alarm class plus "_4Email".
 *
 * Run the Program Object's standard Execute action.
 * Optionally add a no-argument action named duplicateAlarmExts; its handler is
 * onDuplicateAlarmExts() below.
 */

private static final String DEFAULT_EXTENSION_SUFFIX = "Send_Email";
private static final String DEFAULT_ALARM_CLASS_SUFFIX = "_4Email";
private static final String ALARM_CLASS_CRITICAL = "Critical";
private static final String ALARM_CLASS_HIGH = "High";
private static final String ALARM_CLASS_WARNING = "Warning";

public void onStart() throws Exception {
    // No startup work is required.
}

public void onExecute() throws Exception {
    runDuplicateAlarmExts();
}

// Optional handler for a custom no-argument action named duplicateAlarmExts.
public void onDuplicateAlarmExts() throws Exception {
    runDuplicateAlarmExts();
}

private void runDuplicateAlarmExts() {

    final javax.baja.naming.BOrd parentOrd = getParentFolderOrd();
    if (parentOrd == null ||
            parentOrd.equals(javax.baja.naming.BOrd.DEFAULT) ||
            parentOrd.toString().trim().length() == 0) {
        logWarning("Parent Folder Ord is not configured");
        return;
    }

    try {
        final Object resolved = parentOrd.resolve().get();
        if (!(resolved instanceof javax.baja.sys.BComponent)) {
            logWarning("Parent Folder Ord did not resolve to a component: " + parentOrd);
            return;
        }

        // pointsChecked, alarmExtsFound, alarmExtsCreated, alarmExtsSkipped
        final int[] stats = new int[4];

        copyAlarmExts(
                (javax.baja.sys.BComponent) resolved,
                stats);

        logInfo(
                "Duplicate alarm ext completed. pointsChecked=" + stats[0] +
                        ", alarmExtsFound=" + stats[1] +
                        ", alarmExtsCreated=" + stats[2] +
                        ", alarmExtsSkipped=" + stats[3]);
    } catch (Exception e) {
        logWarning("Unable to duplicate alarm extensions: " + errorText(e));
    }
}

private void copyAlarmExts(
        javax.baja.sys.BComponent parent,
        int[] stats) {
    final javax.baja.sys.BComponent[] children = parent.getChildComponents();

    for (int i = 0; i < children.length; i++) {
        final javax.baja.sys.BComponent child = children[i];

        if (child instanceof javax.baja.control.BControlPoint) {
            copyPointAlarmExts(
                    (javax.baja.control.BControlPoint) child,
                    stats);
        } else {
            copyAlarmExts(child, stats);
        }
    }
}

private void copyPointAlarmExts(
        javax.baja.control.BControlPoint point,
        int[] stats) {
    stats[0]++;

    final javax.baja.sys.BComponent[] pointChildren = point.getChildComponents();
    for (int i = 0; i < pointChildren.length; i++) {
        if (!(pointChildren[i] instanceof javax.baja.alarm.ext.BAlarmSourceExt))
            continue;

        final javax.baja.alarm.ext.BAlarmSourceExt alarmExt = (javax.baja.alarm.ext.BAlarmSourceExt) pointChildren[i];

        stats[1]++;

        final String originalClass = trimToEmpty(alarmExt.getAlarmClass());
        if (originalClass.length() == 0) {
            stats[3]++;
            logWarning("Skipping " + point.getSlotPath() + "/" + alarmExt.getName()
                    + " because the source alarm class is empty");
            continue;
        }

        if (originalClass.endsWith(DEFAULT_ALARM_CLASS_SUFFIX)) {
            stats[3]++;
            logInfo("Skipping " + point.getSlotPath() + "/" + alarmExt.getName()
                    + " (already has target suffix: " + originalClass + ")");
            continue;
        }

        if (!isDuplicateAlarmClass(originalClass)) {
            stats[3]++;
            logInfo("Skipping " + point.getSlotPath() + "/" + alarmExt.getName()
                    + " because alarm class is not Critical, High, or Warning: " + originalClass);
            continue;
        }

        final String targetName = targetNameFor(alarmExt);
        if (hasProperty(point, targetName)) {
            stats[3]++;
            logInfo(
                    "Skipping " + point.getSlotPath() + "/" + targetName +
                            " because the slot already exists");
            continue;
        }

        try {
            final javax.baja.alarm.ext.BAlarmSourceExt copy = (javax.baja.alarm.ext.BAlarmSourceExt) alarmExt.newCopy();

            String alarmClass = originalClass + DEFAULT_ALARM_CLASS_SUFFIX;
            copy.setAlarmClass(alarmClass);
            point.add(targetName, copy);
            stats[2]++;
        } catch (Exception e) {
            stats[3]++;
            logWarning(
                    "Unable to copy alarm extension on " + point.getSlotPath() +
                            ": " + errorText(e));
        }
    }
}

private String targetNameFor(javax.baja.alarm.ext.BAlarmSourceExt alarmExt) {
    final String configuredName = trimToEmpty(getNewExtensionName());
    final String baseName = configuredName.length() > 0
            ? configuredName
            : alarmExt.getName() + DEFAULT_EXTENSION_SUFFIX;

    return javax.baja.naming.SlotPath.escape(baseName);
}

private boolean hasProperty(javax.baja.sys.BComponent component, String name) {
    try {
        return component.getProperty(name) != null;
    } catch (Exception e) {
        return false;
    }
}

private boolean isDuplicateAlarmClass(String alarmClass) {
    return ALARM_CLASS_CRITICAL.equalsIgnoreCase(alarmClass) ||
            ALARM_CLASS_HIGH.equalsIgnoreCase(alarmClass) ||
            ALARM_CLASS_WARNING.equalsIgnoreCase(alarmClass);
}

private String trimToEmpty(String value) {
    return value == null ? "" : value.trim();
}

private String errorText(Exception e) {
    final String message = e.getMessage();
    return message == null || message.length() == 0
            ? e.getClass().getName()
            : message;
}

private void logInfo(String message) {
    System.out.println("[DuplicateAlarmExt] " + message);
}

private void logWarning(String message) {
    System.out.println("[DuplicateAlarmExt] WARNING: " + message);
}

public void onStop() throws Exception {
    // No shutdown work is required.
}
