package tw.com.shihyu.archive;

import static java.util.Objects.requireNonNull;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.function.BiFunction;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.utils.IOUtils;


/**
 * Warp {@link ArchiveInputStream} in {@code Iterator}
 * 
 * @author Matt S.Y. Ho
 *
 */
public class ArchiveInputStreamIterator<U> implements Iterator<U>, AutoCloseable {

  private final ArchiveInputStream in;
  private final BiFunction<ArchiveEntry, ArchiveInputStream, U> mapper;
  private U cached;
  private boolean finished = false;

  public ArchiveInputStreamIterator(ArchiveInputStream in,
      BiFunction<ArchiveEntry, ArchiveInputStream, U> mapper) {
    super();
    this.in = requireNonNull(in, "in");
    this.mapper = requireNonNull(mapper, "mapper");
  }

  @Override
  public boolean hasNext() {
    if (cached != null) {
      return true;
    } else if (finished) {
      return false;
    } else {
      try {
        ArchiveEntry next = in.getNextEntry();
        if (next == null) {
          finished = true;
          return false;
        } else {
          cached = mapper.apply(next, in);
          return true;
        }
      } catch (Exception e) {
        close();
        throw new IllegalStateException(e);
      }
    }
  }

  @Override
  public U next() {
    if (!hasNext()) {
      throw new NoSuchElementException("No more archives");
    }
    try {
      return cached;
    } finally {
      cached = null;
    }
  }

  @Override
  public void close() {
    finished = true;
    IOUtils.closeQuietly(in);
    cached = null;
  }

  public void remove() {
    throw new UnsupportedOperationException("Remove unsupported on ArchiveInputStreamIterator");
  }

}
