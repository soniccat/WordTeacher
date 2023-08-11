var FindResult = /** @class */ (function () {
    function FindResult(n, chIndex, chIndexInOriginalNode) {
        if (chIndex === void 0) { chIndex = -1; }
        if (chIndexInOriginalNode === void 0) { chIndexInOriginalNode = -1; }
        this.node = n;
        this.childIndex = chIndex;
        this.childIndexInOriginalNode = chIndexInOriginalNode;
    }
    FindResult.prototype.textContent = function () {
        if (this.node instanceof Element) {
            return this.node.textContent;
        }
        else {
            return null;
        }
    };
    FindResult.prototype.childNodes = function () {
        var n = this.node;
        if (n instanceof Node) {
            return n.childNodes;
        }
        else {
            return null;
        }
    };
    return FindResult;
}());
//# sourceMappingURL=findresult.js.map