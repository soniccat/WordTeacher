// document.body.innerHTML = message;
// let dw = new DOMWalker(new DOMWalkerCursor(document.body))
// dw
// .findNodeWithClass("tool__example tool__correct")
// .goToFoundResult()
// .findNodeContainingText("80 common phrasal verbs")
// .findNodeWithClass("tool__number")
// .splitByFunctionWithDOMWalker(
//   findNodeWithClassSplitter("tool__number"),
//   (dw: DOMWalker) => {
//     dw.textContent((t: string) => { console.log(cutFirstWord(t)) })
//     dw.whileNotEnd(() => {
//       dw.nextSibling()
//         .findNodeWithNotEmptyText()
//         .textContent((t: string) => { 
//           var a = 0
//           if (t.indexOf("FAQs") != -1) {
//             throw "end"
//           }
//           console.log("\t def " + t) }
//         )
//         .findNodeWithClass("tool__example-content")
//         .textContent((t: string) => { 
//           console.log("\t ex " + t.trim()) 
//         })
//       }
//     )
//   }
// )
document.body.innerHTML = message2;
var dw = new DOMWalker(new DOMWalkerCursor(document.body));
dw
    .findNodeWithClass("pos-body")
    .goInFoundResult()
    .splitByFunctionWithDOMWalker(findNodeWithClassSplitter("pr dsense"), function (dw) {
    dw
        .findNodeWithClass("sense-body dsense_b")
        .goInFoundResult()
        .splitByFunctionWithDOMWalker(findNodeWithClassSplitter("def-block ddef_block"), function (dw) {
        dw
            .call(function () { console.log("----"); })
            .findNodeWithClass("def ddef_d db")
            .goToFoundResult()
            .textContent(function (t) { console.log(t); });
    });
    // .findNodeWithClass("def ddef_d db")
    // .goToFoundResult()
    // .textContent((t: string) => { console.log(t) })
    // dw.whileNotEnd(() => {
    //   dw.nextSibling()
    //     .findNodeWithNotEmptyText()
    //     .textContent((t: string) => { 
    //       var a = 0
    //       if (t.indexOf("FAQs") != -1) {
    //         throw "end"
    //       }
    //       console.log("\t def " + t) }
    //     )
    //     .findNodeWithClass("tool__example-content")
    //     .textContent((t: string) => { 
    //       console.log("\t ex " + t.trim()) 
    //     })
    //   }
    // )
});
/*

// select(path like "obj.obj")
// goIn/goOut
// startObject/endObject/set
// startArray/endArray/add,
jb = JSONBuilder()

*/
//# sourceMappingURL=helloweb.js.map