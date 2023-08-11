document.body.innerHTML = message;
var n1 = findNodeContainingClass(document.body, -1, -1, "tool__example tool__correct");
if (n1 == null) {
    console.log("tool__example tool__correct not found");
}
var n = findNodeContainingText(n1.node, n1.childIndex + 1, -1, "80 common phrasal verbs");
if (n == null) {
    console.log("not element");
}
// let n2 = findNodeContainingClass(n1.node, n.childIndex + 1, "tool__number")
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
var frArray = splitByFunction(n1.node, n.childIndex, function (aStartNode, index) {
    return findNodeContainingClass(aStartNode, index, -1, "tool__number");
});
var ranges = Array();
for (var i = 0; i < frArray.length; ++i) {
    if (i == frArray.length - 1) {
        ranges.push([frArray[i].childIndexInOriginalNode, -1]);
    }
    else {
        ranges.push([frArray[i].childIndexInOriginalNode, frArray[i + 1].childIndexInOriginalNode]);
    }
}
var rr = ranges.map(function (range, i) {
    console.log(n1.node.childNodes[range[0]].textContent);
    return Array();
});
// console.log(ranges)
// for(let i=0; i<frArray.length; ++i) {
//   let n = n1.node.childNodes[frArray[i].childIndexInOriginalNode]
//   if (n instanceof Element) {
//     console.log("found text " + n.textContent)
//   }
// }
function splitByFunction(node, startIndex, f) {
    var result = Array();
    for (var index = startIndex; index < node.childNodes.length;) {
        var fr = f(node, index);
        if (fr != null) {
            result.push(fr);
            index = fr.childIndexInOriginalNode + 1;
        }
        else {
            ++index;
        }
    }
    return result;
}
function findNodeContainingClass(startNode, startIndex, endIndex, className) {
    if (startIndex == -1 && startNode instanceof Element) {
        var v = startNode.attributes["class"];
        if (v instanceof Attr) {
            if (v.value == className) {
                return new FindResult(startNode);
            }
        }
    }
    if (startNode instanceof Element) {
        for (var index = startIndex; index < startNode.childNodes.length && (endIndex == -1 || index < endIndex); index++) {
            var element = startNode.childNodes[index];
            var fr = findNodeContainingClass(element, -1, -1, className);
            if (fr != null) {
                if (fr.childIndex == -1) {
                    return new FindResult(startNode, index);
                }
                fr.childIndexInOriginalNode = index;
                return fr;
            }
        }
        ;
    }
    return null;
}
function findNodeContainingText(startNode, startIndex, endIndex, text) {
    if (startNode instanceof Element) {
        for (var index = startIndex; index < startNode.childNodes.length && (endIndex == -1 || index < endIndex); index++) {
            var element = startNode.childNodes[index];
            var fr = findNodeContainingText(element, -1, -1, text);
            if (fr != null) {
                if (fr.childIndex == -1) {
                    return new FindResult(startNode, index);
                }
                fr.childIndexInOriginalNode = index;
                return fr;
            }
        }
        ;
    }
    if (startIndex == -1 && startNode instanceof Element) {
        if (startNode.textContent.indexOf(text) != -1) {
            return new FindResult(startNode);
        }
    }
    return null;
}
//# sourceMappingURL=helloweb.js.map