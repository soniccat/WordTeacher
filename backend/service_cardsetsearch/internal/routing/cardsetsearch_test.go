package routing

import (
	"api"
	"testing"

	"github.com/stretchr/testify/assert"
)

func TestCardSetDistance(t *testing.T) {

	testCases := []struct {
		name             string
		cs1              api.CardSet
		cs2              api.CardSet
		expectedDistance int
	}{
		{
			name: "no terms",
			cs1: api.CardSet{
				Terms: []string{},
			},
			cs2: api.CardSet{
				Terms: []string{},
			},
			expectedDistance: 0,
		},
		{
			name: "second card set is empty",
			cs1: api.CardSet{
				Terms: []string{
					"abc",
				},
			},
			cs2: api.CardSet{
				Terms: []string{},
			},
			expectedDistance: 1,
		},
		{
			name: "first card set is empty",
			cs1: api.CardSet{
				Terms: []string{},
			},
			cs2: api.CardSet{
				Terms: []string{
					"abc",
				},
			},
			expectedDistance: 1,
		},
		{
			name: "the same terms",
			cs1: api.CardSet{
				Terms: []string{
					"abc", "abc2", "abc3",
				},
			},
			cs2: api.CardSet{
				Terms: []string{
					"abc", "abc2", "abc3",
				},
			},
			expectedDistance: 0,
		},
		{
			name: "more terms in the second card set",
			cs1: api.CardSet{
				Terms: []string{
					"abc",
				},
			},
			cs2: api.CardSet{
				Terms: []string{
					"abc", "abc2", "abc3",
				},
			},
			expectedDistance: 2,
		},
		{
			name: "more terms in the first card set",
			cs1: api.CardSet{
				Terms: []string{
					"abc", "abc2", "abc3",
				},
			},
			cs2: api.CardSet{
				Terms: []string{
					"abc",
				},
			},
			expectedDistance: 2,
		},
		{
			name: "terms are completely different",
			cs1: api.CardSet{
				Terms: []string{
					"abc", "abc2", "abc3",
				},
			},
			cs2: api.CardSet{
				Terms: []string{
					"cba", "cba2", "cba3",
				},
			},
			expectedDistance: 6,
		},
	}

	for _, c := range testCases {
		t.Run(
			c.name,
			func(tt *testing.T) {
				d := cardSetDistance(&c.cs1, &c.cs2)
				assert.Equal(tt, c.expectedDistance, d)
			},
		)
	}
}

func TestClusteredCardSets(t *testing.T) {
	testCases := []struct {
		name           string
		cardSets       []*api.CardSet
		resultCardSets []*api.CardSet
	}{
		{
			name: "2 clusters",
			cardSets: []*api.CardSet{
				{
					Terms: []string{
						"abc", "abc2", "abc3",
					},
				},
				{
					Terms: []string{
						"cba1", "cba2", "cba3",
					},
				},
				{
					Terms: []string{
						"abc", "abc2",
					},
				},
				{
					Terms: []string{
						"cba1", "cba2",
					},
				},
			},
			resultCardSets: []*api.CardSet{
				{
					Terms: []string{
						"abc", "abc2", "abc3",
					},
				},
				{
					Terms: []string{
						"cba1", "cba2", "cba3",
					},
				},
			},
		},
		{
			name: "1 big cluster an 1 small cluster",
			cardSets: []*api.CardSet{
				{
					Terms: []string{
						"abc", "abc2",
					},
				},
				{
					Terms: []string{
						"cba1", "cba2", "cba3",
					},
				},
				{
					Terms: []string{
						"abc", "abc2", "abc3", "abc4",
					},
				},
				{
					Terms: []string{
						"abc", "abc2", "abc3", "abc4", "abc5",
					},
				},
				{
					Terms: []string{
						"abc", "abc2", "abc3", "abc4", "abc5", "abc6",
					},
				},
			},
			resultCardSets: []*api.CardSet{
				{
					Terms: []string{
						"abc", "abc2",
					},
				},
				{
					Terms: []string{
						"cba1", "cba2", "cba3",
					},
				},
				{
					Terms: []string{
						"abc", "abc2", "abc3", "abc4", "abc5", "abc6",
					},
				},
			},
		},
	}

	for _, c := range testCases {
		t.Run(
			c.name,
			func(tt *testing.T) {
				cardSets := clusteredCardSets(c.cardSets)
				assert.Equal(tt, c.resultCardSets, cardSets)
			},
		)
	}
}
