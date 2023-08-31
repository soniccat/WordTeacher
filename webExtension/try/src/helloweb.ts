import * as dwlib from "./DOMWalker";
import * as messages from "./message"
import { CardSetBuilder } from './CardSetBuilder';
const jsdom = require("jsdom");

let cardSetBuilder = new CardSetBuilder("100 Words Every Middle Schooler Should Know", "https://www.vocabulary.com/lists/558097")
let dom = new JSDOM(messages.message3)

let dw = new dwlib.DOMWalker(new dwlib.DOMWalkerCursor(document.body))
dw
.goToNodeWithClass("entry learnable")
.splitByFunctionWithDOMWalker(
  dwlib.findNodeWithClassSplitter("entry learnable"),
  (dw: dwlib.DOMWalker) => {
    dw
    .goIn()
    .findNodeWithClass("word")
    .textContent((t: string) => { 
      console.log(t.trim()) 
      cardSetBuilder.startCard()
      cardSetBuilder.setCardTerm(t.trim().replace("\n", " ").replace("  ", " "))
    })
    .findNodeWithClass("definition")
    .textContent((t: string) => { 
      console.log(t.trim()) 
      cardSetBuilder.addCardDefinition(t.trim().replace("\n", " ").replace("  ", " "))
    })
    .findNodeWithClass("example")
    .textContent((t: string) => { 
      console.log(t.trim()) 
      cardSetBuilder.addCardExample(t.trim().replace("\n", " ").replace("  ", " "))
    })
  }
)

console.log(JSON.stringify(cardSetBuilder.json(), null, 4))

// let date = "2023-08-10T16:21:59.452Z"
// let jsonBuilder = new JSONBuilder()
// // jsonBuilder.startArray("updatedCardSets")
// // jsonBuilder.startArrayObject()
// jsonBuilder.set("name", "80 Most Common Phrasal Verbs")
// jsonBuilder.set("source", "https://www.grammarly.com/blog/common-phrasal-verbs/")
// jsonBuilder.set("id", "")
// jsonBuilder.set("creationId", uuidv4())
// jsonBuilder.set("creationDate", date)
// jsonBuilder.set("modificationDate", date)
// jsonBuilder.startArray("cards")
// 
// document.body.innerHTML = messages.message;
// 
// var defs = Array()
// var examples = Array()
// let dw = new dwlib.DOMWalker(new dwlib.DOMWalkerCursor(document.body))
// dw
// .findNodeWithClass("tool__example tool__correct")
// .goToFoundResult()
// .findNodeContainingText("80 common phrasal verbs")
// .findNodeWithClass("tool__number")
// .splitByFunctionWithDOMWalker(
//   dwlib.findNodeWithClassSplitter("tool__number"),
//   (dw: dwlib.DOMWalker) => {
//     dw.textContent((t: string) => { 
//       jsonBuilder.startArrayObject() 
//       jsonBuilder.set("id", "")
//       jsonBuilder.set("creationDate", date)
//       jsonBuilder.set("modificationDate", date)
//       jsonBuilder.set("term", dwlib.cutFirstWord(t).trim().replace("[x]", "sth"))
//       jsonBuilder.set("partOfSpeech", 12)
//       jsonBuilder.set("definitionTermSpans", [])
//       jsonBuilder.set("synonyms", [])
//       jsonBuilder.set("examples", [])
//       jsonBuilder.set("exampleTermSpans", [])
//       jsonBuilder.set("progress", {})
//       jsonBuilder.set("creationId", uuidv4())
//       jsonBuilder.set("needToUpdateDefinitionSpans", true)
//       jsonBuilder.set("needToUpdateExampleSpans", true)
//       console.log(dwlib.cutFirstWord(t).trim()) 
//       jsonBuilder.startArray("definitions")
//       defs = jsonBuilder.cursor as Array<string>
//       jsonBuilder.endArray()
//       jsonBuilder.startArray("examples")
//       examples = jsonBuilder.cursor as Array<string>
//       jsonBuilder.endArray()
//     })
//     .whileNotEnd(() => {
//       dw.nextSibling()
//         .findNodeWithNotEmptyText()
//         .textContent((t: string) => { 
//           var a = 0
//           if (t.indexOf("FAQs") != -1) {
//             throw "end"
//           }
//           defs.push(t.trim())
//           console.log("\t def " + t) }
//         )
//         .findNodeWithClass("tool__example-content")
//         .textContent((t: string) => { 
//           examples.push(t.trim())
//           console.log("\t ex " + t.trim()) 
//         })
//       }
//     ).call(() => { jsonBuilder.endObject() })
//   }
// )

// console.log(JSON.stringify(jsonBuilder.json, null, 4))

// document.body.innerHTML = message2;

// let dw = new DOMWalker(new DOMWalkerCursor(document.body))
// dw
// .goToNodeWithClass("pr dsense")
// .splitByFunctionWithDOMWalker(
//   findNodeWithClassSplitter("pr dsense"),
//   (dw: DOMWalker) => {
//     dw
//     .goToNodeWithClass("def-block ddef_block")
//     .splitByFunctionWithDOMWalker(
//       findNodeWithClassSplitter("def-block ddef_block"),
//       (dw: DOMWalker) => {
//         dw
//           .call(()=>{ console.log("----") })
//           .findNodeWithClass("def ddef_d db")
//           .goToFoundResult()
//           .textContent((t: string) => { console.log("def: " + t.trim()) })
//           .goOut()
//           .try((dw: DOMWalker) => {
//             dw
//               .goToNodeWithClass("examp dexamp")
//               .splitByFunctionWithDOMWalker(
//                 findNodeWithClassSplitter("examp dexamp"),
//                 (dw: DOMWalker) => {
//                   dw
//                     .textContent((t: string) => { console.log("ex: " + t.trim()) })
//                 }
//               )
//               .goOut()
//             }
//           )
//           .try((dw: DOMWalker) => {
//             dw
//               .goToNodeWithClass("eg dexamp hax")
//               .splitByFunctionWithDOMWalker(
//                 findNodeWithClassSplitter("eg dexamp hax"),
//                 (dw: DOMWalker) => {
//                   dw
//                     .textContent((t: string) => { console.log("extra ex: " + t.trim()) })
//                 }
//               )
//             }
//           )
//       }
//     )
//   }
// )
