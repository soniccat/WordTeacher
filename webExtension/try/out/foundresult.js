var FoundResult = /** @class */ (function () {
    function FoundResult(n, chIndex, chIndexInOriginalNode) {
        if (chIndex === void 0) { chIndex = -1; }
        if (chIndexInOriginalNode === void 0) { chIndexInOriginalNode = -1; }
        this.node = n;
        this.childIndex = chIndex;
        this.childIndexInOriginalNode = chIndexInOriginalNode;
    }
    FoundResult.prototype.textContent = function () {
        if (this.node instanceof Element) {
            return this.node.textContent;
        }
        else {
            return null;
        }
    };
    FoundResult.prototype.childNodes = function () {
        var n = this.node;
        if (n instanceof Node) {
            return n.childNodes;
        }
        else {
            return null;
        }
    };
    return FoundResult;
}());
//# sourceMappingURL=foundresult.js.map