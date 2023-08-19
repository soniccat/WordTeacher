var __extends = (this && this.__extends) || (function () {
    var extendStatics = function (d, b) {
        extendStatics = Object.setPrototypeOf ||
            ({ __proto__: [] } instanceof Array && function (d, b) { d.__proto__ = b; }) ||
            function (d, b) { for (var p in b) if (Object.prototype.hasOwnProperty.call(b, p)) d[p] = b[p]; };
        return extendStatics(d, b);
    };
    return function (d, b) {
        if (typeof b !== "function" && b !== null)
            throw new TypeError("Class extends value " + String(b) + " is not a constructor or null");
        extendStatics(d, b);
        function __() { this.constructor = d; }
        d.prototype = b === null ? Object.create(b) : (__.prototype = b.prototype, new __());
    };
})();
// JSON Builder with a cursor
// select(path like "obj.obj")
// startObject/endObject/set
// startArray/endArray/add,
var JSONBuilder = /** @class */ (function () {
    function JSONBuilder(json, cursor) {
        this.json = {};
        this.cursor = this.json;
        this.cursorStack = Array();
        if (json != null) {
            this.json = json;
        }
        if (cursor != null) {
            this.cursor = cursor;
        }
    }
    JSONBuilder.prototype.set = function (key, v) {
        this.cursor[key] = v;
        return this;
    };
    JSONBuilder.prototype.startObject = function (name) {
        this.cursorStack.push(this.cursor);
        var oldCursor = this.cursor;
        this.cursor = {};
        oldCursor[name] = this.cursor;
        return this;
    };
    JSONBuilder.prototype.startArrayObject = function () {
        this.cursorStack.push(this.cursor);
        var obj = {};
        this.push(obj);
        this.cursor = obj;
        return this;
    };
    JSONBuilder.prototype.endObject = function () {
        this.cursor = this.cursorStack.pop();
        return this;
    };
    JSONBuilder.prototype.startArray = function (name) {
        this.cursorStack.push(this.cursor);
        var oldCursor = this.cursor;
        this.cursor = [];
        oldCursor[name] = this.cursor;
        return this;
    };
    JSONBuilder.prototype.endArray = function () {
        this.endObject();
        return this;
    };
    JSONBuilder.prototype.push = function (v) {
        if (!Array.isArray(this.cursor)) {
            throw new JSONBuilderError("push failed as the cursor isn't array in ".concat(JSON.stringify(this.json)));
        }
        this.cursor.push(v);
        return this;
    };
    JSONBuilder.prototype.select = function (path) {
        var c = this.json;
        for (var i = 0; i < path.length; ++i) {
            c = c[path[i]];
            if (c == undefined) {
                throw new JSONBuilderError("select non existing path in in ".concat(JSON.stringify(this.json)));
            }
        }
        this.cursor = c;
        return this;
    };
    return JSONBuilder;
}());
export { JSONBuilder };
var JSONBuilderError = /** @class */ (function (_super) {
    __extends(JSONBuilderError, _super);
    function JSONBuilderError() {
        return _super !== null && _super.apply(this, arguments) || this;
    }
    return JSONBuilderError;
}(Error));
export { JSONBuilderError };
