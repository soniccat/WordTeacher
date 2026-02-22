package storage

import (
	"reflect"
	"testing"
)

func TestReadTags(t *testing.T) {
	params := []struct {
		name           string
		inQuery        string
		expectTags     []string
		expectOutQuery string
	}{
		{
			"just a text",
			"this is just a text",
			[]string{},
			"this is just a text",
		},
		{
			"with one tag",
			"#tag this is just a text",
			[]string{"tag"},
			"this is just a text",
		},
		{
			"with two tags",
			"#tag1 #tag2 this is just a text",
			[]string{"tag1", "tag2"},
			"this is just a text",
		},
		{
			"with two tags and no spaces",
			"#tag1#tag2 this is just a text",
			[]string{"tag1", "tag2"},
			"this is just a text",
		},
		{
			"with two tags separated by a comma",
			"#tag1,#tag2 this is just a text",
			[]string{"tag1", "tag2"},
			"this is just a text",
		},
		{
			"with two tags and space in front",
			" #tag1#tag2 this is just a text",
			[]string{"tag1", "tag2"},
			"this is just a text",
		},
	}

	for _, p := range params {
		t.Run(p.name, func(tt *testing.T) {
			rTags, rOutQuery := readTags(p.inQuery)

			if !reflect.DeepEqual(rTags, p.expectTags) {
				tt.Errorf("Tags aren't the same %v != %v", rTags, p.expectTags)
			}

			if rOutQuery != p.expectOutQuery {
				tt.Errorf("outQuery isn't the same %v != %v", rOutQuery, p.expectOutQuery)
			}
		})
	}
}
