package api_dict

type Files struct {
	Dicts []Dict `json:"dicts"`
}

type Dict struct {
	Name        string `json:"name"`
	Description string `json:"description"`
	Url         string `json:"url"`
	FromLang    string `json:"fromLang"`
	ToLang      string `json:"toLang"`
	Size        int    `json:"size"`
}
