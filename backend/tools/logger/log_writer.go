package logger

import (
	"context"
	"fmt"
	"io"
	"os"
	"strings"
	"sync/atomic"
	"time"
)

// writes logs, creates a new file every 24 hours, removes old files
// usess atomic.Int32 not to lock writing
// it's considered that files are read reagularly by promtail and sent to loki
type LogWriter struct {
	dirPath        string
	filePrefix     string
	topEntry       *fileEntry
	lastRotateTime time.Time
	errWriter      io.Writer
}

type fileEntry struct {
	path       string
	file       *os.File
	isClosed   bool
	writeCount atomic.Int32
	prevEntry  *fileEntry
}

func NewLogWriter(dirPath string, filePrefix string, errWriter io.Writer) (*LogWriter, error) {
	t := time.Now()
	entry, err := resolveEntry(dirPath, filePrefix, t)
	if err != nil {
		writeString(errWriter, "[LogWriter] can't resolve entry: %v", err)
		return nil, err
	}

	// remove 48h old files
	entries, err := os.ReadDir(dirPath)
	if err != nil {
		return nil, err
	}

	for _, e := range entries {
		f, err := e.Info()
		if err != nil {
			writeString(errWriter, "[LogWriter] can't get Info: %v", err)
			continue
		}

		if !f.Mode().IsRegular() || !strings.HasPrefix(f.Name(), filePrefix) {
			continue
		}

		if time.Since(f.ModTime()) > time.Duration(48)*time.Hour {
			p := dirPath + string(os.PathSeparator) + e.Name()
			err := os.Remove(p)
			if err != nil {
				writeString(errWriter, "[LogWriter] can't remove file %s: %v", p, err)
			}
		}
	}

	return &LogWriter{
		dirPath:        dirPath,
		filePrefix:     filePrefix,
		topEntry:       entry,
		lastRotateTime: t,
		errWriter:      errWriter,
	}, nil
}

func (w *LogWriter) ScheduleRotation(ctx context.Context) {
	go func() {
		ticker := time.NewTicker(24 * time.Hour)

		for {
			select {
			case <-ctx.Done():
				return
			case <-ticker.C:
				err := w.CreateNewTopEntryIfNeeded()
				if err != nil {
					writeString(w.errWriter, "[LogWriter] can't update top entry: %v", err)
				}
			}
		}
	}()
}

// Write satisfies the io.Writer interface.
func (w *LogWriter) Write(output []byte) (int, error) {
	entry := w.topEntry
	entry.writeCount.Add(1)
	n, err := entry.file.Write(output)
	entry.writeCount.Add(-1)

	return n, err
}

func (w *LogWriter) Cleanup() {
	if w.topEntry.prevEntry != nil {
		w.tryCleanup(w.topEntry.prevEntry)
	}
}

func (w *LogWriter) tryCleanup(entry *fileEntry) bool {
	if entry.prevEntry != nil {
		if !w.tryCleanup(entry.prevEntry) {
			return false
		} else {
			entry.prevEntry = nil
		}
	}

	if entry.writeCount.Load() == 0 {
		if !entry.isClosed {
			err := entry.file.Close()
			if err != nil {
				writeString(w.errWriter, "[LogWriter] can't close %s: %v", entry.path, err)
				return false
			}
			entry.isClosed = true
		}

		err := os.Remove(entry.path)
		if err != nil {
			writeString(w.errWriter, "[LogWriter] can't delete %s: %v", entry.path, err)
			return false
		}

		writeString(w.errWriter, "[LogWriter] closed %s", entry.path)
		return true
	}

	return false
}

func resolveEntry(dirPath string, prefix string, t time.Time) (*fileEntry, error) {
	path := dirPath + string(os.PathSeparator) + prefix + t.Format(time.RFC3339)
	f, err := os.OpenFile(path, os.O_APPEND|os.O_CREATE|os.O_WRONLY, 0644)
	if err != nil {
		return nil, err
	}

	return &fileEntry{
		path: path,
		file: f,
	}, err
}

// Perform the actual act of rotating
func (w *LogWriter) CreateNewTopEntryIfNeeded() (err error) {
	diff := time.Since(w.lastRotateTime)
	if diff < time.Duration(24)*time.Hour {
		return nil
	}

	newEntry, err := resolveEntry(w.dirPath, w.filePrefix, time.Now())
	if err != nil {
		return err
	}

	newEntry.prevEntry = w.topEntry
	w.topEntry = newEntry
	w.lastRotateTime = w.lastRotateTime.Add(diff)
	return nil
}

func writeString(w io.Writer, format string, a ...any) {
	w.Write([]byte(fmt.Sprintf(format, a...)))
}
