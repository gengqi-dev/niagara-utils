public class AddHistoryExt4NumPoint {
    public void onStart() throws Exception {
        // start up code here
    }

    public void onExecute() throws Exception {
        // execute code (set executeOnChange flag on inputs)
        try {
            // ===================================================================
            // STEP 1: Specify the folder containing Modbus Numeric Points (MUST modify!)
            // Example: station:|slot:/Drivers/ModbusNetwork/MyModbusDevice/Points
            // ===================================================================
            BOrd folderPath = getFolderOrd();
            BComponent folder = (BComponent) folderPath.resolve().get();

            if (folder == null) {
                System.out.println("❌ Target folder not found!");
                return;
            }
            System.out.println("✅ Target folder found: " + folderPath);

            // ===================================================================
            // STEP 2: Iterate through all child components, only process Modbus Numeric
            // Points
            // ===================================================================
            for (BComponent child : folder.getChildren(BComponent.class)) {

                // Only process Numeric Points (supports NumericPoint and NumericWritable)
                if (!(child instanceof BNumericPoint) && !(child instanceof BNumericWritable)) {
                    continue;
                }

                Object proxyExtObj = child.get("proxyExt");
                if (proxyExtObj == null || child.get("proxyExt").isNull()) {
                    continue;
                }
                String pointName = child.getName();

                // Optional: Further filter to only process Modbus points (prevent accidental
                // operations on other network points)
                // if (child.get("proxyExt") == null ||
                // !child.get("proxyExt").getType().toString().contains("modbus")) continue;

                // ===================================================================
                // STEP 3: Check if History Extension already exists (prevent duplicates)
                // ===================================================================
                List<String> toRemove = new ArrayList<>();

                // public final Slot[] getSlotsArray()
                Slot[] slots = child.getSlotsArray();

                for (Slot slot : slots) {
                    if (slot.isDynamic()) {
                        Object val = child.get(slot.getName());
                        if (val instanceof BComponent) {
                            BComponent ext = (BComponent) val;

                            Type historyExtType = BTypeSpec.make("history:HistoryExt").getResolvedType();
                            if (ext.getType().is(historyExtType)) {
                                toRemove.add(slot.getName());
                            }
                        }
                    }
                }

                for (String name : toRemove) {
                    child.remove(name);
                    System.out.println("History Extension (by type): " + pointName + " slot: " + name);
                }

                // ===================================================================
                // STEP 4: Create NumericIntervalHistoryExt
                // ===================================================================
                String typeName = "history:NumericIntervalHistoryExt";
                Type histExtType = BTypeSpec.make(typeName).getResolvedType();
                BComponent historyExt = (BComponent) histExtType.getInstance();

                // ===================================================================
                // STEP 5: Add and configure History Extension
                // ===================================================================
                child.add("NumericIntervalHE", historyExt);

                // Core configuration (modify as needed)
                historyExt.set("enabled", BBoolean.TRUE); // Enable
                historyExt.set("historyName",
                        BFormat.make("%parent.parent.parent.name%_%parent.parent.name%_%parent.name%")); // History name
                                                                                                         // (BFormat
                                                                                                         // recommended)

                // Collection interval (example: collect every 1 minute)
                historyExt.set("interval", BRelTime.make(0, 0, 1, 0));

                BComponent historyConfig = (BComponent) historyExt.get("historyConfig");
                if (historyConfig != null) {
                    // Storage capacity (Unlimited or a specific number)
                    historyConfig.set("capacity", BCapacity.makeByRecordCount(500000)); // or BInteger.UNLIMITED
                }
                // Collection type (Interval or COV)
                // historyExt.set("collectionType", BCollectionType.make("interval")); // or
                // "cov"

                System.out.println("✅ Added History Extension for " + pointName);
            }

            System.out.println("========== All processing completed! ==========");
        } catch (Exception e) {
            System.out.println("❌ Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void onStop() throws Exception {
        // shutdown code here
    }

}
