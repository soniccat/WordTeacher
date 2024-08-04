package com.aglushkov.wordteacher.android_app.features.cardsets.views

//@OptIn(ExperimentalAnimationApi::class, ExperimentalMaterialApi::class)
//@Preview
//@Composable
//fun CardSetTitleViewPreviews() {
//    CardSetItemView(
//        CardSetViewItem(
//            setId = 0L,
//            name = "My card set",
//            date = "Today",
//            readyToLearnProgress = 0.3f,
//            totalProgress = 0.1f,
//        )
//    )
//}

//@ExperimentalAnimationApi
//@ExperimentalMaterialApi
//@ExperimentalComposeUiApi
//@Preview
//@Composable
//fun CardSetsUIPreviewWithArticles() {
//    ComposeAppTheme {
//        CardSetsUI(
//            vm = CardSetsVM(
//                articles = Resource.Loaded(
//                    data = listOf(
//                        ArticleViewItem(1, "Article Name", "Today")
//                    )
//                )
//            )
//        )
//    }
//}

//@Composable
//fun importDBButton() {
//    val context = LocalContext.current
//    val launcher = rememberLauncherForActivityResult(contract = ActivityResultContracts.GetContent()) { result ->
//        result?.let {
//            val byteArray = ByteArray(DEFAULT_BUFFER_SIZE)
//            val importPath = context.getDatabasePath("test2").absolutePath.toPath()
//            context.contentResolver.openInputStream(result)?.buffered()?.use { stream ->
//                FileSystem.SYSTEM.write(importPath, true) {
//                    while (stream.read(byteArray) != -1) {
//                        write(byteArray)
//                    }
//                }
//            }
//
//            val dbPath = context.getDatabasePath("wt.db").absolutePath.toPath()
//            FileSystem.SYSTEM.delete(dbPath)
//            FileSystem.SYSTEM.atomicMove(importPath, dbPath)
//        }
//    }
//
//    return Column {
//        Button(onClick = {
//            launcher.launch("*/*")
//        }) {
//            Text("Import db file")
//        }
//    }
//}
