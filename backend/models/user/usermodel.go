package user

import (
	"context"
	"encoding/json"
	"errors"
	"github.com/alexedwards/scs/v2"
	"go.mongodb.org/mongo-driver/bson"
	"go.mongodb.org/mongo-driver/bson/primitive"
	"go.mongodb.org/mongo-driver/mongo"
	"models/apphelpers"
	"models/logger"
	"models/mongowrapper"
	"models/userauthtoken"
	"models/usernetwork"
	"net/http"
)

// TODO: move in auth module
type UserModel struct {
	Logger         *logger.Logger
	UserCollection *mongo.Collection
	AuthTokens     *mongo.Collection
}

func NewUserModel(logger *logger.Logger, usersDatabase *mongo.Database) (*UserModel, error) {
	model := &UserModel{
		Logger:         logger,
		UserCollection: usersDatabase.Collection(mongowrapper.MongoCollectionUsers),
		AuthTokens:     usersDatabase.Collection(mongowrapper.MongoCollectionAuthTokens),
	}

	return model, nil
}

func (m *UserModel) FindGoogleUser(context context.Context, googleUserId *string) (*User, error) {
	var user = User{}

	err := m.UserCollection.FindOne(
		context,
		bson.M{
			"networks": bson.M{
				"$elemMatch": bson.M{
					"type":          usernetwork.Google,
					"networkUserId": *googleUserId,
				},
			},
		},
	).Decode(&user)
	if err == mongo.ErrNoDocuments {
		return nil, nil
	}

	return &user, err
}

func (m *UserModel) InsertUser(context context.Context, user *User) (*User, error) {
	res, err := m.UserCollection.InsertOne(context, user)
	if err != nil {
		return nil, err
	}

	objId := res.InsertedID.(primitive.ObjectID)

	var newUser = *user
	newUser.ID = objId

	return &newUser, nil
}

// TODO: move in auth module
func (m *UserModel) GenerateUserAuthToken(
	context context.Context,
	userId *primitive.ObjectID,
	networkType usernetwork.UserNetworkType,
	deviceId string,
) (*userauthtoken.UserAuthToken, error) {
	token, err := userauthtoken.Generate(userId, networkType, deviceId)
	if err != nil {
		return nil, err
	}

	return m.insertUserAuthToken(context, token)
}

// TODO: move in auth module
func (m *UserModel) insertUserAuthToken(
	context context.Context,
	token *userauthtoken.UserAuthToken,
) (*userauthtoken.UserAuthToken, error) {
	// Remove stale auth tokens
	_, err := m.AuthTokens.DeleteMany(
		context,
		bson.M{
			"deviceId": token.UserDeviceId,
		},
	)
	if err != nil {
		m.Logger.Error.Printf("InsertUserToken DeleteMany error %f", err.Error())
	}

	// Add the new one
	res, err := m.AuthTokens.InsertOne(
		context,
		token,
	)
	if err != nil {
		return nil, err
	}

	objId := res.InsertedID.(primitive.ObjectID)

	token.ID = &objId
	return token, nil
}

type ValidateSessionError struct {
	StatusCode int
	InnerError error
}

type TokenHolder interface {
	GetAccessToken() string
	GetRefreshToken() *string
}

func NewValidateSessionError(code int, err error) *ValidateSessionError {
	return &ValidateSessionError{
		StatusCode: code,
		InnerError: err,
	}
}

func (v *ValidateSessionError) Error() string {
	return v.InnerError.Error()
}

func ValidateSession[T TokenHolder](
	r *http.Request,
	sessionManager *scs.SessionManager,
) (*T, *ValidateSessionError) {
	_, err := r.Cookie(apphelpers.CookieSession)
	if err != nil {
		return nil, NewValidateSessionError(http.StatusBadRequest, err)
	}

	// Header params
	var deviceId = r.Header.Get(apphelpers.HeaderDeviceId)
	if len(deviceId) == 0 {
		return nil, NewValidateSessionError(http.StatusBadRequest, errors.New("invalid device id"))
	}

	// Body params
	var input T
	err = json.NewDecoder(r.Body).Decode(&input)
	if err != nil {
		return nil, NewValidateSessionError(http.StatusBadRequest, err)
	}

	userAuthToken, err := userauthtoken.Load(r.Context(), sessionManager)
	if err != nil {
		return nil, NewValidateSessionError(http.StatusServiceUnavailable, err)
	}

	if !userAuthToken.IsValid() {
		return nil, NewValidateSessionError(http.StatusUnauthorized, errors.New("invalid auth token"))
	}

	if !userAuthToken.IsMatched(
		input.GetAccessToken(),
		input.GetRefreshToken(),
		deviceId,
	) {
		return nil, NewValidateSessionError(http.StatusUnauthorized, errors.New("invalid auth token"))
	}

	return &input, nil
}
