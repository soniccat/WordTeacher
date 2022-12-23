package cardset

import (
	"go.mongodb.org/mongo-driver/bson/primitive"
	"models/card"
	"models/tools"
	"time"
)

type ApiCardSet struct {
	Id               string          `json:"id,omitempty"`
	Name             string          `json:"name"`
	Cards            []*card.ApiCard `json:"cards"`
	UserId           string          `json:"userId"` // TODO: consider several owners via a permission filed
	CreationDate     string          `json:"creationDate"`
	ModificationDate *string         `json:"modificationDate,omitempty"`
	CreationId       string          `json:"creationId"`
}

func (cs *ApiCardSet) IsEqual(a *ApiCardSet) bool {
	if cs.Id != a.Id {
		return false
	}
	if cs.Name != a.Name {
		return false
	}
	if cs.UserId != a.UserId {
		return false
	}
	if cs.CreationDate != a.CreationDate {
		return false
	}
	if cs.ModificationDate != a.ModificationDate {
		return false
	}
	if cs.CreationId != a.CreationId {
		return false
	}
	if len(cs.Cards) != len(a.Cards) {
		return false
	}
	for i, c := range cs.Cards {
		if !c.IsEqual(a.Cards[i]) {
			return false
		}
	}

	return true
}

func (cs *ApiCardSet) toDb() (*DbCardSet, error) {
	id, err := tools.ParseObjectID(cs.Id)
	if err != nil {
		return nil, err
	}

	userId, err := tools.ParseObjectID(cs.UserId)
	if err != nil {
		return nil, err
	}

	creationDate, err := tools.ApiDateToDbDate(cs.CreationDate)
	if err != nil {
		return nil, err
	}

	modificationDateTime, err := tools.ApiDatePtrToDbDatePtr(cs.ModificationDate)
	if err != nil {
		return nil, err
	}

	cardSetDbs, err := tools.MapOrError(cs.Cards, func(card *card.ApiCard) (*card.DbCard, error) {
		return card.ToDb()
	})
	if err != nil {
		return nil, err
	}

	cardSetDb := &DbCardSet{
		Id:               id,
		Name:             cs.Name,
		Cards:            cardSetDbs,
		UserId:           userId,
		CreationDate:     creationDate,
		ModificationDate: modificationDateTime,
		CreationId:       cs.CreationId,
	}
	return cardSetDb, nil
}

type ApiCardSetSortByName []*ApiCardSet

func (a ApiCardSetSortByName) Len() int           { return len(a) }
func (a ApiCardSetSortByName) Swap(i, j int)      { a[i], a[j] = a[j], a[i] }
func (a ApiCardSetSortByName) Less(i, j int) bool { return a[i].Name < a[j].Name }

type DbCardSet struct {
	Id               *primitive.ObjectID `bson:"_id,omitempty"`
	Name             string              `bson:"name"`
	Cards            []*card.DbCard      `bson:"cards"`
	UserId           *primitive.ObjectID `bson:"userId"`
	CreationDate     primitive.DateTime  `bson:"creationDate"`
	ModificationDate *primitive.DateTime `bson:"modificationDate,omitempty"`
	CreationId       string              `bson:"creationId"`
}

func (cs *DbCardSet) ToApi() *ApiCardSet {
	var md *string
	if cs.ModificationDate != nil {
		md = tools.Ptr(cs.ModificationDate.Time().UTC().Format(time.RFC3339))
	}

	return &ApiCardSet{
		Id:               cs.Id.Hex(),
		Name:             cs.Name,
		Cards:            tools.Map(cs.Cards, func(cardDb *card.DbCard) *card.ApiCard { return cardDb.ToApi() }),
		UserId:           cs.UserId.Hex(),
		CreationDate:     cs.CreationDate.Time().UTC().Format(time.RFC3339),
		ModificationDate: md,
		CreationId:       cs.CreationId,
	}
}

func DbCardSetsToApi(cs []*DbCardSet) []*ApiCardSet {
	return tools.Map(cs, func(cardSetDb *DbCardSet) *ApiCardSet {
		return cardSetDb.ToApi()
	})
}
