
document.body.innerHTML = message;

let jsonBuilder = new JSONBuilder()
jsonBuilder.startArray("updatedCardSets")
jsonBuilder.startArrayObject()
jsonBuilder.set("name", "80 Most Common Phrasal Verbs")
jsonBuilder.set("source", "https://www.grammarly.com/blog/common-phrasal-verbs/")
jsonBuilder.startArray("cards")


var defs = Array()
var examples = Array()
let dw = new DOMWalker(new DOMWalkerCursor(document.body))
dw
.findNodeWithClass("tool__example tool__correct")
.goToFoundResult()
.findNodeContainingText("80 common phrasal verbs")
.findNodeWithClass("tool__number")
.splitByFunctionWithDOMWalker(
  findNodeWithClassSplitter("tool__number"),
  (dw: DOMWalker) => {
    dw.textContent((t: string) => { 
      jsonBuilder.startArrayObject() 
      jsonBuilder.set("term", cutFirstWord(t).trim())
      console.log(cutFirstWord(t).trim()) 
      jsonBuilder.startArray("definitions")
      defs = jsonBuilder.cursor as Array<string>
      jsonBuilder.endArray()
      jsonBuilder.startArray("examples")
      examples = jsonBuilder.cursor as Array<string>
      jsonBuilder.endArray()
    })
    .whileNotEnd(() => {
      dw.nextSibling()
        .findNodeWithNotEmptyText()
        .textContent((t: string) => { 
          var a = 0
          if (t.indexOf("FAQs") != -1) {
            throw "end"
          }
          defs.push(t.trim())
          console.log("\t def " + t) }
        )
        .findNodeWithClass("tool__example-content")
        .textContent((t: string) => { 
          examples.push(t.trim())
          console.log("\t ex " + t.trim()) 
        })
      }
    ).call(() => { jsonBuilder.endObject() })
  }
)

console.log(JSON.stringify(jsonBuilder.json, null, 4))

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

/*

// select(path like "obj.obj")
// goIn/goOut
// startObject/endObject/set
// startArray/endArray/add,
jb = JSONBuilder()

*/
