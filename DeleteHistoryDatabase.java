import javax.baja.history.BHistoryId;
import javax.baja.history.BHistoryRecord;
import javax.baja.history.BHistoryService;
import javax.baja.history.BIHistory;
import javax.baja.history.db.BHistoryDatabase;
import javax.baja.history.db.HistoryDatabaseConnection;
import javax.baja.naming.BOrd;
import javax.baja.sys.BAbsTime;
import javax.baja.sys.Sys;

public class DeleteHistoryDatabase {
    private static final String HISTORY_ORD = "historyId:/MR210A_02_FO/CA01_DH210_SLA02_Rh";

    // Keep true for the first execution. Change to false only after checking the log.
    private static final boolean DRY_RUN = true;

    public void onStart() throws Exception {
        // start up code here
    }

    public void onExecute() throws Exception {
        HistoryDatabaseConnection conn = null;

        try {
            BHistoryService historyService = (BHistoryService) Sys.getService(BHistoryService.TYPE);
            BHistoryDatabase database = historyService.getDatabase();
            conn = database.getDbConnection(null);

            BHistoryId historyId = resolveHistoryId();

            if (!conn.exists(historyId)) {
                System.out.println("History does not exist: " + historyId);
                return;
            }

            BIHistory history = conn.getHistory(historyId);
            int recordCount = conn.getRecordCount(history);
            BHistoryRecord lastRecord = recordCount > 0 ? conn.getLastRecord(history) : null;

            System.out.println("Target history: " + historyId);
            System.out.println("Record count: " + recordCount);
            if (lastRecord != null) {
                System.out.println("Latest record timestamp: " + lastRecord.getTimestamp());
                System.out.println("Latest record: " + lastRecord);
            } else {
                System.out.println("Latest record timestamp: <none>");
            }

            if (DRY_RUN) {
                System.out.println("[DRY RUN] No history database was deleted.");
                System.out.println("Set DRY_RUN = false and execute again to delete this history.");
                return;
            }

            if (isWithinOneMonth(lastRecord)) {
                BAbsTime oneMonthAgo = BAbsTime.now().prevMonth();
                System.out.println("Skip delete: latest record timestamp is within 1 month.");
                System.out.println("One-month cutoff: " + oneMonthAgo);
                return;
            }

            conn.deleteHistory(historyId);
            System.out.println("Deleted history database: " + historyId);
        } catch (Exception e) {
            System.out.println("Failed to delete history database: " + e);
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

    private BHistoryId resolveHistoryId() throws Exception {
        Object target = BOrd.make(HISTORY_ORD).resolve().get();

        if (target instanceof BIHistory) {
            return ((BIHistory) target).getId();
        }

        String idText = HISTORY_ORD;
        if (idText.startsWith("historyId:")) {
            idText = idText.substring("historyId:".length());
        }

        return BHistoryId.make(idText);
    }

    private boolean isWithinOneMonth(BHistoryRecord lastRecord) {
        if (lastRecord == null) {
            return false;
        }

        BAbsTime latestTimestamp = lastRecord.getTimestamp();
        if (latestTimestamp == null || latestTimestamp.isNull()) {
            return false;
        }

        return latestTimestamp.isAfter(BAbsTime.now().prevMonth());
    }
}
