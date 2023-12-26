package time_provider

import "time"

type TimeProvider struct {
}

func (tp *TimeProvider) Now() time.Time {
	return time.Now().UTC().Truncate(time.Millisecond)
}
