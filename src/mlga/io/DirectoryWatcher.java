package mlga.io;

import static java.nio.file.LinkOption.NOFOLLOW_LINKS;
import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;
import static java.nio.file.StandardWatchEventKinds.OVERFLOW;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.WatchEvent;
import java.nio.file.WatchEvent.Kind;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;

/**
 * Generic class for registering with the OS to watch directories for file
 * changes, and passing those events off the simple Java functions.
 *
 * @author ShadowMoose
 */
public class DirectoryWatcher implements Runnable {
	private final File dir;
	private final boolean spider;

	/**
	 * All possible filesystem changes that are listened for.
	 */
	public enum Event {
		/** A new File has been created. */
		CREATE,
		/** A File has been deleted. */
		DELETE,
		/** A File has been modified. */
		MODIFY,
		/** We aren't sure what this event is - we were unable to convert it. */
		UNKNOWN
	}

	/**
	 * Creates and starts a Threaded watcher, that will trigger this
	 * DirectoryWatcher's {@link #handle()} method with any files that are
	 * created or modified within the directory. <br>
	 * This watcher runs as a daemon, and will terminate when all other
	 * non-daemon Threads are complete.
	 *
	 * @param dir    - The base directory to watch.
	 * @param spider - If this Watcher should recursively scan for subdirectories.
	 *               <br>
	 *               If true, you <i>must</i> override {@link #followDir(File)} to
	 *               control which directories are accepted.
	 */
	public DirectoryWatcher(File dir, boolean spider) {
		this.dir = dir;
		this.spider = spider;
		Thread thread = new Thread(this, "FileWatcher-" + dir.getAbsolutePath() + "-" + Math.random());
		thread.setDaemon(true);
		thread.start();
	}

	@Override
	public void run() {
		watchDirectoryPath();
	}

	/**
	 * Creates and starts a Threaded watcher, that will trigger this
	 * DirectoryWatcher's {@link #handle(File, Event)} method with any files that are
	 * created or modified within the directory. <br>
	 * This watcher runs as a daemon, and will terminate when all other
	 * non-daemon Threads are complete.
	 */
	public DirectoryWatcher(File dir) {
		this(dir, false);
	}

	/**
	 * Main monitor loop, internal.
	 */
	private void watchDirectoryPath() {
		Path path = this.dir.toPath();
		try {
			Boolean isFolder = (Boolean) Files.getAttribute(path, "basic:isDirectory", NOFOLLOW_LINKS);
			if (!isFolder) {
				throw new IllegalArgumentException("Path: " + path + " is not a folder");
			}
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}

		FileSystem fs = path.getFileSystem();
		try (WatchService service = fs.newWatchService()) {
			if (!this.spider) {
				path.register(service, ENTRY_CREATE, ENTRY_MODIFY, ENTRY_DELETE);
			} else {
				Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
					public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
						File f = dir.toFile();
						if (f.isDirectory() && followDir(f))
							dir.register(service, ENTRY_CREATE, ENTRY_MODIFY);
						return FileVisitResult.CONTINUE;
					}
				});
			}
			// Start infinite polling loop
			WatchKey key = null;
			while (true) {
				key = service.take();

				for (WatchEvent<?> watchEvent : key.pollEvents()) {
					if (watchEvent.kind() == OVERFLOW) {
						// Ignore misc events.
						continue;
					} else {
						// File was modified/created.
						// Assemble a complete, absolute path:
						Path dir = (Path) key.watchable();
						Path fullPath = dir.resolve((Path) watchEvent.context());

						// Pass fully-built absolute filepath off to 
						// save handler function:
						this.handle(fullPath.toFile(), kindToEvent(watchEvent.kind()));
					}
				}

				if (!key.reset()) {
					break;
				}
			}
		} catch (IOException | InterruptedException ioe) {
			ioe.printStackTrace();
		}

	}

	/**
	 * To avoid import crazyness, we simply provide our own custom wrapper for
	 * Kinds, which we convert to here.
	 *
	 * @param k The Kind to convert.
	 */
	private Event kindToEvent(Kind<?> k) {
		if (k == ENTRY_CREATE)
			return Event.CREATE;
		if (k == ENTRY_DELETE)
			return Event.DELETE;
		if (k == ENTRY_MODIFY)
			return Event.MODIFY;
		return Event.UNKNOWN;
	}

	/**
	 * Called automatically whenever a File is deleted, modified, or created
	 * within this Watcher's Directory.
	 *
	 * @param f - The File that was added or changed. <b>This file may not
	 *          exist on a delete.</b>
	 * @param e - The {@linkplain Event} that triggered this event.
	 */
	public void handle(File f, Event e) {
	}

	/**
	 * Called if this Watcher is to "spider", and recursively check directories.
	 * <br>
	 * This method tells the Watcher which File Directories it should be allowed
	 * to watch. <br>
	 * This method should be overridden if "spider" is used.
	 *
	 * @param base - A File Directory that the Spider is considering monitoring.
	 *
	 * @return True if this File should be followed. <br>
	 * default false
	 */
	public boolean followDir(File base) {
		return false;
	}
}
