package tools

func Ptr[T any](x T) *T {
	return &x
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

func SliceComparablePtrEqual[T comparable](a []*T, b []*T) bool {
	if len(a) != len(b) {
		return false
	}
	for i, v := range a {
		if (*v) != *((b)[i]) {
			return false
		}
	}
	return true
}
