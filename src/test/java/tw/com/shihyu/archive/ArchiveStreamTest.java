package tw.com.shihyu.archive;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.utils.IOUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;


public class ArchiveStreamTest {

  private Map<String, String> contents;
  private File zip;

  @Before
  public void setUp() {
    contents = new HashMap<>();
    for (int i = 0; i < 10; i++) {
      String random = UUID.randomUUID().toString();
      contents.put(random, random);
    }
    zip = new File("zip-test.zip");
    zip.deleteOnExit();
  }

  @After
  public void destroy() {
    zip.deleteOnExit();
  }

  @Test
  public void test() throws IOException, ArchiveException {
    FileOutputStream out = new FileOutputStream(zip);
    Map<ZipArchiveEntry, InputStream> archives = new HashMap<>();
    contents.forEach(
        (k, v) -> archives.put(new ZipArchiveEntry(k), new ByteArrayInputStream(v.getBytes())));
    ArchiveStream.of(out).compress(archives);

    Assert.assertTrue(zip.exists());
    Assert.assertTrue(zip.isFile());
    Assert.assertTrue(zip.length() > 0);

    Map<String, String> entries = ArchiveStream
        .of(new BufferedInputStream(new FileInputStream(zip))).mapToObj((entity, in) -> {
          try {
            return new String[] {entity.getName(), new String(IOUtils.toByteArray(in))};
          } catch (IOException e) {
            throw new RuntimeException(e);
          }
        }).filter(o -> !o[0].equals("__MACOSX")).collect(Collectors.toMap(o -> o[0], o -> o[1]));
    Assert.assertEquals(contents.size(), entries.size());
    entries.forEach((k, v) -> {
      String content = contents.get(k);
      Assert.assertNotNull(content);
      Assert.assertEquals(content, v);
    });
  }

}
