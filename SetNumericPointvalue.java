public void onStart() throws Exception {
    // start up code here
}

public void onExecute() throws Exception {
    SetNumericPointTask task = new SetNumericPointTask();
    task.submit();
}

public class SetNumericPointTask implements Runnable {
    private static final String JOB_NAME = "Set Numeric Point Value";

    public SetNumericPointTask() {
        job = new NamedRunnableJob(this);
    }

    public void submit() {
        job.submit(null);
    }

    public void run() {
        job.log().message("started task [" + Thread.currentThread().getName() + "]");

        try {
            BOrd folderOrd = getFolderOrd();
            job.log().message("Resolving folder: " + folderOrd);

            BComponent folder = (BComponent) folderOrd.resolve().get();
            if (folder == null) {
                job.log().message("Folder not found!");
                return;
            }

            double targetValue = getStartValue();
            int applied = 0;
            int unchanged = 0;
            int skipped = 0;
            BComponent[] children = folder.getChildren(BComponent.class);

            for (int i = 0; i < children.length; i++) {
                BComponent child = children[i];

                if (child instanceof BNumericWritable) {
                    BNumericWritable nw = (BNumericWritable) child;
                    // if (isOutEqual(nw, targetValue)) {
                    // job.log().message("Point skipped unchanged: " + child.getName() + " -> " +
                    // targetValue);
                    // unchanged++;
                    // } else {
                    nw.setIn1(new BStatusNumeric(targetValue));
                    nw.set(BDouble.make(targetValue));
                    job.log().message("Point set successfully: " + child.getName() + " -> " + targetValue);
                    applied++;
                    // }

                    targetValue += 2.0;
                } else {
                    skipped++;
                }

                job.progress(progress(i + 2, children.length));
            }

            job.progress(100);
            job.log().success("ended task [" + Thread.currentThread().getName() + "] applied=" + applied
                    + ", unchanged=" + unchanged + ", skipped=" + skipped);
        } catch (Exception e) {
            job.log().failed("Failed to set numeric point values", e);
        }
    }

    private boolean isOutEqual(BNumericWritable point, double targetValue) {
        return Double.compare(point.getOut().getValue(), targetValue) == 0;
    }

    private int progress(int completed, int total) {
        if (total <= 0) {
            return 100;
        }

        return Math.min(100, Math.max(0, (completed * 100) / total));
    }

    private BRunnableJob job;

    private class NamedRunnableJob extends BRunnableJob {
        private NamedRunnableJob(Runnable runnable) {
            super(runnable);
        }

        public String toString(javax.baja.sys.Context context) {
            return JOB_NAME;
        }
    }
}

public void onReset() throws Exception {

    // Assume you already have a BOrd input slot pointing to the Ord folder
    BOrd folderOrd = getFolderOrd(); // ordFolder is the BOrd input slot you added
    BComponent folder = null;
    try {
        folder = (BComponent) folderOrd.resolve().get(); // or use .resolve().get()
    } catch (Exception e) {
        System.err.println("Failed to resolve folder: " + e.getMessage());
    }

    if (folder == null) {
        System.err.println("Folder not found!");
        return;
    }

    // double targetValue = getStartValue(); // ← Change to your desired value, e.g.
    // 25.0
    for (BComponent child : folder.getChildren(BComponent.class)) {
        try {

            if (child instanceof BNumericWritable) {
                BNumericWritable nw = (BNumericWritable) child;
                nw.setIn1(new BStatusNumeric(0, BStatus.nullStatus));
                nw.set(BDouble.make(0));
                System.out.println("Point value reset");

            }

        } catch (Exception e) {
            System.err.println("Failed to set point: " + child.getName() + " → " + e.getMessage());
        }
    }
}

public void onStop() throws Exception {
    // shutdown code here
}