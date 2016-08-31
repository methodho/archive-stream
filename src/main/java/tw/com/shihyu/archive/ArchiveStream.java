package tw.com.shihyu.archive;

import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.ArchiveOutputStream;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.compress.archivers.ar.ArArchiveEntry;
import org.apache.commons.compress.archivers.cpio.CpioArchiveEntry;
import org.apache.commons.compress.archivers.jar.JarArchiveEntry;
import org.apache.commons.compress.archivers.sevenz.SevenZArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.utils.IOUtils;

/**
 * 將 apache-compress 的 API 再包裝成類似 java.util.Stream, 並且也整合了 java 8 的 functional interface, 讓使用起來更加親切
 * <p>
 * 
 * Archiver stream are: ar, zip, tar, jar, cpio
 * 
 * @author Matt S.Y. Ho
 *
 */
public final class ArchiveStream {

  private ArchiveStream() {}

  public static interface ArchiveStreamWriter {
    /**
     * Will auto detected the archive type by the first entry.key of map
     * <p>
     * NOTE: All {@code ArchiveEntry} type in given map.key must be the same
     * 
     * @param archives
     * @throws IOException
     * @throws ArchiveException
     */
    <E extends ArchiveEntry> void compress(Map<E, InputStream> archives)
        throws IOException, ArchiveException;
  }

  public static interface ArchiveStreamReader {

    void forEach(BiConsumer<ArchiveEntry, ArchiveInputStream> action)
        throws IOException, ArchiveException;

    <U> Stream<U> mapToObj(BiFunction<ArchiveEntry, ArchiveInputStream, U> mapper)
        throws IOException, ArchiveException;

  }

  public static ArchiveStreamWriter of(OutputStream out) {
    requireNonNull(out, "OutputStream must not be null");

    return new ArchiveStreamWriter() {

      @Override
      public <E extends ArchiveEntry> void compress(Map<E, InputStream> archives)
          throws IOException, ArchiveException {
        if (archives == null || archives.isEmpty()) {
          throw new IllegalArgumentException("Archives must not be null or empty");
        }
        ArchiveOutputStream aout = null;
        if (out instanceof ArchiveOutputStream) {
          aout = (ArchiveOutputStream) out;
        }
        try {
          for (final Entry<E, InputStream> archive : archives.entrySet()) {
            if (aout == null) {
              aout = autoDetect(archive.getKey(), out);
            }
            aout.putArchiveEntry(archive.getKey());
            if (archive.getValue() != null) {
              IOUtils.copy(archive.getValue(), aout);
            }
            aout.closeArchiveEntry();
          }
        } finally {
          if (aout != null) {
            aout.close();
          }
        }
      }

      private ArchiveOutputStream autoDetect(ArchiveEntry archive, OutputStream out)
          throws ArchiveException {
        String archiverName;
        if (archive instanceof ArArchiveEntry) {
          archiverName = ArchiveStreamFactory.AR;
        } else if (archive instanceof ZipArchiveEntry) {
          archiverName = ArchiveStreamFactory.ZIP;
        } else if (archive instanceof TarArchiveEntry) {
          archiverName = ArchiveStreamFactory.TAR;
        } else if (archive instanceof JarArchiveEntry) {
          archiverName = ArchiveStreamFactory.JAR;
        } else if (archive instanceof CpioArchiveEntry) {
          archiverName = ArchiveStreamFactory.CPIO;
        } else if (archive instanceof SevenZArchiveEntry) {
          archiverName = ArchiveStreamFactory.SEVEN_Z;
        } else {
          throw new UnsupportedOperationException(
              "Unrecognized archive type: " + archive.getClass().getName());
        }
        return new ArchiveStreamFactory().createArchiveOutputStream(archiverName, out);
      }

    };
  }

  public static ArchiveStreamReader of(InputStream in) {
    requireNonNull(in, "in must not be null");
    if (!in.markSupported()) {
      throw new IllegalArgumentException(
          "Mark is not supported. Maybe you should try to wrap InputStream in BufferedInputStream.");
    }

    return new ArchiveStreamReader() {

      public <U> Stream<U> mapToObj(BiFunction<ArchiveEntry, ArchiveInputStream, U> mapper)
          throws IOException, ArchiveException {
        requireNonNull(mapper, "mapper must not be null");
        ArchiveInputStream ain;
        if (in instanceof ArchiveInputStream) {
          ain = (ArchiveInputStream) in;
        } else {
          ain = new ArchiveStreamFactory().createArchiveInputStream(in);
        }

        ArchiveInputStreamIterator<U> iterator = new ArchiveInputStreamIterator<>(ain, mapper);
        return StreamSupport
            .stream(Spliterators.spliteratorUnknownSize(iterator,
                Spliterator.ORDERED | Spliterator.NONNULL | Spliterator.IMMUTABLE), false)
            .onClose(iterator::close);
      }

      @Override
      public void forEach(BiConsumer<ArchiveEntry, ArchiveInputStream> action)
          throws IOException, ArchiveException {
        mapToObj((entity, in) -> new Object[] {entity, in})
            .forEach(read -> action.accept((ArchiveEntry) read[0], (ArchiveInputStream) read[1]));
      }
    };
  }

}
