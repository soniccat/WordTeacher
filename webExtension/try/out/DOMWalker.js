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
        this.internalDOMWalker = null;
        this.stateStack = Array();
        this.cursor = cursor;
    }
    DOMWalker.prototype.reset = function (cursor) {
        this.cursorStack = Array();
        this.cursor = cursor;
        this.lastFoundResult = null;
        this.internalDOMWalker = null;
    };
    DOMWalker.prototype.pushState = function () {
        this.stateStack.push(new DOMWalkerState(this.cursorStack, this.cursor, this.lastFoundResult));
        return this;
    };
    DOMWalker.prototype.popState = function () {
        var state = this.stateStack.pop();
        this.cursorStack = state.cursorStack;
        this.cursor = state.cursor;
        this.lastFoundResult = state.lastFoundResult;
        return this;
    };
    DOMWalker.prototype.goIn = function () {
        var c = this.cursor;
        this.cursorStack.push(this.cursor);
        this.cursor = c.inCursor();
        return this;
    };
    DOMWalker.prototype.goToFoundResult = function () {
        if (this.lastFoundResult == null) {
            throw new DOMWalkerError("goIn() failed as lastFoundResult is null");
        }
        this.cursorStack.push(this.cursor);
        this.cursor = new DOMWalkerCursor(this.lastFoundResult.node, this.lastFoundResult.childIndex);
        return this;
    };
    DOMWalker.prototype.goInFoundResult = function () {
        if (this.lastFoundResult == null) {
            throw new DOMWalkerError("goIn() failed as lastFoundResult is null");
        }
        this.cursorStack.push(this.cursor);
        this.cursor = new DOMWalkerCursor(this.lastFoundResult.childNode(), 0);
        return this;
    };
    DOMWalker.prototype.goOut = function () {
        this.cursor = this.cursorStack.pop();
        return this;
    };
    DOMWalker.prototype.nextSibling = function () {
        if (this.cursor.childIndex + 1 == this.cursor.node.childNodes.length) {
            throw new DOMWalkerError("nextSibling is called at the last node");
        }
        this.cursor.childIndex += 1;
        return this;
    };
    DOMWalker.prototype.findNodeWithClass = function (name) {
        var fr = findNodeWithClass(this.cursor.node, this.cursor.range(), name);
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
        var fr = findNodeContainingText(this.cursor.node, this.cursor.range(), text);
        if (fr != null) {
            this.lastFoundResult = fr;
            this.cursor.childIndex = fr.childIndexInOriginalNode;
        }
        else {
            throw new DOMWalkerError("findNodeContainingText(" + text + ") failed at " + this.cursor.toString());
        }
        return this;
    };
    DOMWalker.prototype.findNodeWithNotEmptyText = function () {
        var fr = findNodeWithNotEmptyText(this.cursor.node, this.cursor.range());
        if (fr != null) {
            this.lastFoundResult = fr;
            this.cursor.childIndex = fr.childIndexInOriginalNode;
        }
        else {
            throw new DOMWalkerError("findNodeWithNotEmptyText() failed at " + this.cursor.toString());
        }
        return this;
    };
    DOMWalker.prototype.splitByFunctionWithDOMWalker = function (f, itemCallback) {
        var internalDOMWalker = this.requireInternalDOMWalker(this.cursor);
        var foundResults = splitByFunction(this.cursor.node, this.cursor.range(), f);
        for (var index = 0; index < foundResults.length; index++) {
            var fr = foundResults[index];
            if (index == foundResults.length - 1) {
                internalDOMWalker.reset(new DOMWalkerCursor(this.cursor.node, fr.childIndexInOriginalNode, -1));
            }
            else {
                var nextFr = foundResults[index + 1];
                internalDOMWalker.reset(new DOMWalkerCursor(this.cursor.node, fr.childIndexInOriginalNode, nextFr.childIndexInOriginalNode));
            }
            itemCallback(internalDOMWalker);
        }
    };
    DOMWalker.prototype.textContent = function (f) {
        f(this.cursor.childNode().textContent);
        return this;
    };
    DOMWalker.prototype.call = function (f) {
        f();
        return this;
    };
    DOMWalker.prototype.whileNotEnd = function (f) {
        var p = this.cursor.childIndex;
        try {
            while (!this.cursor.atEnd()) {
                f();
                if (p == this.cursor.childIndex) {
                    break;
                }
            }
        }
        catch (e) {
        }
    };
    DOMWalker.prototype.requireInternalDOMWalker = function (cursor) {
        if (this.internalDOMWalker == null) {
            this.internalDOMWalker = new DOMWalker(cursor);
        }
        return this.internalDOMWalker;
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
    DOMWalkerCursor.prototype.range = function () {
        return new DOMWalkerRange(this.childIndex, this.maxIndex);
    };
    DOMWalkerCursor.prototype.atEnd = function () {
        return this.maxIndex != -1 && this.childIndex >= this.maxIndex || this.childIndex >= this.node.childNodes.length;
    };
    return DOMWalkerCursor;
}());
var DOMWalkerState = /** @class */ (function () {
    function DOMWalkerState(cursorStack, cursor, lastFoundResult) {
        this.cursorStack = Array();
        this.lastFoundResult = null;
        this.cursorStack = Array.apply(void 0, cursorStack);
        this.cursor = new DOMWalkerCursor(cursor.node, cursor.childIndex, cursor.maxIndex);
        this.lastFoundResult = lastFoundResult;
    }
    return DOMWalkerState;
}());
// [startIndex..endIndex)
// -1 for startIndex means we include the node itself
// -1 for endIndex means infinity 
var DOMWalkerRange = /** @class */ (function () {
    function DOMWalkerRange(start, end) {
        this.start = start;
        this.end = end;
    }
    DOMWalkerRange.prototype.isInRange = function (v) {
        return v >= this.start && (this.end == -1 || v < this.end);
    };
    DOMWalkerRange.prototype.rangeWithStart = function (start) {
        return new DOMWalkerRange(start, this.end);
    };
    DOMWalkerRange.prototype.rangeWithEnd = function (end) {
        return new DOMWalkerRange(this.start, end);
    };
    return DOMWalkerRange;
}());
var NodeRange = new DOMWalkerRange(-1, -1);
var DOMWalkerError = /** @class */ (function (_super) {
    __extends(DOMWalkerError, _super);
    function DOMWalkerError() {
        return _super !== null && _super.apply(this, arguments) || this;
    }
    return DOMWalkerError;
}(Error));
//// functions
function splitByFunction(node, range, f) {
    var result = Array();
    for (var index = range.start; index < node.childNodes.length && range.isInRange(index);) {
        var fr = f(node, new DOMWalkerRange(index, range.end));
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
function findNodeWithClassSplitter(className) {
    return function (aStartNode, range) {
        return findNodeWithClass(aStartNode, range, className);
    };
}
function findNodeWithClass(startNode, range, className) {
    if (range.start == -1 && startNode instanceof Element) {
        var v = startNode.attributes["class"];
        if (v instanceof Attr) {
            if (v.value.trim() == className) {
                return new FoundResult(startNode);
            }
        }
    }
    if (startNode instanceof Node) {
        for (var index = range.start; index < startNode.childNodes.length && range.isInRange(index); index++) {
            var element = startNode.childNodes[index];
            var fr = findNodeWithClass(element, NodeRange, className);
            if (fr != null) {
                if (fr.childIndex == -1) {
                    return new FoundResult(startNode, index, index);
                }
                fr.childIndexInOriginalNode = index;
                return fr;
            }
        }
        ;
    }
    return null;
}
function findNodeContainingText(startNode, range, text) {
    return findNodeContainingWithTextChecker(startNode, range, function (t) { return t.indexOf(text) != -1; });
}
function findNodeWithNotEmptyText(startNode, range) {
    return findNodeContainingWithTextChecker(startNode, range, function (t) { return t.trim().length > 0; });
}
function findNodeContainingWithTextChecker(startNode, range, textChecker) {
    if (startNode instanceof Node) {
        for (var index = range.start; index < startNode.childNodes.length && range.isInRange(index); index++) {
            var element = startNode.childNodes[index];
            var fr = findNodeContainingWithTextChecker(element, NodeRange, textChecker);
            if (fr != null) {
                if (fr.childIndex == -1) {
                    return new FoundResult(startNode, index, index);
                }
                fr.childIndexInOriginalNode = index;
                return fr;
            }
        }
        ;
    }
    if (range.start == -1 && startNode instanceof Node) {
        if (textChecker(startNode.textContent)) {
            return new FoundResult(startNode);
        }
    }
    return null;
}
function cutFirstWord(str) {
    var i = str.indexOf(' ');
    if (i != -1) {
        return str.substring(i + 1);
    }
    return str;
}
//# sourceMappingURL=DOMWalker.js.map