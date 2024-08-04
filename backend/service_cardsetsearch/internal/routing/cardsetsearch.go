package routing

import (
	"api"
	"errors"
	"models"
	"models/session_validator"
	"net/http"
	"service_cardsetsearch/internal/storage"
	"tools/logger"

	"tools"

	"github.com/google/uuid"
)

const (
	ArgumentQuery        = "query"
	MaxDistanceInCluster = 3
)

type CardSetSearchResponse struct {
	CardSets []*api.CardSet `json:"cardSets,omitempty"`
}

type CardSetSearchHandler struct {
	tools.BaseHandler
	sessionValidator        session_validator.SessionValidator
	cardSetSearchRepository *storage.Repository
}

type cluster struct {
	cardSets []*api.CardSet
}

func NewCluster() *cluster {
	return &cluster{
		cardSets: make([]*api.CardSet, 0),
	}
}

func (h *cluster) add(cardSet *api.CardSet) {
	h.cardSets = append(h.cardSets, cardSet)
}

func (h *cluster) distance(cardSet *api.CardSet) int {
	maxDistance := 0
	for _, v := range h.cardSets {
		d := cardSetDistance(v, cardSet)
		if d > maxDistance {
			maxDistance = d
		}
	}

	return maxDistance
}

func NewCardSetSearchHandler(
	logger *logger.Logger,
	timeProvider tools.TimeProvider,
	sessionValidator session_validator.SessionValidator,
	cardSetSearchRepository *storage.Repository,
) *CardSetSearchHandler {
	return &CardSetSearchHandler{
		BaseHandler:             *tools.NewBaseHandler(logger, timeProvider),
		sessionValidator:        sessionValidator,
		cardSetSearchRepository: cardSetSearchRepository,
	}
}

func (h *CardSetSearchHandler) CardSetSearch(w http.ResponseWriter, r *http.Request) {
	authToken, _ := h.sessionValidator.Validate(r) // get authToken just for logging

	if r.Body == nil {
		h.SetError(w, errors.New("body is empty"), http.StatusBadRequest)
		return
	}

	var query = r.URL.Query().Get(ArgumentQuery)
	if len(query) == 0 {
		h.SetError(w, errors.New("query is empty"), http.StatusBadRequest)
		return
	}

	ctx := logger.WrapContext(
		r.Context(),
		append(
			[]any{
				"logId", uuid.NewString(),
				"query", query,
			},
			models.LogParams(authToken, r.Header)...,
		),
	)

	cardSets, err := h.cardSetSearchRepository.SearchCardSets(ctx, query, 100)
	if err != nil {
		h.SetError(w, err, http.StatusInternalServerError)
		return
	}

	// TODO: consider returning cluster with grouped card sets
	cardSets = clusteredCardSets(cardSets)

	response := CardSetSearchResponse{
		CardSets: cardSets,
	}
	h.WriteResponse(w, response)
}

func clusteredCardSets(cardSets []*api.CardSet) []*api.CardSet {
	// trivial cardsets clusterizing taking into account initial search sort relevance
	// so, just form cluster moving from left to right and marking new members as taken in takenCardSets
	clusterCount := 0
	clusters := make([]*cluster, len(cardSets))
	takenCardSets := make([]bool, len(cardSets))

	for a := range cardSets {
		if takenCardSets[a] {
			continue
		}

		cluster := NewCluster()
		cluster.add(cardSets[a])
		clusters[a] = cluster
		takenCardSets[a] = true
		clusterCount += 1

		for b := range cardSets {
			if a == b {
				continue
			}

			d := cluster.distance(cardSets[b])
			if d <= MaxDistanceInCluster {
				cluster.add(cardSets[b])
				takenCardSets[b] = true
			}
		}
	}

	winnerCardSets := make([]*api.CardSet, 0, clusterCount)
	for _, v := range clusters {
		if v == nil {
			continue
		}

		// for now consider the first card set is most relevant in a cluster
		// but there could be more words in other sets...
		winnerCardSets = append(winnerCardSets, v.cardSets[0])
	}

	return winnerCardSets
}

func cardSetDistance(a *api.CardSet, b *api.CardSet) int {
	diff := 0
	ai := 0
	bi := 0
	al := len(a.Terms)
	bl := len(b.Terms)

	// use the fact that terms are sorted
	if al > 0 && bl > 0 {
		for {
			if a.Terms[ai] == b.Terms[bi] {
				ai += 1
				bi += 1
			} else if a.Terms[ai] < b.Terms[bi] {
				bi += 1
				diff += 1
			} else {
				ai += 1
				diff += 1
			}

			if ai == al || bi == bl {
				break
			}
		}
	}

	// count left items
	diff += (al - ai) + (bl - bi)
	return diff
}
