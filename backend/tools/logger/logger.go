package logger

import (
	"context"
	"errors"
	"fmt"
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
const errorKey = "error"

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
					// AddSource:   true,
					Level:       minimumLevel,
					ReplaceAttr: replacer,
				},
			),
		),
	}
}

func (l *Logger) Info(ctx context.Context, msg string, args ...any) {
	if !l.log.Enabled(ctx, slog.LevelInfo) {
		return
	}

	l.log.InfoContext(ctx, msg, l.buildArgsFromContext(ctx, args...)...)
}

func (l *Logger) InfoWithError(ctx context.Context, err error, msg string, args ...any) {
	if !l.log.Enabled(ctx, slog.LevelInfo) {
		return
	}

	l.log.InfoContext(ctx, msg, l.buildArgsFromError(err, args...)...)
}

func (l *Logger) Warn(ctx context.Context, msg string, args ...any) {
	if !l.log.Enabled(ctx, slog.LevelInfo) {
		return
	}

	l.log.WarnContext(ctx, msg, l.buildArgsFromContext(ctx, args...)...)
}

func (l *Logger) WarnWithError(ctx context.Context, err error, msg string, args ...any) {
	if !l.log.Enabled(ctx, slog.LevelInfo) {
		return
	}

	l.log.WarnContext(ctx, msg, l.buildArgsFromError(err, args...)...)
}

func (l *Logger) Debug(ctx context.Context, msg string, args ...any) {
	if !l.log.Enabled(ctx, slog.LevelInfo) {
		return
	}

	l.log.DebugContext(ctx, msg, l.buildArgsFromContext(ctx, args...)...)
}

func (l *Logger) DebugWithError(ctx context.Context, err error, msg string, args ...any) {
	if !l.log.Enabled(ctx, slog.LevelInfo) {
		return
	}

	l.log.DebugContext(ctx, msg, l.buildArgsFromError(err, args...)...)
}

func (l *Logger) Error(ctx context.Context, msg string, args ...any) {
	if !l.log.Enabled(ctx, slog.LevelInfo) {
		return
	}

	l.log.ErrorContext(ctx, msg, l.buildArgsFromContext(ctx, args...)...)
}

func (l *Logger) ErrorWithError(ctx context.Context, err error, msg string, args ...any) {
	if !l.log.Enabled(ctx, slog.LevelInfo) {
		return
	}

	l.log.ErrorContext(ctx, msg, l.buildArgsFromError(err, args...)...)
}

// Tools

func (l *Logger) buildArgsFromContext(ctx context.Context, args ...any) []any {
	resultArgs := make([]any, 0, len(args))
	resultArgs = append(resultArgs, args...)

	lCtx, ok := ctx.Value(logCtxKey).(logCtx)
	if ok {
		resultArgs = l.buildArgsFromLContext(lCtx, resultArgs)
	}
	return resultArgs
}

func (l *Logger) buildArgsFromError(err error, args ...any) []any {
	resultArgs := make([]any, 0, len(args))
	resultArgs = append(resultArgs, errorKey, fmt.Errorf("%w", err))
	resultArgs = append(resultArgs, args...)

	var lError LogError
	if errors.As(err, &lError) {
		resultArgs = append(resultArgs, stackKey, string(lError.stack))

		if lError.lCtx != nil {
			resultArgs = l.buildArgsFromLContext(*lError.lCtx, resultArgs)
		}
	}
	return resultArgs
}

func (*Logger) buildArgsFromLContext(lCtx logCtx, dst []any) []any {
	innerLCtx := &lCtx
	for innerLCtx != nil {
		for k, v := range innerLCtx.params {
			dst = append(dst, k, v)
		}

		innerLCtx = innerLCtx.parent
	}

	return dst
}

func WrapContext(ctx context.Context, args ...any) context.Context {
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

func Error(ctx context.Context, str string) error {
	return WrapError(ctx, errors.New(str))
}

func WrapError(ctx context.Context, err error) error {
	if err == nil {
		return nil
	}

	var lError LogError
	if errors.As(err, &lError) {
		return err
	}

	var lCtx *logCtx
	if c, ok := ctx.Value(logCtxKey).(logCtx); ok {
		lCtx = &c
	}

	// var pcs [1]uintptr
	// runtime.Callers(2, pcs[:]) // skip [Callers, Info]
	return LogError{
		lCtx:       lCtx,
		innerError: err,
		stack:      debug.Stack(), // TODO: try runtime.CallersFrames()/runtime.Callers()
	}
}

func (le LogError) Error() string {
	return le.innerError.Error()
}

func (le LogError) Unwrap() error {
	return le.innerError
}
