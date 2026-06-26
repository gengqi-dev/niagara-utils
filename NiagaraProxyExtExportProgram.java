import javax.baja.collection.BITable;
import javax.baja.collection.Column;
import javax.baja.collection.TableCursor;
import javax.baja.job.BRunnableJob;
import javax.baja.naming.BOrd;
import javax.baja.sys.Sys;

public void onExecute() throws Exception {
  MyTask task = new MyTask();
  task.submit();
}

public class MyTask implements Runnable {
  private BRunnableJob job;

  private static final String BQL_ORD = "station:|slot:/Drivers/NiagaraNetwork|bql:select proxyExt where proxyExt like 's%'";

  private static final String OUTPUT_DIR_NAME = "PointList";
  private static final String OUTPUT_FILE_NAME = "NiagaraProxyExt.csv";

  public MyTask() {
    job = new BRunnableJob(this);
  }

  public void submit() {
    job.submit(null);
  }

  public void run() {
    try {
      BITable table = (BITable) BOrd.make(BQL_ORD).resolve().get();

      Column proxyExtCol = table.getColumns().get("proxyExt");
      if (proxyExtCol == null) {
        throw new IllegalStateException("BQL result does not contain proxyExt column");
      }

      java.io.File out = resolveOutputFile();

      try (java.io.OutputStream os = new java.io.FileOutputStream(out);
          java.io.BufferedWriter writer = new java.io.BufferedWriter(
              new java.io.OutputStreamWriter(os, "UTF-8"))) {

        writer.write('\uFEFF');
        writer.write("proxyExtDecoded");
        writer.newLine();

        java.util.TreeSet rows = new java.util.TreeSet();
        TableCursor cursor = table.cursor();
        while (cursor.next()) {
          String raw = cursor.cell(proxyExtCol).toString();
          String decoded = decodeNiagaraEscapes(raw);
          rows.add(csv(decoded));
        }

        java.util.Iterator it = rows.iterator();
        while (it.hasNext()) {
          writer.write((String) it.next());
          writer.newLine();
        }
      }

      job.log().success("Exported BQL result to " + out.getAbsolutePath());
    } catch (Exception e) {
      job.log().failed("Failed to export BQL result", e);
    }
  }

  private java.io.File resolveOutputFile() throws Exception {
    java.io.File stationHome = Sys.getStationHome();
    java.io.File outputDir = new java.io.File(stationHome, OUTPUT_DIR_NAME);

    if (!outputDir.exists() && !outputDir.mkdirs()) {
      throw new java.io.IOException("Could not create output directory: " + outputDir.getAbsolutePath());
    }

    if (!outputDir.isDirectory()) {
      throw new java.io.IOException("Output path is not a directory: " + outputDir.getAbsolutePath());
    }

    java.io.File outputFile = new java.io.File(outputDir, OUTPUT_FILE_NAME);
    if (!outputFile.exists() && !outputFile.createNewFile()) {
      throw new java.io.IOException("Could not create output file: " + outputFile.getAbsolutePath());
    }

    return outputFile;
  }

  private String csv(String value) {
    if (value == null)
      return "\"\"";
    return "\"" + value.replace("\"", "\"\"") + "\"";
  }

  private String decodeNiagaraEscapes(String value) throws Exception {
    if (value == null || value.length() == 0)
      return "";

    StringBuffer out = new StringBuffer();

    for (int i = 0; i < value.length(); i++) {
      char c = value.charAt(i);

      if (c == '$' || c == '%') {
        if (i + 5 < value.length()
            && (value.charAt(i + 1) == 'u' || value.charAt(i + 1) == 'U')
            && isHex(value.charAt(i + 2))
            && isHex(value.charAt(i + 3))
            && isHex(value.charAt(i + 4))
            && isHex(value.charAt(i + 5))) {
          int codePoint = Integer.parseInt(value.substring(i + 2, i + 6), 16);
          out.append(new String(Character.toChars(codePoint)));
          i += 5;
          continue;
        }

        if (i + 2 < value.length()
            && isHex(value.charAt(i + 1))
            && isHex(value.charAt(i + 2))) {
          int ch = Integer.parseInt(value.substring(i + 1, i + 3), 16);
          out.append((char) ch);
          i += 2;
          continue;
        }
      }

      out.append(c);
    }

    return out.toString();
  }

  private boolean isHex(char c) {
    return (c >= '0' && c <= '9')
        || (c >= 'a' && c <= 'f')
        || (c >= 'A' && c <= 'F');
  }
}


