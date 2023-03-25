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
	for i := range a {
		if !SliceComparableEqual(a[i], (b)[i]) {
			return false
		}
	}
	return true
}

func SliceAppend[T any](s1 []T, s2 []T) []T {
	res := make([]T, 0, len(s1)+len(s2))
	for i := range s1 {
		res = append(res, s1[i])
	}

	for i := range s2 {
		res = append(res, s2[i])
	}

	return res
}

func SliceComparableEqual[T comparable](a []T, b []T) bool {
	if len(a) != len(b) {
		return false
	}
	for i := range a {
		if a[i] != (b)[i] {
			return false
		}
	}
	return true
}

func Map[T, U any](data []T, f func(T) U) []U {
	res := make([]U, 0, len(data))

	for i := range data {
		res = append(res, f(data[i]))
	}

	return res
}

func MapOrError[T, U any](data []T, f func(T) (U, error)) ([]U, error) {
	res := make([]U, 0, len(data))

	for i := range data {
		v, err := f(data[i])
		if err != nil {
			return nil, err
		}

		res = append(res, v)
	}

	return res, nil
}

func MapNotNilOrError[T, U any](data []T, f func(T) (*U, error)) ([]*U, error) {
	res := make([]*U, 0, len(data))

	for i := range data {
		v, err := f(data[i])
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
	for i := range s {
		v := s[i]
		if f(v) {
			r = append(r, v)
		}
	}
	return r
}

func FindOrNil[T any](s []T, f func(T) bool) *T {
	for i := range s {
		v := &s[i]
		if f(*v) {
			return v
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

func ParseObjectID(idString string) (*primitive.ObjectID, error) {
	var cardDbId *primitive.ObjectID
	if len(idString) != 0 {
		anId, err := primitive.ObjectIDFromHex(idString)
		if err != nil {
			return nil, err
		}
		cardDbId = &anId
	}

	return cardDbId, nil
}

func IdsToMongoIds(ids []string) ([]primitive.ObjectID, error) {
	return MapOrError(ids, func(hex string) (primitive.ObjectID, error) {
		id, err := primitive.ObjectIDFromHex(hex)
		return id, err
	})
}

func MapKeys[T comparable, V any](m map[T]V) []T {
	i := 0
	keys := make([]T, len(m))
	for k := range m {
		keys[i] = k
		i++
	}

	return keys
}

func MapValues[T comparable, V comparable](m map[T]V) []V {
	i := 0
	keys := make([]V, len(m))
	for _, v := range m {
		keys[i] = v
		i++
	}

	return keys
}

func ComparePtrs[T comparable](a *T, b *T) bool {
	if b != a && (b == nil || a == nil) {
		return false
	}
	if b != nil && a != nil && *b != *a {
		return false
	}

	return true
}

func CompareSlices[T comparable](a []T, b []T) bool {
	var aLen = len(a)
	if aLen != len(b) {
		return false
	}

	for i := 0; i < aLen; i++ {
		if a[i] != b[i] {
			return false
		}
	}

	return true
}
