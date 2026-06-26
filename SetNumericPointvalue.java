public void onStart() throws Exception {
    // start up code here
}

public void onStop() throws Exception {
    // shutdown code here
}

public void onExecute() throws Exception {
    SetNumericPointTask task = new SetNumericPointTask(false);
    task.submit();
}

public class SetNumericPointTask implements Runnable {
    private static final String JOB_NAME = "Set Numeric Point Value";
    private static final String RESET_JOB_NAME = "Reset Numeric Point Value";

    public SetNumericPointTask(boolean resetMode) {
        this.resetMode = resetMode;
        job = new NamedRunnableJob(this);
    }

    public void submit() {
        job.submit(null);
    }

    public void run() {
        job.log().message("started task [" + Thread.currentThread().getName() + "]");

        try {
            BOrd folderOrd = getFolderOrd();
            if (folderOrd == null) {
                job.log().message("Folder Ord is null!");
                return;
            }

            job.log().message("Resolving folder: " + folderOrd);

            BComponent folder = (BComponent) folderOrd.resolve().get();
            if (folder == null) {
                job.log().message("Folder not found!");
                return;
            }

            if (resetMode) {
                resetRoot(folder);
                return;
            }

            int[] stats = new int[1];
            int[] completed = new int[1];
            java.util.List<java.util.List<BNumericWritable>> pointGroups = findNumericWritablePointGroups(folder);
            int totalPoints = countPoints(pointGroups);
            job.log().message("Found numeric writable points: " + totalPoints);

            processPointGroups(pointGroups, stats, completed, totalPoints);

            job.progress(100);
            job.log().success("ended task [" + Thread.currentThread().getName() + "] applied=" + stats[0]);
        } catch (Exception e) {
            job.log().failed("Failed to set numeric point values", e);
        }
    }

    private void processPointGroups(java.util.List<java.util.List<BNumericWritable>> pointGroups, int[] stats,
            int[] completed, int totalPoints) {
        for (int i = 0; i < pointGroups.size(); i++) {
            processPointList(pointGroups.get(i), stats, completed, totalPoints);
        }
    }

    private void processPointList(java.util.List<BNumericWritable> points, int[] stats, int[] completed,
            int totalPoints) {
        double targetValue = getStartValue();

        for (int i = 0; i < points.size(); i++) {
            BNumericWritable nw = points.get(i);

            nw.setIn1(new BStatusNumeric(targetValue));
            nw.set(BDouble.make(targetValue));
            job.log().message("Point set successfully: " + nw.getName() + " -> " + targetValue);
            stats[0]++;

            targetValue += 2.0;
            completed[0]++;
            job.progress(progress(completed[0], totalPoints));
        }
    }

    private void resetRoot(BComponent root) {
        java.util.List<java.util.List<BNumericWritable>> pointGroups = findNumericWritablePointGroups(root);
        int totalPoints = countPoints(pointGroups);
        int[] completed = new int[1];
        job.log().message("Found numeric writable points to reset: " + totalPoints);

        resetPointGroups(pointGroups, completed, totalPoints);

        job.progress(100);
        job.log().success("ended task [" + Thread.currentThread().getName() + "] reset=" + completed[0]);
    }

    private void resetPointGroups(java.util.List<java.util.List<BNumericWritable>> pointGroups, int[] completed,
            int totalPoints) {
        for (int i = 0; i < pointGroups.size(); i++) {
            resetPointList(pointGroups.get(i), completed, totalPoints);
        }
    }

    private void resetPointList(java.util.List<BNumericWritable> points, int[] completed, int totalPoints) {
        for (int i = 0; i < points.size(); i++) {
            BNumericWritable nw = points.get(i);
            nw.setIn1(new BStatusNumeric(0, BStatus.nullStatus));
            nw.set(BDouble.make(0));
            job.log().message("Point value reset: " + nw.getName());
            completed[0]++;
            job.progress(progress(completed[0], totalPoints));
        }
    }

    private java.util.List<java.util.List<BNumericWritable>> findNumericWritablePointGroups(BComponent root) {
        java.util.List<java.util.List<BNumericWritable>> pointGroups = new java.util.ArrayList<java.util.List<BNumericWritable>>();

        if (root instanceof BModbusTcpSlaveNetwork) {
            BComponent[] devices = ((BModbusTcpSlaveNetwork) root).getChildComponents();
            for (int i = 0; i < devices.length; i++) {
                if (devices[i] instanceof BModbusTcpSlaveDevice) {
                    pointGroups.add(findDevicePoints((BModbusTcpSlaveDevice) devices[i]));
                }
            }
        } else if (root instanceof BModbusTcpSlaveDevice) {
            pointGroups.add(findDevicePoints((BModbusTcpSlaveDevice) root));
        } else {
            pointGroups.add(findDirectChildPoints(root));
        }

        return pointGroups;
    }

    private int countPoints(java.util.List<java.util.List<BNumericWritable>> pointGroups) {
        long count = 0;
        for (int i = 0; i < pointGroups.size(); i++) {
            count += pointGroups.get(i).size();
        }

        return count > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) count;
    }

    private java.util.List<BNumericWritable> findDevicePoints(BModbusTcpSlaveDevice device) {
        java.util.List<BNumericWritable> points = new java.util.ArrayList<BNumericWritable>();
        BModbusServerPointDeviceExt pointDeviceExt = device.getPoints();
        if (pointDeviceExt == null) {
            job.log().message("Device has no points extension: " + device.getName());
            return points;
        }

        BComponent[] children = pointDeviceExt.getChildComponents();
        for (int i = 0; i < children.length; i++) {
            if (children[i] instanceof BNumericWritable) {
                points.add((BNumericWritable) children[i]);
            }
        }
        return points;
    }

    private java.util.List<BNumericWritable> findDirectChildPoints(BComponent folder) {
        java.util.List<BNumericWritable> points = new java.util.ArrayList<BNumericWritable>();
        BComponent[] children = folder.getChildren(BComponent.class);
        for (int i = 0; i < children.length; i++) {
            if (children[i] instanceof BNumericWritable) {
                points.add((BNumericWritable) children[i]);
            }
        }
        return points;
    }

    private int progress(int completed, int total) {
        if (total <= 0) {
            return 100;
        }

        long percent = ((long) completed * 100L) / (long) total;
        return (int) Math.min(100L, Math.max(0L, percent));
    }

    private BRunnableJob job;
    private boolean resetMode;

    private class NamedRunnableJob extends BRunnableJob {
        private NamedRunnableJob(Runnable runnable) {
            super(runnable);
        }

        public String toString(javax.baja.sys.Context context) {
            return resetMode ? RESET_JOB_NAME : JOB_NAME;
        }
    }
}

public void onReset() throws Exception {
    SetNumericPointTask task = new SetNumericPointTask(true);
    task.submit();
}
