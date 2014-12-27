/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.squareup.okhttp.internal;

import java.io.Closeable;
import java.io.EOFException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import okio.Buffer;
import okio.BufferedSink;
import okio.BufferedSource;
import okio.ForwardingSink;
import okio.Okio;
import okio.Sink;
import okio.Source;
import okio.Timeout;

/**
 * A cache that uses a bounded amount of space on a filesystem. Each cache
 * entry has a string key and a fixed number of values. Each key must match
 * the regex <strong>[a-z0-9_-]{1,64}</strong>. Values are byte sequences,
 * accessible as streams or files. Each value must be between {@code 0} and
 * {@code Integer.MAX_VALUE} bytes in length.
 *
 * <p>The cache stores its data in a directory on the filesystem. This
 * directory must be exclusive to the cache; the cache may delete or overwrite
 * files from its directory. It is an error for multiple processes to use the
 * same cache directory at the same time.
 *
 * <p>This cache limits the number of bytes that it will store on the
 * filesystem. When the number of stored bytes exceeds the limit, the cache will
 * remove entries in the background until the limit is satisfied. The limit is
 * not strict: the cache may temporarily exceed it while waiting for files to be
 * deleted. The limit does not include filesystem overhead or the cache
 * journal so space-sensitive applications should set a conservative limit.
 *
 * <p>Clients call {@link #edit} to create or update the values of an entry. An
 * entry may have only one editor at one time; if a value is not available to be
 * edited then {@link #edit} will return null.
 * <ul>
 *     <li>When an entry is being <strong>created</strong> it is necessary to
 *         supply a full set of values; the empty value should be used as a
 *         placeholder if necessary.
 *     <li>When an entry is being <strong>edited</strong>, it is not necessary
 *         to supply data for every value; values default to their previous
 *         value.
 * </ul>
 * Every {@link #edit} call must be matched by a call to {@link Editor#commit}
 * or {@link Editor#abort}. Committing is atomic: a read observes the full set
 * of values as they were before or after the commit, but never a mix of values.
 *
 * <p>Clients call {@link #get} to read a snapshot of an entry. The read will
 * observe the value at the time that {@link #get} was called. Updates and
 * removals after the call do not impact ongoing reads.
 *
 * <p>This class is tolerant of some I/O errors. If files are missing from the
 * filesystem, the corresponding entries will be dropped from the cache. If
 * an error occurs while writing a cache value, the edit will fail silently.
 * Callers should handle other problems by catching {@code IOException} and
 * responding appropriately.
 */
public final class DiskLruCache implements Closeable {
  static final String JOURNAL_FILE = "journal";
  static final String JOURNAL_FILE_TEMP = "journal.tmp";
  static final String JOURNAL_FILE_BACKUP = "journal.bkp";
  static final String MAGIC = "libcore.io.DiskLruCache";
  static final String VERSION_1 = "1";
  static final long ANY_SEQUENCE_NUMBER = -1;
  static final Pattern LEGAL_KEY_PATTERN = Pattern.compile("[a-z0-9_-]{1,120}");
  private static final String CLEAN = "CLEAN";
  private static final String DIRTY = "DIRTY";
  private static final String REMOVE = "REMOVE";
  private static final String READ = "READ";

    /*
     * This cache uses a journal file named "journal". A typical journal file
     * looks like this:
     *     libcore.io.DiskLruCache
     *     1
     *     100
     *     2
     *
     *     CLEAN 3400330d1dfc7f3f7f4b8d4d803dfcf6 832 21054
     *     DIRTY 335c4c6028171cfddfbaae1a9c313c52
     *     CLEAN 335c4c6028171cfddfbaae1a9c313c52 3934 2342
     *     REMOVE 335c4c6028171cfddfbaae1a9c313c52
     *     DIRTY 1ab96a171faeeee38496d8b330771a7a
     *     CLEAN 1ab96a171faeeee38496d8b330771a7a 1600 234
     *     READ 335c4c6028171cfddfbaae1a9c313c52
     *     READ 3400330d1dfc7f3f7f4b8d4d803dfcf6
     *
     * The first five lines of the journal form its header. They are the
     * constant string "libcore.io.DiskLruCache", the disk cache's version,
     * the application's version, the value count, and a blank line.
     *
     * Each of the subsequent lines in the file is a record of the state of a
     * cache entry. Each line contains space-separated values: a state, a key,
     * and optional state-specific values.
     *   o DIRTY lines track that an entry is actively being created or updated.
     *     Every successful DIRTY action should be followed by a CLEAN or REMOVE
     *     action. DIRTY lines without a matching CLEAN or REMOVE indicate that
     *     temporary files may need to be deleted.
     *   o CLEAN lines track a cache entry that has been successfully published
     *     and may be read. A publish line is followed by the lengths of each of
     *     its values.
     *   o READ lines track accesses for LRU.
     *   o REMOVE lines track entries that have been deleted.
     *
     * The journal file is appended to as cache operations occur. The journal may
     * occasionally be compacted by dropping redundant lines. A temporary file named
     * "journal.tmp" will be used during compaction; that file should be deleted if
     * it exists when the cache is opened.
     */

