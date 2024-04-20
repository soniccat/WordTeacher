package logger

import (
	"context"
	"errors"
	"io"
	"log/slog"
	"path/filepath"
	"runtime/debug"
)

type Logger struct {
	log *slog.Logger
}

type logCtxKeyType string

const logCtxKey logCtxKeyType = "logCtxKey"
const invalidLogParamKey = "invalidKey"
const stackKey = "stack"

type logCtx struct {
	parent *logCtx
	params map[string]any
}

func New(w io.Writer, minimumLevel slog.Level) *Logger {
	replacer := func(groups []string, a slog.Attr) slog.Attr {
		// Remove the directory from the source's filename.
		if a.Key == slog.SourceKey {
			source := a.Value.Any().(*slog.Source)
			source.File = filepath.Base(source.File)
		}
		return a
	}

	return &Logger{
		log: slog.New(
			slog.NewJSONHandler(
				w,
				&slog.HandlerOptions{
					AddSource:   true,
					Level:       minimumLevel,
					ReplaceAttr: replacer,
				},
			),
		),
	}
}

func (l *Logger) Info(ctx context.Context, msg string, args ...any) {
	l.InfoWithError(ctx, nil, msg, args...)
}

func (l *Logger) InfoWithError(ctx context.Context, err error, msg string, args ...any) {
	if !l.log.Enabled(ctx, slog.LevelInfo) {
		return
	}

	l.log.InfoContext(ctx, msg, l.buildArgs(err, args)...)

	// var pcs [1]uintptr
	// runtime.Callers(2, pcs[:]) // skip [Callers, Info]
	// r := slog.NewRecord(time.Now(), slog.LevelInfo, fmt.Sprintf(format, args...), pcs[0])
	// _ = l.log.Handler().Handle(ctx, r)
	// l.log.Info()
}

func (*Logger) buildArgs(err error, args []any) []any {
	resultArgs := make([]any, 0, len(args))
	resultArgs = append(resultArgs, args...)

	var lError LogError
	if errors.As(err, &lError) {
		resultArgs = append(resultArgs, stackKey, string(lError.stack))

		if lError.lCtx != nil {
			lCtx := lError.lCtx
			for lCtx != nil {
				for k, v := range lCtx.params {
					resultArgs = append(resultArgs, k, v)
				}

				lCtx = lCtx.parent
			}
		}
	}
	return resultArgs
}

// func buildSlogArgs(ctx context.Context, args ...any) []any {
// 	resultArgs := make([]any, 0, len(args))

// 	for i, _ := range args {

// 		resultArgs = append(resultArgs)
// 	}

// 	return resultArgs
// }

// func convertArgToSlogArg(in any) any {
// 	switch x := in.(type) {
// 	case string:
// 		return x
// 	case error:
// 		return fmt.Errorf("error: %w", x)
// 	default:
// 		return in
// 	}
// }

// func buildRecord(ctx context.Context, format string, args ...any) slog.Record {
// 	lCtx, ok := ctx.Value(logCtxKey).(logCtx)

// }

func WithLogParams(ctx context.Context, args ...any) context.Context {
	var parentLogCtx *logCtx
	if lCtx, ok := ctx.Value(logCtxKey).(logCtx); ok {
		parentLogCtx = &lCtx
	}

	params := make(map[string]any)
	putLogParamsFromArgs(params, args...)

	return context.WithValue(
		ctx,
		logCtxKey,
		logCtx{
			parent: parentLogCtx,
			params: params,
		},
	)
}

func putLogParamsFromArgs(m map[string]any, args ...any) {
	i := 0
	l := len(args)

	for i < l {
		if i+1 < l {
			m[args[i].(string)] = args[i+1]
			i += 2
		} else {
			m[invalidLogParamKey] = args[i]
			i++
		}
	}
}

type LogError struct {
	lCtx       *logCtx
	innerError error
	stack      []byte
}

func NewLogError(ctx context.Context, err error) LogError {
	var lCtx *logCtx
	if c, ok := ctx.Value(logCtxKey).(logCtx); ok {
		lCtx = &c
	}

	return LogError{
		lCtx:       lCtx,
		innerError: err,
		stack:      debug.Stack(),
	}
}

func (le LogError) Error() string {
	return le.innerError.Error()
}
