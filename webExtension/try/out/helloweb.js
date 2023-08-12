document.body.innerHTML = message;
var dw = new DOMWalker(new DOMWalkerCursor(document.body));
dw
    .findNodeWithClass("tool__example tool__correct")
    .goInFoundResult()
    .findNodeContainingText("80 common phrasal verbs")
    .findNodeWithClass("tool__number")
    //.textContent((t: string) => { console.log(t) })
    .splitByFunctionWithDOMWalker(findNodeWithClassSplitter("tool__number"), function (dw) {
    dw.textContent(function (t) { console.log(cutFirstWord(t)); });
    dw.whileNotEnd(function () {
        dw.nextSibling()
            .findNodeWithNotEmptyText()
            .textContent(function (t) { console.log("\t def " + t); })
            .findNodeWithClass("tool__example-content")
            .textContent(function (t) { console.log("\t ex " + t.trim()); });
    });
});
// let n1 = findNodeWithClass(document.body, -1, -1, "tool__example tool__correct")
// if (n1 == null) {
//   console.log("tool__example tool__correct not found")
// }
// let n = findNodeContainingText(n1.node, n1.childIndex + 1, -1, "80 common phrasal verbs")
// if (n == null) {
//   console.log("not element")
// }
// let n2 = findNodeWithClass(n1.node, n.childIndex + 1, "tool__number")
// if (n2 == null) {
//   console.log("not element")
// }
// let nNode = n.node
// if (nNode instanceof Element) {
//   let chNode = nNode.childNodes[n2.childIndexInOriginalNode]
//   if (chNode instanceof Element) {
//     console.log("found text " + chNode.textContent)
//   }
// }
// let frArray = splitByFunction(n1.node, n.childIndex, function(aStartNode: Node, index: number): FindResult | null {
//   return findNodeWithClass(aStartNode, index,  -1, "tool__number")
// })
// let ranges = Array<[number, number]>()
// for(let i=0; i<frArray.length; ++i) {
//   if (i == frArray.length-1) {
//     ranges.push([frArray[i].childIndexInOriginalNode, -1])
//   } else {
//     ranges.push([frArray[i].childIndexInOriginalNode, frArray[i+1].childIndexInOriginalNode])
//   }
// }
// let rr = ranges.map(function(range: [number, number], i: number):Array<Node> {
//   console.log(n1.node.childNodes[range[0]].textContent)
//   return Array()
// })
// console.log(ranges)
// for(let i=0; i<frArray.length; ++i) {
//   let n = n1.node.childNodes[frArray[i].childIndexInOriginalNode]
//   if (n instanceof Element) {
//     console.log("found text " + n.textContent)
//   }
// }
/*

// select(path like "obj.obj")
// goIn/goOut
// startObject/endObject/set
// startArray/endArray/add,
jb = JSONBuilder()

*/
//# sourceMappingURL=helloweb.js.map