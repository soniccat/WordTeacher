package tools

import (
	"go.mongodb.org/mongo-driver/bson/primitive"
	"time"
)

func Ptr[T any](x T) *T {
	return &x
}

func ApiDateToDbDate(date string) (primitive.DateTime, error) {
	dateTime, err := time.Parse(time.RFC3339, date)
	if err != nil {
		return 0, err
	}

	return primitive.NewDateTimeFromTime(dateTime), nil
}

func ApiDatePtrToDbDatePtr(date *string) (*primitive.DateTime, error) {
	if date == nil {
		return nil, nil
	}

	dbDate, err := ApiDateToDbDate(*date)
	if err != nil {
		return nil, err
	}

	return &dbDate, err
}

func DbDateToApiDate(date primitive.DateTime) string {
	return date.Time().UTC().Format(time.RFC3339)
}

func DoubleSliceComparableEqual[T comparable](a [][]T, b [][]T) bool {
	if len(a) != len(b) {
		return false
	}
	for i, v := range a {
		if !SliceComparableEqual(v, (b)[i]) {
			return false
		}
	}
	return true
}

func SliceAppend[T any](s1 []T, s2 []T) []T {
	res := make([]T, 0, len(s1)+len(s2))
	for _, v := range s1 {
		res = append(res, v)
	}

	for _, v := range s2 {
		res = append(res, v)
	}

	return res
}

func SliceComparableEqual[T comparable](a []T, b []T) bool {
	if len(a) != len(b) {
		return false
	}
	for i, v := range a {
		if v != (b)[i] {
			return false
		}
	}
	return true
}

func Map[T, U any](data []T, f func(T) U) []U {
	res := make([]U, 0, len(data))

	for _, e := range data {
		res = append(res, f(e))
	}

	return res
}

func MapOrError[T, U any](data []T, f func(T) (U, error)) ([]U, error) {
	res := make([]U, 0, len(data))

	for _, e := range data {
		v, err := f(e)
		if err != nil {
			return nil, err
		}

		res = append(res, v)
	}

	return res, nil
}

func MapNotNilOrError[T, U any](data []T, f func(T) (*U, error)) ([]*U, error) {
	res := make([]*U, 0, len(data))

	for _, e := range data {
		v, err := f(e)
		if err != nil {
			return nil, err
		}

		if v != nil {
			res = append(res, v)
		}
	}

	return res, nil
}

func Filter[T any](s []T, f func(T) bool) []T {
	var r []T
	for _, v := range s {
		if f(v) {
			r = append(r, v)
		}
	}
	return r
}

func FindOrNil[T any](s []T, f func(T) bool) *T {
	for _, v := range s {
		if f(v) {
			return &v
		}
	}
	return nil
}

func Reduce[T, U any](s []T, init U, f func(U, T) U) U {
	r := init
	for _, v := range s {
		r = f(r, v)
	}
	return r
}
