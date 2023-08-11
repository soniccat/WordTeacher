document.body.innerHTML = message;
var n1 = findNodeContainingClass(document.body, -1, "tool__example tool__correct");
if (n1 == null) {
    console.log("tool__example tool__correct not found");
}
var n = findNodeContainingText(n1.node, n1.childIndex, "80 common phrasal verbs");
if (n == null) {
    console.log("not element");
}
var n2 = findNodeContainingClass(n.node, n.childIndex, "tool__number");
if (n2 == null) {
    console.log("not element");
}
var nNode = n.node;
if (nNode instanceof Element) {
    var chNode = nNode.childNodes[n2.childIndexInOriginalNode];
    if (chNode instanceof Element) {
        console.log("found text " + chNode.textContent);
    }
}
function findNodeContainingClass(startNode, startIndex, className) {
    if (startIndex == -1 && startNode instanceof Element) {
        var v = startNode.attributes["class"];
        if (v instanceof Attr) {
            if (v.value == className) {
                return new FindResult(startNode);
            }
        }
    }
    if (startNode instanceof Element) {
        for (var index = startIndex; index < startNode.childNodes.length; index++) {
            var element = startNode.childNodes[index];
            var fr = findNodeContainingClass(element, -1, className);
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
function findNodeContainingText(startNode, startIndex, text) {
    if (startNode instanceof Element) {
        for (var index = startIndex; index < startNode.childNodes.length; index++) {
            var element = startNode.childNodes[index];
            var fr = findNodeContainingText(element, -1, text);
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