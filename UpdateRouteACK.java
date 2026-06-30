import javax.baja.collection.BITable;
import javax.baja.collection.Column;
import javax.baja.collection.TableCursor;
import javax.baja.naming.BOrd;
import javax.baja.sys.BBoolean;
import javax.baja.sys.BComponent;

/*
 * Execute this program to find all email:EmailRecipient components by BQL and
 * set their routeAcks baja:Boolean value to false.
 */

private static final String EMAIL_RECIPIENT_BQL =
    "station:|slot:/|bql:select * from email:EmailRecipient";
private static final String ROUTE_ACK_SLOT = "routeAcks";

public void onStart() throws Exception {
  // No startup work is required.
}

public void onExecute() throws Exception {
  updateRouteAck();
}

// Optional handler for a custom no-argument action named updateRouteACK.
public void onUpdateRouteACK() throws Exception {
  updateRouteAck();
}

private void updateRouteAck() {
  try {
    BITable table = (BITable) BOrd.make(EMAIL_RECIPIENT_BQL).resolve().get();
    TableCursor cursor = table.cursor();
    int found = 0;
    int updated = 0;
    int skipped = 0;

    while (cursor.next()) {
      found++;

      BComponent recipient = resolveRecipient(table, cursor);
      if (recipient == null) {
        skipped++;
        logWarning("Skipping EmailRecipient #" + found
            + " because the BQL row could not be resolved to a component: "
            + rowText(table, cursor));
        continue;
      }

      String routeAckSlot = routeAckSlotName(recipient);
      if (routeAckSlot == null) {
        skipped++;
        logWarning("Skipping " + recipient.getSlotPath()
            + " because routeAcks slot was not found");
        continue;
      }

      Object oldValue = recipient.get(routeAckSlot);
      if (oldValue instanceof BBoolean && !((BBoolean) oldValue).getBoolean()) {
        logInfo("No change needed for " + recipient.getSlotPath() + "."
            + routeAckSlot + " because it is already false");
        continue;
      }

      recipient.set(routeAckSlot, BBoolean.FALSE);
      updated++;
      logInfo("Set " + recipient.getSlotPath() + "."
          + routeAckSlot + " from " + safeText(oldValue) + " to false");
    }

    logInfo("Update completed. emailRecipientsFound=" + found
        + ", updated=" + updated
        + ", skipped=" + skipped);
  } catch (Exception e) {
    logWarning("Unable to update routeAcks by BQL: " + errorText(e));
  }
}

private BComponent resolveRecipient(BITable table, TableCursor cursor) {
  BComponent fromCursor = recipientFromCursor(cursor);
  if (fromCursor != null) {
    return fromCursor;
  }

  return recipientFromSlotPath(table, cursor);
}

private BComponent recipientFromCursor(TableCursor cursor) {
  String[] methodNames = new String[] {
      "get",
      "getObject",
      "row"
  };

  for (int i = 0; i < methodNames.length; i++) {
    try {
      java.lang.reflect.Method method = cursor.getClass().getMethod(methodNames[i], new Class[0]);
      Object value = method.invoke(cursor, new Object[0]);
      if (value instanceof BComponent) {
        return (BComponent) value;
      }
    } catch (Exception e) {
      // Try the next known cursor accessor.
    }
  }

  return null;
}

private BComponent recipientFromSlotPath(BITable table, TableCursor cursor) {
  String slotPath = cellText(table, cursor, "slotPath");
  if (slotPath.length() == 0) {
    return null;
  }

  try {
    Object resolved = BOrd.make(slotPathToOrd(slotPath)).resolve().get();
    if (resolved instanceof BComponent) {
      return (BComponent) resolved;
    }
  } catch (Exception e) {
    logWarning("Unable to resolve slotPath " + slotPath + ": " + errorText(e));
  }

  return null;
}

private String slotPathToOrd(String slotPath) {
  if (slotPath.indexOf("station:") == 0) {
    return slotPath;
  }
  if (slotPath.indexOf("slot:") == 0) {
    return "station:|" + slotPath;
  }
  if (slotPath.indexOf("/") == 0) {
    return "station:|slot:" + slotPath;
  }
  return "station:|slot:/" + slotPath;
}

private String routeAckSlotName(BComponent component) {
  if (hasProperty(component, ROUTE_ACK_SLOT)) {
    return ROUTE_ACK_SLOT;
  }

  return null;
}

private boolean hasProperty(BComponent component, String name) {
  try {
    return component.getProperty(name) != null;
  } catch (Exception e) {
    return false;
  }
}

private String cellText(BITable table, TableCursor cursor, String columnName) {
  try {
    Column column = table.getColumns().get(columnName);
    if (column == null) {
      return "";
    }
    return safeText(cursor.cell(column));
  } catch (Exception e) {
    return "";
  }
}

private String rowText(BITable table, TableCursor cursor) {
  String slotPath = cellText(table, cursor, "slotPath");
  String name = cellText(table, cursor, "name");
  String displayName = cellText(table, cursor, "displayName");

  StringBuffer out = new StringBuffer();
  if (slotPath.length() > 0) {
    appendField(out, "slotPath", slotPath);
  }
  if (name.length() > 0) {
    appendField(out, "name", name);
  }
  if (displayName.length() > 0) {
    appendField(out, "displayName", displayName);
  }

  return out.toString();
}

private void appendField(StringBuffer out, String name, String value) {
  if (out.length() > 0) {
    out.append(", ");
  }
  out.append(name);
  out.append("=");
  out.append(value);
}

private String safeText(Object value) {
  return value == null ? "" : value.toString();
}

private String errorText(Exception e) {
  String message = e.getMessage();
  return message == null || message.length() == 0
      ? e.getClass().getName()
      : message;
}

private void logInfo(String message) {
  System.out.println("[UpdateRouteACK] " + message);
}

private void logWarning(String message) {
  System.out.println("[UpdateRouteACK] WARNING: " + message);
}

public void onStop() throws Exception {
  // No shutdown work is required.
}