  private final File directory;
  private final File journalFile;
  private final File journalFileTmp;
  private final File journalFileBackup;
  private final int appVersion;
  private long maxSize;
  private final int valueCount;
  private long size = 0;
  private BufferedSink journalWriter;
  private final LinkedHashMap<String, Entry> lruEntries = new LinkedHashMap<String, Entry>(0, 0.75f, true);
  private int redundantOpCount;

  /**
   * To differentiate between old and current snapshots, each entry is given
   * a sequence number each time an edit is committed. A snapshot is stale if
   * its sequence number is not equal to its entry's sequence number.
   */
  private long nextSequenceNumber = 0;

  /** This cache uses a single background thread to evict entries. */
  final ThreadPoolExecutor executorService = new ThreadPoolExecutor(0, 1, 60L, TimeUnit.SECONDS,
      new LinkedBlockingQueue<Runnable>(), Util.threadFactory("OkHttp DiskLruCache", true));
  private final Runnable cleanupRunnable = new Runnable() {
    public void run() {
      synchronized (DiskLruCache.this) {
        if (journalWriter == null) {
          return; // Closed.
        }
        try {
          trimToSize();
          if (journalRebuildRequired()) {
            rebuildJournal();
            redundantOpCount = 0;
          }
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
      }
    }
  };

  private DiskLruCache(File directory, int appVersion, int valueCount, long maxSize) {
    this.directory = directory;
    this.appVersion = appVersion;
    this.journalFile = new File(directory, JOURNAL_FILE);
    this.journalFileTmp = new File(directory, JOURNAL_FILE_TEMP);
    this.journalFileBackup = new File(directory, JOURNAL_FILE_BACKUP);
    this.valueCount = valueCount;
    this.maxSize = maxSize;
  }

  /**
   * Opens the cache in {@code directory}, creating a cache if none exists
   * there.
   *
   * @param directory a writable directory
   * @param valueCount the number of values per cache entry. Must be positive.
   * @param maxSize the maximum number of bytes this cache should use to store
   * @throws IOException if reading or writing the cache directory fails
   */
  public static DiskLruCache open(File directory, int appVersion, int valueCount, long maxSize)
      throws IOException {
    if (maxSize <= 0) {
      throw new IllegalArgumentException("maxSize <= 0");
    }
    if (valueCount <= 0) {
      throw new IllegalArgumentException("valueCount <= 0");
    }

    // If a bkp file exists, use it instead.
    File backupFile = new File(directory, JOURNAL_FILE_BACKUP);
    if (backupFile.exists()) {
      File journalFile = new File(directory, JOURNAL_FILE);
      // If journal file also exists just delete backup file.
      if (journalFile.exists()) {
        backupFile.delete();
      } else {
        renameTo(backupFile, journalFile, false);
      }
    }

    // Prefer to pick up where we left off.
    DiskLruCache cache = new DiskLruCache(directory, appVersion, valueCount, maxSize);
    if (cache.journalFile.exists()) {
      try {
        cache.readJournal();
        cache.processJournal();
        return cache;
      } catch (IOException journalIsCorrupt) {
        Platform.get().logW("DiskLruCache " + directory + " is corrupt: "
            + journalIsCorrupt.getMessage() + ", removing");
        cache.delete();
      }
    }

    // Create a new empty cache.
    directory.mkdirs();
    cache = new DiskLruCache(directory, appVersion, valueCount, maxSize);
    cache.rebuildJournal();
    return cache;
  }

  private void readJournal() throws IOException {
    BufferedSource source = Okio.buffer(Okio.source(journalFile));
    try {
      String magic = source.readUtf8LineStrict();
      String version = source.readUtf8LineStrict();
      String appVersionString = source.readUtf8LineStrict();
      String valueCountString = source.readUtf8LineStrict();
      String blank = source.readUtf8LineStrict();
      if (!MAGIC.equals(magic)
          || !VERSION_1.equals(version)
          || !Integer.toString(appVersion).equals(appVersionString)
          || !Integer.toString(valueCount).equals(valueCountString)
          || !"".equals(blank)) {
        throw new IOException("unexpected journal header: [" + magic + ", " + version + ", "
            + valueCountString + ", " + blank + "]");
      }

      int lineCount = 0;
      while (true) {
        try {
          readJournalLine(source.readUtf8LineStrict());
          lineCount++;
        } catch (EOFException endOfJournal) {
          break;
        }
      }
      redundantOpCount = lineCount - lruEntries.size();

      // If we ended on a truncated line, rebuild the journal before appending to it.
      if (!source.exhausted()) {
        rebuildJournal();
      } else {
        journalWriter = Okio.buffer(Okio.appendingSink(journalFile));
      }
    } finally {
      Util.closeQuietly(source);
    }
  }

  private void readJournalLine(String line) throws IOException {
    int firstSpace = line.indexOf(' ');
    if (firstSpace == -1) {
      throw new IOException("unexpected journal line: " + line);
    }

    int keyBegin = firstSpace + 1;
    int secondSpace = line.indexOf(' ', keyBegin);
    final String key;
    if (secondSpace == -1) {
      key = line.substring(keyBegin);
      if (firstSpace == REMOVE.length() && line.startsWith(REMOVE)) {
        lruEntries.remove(key);
        return;
      }
    } else {
      key = line.substring(keyBegin, secondSpace);
    }

    Entry entry = lruEntries.get(key);
    if (entry == null) {
      entry = new Entry(key);
      lruEntries.put(key, entry);
    }

    if (secondSpace != -1 && firstSpace == CLEAN.length() && line.startsWith(CLEAN)) {
      String[] parts = line.substring(secondSpace + 1).split(" ");
      entry.readable = true;
      entry.currentEditor = null;
      entry.setLengths(parts);
    } else if (secondSpace == -1 && firstSpace == DIRTY.length() && line.startsWith(DIRTY)) {
      entry.currentEditor = new Editor(entry);
    } else if (secondSpace == -1 && firstSpace == READ.length() && line.startsWith(READ)) {
      // This work was already done by calling lruEntries.get().
    } else {
      throw new IOException("unexpected journal line: " + line);
    }
  }

  /**
   * Computes the initial size and collects garbage as a part of opening the
   * cache. Dirty entries are assumed to be inconsistent and will be deleted.
   */
  private void processJournal() throws IOException {
    deleteIfExists(journalFileTmp);
    for (Iterator<Entry> i = lruEntries.values().iterator(); i.hasNext(); ) {
      Entry entry = i.next();
      if (entry.currentEditor == null) {
        for (int t = 0; t < valueCount; t++) {
          size += entry.lengths[t];
        }
      } else {
        entry.currentEditor = null;
        for (int t = 0; t < valueCount; t++) {
          deleteIfExists(entry.cleanFiles[t]);
          deleteIfExists(entry.dirtyFiles[t]);
        }
        i.remove();
      }
    }
  }

  /**
   * Creates a new journal that omits redundant information. This replaces the
   * current journal if it exists.
   */
  private synchronized void rebuildJournal() throws IOException {
    if (journalWriter != null) {
      journalWriter.close();
    }

    BufferedSink writer = Okio.buffer(Okio.sink(journalFileTmp));
    try {
      writer.writeUtf8(MAGIC).writeByte('\n');
      writer.writeUtf8(VERSION_1).writeByte('\n');
      writer.writeUtf8(Integer.toString(appVersion)).writeByte('\n');
      writer.writeUtf8(Integer.toString(valueCount)).writeByte('\n');
      writer.writeByte('\n');

      for (Entry entry : lruEntries.values()) {
        if (entry.currentEditor != null) {
          writer.writeUtf8(DIRTY).writeByte(' ');
          writer.writeUtf8(entry.key);
          writer.writeByte('\n');
        } else {
          writer.writeUtf8(CLEAN).writeByte(' ');
          writer.writeUtf8(entry.key);
          writer.writeUtf8(entry.getLengths());
          writer.writeByte('\n');
        }
      }
    } finally {
      writer.close();
    }

    if (journalFile.exists()) {
      renameTo(journalFile, journalFileBackup, true);
    }
    renameTo(journalFileTmp, journalFile, false);
    journalFileBackup.delete();

    journalWriter = Okio.buffer(Okio.appendingSink(journalFile));
  }

  private static void deleteIfExists(File file) throws IOException {
    // If delete() fails, make sure it's because the file didn't exist!
    if (!file.delete() && file.exists()) {
      throw new IOException("failed to delete " + file);
    }
  }

  private static void renameTo(File from, File to, boolean deleteDestination) throws IOException {
    if (deleteDestination) {
      deleteIfExists(to);
    }
    if (!from.renameTo(to)) {
      throw new IOException();
    }
  }

  /**
   * Returns a snapshot of the entry named {@code key}, or null if it doesn't
   * exist is not currently readable. If a value is returned, it is moved to
   * the head of the LRU queue.
   */
  public synchronized Snapshot get(String key) throws IOException {
    checkNotClosed();
    validateKey(key);
    Entry entry = lruEntries.get(key);
    if (entry == null) {
      return null;
    }

    if (!entry.readable) {
      return null;
    }

    // Open all streams eagerly to guarantee that we see a single published
    // snapshot. If we opened streams lazily then the streams could come
    // from different edits.
    Source[] sources = new Source[valueCount];
    try {
      for (int i = 0; i < valueCount; i++) {
        sources[i] = Okio.source(entry.cleanFiles[i]);
      }
    } catch (FileNotFoundException e) {
      // A file must have been deleted manually!
      for (int i = 0; i < valueCount; i++) {
        if (sources[i] != null) {
          Util.closeQuietly(sources[i]);
        } else {
          break;
        }
      }
      return null;
    }

    redundantOpCount++;
    journalWriter.writeUtf8(READ).writeByte(' ').writeUtf8(key).writeByte('\n');
    if (journalRebuildRequired()) {
      executorService.execute(cleanupRunnable);
    }

    return new Snapshot(key, entry.sequenceNumber, sources, entry.lengths);
  }

  /**
   * Returns an editor for the entry named {@code key}, or null if another
   * edit is in progress.
   */
  public Editor edit(String key) throws IOException {
    return edit(key, ANY_SEQUENCE_NUMBER);
  }

  private synchronized Editor edit(String key, long expectedSequenceNumber) throws IOException {
    checkNotClosed();
    validateKey(key);
    Entry entry = lruEntries.get(key);
    if (expectedSequenceNumber != ANY_SEQUENCE_NUMBER && (entry == null
        || entry.sequenceNumber != expectedSequenceNumber)) {
      return null; // Snapshot is stale.
    }
    if (entry == null) {
      entry = new Entry(key);
      lruEntries.put(key, entry);
    } else if (entry.currentEditor != null) {
      return null; // Another edit is in progress.
    }

    Editor editor = new Editor(entry);
    entry.currentEditor = editor;

    // Flush the journal before creating files to prevent file leaks.
    journalWriter.writeUtf8(DIRTY).writeByte(' ').writeUtf8(key).writeByte('\n');
    journalWriter.flush();
    return editor;
  }

  /** Returns the directory where this cache stores its data. */
  public File getDirectory() {
    return directory;
  }

  /**
   * Returns the maximum number of bytes that this cache should use to store
   * its data.
   */
  public synchronized long getMaxSize() {
    return maxSize;
  }

  /**
   * Changes the maximum number of bytes the cache can store and queues a job
   * to trim the existing store, if necessary.
   */
  public synchronized void setMaxSize(long maxSize) {
    this.maxSize = maxSize;
    executorService.execute(cleanupRunnable);
  }

  /**
   * Returns the number of bytes currently being used to store the values in
   * this cache. This may be greater than the max size if a background
   * deletion is pending.
   */
  public synchronized long size() {
    return size;
  }

  private synchronized void completeEdit(Editor editor, boolean success) throws IOException {
    Entry entry = editor.entry;
    if (entry.currentEditor != editor) {
      throw new IllegalStateException();
    }

    // If this edit is creating the entry for the first time, every index must have a value.
    if (success && !entry.readable) {
      for (int i = 0; i < valueCount; i++) {
        if (!editor.written[i]) {
          editor.abort();
          throw new IllegalStateException("Newly created entry didn't create value for index " + i);
        }
        if (!entry.dirtyFiles[i].exists()) {
          editor.abort();
          return;
        }
      }
    }

    for (int i = 0; i < valueCount; i++) {
      File dirty = entry.dirtyFiles[i];
      if (success) {
        if (dirty.exists()) {
          File clean = entry.cleanFiles[i];
          dirty.renameTo(clean);
          long oldLength = entry.lengths[i];
          long newLength = clean.length();
          entry.lengths[i] = newLength;
          size = size - oldLength + newLength;
        }
      } else {
        deleteIfExists(dirty);
      }
    }

    redundantOpCount++;
    entry.currentEditor = null;
    if (entry.readable | success) {
      entry.readable = true;
      journalWriter.writeUtf8(CLEAN).writeByte(' ');
      journalWriter.writeUtf8(entry.key);
      journalWriter.writeUtf8(entry.getLengths());
      journalWriter.writeByte('\n');
      if (success) {
        entry.sequenceNumber = nextSequenceNumber++;
      }
    } else {
      lruEntries.remove(entry.key);
      journalWriter.writeUtf8(REMOVE).writeByte(' ');
      journalWriter.writeUtf8(entry.key);
      journalWriter.writeByte('\n');
    }
    journalWriter.flush();

    if (size > maxSize || journalRebuildRequired()) {
      executorService.execute(cleanupRunnable);
    }
  }

  /**
   * We only rebuild the journal when it will halve the size of the journal
   * and eliminate at least 2000 ops.
   */
  private boolean journalRebuildRequired() {
    final int redundantOpCompactThreshold = 2000;
    return redundantOpCount >= redundantOpCompactThreshold
        && redundantOpCount >= lruEntries.size();
  }

  /**
   * Drops the entry for {@code key} if it exists and can be removed. If the
   * entry for {@code key} is currently being edited, that edit will complete
   * normally but its value will not be stored.
   *
   * @return true if an entry was removed.
   */
  public synchronized boolean remove(String key) throws IOException {
    checkNotClosed();
    validateKey(key);
    Entry entry = lruEntries.get(key);
    if (entry == null) return false;
    return removeEntry(entry);
  }

  private boolean removeEntry(Entry entry) throws IOException {
    if (entry.currentEditor != null) {
      entry.currentEditor.hasErrors = true; // Prevent the edit from completing normally.
    }

    for (int i = 0; i < valueCount; i++) {
      File file = entry.cleanFiles[i];
      deleteIfExists(file);
      size -= entry.lengths[i];
      entry.lengths[i] = 0;
    }

    redundantOpCount++;
    journalWriter.writeUtf8(REMOVE).writeByte(' ').writeUtf8(entry.key).writeByte('\n');
    lruEntries.remove(entry.key);

    if (journalRebuildRequired()) {
      executorService.execute(cleanupRunnable);
    }

    return true;
  }

  /** Returns true if this cache has been closed. */
  public boolean isClosed() {
    return journalWriter == null;
  }

  private void checkNotClosed() {
    if (journalWriter == null) {
      throw new IllegalStateException("cache is closed");
    }
  }

  /** Force buffered operations to the filesystem. */
  public synchronized void flush() throws IOException {
    checkNotClosed();
    trimToSize();
    journalWriter.flush();
  }

  /** Closes this cache. Stored values will remain on the filesystem. */
  public synchronized void close() throws IOException {
    if (journalWriter == null) {
      return; // Already closed.
    }
    // Copying for safe iteration.
    for (Entry entry : lruEntries.values().toArray(new Entry[lruEntries.size()])) {
      if (entry.currentEditor != null) {
        entry.currentEditor.abort();
      }
    }
    trimToSize();
    journalWriter.close();
    journalWriter = null;
  }

  private void trimToSize() throws IOException {
    while (size > maxSize) {
      Entry toEvict = lruEntries.values().iterator().next();
      removeEntry(toEvict);
    }
  }

  /**
   * Closes the cache and deletes all of its stored values. This will delete
   * all files in the cache directory including files that weren't created by
   * the cache.
   */
  public void delete() throws IOException {
    close();
    Util.deleteContents(directory);
  }

  /**
   * Deletes all stored values from the cache. In-flight edits will complete
   * normally but their values will not be stored.
   */
  public synchronized void evictAll() throws IOException {
    // Copying for safe iteration.
    for (Entry entry : lruEntries.values().toArray(new Entry[lruEntries.size()])) {
      removeEntry(entry);
    }
  }

  private void validateKey(String key) {
    Matcher matcher = LEGAL_KEY_PATTERN.matcher(key);
    if (!matcher.matches()) {
      throw new IllegalArgumentException(
          "keys must match regex [a-z0-9_-]{1,120}: \"" + key + "\"");
    }
  }

  private static String sourceToString(Source in) throws IOException {
    try {
      return Okio.buffer(in).readUtf8();
    } finally {
      Util.closeQuietly(in);
    }
  }

  /** A snapshot of the values for an entry. */
  public final class Snapshot implements Closeable {
    private final String key;
    private final long sequenceNumber;
    private final Source[] sources;
    private final long[] lengths;

    private Snapshot(String key, long sequenceNumber, Source[] sources, long[] lengths) {
      this.key = key;
      this.sequenceNumber = sequenceNumber;
      this.sources = sources;
      this.lengths = lengths;
    }

    /**
     * Returns an editor for this snapshot's entry, or null if either the
     * entry has changed since this snapshot was created or if another edit
     * is in progress.
     */
    public Editor edit() throws IOException {
      return DiskLruCache.this.edit(key, sequenceNumber);
    }

    /** Returns the unbuffered stream with the value for {@code index}. */
    public Source getSource(int index) {
      return sources[index];
    }

    /** Returns the string value for {@code index}. */
    public String getString(int index) throws IOException {
      return sourceToString(getSource(index));
    }

    /** Returns the byte length of the value for {@code index}. */
    public long getLength(int index) {
      return lengths[index];
    }

    public void close() {
      for (Source in : sources) {
        Util.closeQuietly(in);
      }
    }
  }

  private static final Sink NULL_SINK = new Sink() {
    @Override public void write(Buffer source, long byteCount) throws IOException {
      // Eat all writes silently. Nom nom.
    }

    @Override public void flush() throws IOException {
    }

    @Override public Timeout timeout() {
      return Timeout.NONE;
    }

    @Override public void close() throws IOException {
    }
  };

  /** Edits the values for an entry. */
  public final class Editor {
    private final Entry entry;
    private final boolean[] written;
    private boolean hasErrors;
    private boolean committed;

    private Editor(Entry entry) {
      this.entry = entry;
      this.written = (entry.readable) ? null : new boolean[valueCount];
    }

    /**
     * Returns an unbuffered input stream to read the last committed value,
     * or null if no value has been committed.
     */
    public Source newSource(int index) throws IOException {
      synchronized (DiskLruCache.this) {
        if (entry.currentEditor != this) {
          throw new IllegalStateException();
        }
        if (!entry.readable) {
          return null;
        }
        try {
          return Okio.source(entry.cleanFiles[index]);
        } catch (FileNotFoundException e) {
          return null;
        }
      }
    }

    /**
     * Returns the last committed value as a string, or null if no value
     * has been committed.
     */
    public String getString(int index) throws IOException {
      Source source = newSource(index);
      return source != null ? sourceToString(source) : null;
    }

    /**
     * Returns a new unbuffered output stream to write the value at
     * {@code index}. If the underlying output stream encounters errors
     * when writing to the filesystem, this edit will be aborted when
     * {@link #commit} is called. The returned output stream does not throw
     * IOExceptions.
     */
    public Sink newSink(int index) throws IOException {
      synchronized (DiskLruCache.this) {
        if (entry.currentEditor != this) {
          throw new IllegalStateException();
        }
        if (!entry.readable) {
          written[index] = true;
        }
        File dirtyFile = entry.dirtyFiles[index];
        Sink sink;
        try {
          sink = Okio.sink(dirtyFile);
        } catch (FileNotFoundException e) {
          // Attempt to recreate the cache directory.
          directory.mkdirs();
          try {
            sink = Okio.sink(dirtyFile);
          } catch (FileNotFoundException e2) {
            // We are unable to recover. Silently eat the writes.
            return NULL_SINK;
          }
        }
        return new FaultHidingSink(sink);
      }
    }

    /** Sets the value at {@code index} to {@code value}. */
    public void set(int index, String value) throws IOException {
      BufferedSink writer = Okio.buffer(newSink(index));
      writer.writeUtf8(value);
      writer.close();
    }

    /**
     * Commits this edit so it is visible to readers.  This releases the
     * edit lock so another edit may be started on the same key.
     */
    public void commit() throws IOException {
      synchronized (DiskLruCache.this) {
        if (hasErrors) {
          completeEdit(this, false);
          removeEntry(entry); // The previous entry is stale.
        } else {
          completeEdit(this, true);
        }
        committed = true;
      }
    }

    /**
     * Aborts this edit. This releases the edit lock so another edit may be
     * started on the same key.
     */
    public void abort() throws IOException {
      synchronized (DiskLruCache.this) {
        completeEdit(this, false);
      }
    }

    public void abortUnlessCommitted() {
      synchronized (DiskLruCache.this) {
        if (!committed) {
          try {
            completeEdit(this, false);
          } catch (IOException ignored) {
          }
        }
      }
    }

    private class FaultHidingSink extends ForwardingSink {
      public FaultHidingSink(Sink delegate) {
        super(delegate);
      }

      @Override public void write(Buffer source, long byteCount) throws IOException {
        try {
          super.write(source, byteCount);
        } catch (IOException e) {
          synchronized (DiskLruCache.this) {
            hasErrors = true;
          }
        }
      }

      @Override public void flush() throws IOException {
        try {
          super.flush();
        } catch (IOException e) {
          synchronized (DiskLruCache.this) {
            hasErrors = true;
          }
        }
      }

      @Override public void close() throws IOException {
        try {
          super.close();
        } catch (IOException e) {
          synchronized (DiskLruCache.this) {
            hasErrors = true;
          }
        }
      }
    }
  }

  private final class Entry {
    private final String key;

    /** Lengths of this entry's files. */
    private final long[] lengths;
    private final File[] cleanFiles;
    private final File[] dirtyFiles;

    /** True if this entry has ever been published. */
    private boolean readable;

    /** The ongoing edit or null if this entry is not being edited. */
    private Editor currentEditor;

    /** The sequence number of the most recently committed edit to this entry. */
    private long sequenceNumber;

    private Entry(String key) {
      this.key = key;

      lengths = new long[valueCount];
      cleanFiles = new File[valueCount];
      dirtyFiles = new File[valueCount];

      // The names are repetitive so re-use the same builder to avoid allocations.
      StringBuilder fileBuilder = new StringBuilder(key).append('.');
      int truncateTo = fileBuilder.length();
      for (int i = 0; i < valueCount; i++) {
        fileBuilder.append(i);
        cleanFiles[i] = new File(directory, fileBuilder.toString());
        fileBuilder.append(".tmp");
        dirtyFiles[i] = new File(directory, fileBuilder.toString());
        fileBuilder.setLength(truncateTo);
      }
    }

    public String getLengths() throws IOException {
      StringBuilder result = new StringBuilder();
      for (long size : lengths) {
        result.append(' ').append(size);
      }
      return result.toString();
    }

    /** Set lengths using decimal numbers like "10123". */
    private void setLengths(String[] strings) throws IOException {
      if (strings.length != valueCount) {
        throw invalidLengths(strings);
      }

      try {
        for (int i = 0; i < strings.length; i++) {
          lengths[i] = Long.parseLong(strings[i]);
        }
      } catch (NumberFormatException e) {
        throw invalidLengths(strings);
      }
    }

    private IOException invalidLengths(String[] strings) throws IOException {
      throw new IOException("unexpected journal line: " + Arrays.toString(strings));
    }
  }
}
