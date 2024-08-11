# Word Teacher
![logo](./docs/images/logo.png)

A complete solution for learning english word and phrases with spaced repetition technique. There're Android application, Desktop application, backend part and some tools. The repository has everything to set up all the parts yourself.

There're terms I use in the docs and the source code:  
* **Card** - a word or phrase with related definitions, synonyms and examples. Word Teacher tracks your learning progress for each card separately. When a word you want to start learning has different meanings, it's preferable to create a card per a meaning.  
* **Card Set** - a list of cards grouped by a topic in the title. Also a description and a source url could be set.   
* **Article** - text put in the app and proccessed with [OpenNLP](https://opennlp.apache.org/). The common way of making an article is sharing a web page to the app.  
* **Space** - Word Teacher account you can sign in. If you do, all your card sets would be stored here with constant synching between your devices. Also your shared card sets could be found by other users while searching.

## Applications

Apps are based on KMP to be able to share most of the logic between platforms. The main pattern is MVVM with UDF in mind. Dagger2 DI is used because of it compile time dependency checking. 

Each app screen is a feature and all of them with ViewModels are in [commonMain/features](./shared/src/commonMain/kotlin/com/aglushkov/wordteacher/shared/features). Their UI is in [composeSharedMain/features](./shared/src/composeSharedMain/kotlin/com/aglushkov/wordteacher/shared/features).

### Main Features

#### Definitions

A screen where you can search for a word and find its definitions, examples, synonyms and antonyms. If you add your DSL dictionaries, searching will work through them too with showing live suggestions while typing.



## Backend

It's written in Go and split in microservices. 

TBD


## Roadmap

TBD