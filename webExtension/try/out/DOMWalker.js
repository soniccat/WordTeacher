/*
// cursor - a child position of a parent node, we move cursor down the siblings
// we keep a stack of cursor positions, so a cursor position is restored after going deeper and coming back
dw = DOMWalker()
  .findNodeWithClass("tool__correct") // moves cursor to the node with a class
  .goIn() // go deeper, starts searching through cursor's childs (goOut to go back)
  .findNodeContainingText("80 common phrasal verbs") // moves cursor to the node containing text
  .textContent { text -> jb.set("title", text) } // extract cursor's text and set it to json title property
  .call { jb.startArray("cards") }
  .splitByFunction( // splitByFunctionWithDOMWalker
    dw.findNodeWithClassSplitter("tool__number"),
    { node (cursor's parent), index (child index), length (in sibling count) ->
      cardDw = DOMWalker(node, index, lenght)
        .findNodeWithClass("tool__number")
        .textContent { text -> jb.set("title", text.trimmed()) }
        .nextSibling() // go to the next sibling
        .textContent { text -> jb.set("definition", text) }
    }
  )
  .call { jb.endArray() }
*/
var __extends = (this && this.__extends) || (function () {
    var extendStatics = Object.setPrototypeOf ||
        ({ __proto__: [] } instanceof Array && function (d, b) { d.__proto__ = b; }) ||
        function (d, b) { for (var p in b) if (b.hasOwnProperty(p)) d[p] = b[p]; };
    return function (d, b) {
        extendStatics(d, b);
        function __() { this.constructor = d; }
        d.prototype = b === null ? Object.create(b) : (__.prototype = b.prototype, new __());
    };
})();
var DOMWalker = /** @class */ (function () {
    function DOMWalker(cursor) {
        this.cursorStack = Array();
        this.lastFoundResult = null;
        this.cursor = cursor;
    }
    DOMWalker.prototype.goIn = function () {
        var c = this.cursor;
        this.cursorStack.push(this.cursor);
        this.cursor = c.inCursor();
        return this;
    };
    DOMWalker.prototype.goInFoundResult = function () {
        if (this.lastFoundResult == null) {
            throw new DOMWalkerError("goIn() failed as lastFoundResult is null");
        }
        this.cursorStack.push(this.cursor);
        this.cursor = new DOMWalkerCursor(this.lastFoundResult.node, this.lastFoundResult.childIndex);
        return this;
    };
    DOMWalker.prototype.goOut = function () {
        this.cursor = this.cursorStack.pop();
        return this;
    };
    DOMWalker.prototype.findNodeWithClass = function (name) {
        var fr = findNodeWithClass(this.cursor.node, this.cursor.childIndex, this.cursor.maxIndex, name);
        if (fr != null) {
            this.lastFoundResult = fr;
            this.cursor.childIndex = fr.childIndexInOriginalNode;
        }
        else {
            throw new DOMWalkerError("findNodeWithClass(" + name + ") failed at " + this.cursor.toString());
        }
        return this;
    };
    DOMWalker.prototype.findNodeContainingText = function (text) {
        var fr = findNodeContainingText(this.cursor.node, this.cursor.childIndex, this.cursor.maxIndex, text);
        if (fr != null) {
            this.lastFoundResult = fr;
            this.cursor.childIndex = fr.childIndexInOriginalNode;
        }
        else {
            throw new DOMWalkerError("findNodeContainingText(" + text + ") failed at " + this.cursor.toString());
        }
        return this;
    };
    DOMWalker.prototype.textContent = function (f) {
        f(this.cursor.childNode().textContent);
        return this;
    };
    DOMWalker.prototype.call = function (f) {
        f();
        return this;
    };
    return DOMWalker;
}());
var DOMWalkerCursor = /** @class */ (function () {
    function DOMWalkerCursor(node, childIndex, maxIndex) {
        this.childIndex = -1;
        this.maxIndex = -1;
        this.node = node;
        if (childIndex != null) {
            this.childIndex = childIndex;
        }
        if (maxIndex != null) {
            this.maxIndex = maxIndex;
        }
    }
    DOMWalkerCursor.prototype.childNode = function () {
        if (this.childIndex != -1) {
            return this.node.childNodes[this.childIndex];
        }
        else {
            return null;
        }
    };
    DOMWalkerCursor.prototype.inCursor = function () {
        return new DOMWalkerCursor(this.node.childNodes[this.childIndex], 0);
    };
    DOMWalkerCursor.prototype.toString = function () {
        return "node: " + this.node.textContent + "\nchildIndex: " + this.childIndex + "\nmaxIndex: " + this.maxIndex;
    };
    return DOMWalkerCursor;
}());
var DOMWalkerError = /** @class */ (function (_super) {
    __extends(DOMWalkerError, _super);
    function DOMWalkerError() {
        return _super !== null && _super.apply(this, arguments) || this;
    }
    return DOMWalkerError;
}(Error));
//// functions
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
function findNodeWithClass(startNode, startIndex, endIndex, className) {
    if (startIndex == -1 && startNode instanceof Element) {
        var v = startNode.attributes["class"];
        if (v instanceof Attr) {
            if (v.value == className) {
                return new FoundResult(startNode);
            }
        }
    }
    if (startNode instanceof Node) {
        for (var index = startIndex; index < startNode.childNodes.length && (endIndex == -1 || index < endIndex); index++) {
            var element = startNode.childNodes[index];
            var fr = findNodeWithClass(element, -1, -1, className);
            if (fr != null) {
                if (fr.childIndex == -1) {
                    return new FoundResult(startNode, index);
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
    if (startNode instanceof Node) {
        for (var index = startIndex; index < startNode.childNodes.length && (endIndex == -1 || index < endIndex); index++) {
            var element = startNode.childNodes[index];
            var fr = findNodeContainingText(element, -1, -1, text);
            if (fr != null) {
                if (fr.childIndex == -1) {
                    return new FoundResult(startNode, index);
                }
                fr.childIndexInOriginalNode = index;
                return fr;
            }
        }
        ;
    }
    if (startIndex == -1 && startNode instanceof Node) {
        if (startNode.textContent.indexOf(text) != -1) {
            return new FoundResult(startNode);
        }
    }
    return null;
}
//# sourceMappingURL=DOMWalker.js.map