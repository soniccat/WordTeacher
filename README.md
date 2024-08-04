# Word Teacher
![logo](./docs/images/logo.png)

A complete solution for learning english word and phrases with spaced repetition technique. There're Android application, Desktop application, backend part and some tools. The repository has everything to set up all the parts yourself.

There're terms used in the docs and the source code:  
* **Card** - a word or phrase with related definitions, synonyms and examples. When a word has different meanings each meaning should be put in a separate card.  
* **Card Set** - a list of cards.  
* **Article** - text put in the app and proccessed with [OpenNLP](https://opennlp.apache.org/). The common way of making an article is sharing a web page to the app.  
* **Space** - Word Teacher account you can sign in. If you do, all you card sets would be stored here with constant synching between your devices.  

## Applications

Apps are based on KMP. The main pattern is MVVM, Dagger2 DI is used. Each app screen is a feature and all of them with ViewModels are in [commonMain/features](./shared/src/commonMain/kotlin/com/aglushkov/wordteacher/shared/features). Their UI is in [composeSharedMain/features](./shared/src/composeSharedMain/kotlin/com/aglushkov/wordteacher/shared/features).

### Main Features

#### Definitions

TBD


## Backend

It's written in Go and split in microservices. 

TBD


## Roadmap

TBD