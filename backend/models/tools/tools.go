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

//func SliceComparablePtrEqual[T comparable](a []*T, b []*T) bool {
//	if len(a) != len(b) {
//		return false
//	}
//	for i, v := range a {
//		if (*v) != *((b)[i]) {
//			return false
//		}
//	}
//	return true
//}

func Map[T, U any](data []T, f func(T) U) []U {
	res := make([]U, 0, len(data))

	for _, e := range data {
		res = append(res, f(e))
	}

	return res
}

func MapOrError[T, U any](data []T, f func(*T) (U, error)) ([]U, error) {
	res := make([]U, 0, len(data))

	for _, e := range data {
		v, err := f(&e)
		if err != nil {
			return nil, err
		}

		res = append(res, v)
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

func FindOrNil[T any](s []*T, f func(*T) bool) *T {
	for _, v := range s {
		if f(v) {
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
