import { v4 as uuidv4 } from 'uuid';
import { JSONBuilder } from "./JSONBuilder";
document.body.innerHTML = message;
var jsonBuilder = new JSONBuilder();
// jsonBuilder.startArray("updatedCardSets")
// jsonBuilder.startArrayObject()
jsonBuilder.set("name", "80 Most Common Phrasal Verbs");
jsonBuilder.set("source", "https://www.grammarly.com/blog/common-phrasal-verbs/");
jsonBuilder.startArray("cards");
var defs = Array();
var examples = Array();
var dw = new DOMWalker(new DOMWalkerCursor(document.body));
dw
    .findNodeWithClass("tool__example tool__correct")
    .goToFoundResult()
    .findNodeContainingText("80 common phrasal verbs")
    .findNodeWithClass("tool__number")
    .splitByFunctionWithDOMWalker(findNodeWithClassSplitter("tool__number"), function (dw) {
    dw.textContent(function (t) {
        jsonBuilder.startArrayObject();
        jsonBuilder.set("id", "");
        jsonBuilder.set("creationDate", "2023-08-10T16:21:59.452Z");
        jsonBuilder.set("modificationDate", "2023-08-10T16:21:59.452Z");
        jsonBuilder.set("term", cutFirstWord(t).trim().replace("[x]", "sth"));
        jsonBuilder.set("partOfSpeech", 12);
        jsonBuilder.set("definitionTermSpans", []);
        jsonBuilder.set("synonyms", []);
        jsonBuilder.set("examples", []);
        jsonBuilder.set("exampleTermSpans", []);
        jsonBuilder.set("progress", {});
        jsonBuilder.set("creationId", uuidv4());
        jsonBuilder.set("needToUpdateDefinitionSpans", true);
        jsonBuilder.set("needToUpdateExampleSpans", true);
        console.log(cutFirstWord(t).trim());
        jsonBuilder.startArray("definitions");
        defs = jsonBuilder.cursor;
        jsonBuilder.endArray();
        jsonBuilder.startArray("examples");
        examples = jsonBuilder.cursor;
        jsonBuilder.endArray();
    })
        .whileNotEnd(function () {
        dw.nextSibling()
            .findNodeWithNotEmptyText()
            .textContent(function (t) {
            var a = 0;
            if (t.indexOf("FAQs") != -1) {
                throw "end";
            }
            defs.push(t.trim());
            console.log("\t def " + t);
        })
            .findNodeWithClass("tool__example-content")
            .textContent(function (t) {
            examples.push(t.trim());
            console.log("\t ex " + t.trim());
        });
    }).call(function () { jsonBuilder.endObject(); });
});
console.log(JSON.stringify(jsonBuilder.json, null, 4));
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
