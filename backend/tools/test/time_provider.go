package test

import "time"

type TimeProvider struct {
	Time time.Time
}

func (t *TimeProvider) Now() time.Time {
	return t.Time
}
