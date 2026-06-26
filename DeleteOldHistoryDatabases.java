public void onStart() throws Exception {
    // start up code here
}

public void onExecute() throws Exception {
    HistoryDatabaseConnection conn = null;
    BOrd deviceOrd = getDeviceOrd();
    boolean dryRun = getDryRun();

    int checkedCount = 0;
    int deletedCount = 0;
    int wouldDeleteCount = 0;
    int skippedRecentCount = 0;
    int skippedEmptyCount = 0;
    int errorCount = 0;
    java.util.List<String> deletedTableNames = new java.util.ArrayList<String>();
    java.util.List<String> wouldDeleteTableNames = new java.util.ArrayList<String>();

    try {
        BHistoryService historyService = (BHistoryService) Sys.getService(BHistoryService.TYPE);
        if (historyService == null) {
            System.out.println("HistoryService not available, aborting.");
            return;
        }
        BHistoryDatabase database = historyService.getDatabase();
        if (database == null) {
            System.out.println("HistoryDatabase not available, aborting.");
            return;
        }
        conn = database.getDbConnection(null);
        if (conn == null) {
            System.out.println("Failed to obtain database connection, aborting.");
            return;
        }

        Object target = deviceOrd.resolve().get();
        if (!(target instanceof BHistoryDevice)) {
            System.out.println("History device does not exist: " + deviceOrd);
            return;
        }

        BHistoryDevice device = (BHistoryDevice) target;
        BIHistory[] histories = database.listHistories(device);
        BAbsTime cutoff = BAbsTime.now().prevMonth();

        System.out.println("Start deleting old histories under: " + deviceOrd);
        System.out.println("Dry run: " + dryRun);
        System.out.println("Delete cutoff: " + cutoff);
        if (histories == null) {
            System.out.println("Histories found: 0 (null returned)");
            return;
        }
        System.out.println("Histories found: " + histories.length);

        for (int i = 0; i < histories.length; i++) {
            BIHistory history = histories[i];
            checkedCount++;

            BHistoryId historyId = history.getId();
            if (historyId == null) {
                skippedEmptyCount++;
                System.out.println("Skip history with null id");
                continue;
            }

            try {
                int recordCount = conn.getRecordCount(history);
                if (recordCount <= 0) {
                    skippedEmptyCount++;
                    System.out.println("Skip empty history: " + historyId);
                    continue;
                }

                BHistoryRecord lastRecord = conn.getLastRecord(history);
                BAbsTime lastTimestamp = lastRecord == null ? null : lastRecord.getTimestamp();

                if (lastTimestamp == null || lastTimestamp.isNull()) {
                    skippedEmptyCount++;
                    System.out.println("Skip history with no latest timestamp: " + historyId);
                    continue;
                }

                System.out.println("History: " + historyId
                        + " recordCount=" + recordCount
                        + " latestTimestamp=" + lastTimestamp);

                if (lastTimestamp.isBefore(cutoff)) {
                    if (dryRun) {
                        wouldDeleteCount++;
                        wouldDeleteTableNames.add(historyId.toString());
                        System.out.println("[DRY RUN] Would delete old history: " + historyId);
                    } else {
                        conn.deleteHistory(historyId);
                        deletedCount++;
                        deletedTableNames.add(historyId.toString());
                        System.out.println("Deleted old history: " + historyId);
                    }
                } else {
                    skippedRecentCount++;
                    System.out.println("Skip recent history: " + historyId);
                }
            } catch (Exception e) {
                errorCount++;
                System.out.println("Error processing history " + historyId + ": " + e);
                e.printStackTrace();
            }
        }

        System.out.println("Done.");
        System.out.println("Checked histories: " + checkedCount);
        System.out.println("Would delete histories: " + wouldDeleteCount);
        System.out.println("Deleted histories: " + deletedCount);
        System.out.println("Skipped recent histories: " + skippedRecentCount);
        System.out.println("Skipped empty histories: " + skippedEmptyCount);
        System.out.println("Errors: " + errorCount);
        printTableNames("Would delete table names:", wouldDeleteTableNames);
        printTableNames("Deleted table names:", deletedTableNames);
    } catch (Exception e) {
        System.out.println("Failed to delete old histories under " + deviceOrd + ": " + e);
        e.printStackTrace();
    } finally {
        if (conn != null) {
            conn.close();
        }
    }
}

public void onStop() throws Exception {
    // shutdown code here
}

private BOrd getDeviceOrd() {
    return getDEVICE_ORD();
}

private boolean getDryRun() {
    return getDRY_RUN();
}

private void printTableNames(String title, java.util.List<String> tableNames) {
    System.out.println(title);
    if (tableNames.isEmpty()) {
        System.out.println("<none>");
        return;
    }

    for (int i = 0; i < tableNames.size(); i++) {
        System.out.println(tableNames.get(i));
    }
}
